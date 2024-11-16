package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.CourseTeacher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
public class CourseTeacherRepositoryTest {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private CourseTeacherRepository courseTeacherRepository;
    
    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Test
    void testSaveCourseTeacher() {
        // 准备测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase = courseBaseRepository.save(courseBase);

        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setPosition("讲师");
        teacher.setDescription("测试教师简介");
        teacher.setCourseBase(courseBase);
        teacher.setCreateTime(new Date());
        teacher.setUpdateTime(new Date());

        // 执行保存
        CourseTeacher saved = courseTeacherRepository.save(teacher);

        // 验证
        assertNotNull(saved.getId());
        assertEquals("测试教师", saved.getName());
        assertEquals("讲师", saved.getPosition());
    }

    @Test
    void testFindByNameContaining() {
        // 准备测试数据
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("张老师");
        teacher.setPosition("讲师");
        teacher.setCreateTime(new Date());
        teacher.setUpdateTime(new Date());
        courseTeacherRepository.save(teacher);

        // 执行查询
        List<CourseTeacher> teachers = courseTeacherRepository.findByNameContaining("张");

        // 验证
        assertFalse(teachers.isEmpty());
        assertTrue(teachers.stream().anyMatch(t -> t.getName().equals("张老师")));
    }

    @Test
    void testFindByPosition() {
        // 准备测试数据
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setPosition("高级讲师");
        teacher.setCreateTime(new Date());
        teacher.setUpdateTime(new Date());
        courseTeacherRepository.save(teacher);

        // 执行查询
        List<CourseTeacher> teachers = courseTeacherRepository.findByPosition("高级讲师");

        // 验证
        assertFalse(teachers.isEmpty());
        assertEquals("高级讲师", teachers.get(0).getPosition());
    }
} 