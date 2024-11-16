package com.double2and9.content_service.service.impl;

import com.double2and9.base.enums.ContentErrorCode;
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

import java.util.Date;
import java.util.List;
import java.util.Set;
import java.util.stream.Collectors;

@Slf4j
@Service
public class CourseTeacherServiceImpl implements CourseTeacherService {

    private final CourseTeacherRepository courseTeacherRepository;
    private final CourseBaseRepository courseBaseRepository;
    private final ModelMapper modelMapper;

    public CourseTeacherServiceImpl(CourseTeacherRepository courseTeacherRepository,
                                  CourseBaseRepository courseBaseRepository,
                                  ModelMapper modelMapper) {
        this.courseTeacherRepository = courseTeacherRepository;
        this.courseBaseRepository = courseBaseRepository;
        this.modelMapper = modelMapper;
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
        if (!courses.stream().allMatch(course -> 
                course.getOrganizationId().equals(teacherDTO.getOrganizationId()))) {
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
} 