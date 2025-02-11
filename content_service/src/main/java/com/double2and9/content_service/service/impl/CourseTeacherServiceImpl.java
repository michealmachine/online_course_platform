package com.double2and9.content_service.service.impl;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.client.MediaFeignClient;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.CourseBaseDTO;
import com.double2and9.content_service.dto.CourseTeacherDTO;
import com.double2and9.content_service.dto.SaveCourseTeacherDTO;
import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.CourseTeacher;
import com.double2and9.content_service.repository.CourseBaseRepository;
import com.double2and9.content_service.repository.CourseTeacherRepository;
import com.double2and9.content_service.service.CourseTeacherService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import java.time.LocalDateTime;

@Slf4j
@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    private final CourseTeacherRepository courseTeacherRepository;
    private final CourseBaseRepository courseBaseRepository;
    private final ModelMapper modelMapper;
    private final MediaFeignClient mediaFeignClient;

    public CourseTeacherServiceImpl(CourseTeacherRepository courseTeacherRepository,
            CourseBaseRepository courseBaseRepository,
            ModelMapper modelMapper,
            MediaFeignClient mediaFeignClient) {
        this.courseTeacherRepository = courseTeacherRepository;
        this.courseBaseRepository = courseBaseRepository;
        this.modelMapper = modelMapper;
        this.mediaFeignClient = mediaFeignClient;
    }

    @Override
    public List<CourseTeacherDTO> listByCourseId(Long courseId) {
        // 修改为使用分页查询，但获取所有记录
        Page<CourseTeacher> teacherPage = courseTeacherRepository.findByCourseId(
                courseId,
                PageRequest.of(0, Integer.MAX_VALUE) // 获取所有记录
        );

        return teacherPage.getContent().stream()
                .map(teacher -> {
                    CourseTeacherDTO dto = modelMapper.map(teacher, CourseTeacherDTO.class);
                    dto.setCourseIds(teacher.getCourses().stream()
                            .map(CourseBase::getId)
                            .collect(Collectors.toSet()));
                    return dto;
                })
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveCourseTeacher(SaveCourseTeacherDTO teacherDTO) {
        // 获取或创建教师
        CourseTeacher teacher;
        if (teacherDTO.getId() != null) {
            teacher = courseTeacherRepository.findById(teacherDTO.getId())
                    .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));
        } else {
            teacher = new CourseTeacher();
            teacher.setCreateTime(LocalDateTime.now());
            teacher.setCourses(new HashSet<>()); // 初始化空的课程集合
        }

        // 设置基本信息
        modelMapper.map(teacherDTO, teacher);
        teacher.setUpdateTime(LocalDateTime.now());
        teacher.setOrganizationId(teacherDTO.getOrganizationId());

        // 保存教师信息
        courseTeacherRepository.save(teacher);
        log.info("保存教师信息成功，教师ID：{}", teacher.getId());
    }

    @Transactional
    @Override
    public void deleteCourseTeacher(Long courseId, Long teacherId) {
        CourseTeacher teacher = courseTeacherRepository.findById(teacherId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));

        // 获取要解除关联的课程
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        // 验证课程是否与教师关联
        if (!teacher.getCourses().contains(courseBase)) {
            throw new ContentException(ContentErrorCode.TEACHER_COURSE_NOT_MATCH);
        }

        // 解除课程关联
        teacher.getCourses().remove(courseBase);

        // 如果教师不再关联任何课程，则删除教师
        if (teacher.getCourses().isEmpty()) {
            courseTeacherRepository.delete(teacher);
            log.info("教师已删除，教师ID：{}", teacherId);
        } else {
            courseTeacherRepository.save(teacher);
            log.info("解除教师与课程的关联，教师ID：{}，课程ID：{}", teacherId, courseId);
        }
    }

    @Override
    public List<CourseTeacherDTO> listByOrganizationId(Long organizationId) {
        // 修改为使用分页查询，但获取所有记录
        Page<CourseTeacher> teacherPage = courseTeacherRepository.findByOrganizationId(
                organizationId,
                PageRequest.of(0, Integer.MAX_VALUE) // 获取所有记录
        );

        return teacherPage.getContent().stream()
                .map(teacher -> modelMapper.map(teacher, CourseTeacherDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public List<CourseBaseDTO> listCoursesByTeacherId(Long teacherId) {
        // 使用分页查询，但获取所有记录
        Page<CourseBase> page = courseBaseRepository.findCoursesByTeacherId(
                teacherId,
                PageRequest.of(0, Integer.MAX_VALUE) // 获取所有记录
        );

        return page.getContent().stream()
                .map(course -> modelMapper.map(course, CourseBaseDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional(readOnly = true)
    public CourseTeacherDTO getTeacherDetail(Long organizationId, Long teacherId) {
        CourseTeacher teacher = courseTeacherRepository.findById(teacherId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));

        if (!teacher.getOrganizationId().equals(organizationId)) {
            throw new ContentException(ContentErrorCode.COURSE_ORG_NOT_MATCH);
        }

        CourseTeacherDTO dto = modelMapper.map(teacher, CourseTeacherDTO.class);
        dto.setCourseIds(teacher.getCourses().stream()
                .map(CourseBase::getId)
                .collect(Collectors.toSet()));
        return dto;
    }

    @Override
    @Transactional
    public String uploadTeacherAvatarTemp(Long teacherId, MultipartFile file) {
        // 1. 验证教师存在
        CourseTeacher teacher = courseTeacherRepository.findById(teacherId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));

        try {
            // 2. 上传到临时存储
            CommonResponse<String> tempResponse = mediaFeignClient.uploadImageTemp(file);
            if (!tempResponse.isSuccess()) {
                throw new ContentException(ContentErrorCode.UPLOAD_LOGO_FAILED, tempResponse.getMessage());
            }

            log.info("教师头像上传到临时存储成功，教师ID：{}，临时key：{}", teacherId, tempResponse.getData());
            return tempResponse.getData();
        } catch (ContentException e) {
            throw e;
        } catch (Exception e) {
            log.error("上传教师头像到临时存储失败：", e);
            throw new ContentException(ContentErrorCode.UPLOAD_LOGO_FAILED);
        }
    }

    @Override
    @Transactional
    public void confirmTeacherAvatar(Long teacherId, String tempKey) {
        // 1. 获取教师信息
        CourseTeacher teacher = courseTeacherRepository.findById(teacherId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));

        try {
            // 2. 保存到永久存储
            Map<String, String> params = new HashMap<>();
            params.put("tempKey", tempKey);
            CommonResponse<MediaFileDTO> saveResponse = mediaFeignClient.saveTempFile(params);

            if (!saveResponse.isSuccess()) {
                throw new ContentException(ContentErrorCode.UPLOAD_LOGO_FAILED, saveResponse.getMessage());
            }

            // 3. 更新教师头像URL
            MediaFileDTO mediaFileDTO = saveResponse.getData();
            teacher.setAvatar(mediaFileDTO.getUrl());
            courseTeacherRepository.save(teacher);

            log.info("教师头像确认保存成功，教师ID：{}，文件ID：{}", teacherId, mediaFileDTO.getMediaFileId());
        } catch (ContentException e) {
            throw e;
        } catch (Exception e) {
            log.error("确认保存教师头像失败：", e);
            throw new ContentException(ContentErrorCode.UPLOAD_LOGO_FAILED);
        }
    }

    @Override
    @Transactional
    public void deleteTeacherAvatar(Long teacherId) {
        // 1. 获取教师信息
        CourseTeacher teacher = courseTeacherRepository.findById(teacherId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));

        String avatarUrl = teacher.getAvatar();
        if (avatarUrl == null || avatarUrl.isEmpty()) {
            return; // 没有头像，直接返回
        }

        try {
            // 2. 调用媒体服务删除文件
            CommonResponse<?> response = mediaFeignClient.deleteMediaFile(avatarUrl);
            if (!response.isSuccess()) {
                throw new ContentException(ContentErrorCode.DELETE_LOGO_FAILED, response.getMessage());
            }

            // 3. 清除教师头像URL
            teacher.setAvatar(null);
            courseTeacherRepository.save(teacher);

            log.info("教师头像删除成功，教师ID：{}", teacherId);
        } catch (ContentException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除教师头像失败：", e);
            throw new ContentException(ContentErrorCode.DELETE_LOGO_FAILED);
        }
    }

    @Override
    @Transactional
    public void associateTeacherToCourse(Long organizationId, Long courseId, Long teacherId) {
        // 获取教师信息
        CourseTeacher teacher = courseTeacherRepository.findByOrganizationIdAndId(organizationId, teacherId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));

        // 获取课程信息
        CourseBase course = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        // 验证机构ID匹配
        if (!teacher.getOrganizationId().equals(organizationId) ||
                !course.getOrganizationId().equals(organizationId)) {
            throw new ContentException(ContentErrorCode.COURSE_ORG_NOT_MATCH);
        }

        // 添加课程关联
        if (teacher.getCourses() == null) {
            teacher.setCourses(new HashSet<>());
        }
        teacher.getCourses().add(course);
        teacher.setUpdateTime(LocalDateTime.now());

        courseTeacherRepository.save(teacher);
        log.info("教师关联课程成功，教师ID：{}，课程ID：{}", teacherId, courseId);
    }

    @Override
    @Transactional
    public void dissociateTeacherFromCourse(Long organizationId, Long courseId, Long teacherId) {
        // 获取教师信息
        CourseTeacher teacher = courseTeacherRepository.findByOrganizationIdAndId(organizationId, teacherId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));

        // 获取课程信息
        CourseBase course = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        // 验证机构ID匹配
        if (!teacher.getOrganizationId().equals(organizationId) ||
                !course.getOrganizationId().equals(organizationId)) {
            throw new ContentException(ContentErrorCode.COURSE_ORG_NOT_MATCH);
        }

        // 移除课程关联
        if (teacher.getCourses() != null) {
            teacher.getCourses().remove(course);
            teacher.setUpdateTime(LocalDateTime.now());
            courseTeacherRepository.save(teacher);
        }

        log.info("解除教师与课程的关联成功，教师ID：{}，课程ID：{}", teacherId, courseId);
    }

    @Override
    @Transactional
    public Long saveTeacher(SaveCourseTeacherDTO teacherDTO) {
        // 获取或创建教师
        CourseTeacher teacher;
        if (teacherDTO.getId() != null) {
            teacher = courseTeacherRepository.findByOrganizationIdAndId(
                    teacherDTO.getOrganizationId(), teacherDTO.getId())
                    .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));
        } else {
            teacher = new CourseTeacher();
            teacher.setCreateTime(LocalDateTime.now());
            teacher.setCourses(new HashSet<>()); // 初始化空的课程集合
        }

        // 设置基本信息
        modelMapper.map(teacherDTO, teacher);
        teacher.setUpdateTime(LocalDateTime.now());
        teacher.setOrganizationId(teacherDTO.getOrganizationId());

        // 保存教师信息
        CourseTeacher savedTeacher = courseTeacherRepository.save(teacher);
        log.info("保存教师信息成功，教师ID：{}", savedTeacher.getId());

        return savedTeacher.getId();
    }

    @Override
    @Transactional
    public void deleteTeacher(Long organizationId, Long teacherId) {
        CourseTeacher teacher = courseTeacherRepository.findByOrganizationIdAndId(organizationId, teacherId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));

        // 删除教师头像
        if (StringUtils.hasText(teacher.getAvatar())) {
            try {
                deleteTeacherAvatar(teacherId);
            } catch (Exception e) {
                log.error("删除教师头像失败：", e);
                // 继续删除教师，不影响主流程
            }
        }

        // 删除教师
        courseTeacherRepository.delete(teacher);
        log.info("删除教师成功，教师ID：{}", teacherId);
    }

    @Override
    public PageResult<CourseTeacherDTO> listByOrganizationId(Long organizationId, PageParams pageParams) {
        Page<CourseTeacher> page = courseTeacherRepository.findByOrganizationId(
                organizationId,
                PageRequest.of(pageParams.getPageNo().intValue() - 1, pageParams.getPageSize().intValue()));

        List<CourseTeacherDTO> items = page.getContent().stream()
                .map(teacher -> modelMapper.map(teacher, CourseTeacherDTO.class))
                .collect(Collectors.toList());

        return new PageResult<>(items, page.getTotalElements(), pageParams.getPageNo(), pageParams.getPageSize());
    }

    @Override
    public PageResult<CourseTeacherDTO> listByCourseId(Long courseId, PageParams pageParams) {
        Page<CourseTeacher> page = courseTeacherRepository.findByCourseId(
                courseId,
                PageRequest.of(pageParams.getPageNo().intValue() - 1, pageParams.getPageSize().intValue()));

        List<CourseTeacherDTO> items = page.getContent().stream()
                .map(teacher -> modelMapper.map(teacher, CourseTeacherDTO.class))
                .collect(Collectors.toList());

        return new PageResult<>(items, page.getTotalElements(), pageParams.getPageNo(), pageParams.getPageSize());
    }

    @Override
    public PageResult<CourseBaseDTO> listCoursesByTeacherId(Long teacherId, PageParams pageParams) {
        Page<CourseBase> page = courseBaseRepository.findCoursesByTeacherId(
                teacherId,
                PageRequest.of(pageParams.getPageNo().intValue() - 1, pageParams.getPageSize().intValue()));

        List<CourseBaseDTO> items = page.getContent().stream()
                .map(course -> modelMapper.map(course, CourseBaseDTO.class))
                .collect(Collectors.toList());

        return new PageResult<>(
                items,
                page.getTotalElements(),
                pageParams.getPageNo(),
                pageParams.getPageSize());
    }

    private void validateOrganizationAccess(Long organizationId) {
        // TODO: 后续通过认证信息验证，当前仅做参数验证
        if (organizationId == null) {
            throw new ContentException(ContentErrorCode.PARAMS_EMPTY, "机构ID不能为空");
        }
    }
}