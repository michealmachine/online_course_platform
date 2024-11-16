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
        // 准备测试数据 - 创建课程
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase = courseBaseRepository.save(courseBase);

        // 创建教师并关联课程
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setPosition("讲师");
        teacher.setDescription("测试教师简介");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.getCourses().add(courseBase);  // 使用Set添加课程
        teacher.setCreateTime(new Date());
        teacher.setUpdateTime(new Date());

        // 执行保存
        CourseTeacher saved = courseTeacherRepository.save(teacher);

        // 验证
        assertNotNull(saved.getId());
        assertEquals("测试教师", saved.getName());
        assertEquals("讲师", saved.getPosition());
        assertEquals(1, saved.getCourses().size());
        assertTrue(saved.getCourses().contains(courseBase));
    }

    @Test
    void testFindByCourseId() {
        // 准备测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase = courseBaseRepository.save(courseBase);

        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("张老师");
        teacher.setPosition("讲师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.getCourses().add(courseBase);
        teacher.setCreateTime(new Date());
        teacher.setUpdateTime(new Date());
        courseTeacherRepository.save(teacher);

        // 执行查询
        List<CourseTeacher> teachers = courseTeacherRepository.findByCourseId(courseBase.getId());

        // 验证
        assertFalse(teachers.isEmpty());
        assertTrue(teachers.stream()
            .anyMatch(t -> t.getName().equals("张老师")));
    }

    @Test
    void testFindByOrganizationId() {
        // 准备测试数据
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setPosition("讲师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.setCreateTime(new Date());
        teacher.setUpdateTime(new Date());
        courseTeacherRepository.save(teacher);

        // 执行查询
        List<CourseTeacher> teachers = courseTeacherRepository.findByOrganizationId(TEST_ORG_ID);

        // 验证
        assertFalse(teachers.isEmpty());
        assertEquals(TEST_ORG_ID, teachers.get(0).getOrganizationId());
        assertEquals("讲师", teachers.get(0).getPosition());
    }

    @Test
    void testTeacherCourseRelation() {
        // 创建多个课程
        CourseBase course1 = createTestCourse("课程1");
        CourseBase course2 = createTestCourse("课程2");

        // 创建教师并关联课程
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.getCourses().add(course1);
        teacher.getCourses().add(course2);
        teacher.setCreateTime(new Date());
        teacher.setUpdateTime(new Date());
        
        CourseTeacher saved = courseTeacherRepository.save(teacher);
        
        // 验证
        assertNotNull(saved.getId());
        assertEquals(2, saved.getCourses().size());
        assertTrue(saved.getCourses().contains(course1));
        assertTrue(saved.getCourses().contains(course2));
    }
    
    private CourseBase createTestCourse(String name) {
        CourseBase course = new CourseBase();
        course.setName(name);
        course.setOrganizationId(TEST_ORG_ID);
        return courseBaseRepository.save(course);
    }
} 