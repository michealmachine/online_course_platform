package com.double2and9.content_service.service;

import com.double2and9.content_service.dto.AddCourseDTO;
import com.double2and9.content_service.dto.CourseTeacherDTO;
import com.double2and9.content_service.dto.SaveCourseTeacherDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CourseTeacherServiceTests {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private CourseTeacherService courseTeacherService;
    
    @Autowired
    private CourseBaseService courseBaseService;
    
    private Long courseId;

    @BeforeEach
    @Transactional
    public void setUp() {
        // 创建一个测试课程
        AddCourseDTO courseDTO = new AddCourseDTO();
        courseDTO.setName("测试课程");
        courseDTO.setBrief("这是一个测试课程");
        courseDTO.setMt(1L);
        courseDTO.setSt(2L);
        courseDTO.setCharge("201001");
        courseDTO.setPrice(BigDecimal.ZERO);
        courseDTO.setValid(true);
        courseDTO.setOrganizationId(TEST_ORG_ID);

        courseId = courseBaseService.createCourse(courseDTO);
        assertNotNull(courseId);
    }

    @Test
    @Transactional
    public void testTeacherCRUD() {
        // 1. 添加教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setCourseId(courseId);  // 使用刚创建的课程ID
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setDescription("测试教师简介");

        courseTeacherService.saveCourseTeacher(teacherDTO);

        // 2. 查询教师列表
        List<CourseTeacherDTO> teachers = courseTeacherService.listByCourseId(courseId);
        assertNotNull(teachers);
        assertFalse(teachers.isEmpty());

        // 验证新增的教师
        CourseTeacherDTO teacher = teachers.stream()
                .filter(t -> "测试教师".equals(t.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(teacher);
        assertEquals("讲师", teacher.getPosition());

        // 3. 修改教师信息
        teacherDTO.setId(teacher.getId());
        teacherDTO.setPosition("高级讲师");
        courseTeacherService.saveCourseTeacher(teacherDTO);

        // 4. 再次查询验证
        teachers = courseTeacherService.listByCourseId(courseId);
        teacher = teachers.stream()
                .filter(t -> "测试教师".equals(t.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(teacher);
        assertEquals("高级讲师", teacher.getPosition());

        // 5. 删除测试
        courseTeacherService.deleteCourseTeacher(courseId, teacher.getId());
        teachers = courseTeacherService.listByCourseId(courseId);
        assertTrue(teachers.stream().noneMatch(t -> "测试教师".equals(t.getName())));
    }
} 