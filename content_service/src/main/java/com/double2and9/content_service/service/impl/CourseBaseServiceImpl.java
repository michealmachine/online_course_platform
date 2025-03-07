package com.double2and9.content_service.service.impl;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.client.MediaFeignClient;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.*;
import com.double2and9.content_service.entity.*;
import com.double2and9.content_service.repository.CourseBaseRepository;
import com.double2and9.content_service.repository.CourseCategoryRepository;
import com.double2and9.content_service.repository.TeachplanRepository;
import com.double2and9.content_service.repository.CourseTeacherRepository;
import com.double2and9.content_service.repository.MediaFileRepository;
import com.double2and9.content_service.service.CourseBaseService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

/**
 * 课程基本信息服务实现类
 * 处理课程的CRUD、审核、发布等核心业务逻辑
 */
@Slf4j
@Service
public class CourseBaseServiceImpl implements CourseBaseService {

    private final CourseBaseRepository courseBaseRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final TeachplanRepository teachplanRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ModelMapper modelMapper;
    private final MediaFeignClient mediaFeignClient;

    /**
     * 构造函数注入依赖
     */
    public CourseBaseServiceImpl(CourseBaseRepository courseBaseRepository,
            CourseCategoryRepository courseCategoryRepository,
            TeachplanRepository teachplanRepository,
            CourseTeacherRepository courseTeacherRepository,
            MediaFileRepository mediaFileRepository,
            ModelMapper modelMapper,
            MediaFeignClient mediaFeignClient) {
        this.courseBaseRepository = courseBaseRepository;
        this.courseCategoryRepository = courseCategoryRepository;
        this.teachplanRepository = teachplanRepository;
        this.courseTeacherRepository = courseTeacherRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.modelMapper = modelMapper;
        this.mediaFeignClient = mediaFeignClient;
    }

    /**
     * 分页查询课程信息
     * 
     * @param params      分页参数
     * @param queryParams 查询条件
     * @return 分页结果
     */
    @Override
    public PageResult<CourseBaseDTO> queryCourseList(PageParams params, QueryCourseParamsDTO queryParams) {
        log.info("分页查询课程列表，参数：params={}, queryParams={}", params, queryParams);

        // 使用查询参数中的机构ID
        Long organizationId = queryParams.getOrganizationId();
        String courseName = queryParams.getCourseName();
        String status = queryParams.getStatus();

        log.info("查询参数：organizationId={}, courseName={}, status={}",
                organizationId, courseName, status);

        // 使用机构ID进行查询
        Page<CourseBase> page = courseBaseRepository.findByConditions(
                organizationId,
                courseName,
                status,
                PageRequest.of(params.getPageNo().intValue() - 1, params.getPageSize().intValue()));

        log.info("查询结果：total={}, content.size={}", page.getTotalElements(), page.getContent().size());

        // 数据转换
        List<CourseBaseDTO> items = page.getContent().stream()
                .map(this::convertToCourseBaseDTO)
                .collect(Collectors.toList());

        return new PageResult<>(items, page.getTotalElements(), params.getPageNo(), params.getPageSize());
    }

    /**
     * 创建新课程
     * 
     * @param addCourseDTO 课程创建信息
     * @return 新创建的课程ID
     * @throws ContentException 如果课程信息不完整或创建失败
     */
    @Override
    @Transactional
    public Long createCourse(AddCourseDTO addCourseDTO) {
        // 直接使用DTO中的机构ID
        Long organizationId = addCourseDTO.getOrganizationId();

        CourseBase courseBase = modelMapper.map(addCourseDTO, CourseBase.class);
        courseBase.setOrganizationId(organizationId);

        // 先保存CourseBase以获取ID
        CourseBase savedCourse = courseBaseRepository.save(courseBase);

        // 创建课程营销信息
        CourseMarket courseMarket = modelMapper.map(addCourseDTO, CourseMarket.class);
        courseMarket.setId(savedCourse.getId()); // 设置相同的ID
        courseMarket.setCourseBase(savedCourse);
        courseMarket.setCreateTime(new Date());
        courseMarket.setUpdateTime(new Date());
        courseBase.setCourseMarket(courseMarket);

        // 再次保存以更新关联关系
        courseBaseRepository.save(courseBase);

        log.info("课程创建成功，课程ID：{}", savedCourse.getId());
        return savedCourse.getId();
    }

