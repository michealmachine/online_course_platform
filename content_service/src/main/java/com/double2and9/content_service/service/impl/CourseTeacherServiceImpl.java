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
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.*;
import java.util.stream.Collectors;

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
        List<CourseTeacher> teachers = courseTeacherRepository.findByCourseId(courseId);
        return teachers.stream()
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
            teacher.setCreateTime(new Date());
        }

        // 设置基本信息
        modelMapper.map(teacherDTO, teacher);
        teacher.setUpdateTime(new Date());

        // 处理课程关联
        Set<CourseBase> courses = courseBaseRepository.findAllById(teacherDTO.getCourseIds())
                .stream()
                .collect(Collectors.toSet());

        // 验证所有课程都属于同一机构
        if (!courses.stream().allMatch(course -> course.getOrganizationId().equals(teacherDTO.getOrganizationId()))) {
            throw new ContentException(ContentErrorCode.COURSE_ORG_NOT_MATCH);
        }

        teacher.setCourses(courses);
        courseTeacherRepository.save(teacher);

        log.info("保存教师信息成功，教师ID：{}，关联课程数：{}", teacher.getId(), courses.size());
    }

    @Override
    @Transactional
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
        List<CourseTeacher> teachers = courseTeacherRepository.findByOrganizationId(organizationId);
        return teachers.stream()
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
    @Transactional(readOnly = true)
    public List<CourseBaseDTO> listCoursesByTeacherId(Long teacherId) {
        CourseTeacher teacher = courseTeacherRepository.findById(teacherId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));

        return teacher.getCourses().stream()
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
}