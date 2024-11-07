package com.double2and9.content_service.service.impl;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.enums.ContentErrorCode;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.*;
import com.double2and9.content_service.entity.*;
import com.double2and9.content_service.repository.CourseBaseRepository;
import com.double2and9.content_service.repository.CourseCategoryRepository;
import com.double2and9.content_service.repository.TeachplanRepository;
import com.double2and9.content_service.repository.CourseTeacherRepository;
import com.double2and9.content_service.service.CourseBaseService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.util.StringUtils;
import org.springframework.transaction.annotation.Transactional;

import jakarta.persistence.criteria.Predicate;
import java.util.ArrayList;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;
import java.util.HashMap;
import java.util.Map;

@Slf4j
@Service
public class CourseBaseServiceImpl implements CourseBaseService {

    private final CourseBaseRepository courseBaseRepository;
    private final CourseCategoryRepository courseCategoryRepository;
    private final TeachplanRepository teachplanRepository;
    private final CourseTeacherRepository courseTeacherRepository;
    private final ModelMapper modelMapper;

    public CourseBaseServiceImpl(CourseBaseRepository courseBaseRepository,
                               CourseCategoryRepository courseCategoryRepository,
                               TeachplanRepository teachplanRepository,
                               CourseTeacherRepository courseTeacherRepository,
                               ModelMapper modelMapper) {
        this.courseBaseRepository = courseBaseRepository;
        this.courseCategoryRepository = courseCategoryRepository;
        this.teachplanRepository = teachplanRepository;
        this.courseTeacherRepository = courseTeacherRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public PageResult<CourseBaseDTO> queryCourseList(PageParams params, QueryCourseParamsDTO queryParams) {
        //构建查询条件
        Specification<CourseBase> specification = (root, query, cb) -> {
            List<Predicate> predicates = new ArrayList<>();
            
            //课程名称模糊查询
            if (StringUtils.hasText(queryParams.getCourseName())) {
                predicates.add(cb.like(root.get("name"), 
                    "%" + queryParams.getCourseName() + "%"));
            }

            //课程状态
            if (StringUtils.hasText(queryParams.getStatus())) {
                predicates.add(cb.equal(root.get("status"), 
                    queryParams.getStatus()));
            }

            //课程审核状态
            if (StringUtils.hasText(queryParams.getAuditStatus())) {
                // 关联预发布表查询审核状态
                predicates.add(cb.equal(
                    root.join("coursePublishPre").get("status"), 
                    queryParams.getAuditStatus()));
            }

            //课程分类
            if (queryParams.getMt() != null) {
                predicates.add(cb.equal(root.get("mt"), 
                    queryParams.getMt()));
            }
            if (queryParams.getSt() != null) {
                predicates.add(cb.equal(root.get("st"), 
                    queryParams.getSt()));
            }

            return cb.and(predicates.toArray(new Predicate[0]));
        };

        //分页参数
        Pageable pageable = PageRequest.of(
            params.getPageNo().intValue() - 1, 
            params.getPageSize().intValue());

        //分页查询
        Page<CourseBase> page = courseBaseRepository.findAll(specification, pageable);

        //数据转换
        List<CourseBaseDTO> items = page.getContent().stream()
            .map(this::convertToCourseBaseDTO)
            .collect(Collectors.toList());

        //构建结果
        return new PageResult<>(
            items, 
            page.getTotalElements(), 
            params.getPageNo(), 
            params.getPageSize());
    }

    /**
     * 将CourseBase转换为CourseBaseDTO
     */
    private CourseBaseDTO convertToCourseBaseDTO(CourseBase courseBase) {
        CourseBaseDTO dto = modelMapper.map(courseBase, CourseBaseDTO.class);
        
        // 设置课程分类名称
        courseCategoryRepository.findById(courseBase.getMt())
            .ifPresent(category -> dto.setMtName(category.getName()));
        courseCategoryRepository.findById(courseBase.getSt())
            .ifPresent(category -> dto.setStName(category.getName()));
        
        // 设置课程营销信息
        CourseMarket courseMarket = courseBase.getCourseMarket();
        if (courseMarket != null) {
            modelMapper.map(courseMarket, dto);
        }
        
        return dto;
    }

    /**
     * 将AddCourseDTO转换为CourseBase
     */
    private CourseBase convertToEntity(AddCourseDTO dto) {
        CourseBase courseBase = modelMapper.map(dto, CourseBase.class);
        // 设置初始状态
        courseBase.setStatus("202001"); // 未发布
        courseBase.setValid(true);      // 有效
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        return courseBase;
    }

    @Override
    @Transactional
    public Long createCourse(AddCourseDTO addCourseDTO) {
        // 转换DTO为实体
        CourseBase courseBase = convertToEntity(addCourseDTO);
        
        // 先保存CourseBase以获取ID
        CourseBase savedCourse = courseBaseRepository.save(courseBase);
        
        // 创建课程营销信息
        CourseMarket courseMarket = modelMapper.map(addCourseDTO, CourseMarket.class);
        courseMarket.setId(savedCourse.getId());  // 设置相同的ID
        courseMarket.setCourseBase(savedCourse);
        courseMarket.setCreateTime(new Date());
        courseMarket.setUpdateTime(new Date());
        courseBase.setCourseMarket(courseMarket);
        
        // 再次保存以更新关联关系
        courseBaseRepository.save(courseBase);
        
        log.info("课程创建成功，课程ID：{}", savedCourse.getId());
        return savedCourse.getId();
    }

    @Override
    public void saveTeachplan(SaveTeachplanDTO teachplanDTO) {
    }

    @Override
    public void saveCourseTeacher(SaveCourseTeacherDTO teacherDTO) {
    }

    @Override
    @Transactional(readOnly = true)  // 添加事务注解，readOnly=true因为只是读取操作
    public CoursePreviewDTO preview(Long courseId) {
        // 获取课程基本信息
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        
        CoursePreviewDTO previewDTO = new CoursePreviewDTO();
        
        // 设置课程基本信息
        previewDTO.setCourseBase(convertToCourseBaseDTO(courseBase));
        
        // 获取课程计划信息
        List<Teachplan> teachplans = teachplanRepository.findByCourseBaseIdOrderByOrderBy(courseId);
        List<TeachplanDTO> teachplanDTOs = teachplans.stream()
                .map(teachplan -> modelMapper.map(teachplan, TeachplanDTO.class))
                .collect(Collectors.toList());
        previewDTO.setTeachplans(teachplanDTOs);
        
        // 获取课程教师信息 - 使用JOIN FETCH避免N+1问题
        List<CourseTeacher> teachers = courseTeacherRepository.findByCourseBaseId(courseId);
        List<CourseTeacherDTO> teacherDTOs = teachers.stream()
                .map(teacher -> modelMapper.map(teacher, CourseTeacherDTO.class))
                .collect(Collectors.toList());
        previewDTO.setTeachers(teacherDTOs);
        
        log.info("课程预览信息获取成功，课程ID：{}", courseId);
        return previewDTO;
    }

    @Override
    @Transactional
    public void submitForAudit(Long courseId) {
        // 获取课程信息
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        
        // 检查课程基本信息是否完整
        validateCourseInfo(courseBase);
        
        // 创建预发布记录
        CoursePublishPre publishPre = courseBase.getCoursePublishPre();
        if (publishPre == null) {
            publishPre = new CoursePublishPre();
            publishPre.setId(courseId);
            publishPre.setCourseBase(courseBase);
        }
        
        // 更新预发布状态
        publishPre.setStatus("202301"); // 已提交
        publishPre.setCreateTime(new Date());
        publishPre.setUpdateTime(new Date());
        
        courseBase.setCoursePublishPre(publishPre);
        courseBaseRepository.save(courseBase);
        
        log.info("课程提交审核成功，课程ID：{}", courseId);
    }

    @Override
    @Transactional
    public void auditCourse(CourseAuditDTO auditDTO) {
        CourseBase courseBase = courseBaseRepository.findById(auditDTO.getCourseId())
                .orElseThrow(() -> new RuntimeException("课程不存在"));
        
        CoursePublishPre publishPre = courseBase.getCoursePublishPre();
        if (publishPre == null) {
            throw new RuntimeException("课程未提交审核");
        }
        
        // 更新审核状态
        publishPre.setStatus(auditDTO.getAuditStatus());
        publishPre.setUpdateTime(new Date());
        
        // 如果审核通过，更新课程状态为待发布
        if ("202303".equals(auditDTO.getAuditStatus())) {
            courseBase.setStatus("202001"); // 未发布
        }
        
        courseBaseRepository.save(courseBase);
        
        log.info("课程审核完成，课程ID：{}，审核状态：{}", auditDTO.getCourseId(), auditDTO.getAuditStatus());
    }

    @Override
    public String getAuditStatus(Long courseId) {
        return courseBaseRepository.findById(courseId)
                .map(CourseBase::getCoursePublishPre)
                .map(CoursePublishPre::getStatus)
                .orElse(null);
    }

    /**
     * 验证课程信息是否完整
     */
    private void validateCourseInfo(CourseBase courseBase) {
        if (courseBase.getName() == null || courseBase.getBrief() == null) {
            throw new RuntimeException("课程基本信息不完整");
        }
        
        // 验证课程计划
        List<Teachplan> teachplans = teachplanRepository.findByCourseBaseIdOrderByOrderBy(courseBase.getId());
        if (teachplans.isEmpty()) {
            throw new RuntimeException("请添加课程计划");
        }
        
        // 验证课程教师
        List<CourseTeacher> teachers = courseTeacherRepository.findByCourseBaseId(courseBase.getId());
        if (teachers.isEmpty()) {
            throw new RuntimeException("请添加课程教师");
        }
    }

    @Override
    public void publishCourse(Long courseId) {
    }

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
} 