    /**
     * 更新课程信息
     * 
     * @param editCourseDTO 课程更新信息
     * @throws ContentException 如果课程不存在或更新失败
     */
    @Override
    @Transactional
    public void updateCourse(EditCourseDTO editCourseDTO) {
        // 获取课程信息
        CourseBase courseBase = courseBaseRepository.findById(editCourseDTO.getId())
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        // 更新基本信息
        modelMapper.map(editCourseDTO, courseBase);
        courseBase.setUpdateTime(new Date());

        // 更新营销信息
        CourseMarket courseMarket = courseBase.getCourseMarket();
        if (courseMarket == null) {
            courseMarket = new CourseMarket();
            courseMarket.setCourseBase(courseBase);
            courseBase.setCourseMarket(courseMarket);
        }
        modelMapper.map(editCourseDTO, courseMarket);
        courseMarket.setUpdateTime(new Date());

        // 保存更新
        courseBaseRepository.save(courseBase);

        log.info("课程更新成功，课程ID：{}", courseBase.getId());
    }

    /**
     * 获取课程分类树
     * 
     * @return 课程分类树形结构
     */
    @Override
    public List<CourseCategoryTreeDTO> queryCourseCategoryTree() {
        // 查询所有课程分类
        List<CourseCategory> categories = courseCategoryRepository.findAll();

        // 将课程分类转换为树形结构
        List<CourseCategoryTreeDTO> rootNodes = new ArrayList<>();
        Map<Long, CourseCategoryTreeDTO> nodeMap = new HashMap<>();

        // 转换所有节点
        categories.forEach(category -> {
            CourseCategoryTreeDTO node = modelMapper.map(category, CourseCategoryTreeDTO.class);
            nodeMap.put(node.getId(), node);

            if (category.getParentId() == 0L) {
                rootNodes.add(node);
            } else {
                CourseCategoryTreeDTO parentNode = nodeMap.get(category.getParentId());
                if (parentNode != null) {
                    if (parentNode.getChildrenTreeNodes() == null) {
                        parentNode.setChildrenTreeNodes(new ArrayList<>());
                    }
                    parentNode.getChildrenTreeNodes().add(node);
                }
            }
        });

        return rootNodes;
    }

    /**
     * 预览课程信息
     * 
     * @param courseId 课程ID
     * @return 课程预览信息
     * @throws ContentException 如果课程不存在
     */
    @Override
    @Transactional(readOnly = true)
    public CoursePreviewDTO preview(Long courseId) {
        // 获取课程基本信息
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        CoursePreviewDTO previewDTO = new CoursePreviewDTO();

        // 设课程基本信息
        previewDTO.setCourseBase(convertToCourseBaseDTO(courseBase));

        // 获取课程计划信息
        List<Teachplan> teachplans = teachplanRepository.findByCourseBaseIdOrderByOrderBy(courseId);
        List<TeachplanDTO> teachplanDTOs = teachplans.stream()
                .map(teachplan -> modelMapper.map(teachplan, TeachplanDTO.class))
                .collect(Collectors.toList());
        previewDTO.setTeachplans(teachplanDTOs);

        // 获取课程教师信息
        List<CourseTeacher> teachers = courseTeacherRepository.findByCourseId(courseId);
        List<CourseTeacherDTO> teacherDTOs = teachers.stream()
                .map(teacher -> modelMapper.map(teacher, CourseTeacherDTO.class))
                .collect(Collectors.toList());
        previewDTO.setTeachers(teacherDTOs);

        log.info("课程预览信息获取成功，课程ID：{}", courseId);
        return previewDTO;
    }

    /**
     * 提交课程审核
     * 
     * @param courseId 课程ID
     * @throws ContentException 如果课程不存在或信息不完整
     */
    @Override
    @Transactional
    public void submitForAudit(Long courseId) {
        // 获取课程信息
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        // 检查课程基本信息是否完整
        validateCourseInfo(courseBase);

        // 创建预发布记录
        CoursePublishPre publishPre = courseBase.getCoursePublishPre();
        if (publishPre == null) {
            publishPre = new CoursePublishPre();
            publishPre.setId(courseId);
            publishPre.setCourseBase(courseBase);
        }

        // 设置预发布信息
        publishPre.setName(courseBase.getName()); // 设置课程名称
        publishPre.setStatus("202301"); // 已提交
        publishPre.setPreviewTime(new Date());
        publishPre.setCreateTime(new Date());
        publishPre.setUpdateTime(new Date());

        courseBase.setCoursePublishPre(publishPre);
        courseBaseRepository.save(courseBase);

        log.info("课程提交审核成功，课程ID：{}", courseId);
    }

    /**
     * 将DTO转换为实体对象
     * 
     * @param dto 数据传输对象
     * @return 实体对象
     */
    private CourseBase convertToEntity(AddCourseDTO dto) {
        CourseBase courseBase = modelMapper.map(dto, CourseBase.class);
        // 设置初始状态
        courseBase.setStatus("202001"); // 未发布
        courseBase.setValid(true); // 有效
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        return courseBase;
    }

