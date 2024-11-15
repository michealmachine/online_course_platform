package com.double2and9.content_service.service.impl;

import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.common.exception.ContentException;
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
        List<CourseTeacher> teachers = courseTeacherRepository.findByCourseBaseId(courseId);
        return teachers.stream()
                .map(teacher -> modelMapper.map(teacher, CourseTeacherDTO.class))
                .collect(Collectors.toList());
    }

    @Override
    @Transactional
    public void saveCourseTeacher(SaveCourseTeacherDTO teacherDTO) {
        CourseBase courseBase = courseBaseRepository.findById(teacherDTO.getCourseId())
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        CourseTeacher teacher;
        if (teacherDTO.getId() != null) {
            teacher = courseTeacherRepository.findById(teacherDTO.getId())
                    .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));
        } else {
            teacher = new CourseTeacher();
            teacher.setCreateTime(new Date());
        }

        // 更新教师信息
        modelMapper.map(teacherDTO, teacher);
        teacher.setCourseBase(courseBase);
        teacher.setUpdateTime(new Date());

        courseTeacherRepository.save(teacher);
        log.info("保存课程教师信息成功，课程ID：{}，教师ID：{}", courseBase.getId(), teacher.getId());
    }

    @Override
    @Transactional
    public void deleteCourseTeacher(Long courseId, Long teacherId) {
        CourseTeacher teacher = courseTeacherRepository.findById(teacherId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS));

        // 验证课程ID是否匹配
        if (!teacher.getCourseBase().getId().equals(courseId)) {
            throw new ContentException(ContentErrorCode.TEACHER_COURSE_NOT_MATCH);
        }

        courseTeacherRepository.delete(teacher);
        log.info("删除课程教师成功，课程ID：{}，教师ID：{}", courseId, teacherId);
    }
} 