package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.CourseTeacher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CourseBaseRepositoryTest {

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Autowired
    private CourseTeacherRepository courseTeacherRepository;

    private static final Long TEST_ORG_ID = 1234L; // 测试用机构ID

    @Test
    void testSaveCourse() {
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setBrief("测试课程简介");
        courseBase.setMt(1L);
        courseBase.setSt(1L);
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase.setValid(true);
        courseBase.setStatus("202001");
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());

        CourseBase saved = courseBaseRepository.save(courseBase);
        assertNotNull(saved.getId());
        assertEquals("测试课程", saved.getName());
        assertEquals(TEST_ORG_ID, saved.getOrganizationId());
    }

    @Test
    void testFindByConditions() {
        // 测试按机构ID和其他条件查询
        Page<CourseBase> result = courseBaseRepository.findByConditions(
                TEST_ORG_ID,
                "测试",
                "202001",
                PageRequest.of(0, 10));

        assertNotNull(result);
        assertTrue(result.getContent().stream()
                .allMatch(course -> course.getOrganizationId().equals(TEST_ORG_ID)));
    }

    @Test
    void testFindByOrganizationId() {
        Page<CourseBase> result = courseBaseRepository.findByOrganizationId(
                TEST_ORG_ID,
                PageRequest.of(0, 10));

        assertNotNull(result);
        assertTrue(result.getContent().stream()
                .allMatch(course -> course.getOrganizationId().equals(TEST_ORG_ID)));
    }

    @Test
    void testFindByTeacherId() {
        // 创建测试数据
        CourseBase course = new CourseBase();
        course.setName("测试课程");
        course.setOrganizationId(TEST_ORG_ID);
        course = courseBaseRepository.save(course);

        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.getCourses().add(course);
        // 添加必需的时间字段
        teacher.setCreateTime(new Date());
        teacher.setUpdateTime(new Date());
        teacher = courseTeacherRepository.save(teacher);

        // 测试分页查询 - 使用新的方法名
        Page<CourseBase> page = courseBaseRepository.findCoursesByTeacherId(
                teacher.getId(),
                PageRequest.of(0, 10));

        // 验证
        assertEquals(1, page.getTotalElements());
        assertEquals("测试课程", page.getContent().get(0).getName());
    }

    @Test
    void testFindByTeacherIdAndOrganizationId() {
        // 创建测试数据
        CourseBase course = new CourseBase();
        course.setName("测试课程");
        course.setOrganizationId(TEST_ORG_ID);
        course = courseBaseRepository.save(course);

        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.getCourses().add(course);
        teacher.setCreateTime(new Date());
        teacher.setUpdateTime(new Date());
        teacher = courseTeacherRepository.save(teacher);

        // 测试分页查询 - 使用新的方法名
        Page<CourseBase> page = courseBaseRepository.findCoursesByTeacherIdAndOrganizationId(
                teacher.getId(),
                TEST_ORG_ID,
                PageRequest.of(0, 10));

        // 验证
        assertEquals(1, page.getTotalElements());
        assertEquals("测试课程", page.getContent().get(0).getName());
    }

    @Test
    void testFindByOrganizationIdAndNameContainingAndStatus() {
        // 准备测试数据
        CourseBase course1 = new CourseBase();
        course1.setName("Java课程");
        course1.setOrganizationId(TEST_ORG_ID);
        course1.setStatus("202001");
        courseBaseRepository.save(course1);

        CourseBase course2 = new CourseBase();
        course2.setName("Python课程");
        course2.setOrganizationId(TEST_ORG_ID);
        course2.setStatus("202001");
        courseBaseRepository.save(course2);

        // 测试分页查询
        Page<CourseBase> result = courseBaseRepository.findByOrganizationIdAndNameContainingAndStatus(
                TEST_ORG_ID,
                "Java",
                "202001",
                PageRequest.of(0, 10));

        // 验证结果
        assertEquals(1, result.getTotalElements());
        assertEquals("Java课程", result.getContent().get(0).getName());
    }
}