    /**
     * 验证课程信息完整性
     * 
     * @param courseBase 课程基本信息
     * @throws ContentException 如果信息不完整
     */
    private void validateCourseInfo(CourseBase courseBase) {
        if (courseBase.getName() == null || courseBase.getBrief() == null) {
            throw new ContentException(ContentErrorCode.COURSE_NAME_EMPTY);
        }

        // 验证课程计划
        List<Teachplan> teachplans = teachplanRepository.findByCourseBaseIdOrderByOrderBy(courseBase.getId());
        if (teachplans.isEmpty()) {
            throw new ContentException(ContentErrorCode.TEACHPLAN_NOT_EXISTS, "请添加课程计划");
        }

        // 验证课程教师
        List<CourseTeacher> teachers = courseTeacherRepository.findByCourseId(courseBase.getId());
        if (teachers.isEmpty()) {
            throw new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS, "请添加课程教师");
        }
    }

    @Override
    public void saveTeachplan(SaveTeachplanDTO teachplanDTO) {
    }

    @Override
    public void saveCourseTeacher(SaveCourseTeacherDTO teacherDTO) {
    }

    @Override
    @Transactional
    public void publishCourse(Long courseId) {
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        CoursePublishPre publishPre = courseBase.getCoursePublishPre();
        if (publishPre == null || !"202303".equals(publishPre.getStatus())) {
            throw new ContentException(ContentErrorCode.COURSE_AUDIT_STATUS_ERROR, "课程未通过审核，不能发布");
        }

        // 创建发布记录
        CoursePublish coursePublish = courseBase.getCoursePublish();
        if (coursePublish == null) {
            coursePublish = new CoursePublish();
            coursePublish.setId(courseId);
            coursePublish.setCourseBase(courseBase);
        }

        // 设置发布信息
        coursePublish.setName(courseBase.getName());
        coursePublish.setStatus("202002"); // 已发布
        coursePublish.setPublishTime(new Date());
        coursePublish.setUpdateTime(new Date());

        // 更新课程状态
        courseBase.setStatus("202002"); // 已发布
        courseBase.setCoursePublish(coursePublish);

        courseBaseRepository.save(courseBase);

        log.info("课程发布成功，课程ID：{}", courseId);
    }

    @Override
    public String getAuditStatus(Long courseId) {
        return courseBaseRepository.findById(courseId)
                .map(CourseBase::getCoursePublishPre)
                .map(CoursePublishPre::getStatus)
                .orElse(null);
    }

    /**
     * 将CourseBase实体转换为DTO
     * 
     * @param courseBase 课程基本信息实体
     * @return 课程基本信息DTO
     */
    private CourseBaseDTO convertToCourseBaseDTO(CourseBase courseBase) {
        CourseBaseDTO dto = modelMapper.map(courseBase, CourseBaseDTO.class);

        // 设置课程分类名称 - 添加空值检查
        if (courseBase.getMt() != null) {
            courseCategoryRepository.findById(courseBase.getMt())
                    .ifPresent(category -> dto.setMtName(category.getName()));
        }
        if (courseBase.getSt() != null) {
            courseCategoryRepository.findById(courseBase.getSt())
                    .ifPresent(category -> dto.setStName(category.getName()));
        }

        return dto;
    }

    /**
     * 审核课程
     * 
     * @param auditDTO 审核信息
     * @throws ContentException 如果课程不存在或状态错误
     */
    @Override
    @Transactional
    public void auditCourse(CourseAuditDTO auditDTO) {
        CourseBase courseBase = courseBaseRepository.findById(auditDTO.getCourseId())
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        CoursePublishPre publishPre = courseBase.getCoursePublishPre();
        if (publishPre == null) {
            throw new ContentException(ContentErrorCode.COURSE_AUDIT_STATUS_ERROR, "课程未提交审核");
        }

        // 更新审核状态
        publishPre.setStatus(auditDTO.getAuditStatus());
        publishPre.setAuditMessage(auditDTO.getAuditMessage()); // 设置审核意见
        publishPre.setUpdateTime(new Date());

        // 如果审核通过，更新课程状态为待发布
        if ("202303".equals(auditDTO.getAuditStatus())) {
            courseBase.setStatus("202001"); // 修改这里：审核通过后状态为未发布，等待机构主动发布
        } else {
            // 如果审核不通过，状态保持未发布
            courseBase.setStatus("202001");
        }
        courseBase.setUpdateTime(new Date());

        // 保存更新
        courseBaseRepository.save(courseBase);

        log.info("课程审核完成，课程ID：{}，审核状态：{}, 课程状态：{}",
                auditDTO.getCourseId(),
                auditDTO.getAuditStatus(),
                courseBase.getStatus());
    }

    @Override
    public CourseBaseDTO getCourseById(Long courseId) {
        log.info("获取课程信息，courseId：{}", courseId);

        // 查询课程基本信息
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        // 使用modelMapper替代BeanUtils
        CourseBaseDTO courseBaseDTO = modelMapper.map(courseBase, CourseBaseDTO.class);

        log.info("获取课程信息成功，courseId：{}", courseId);
        return courseBaseDTO;
    }

    @Override
    @Transactional
    public void deleteCourse(Long courseId) {
        log.info("删除课程，courseId：{}", courseId);

        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        // 检查课程状态，已发布的课程不能删除
        if ("202002".equals(courseBase.getStatus())) {
            throw new ContentException(ContentErrorCode.COURSE_STATUS_ERROR, "已发布的课程不能删除");
        }

        // 删除课程封面
        if (StringUtils.hasText(courseBase.getLogo())) {
            try {
                deleteCourseLogo(courseId);
            } catch (Exception e) {
                log.error("删除课程封面失败：", e);
                // 继续删除课程，不影响主流程
            }
        }

        // 删除课程相关数据
        courseBaseRepository.delete(courseBase);

        log.info("删除课程成功，courseId：{}", courseId);
    }

    @Override
    @Transactional
    public void offlineCourse(Long courseId) {
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        // 只有已发布的课程才能下架
        if (!"202002".equals(courseBase.getStatus())) {
            throw new ContentException(ContentErrorCode.COURSE_STATUS_ERROR, "只有已发布的课程才能下架");
        }

        // 更新课程状态为已下架
        courseBase.setStatus("202003"); // 已下架
        courseBase.setUpdateTime(new Date());

        courseBaseRepository.save(courseBase);

        log.info("课程下架成功，课程ID：{}", courseId);
    }

    /**
     * 更新课程封面
     * 
     * @param courseId 课程ID
     * @param file     封面图片文件
     * @throws ContentException 如果课程不存在或上传失败
     */
    @Transactional
    public void updateCourseLogo(Long courseId, MultipartFile file) {
        // 1. 获取课程信息
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        try {
            // 2. 调用媒体服务上传图片
            CommonResponse<MediaFileDTO> response = mediaFeignClient.uploadCourseLogo(
                    courseId,
                    courseBase.getOrganizationId(),
                    file);

            if (!response.isSuccess()) {
                throw new ContentException(ContentErrorCode.UPLOAD_LOGO_FAILED, response.getMessage());
            }

            MediaFileDTO mediaFileDTO = response.getData();

            // 3. 保存或更新媒体文件记录
            MediaFile mediaFile = mediaFileRepository.findByMediaFileId(mediaFileDTO.getMediaFileId())
                    .orElse(new MediaFile());
            modelMapper.map(mediaFileDTO, mediaFile);
            mediaFileRepository.save(mediaFile);

            // 4. 更新课程封面URL
            courseBase.setLogo(mediaFileDTO.getUrl());
            courseBaseRepository.save(courseBase);

            log.info("课程封面更新成功，课程ID：{}，文件ID：{}", courseId, mediaFileDTO.getMediaFileId());
        } catch (ContentException e) {
            throw e;
        } catch (Exception e) {
            log.error("更新课程封面失败：", e);
            throw new ContentException(ContentErrorCode.UPLOAD_LOGO_FAILED);
        }
    }

    @Transactional
    public void deleteCourseLogo(Long courseId) {
        // 1. 获取课程信息
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        String logoUrl = courseBase.getLogo();
        if (logoUrl == null || logoUrl.isEmpty()) {
            return; // 没有封面，直接返回
        }

        try {
            // 2. 调用媒体服务删除文件
            CommonResponse<?> response = mediaFeignClient.deleteMediaFile(logoUrl);
            if (!response.isSuccess()) {
                throw new ContentException(ContentErrorCode.DELETE_LOGO_FAILED, response.getMessage());
            }

            // 3. 清除课程封面URL
            courseBase.setLogo(null);
            courseBaseRepository.save(courseBase);

            log.info("课程封面删除成功，课程ID：{}", courseId);
        } catch (ContentException e) {
            throw e;
        } catch (Exception e) {
            log.error("删除课程封面失败：", e);
            throw new ContentException(ContentErrorCode.DELETE_LOGO_FAILED);
        }
    }
}