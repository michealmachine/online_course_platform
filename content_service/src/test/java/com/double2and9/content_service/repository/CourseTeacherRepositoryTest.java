package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.CourseTeacher;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

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
        teacher.getCourses().add(courseBase); // 使用Set添加课程
        teacher.setCreateTime(LocalDateTime.now());
        teacher.setUpdateTime(LocalDateTime.now());

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
        teacher.setCreateTime(LocalDateTime.now());
        teacher.setUpdateTime(LocalDateTime.now());
        courseTeacherRepository.save(teacher);

        // 执行查询 - 修改为使用分页版本
        Page<CourseTeacher> teacherPage = courseTeacherRepository.findByCourseId(
                courseBase.getId(),
                PageRequest.of(0, 10));

        // 验证
        assertFalse(teacherPage.getContent().isEmpty());
        assertTrue(teacherPage.getContent().stream()
                .anyMatch(t -> t.getName().equals("张老师")));
    }

    @Test
    void testFindByOrganizationId() {
        // 创建测试教师
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.setCreateTime(LocalDateTime.now());
        teacher.setUpdateTime(LocalDateTime.now());
        courseTeacherRepository.save(teacher);

        // 测试分页查询
        Page<CourseTeacher> result = courseTeacherRepository.findByOrganizationId(
                TEST_ORG_ID,
                PageRequest.of(0, 10));

        assertNotNull(result);
        assertTrue(result.getContent().stream()
                .allMatch(t -> t.getOrganizationId().equals(TEST_ORG_ID)));
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
        teacher.setCreateTime(LocalDateTime.now());
        teacher.setUpdateTime(LocalDateTime.now());

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

    @Test
    void testFindByOrganizationIdAndId() {
        // 创建测试教师
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setPosition("讲师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.setCreateTime(LocalDateTime.now());
        teacher.setUpdateTime(LocalDateTime.now());
        CourseTeacher savedTeacher = courseTeacherRepository.save(teacher);

        // 使用正确的机构ID查询
        Optional<CourseTeacher> found = courseTeacherRepository
                .findByOrganizationIdAndId(TEST_ORG_ID, savedTeacher.getId());
        assertTrue(found.isPresent());
        assertEquals("测试教师", found.get().getName());
        assertEquals(TEST_ORG_ID, found.get().getOrganizationId());

        // 使用错误的机构ID查询
        Optional<CourseTeacher> notFound = courseTeacherRepository
                .findByOrganizationIdAndId(999L, savedTeacher.getId());
        assertTrue(notFound.isEmpty());
    }

    @Test
    void testDeleteTeacher() {
        // 创建测试教师
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("待删除教师");
        teacher.setPosition("讲师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.setCreateTime(LocalDateTime.now());
        teacher.setUpdateTime(LocalDateTime.now());
        CourseTeacher savedTeacher = courseTeacherRepository.save(teacher);

        // 验证教师已创建
        assertTrue(courseTeacherRepository.findById(savedTeacher.getId()).isPresent());

        // 删除教师
        courseTeacherRepository.delete(savedTeacher);

        // 验证教师已删除
        assertTrue(courseTeacherRepository.findById(savedTeacher.getId()).isEmpty());
    }

    @Test
    void testDeleteTeacherWithCourses() {
        // 创建测试课程
        CourseBase course = createTestCourse("测试课程");

        // 创建教师并关联课程
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("待删除教师");
        teacher.setPosition("讲师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.getCourses().add(course);
        teacher.setCreateTime(LocalDateTime.now());
        teacher.setUpdateTime(LocalDateTime.now());
        CourseTeacher savedTeacher = courseTeacherRepository.save(teacher);

        // 验证教师和课程关联已创建
        assertTrue(courseTeacherRepository.findById(savedTeacher.getId()).isPresent());
        assertEquals(1, savedTeacher.getCourses().size());

        // 删除教师
        courseTeacherRepository.delete(savedTeacher);

        // 验证教师已删除，但课程仍然存在
        assertTrue(courseTeacherRepository.findById(savedTeacher.getId()).isEmpty());
        assertTrue(courseBaseRepository.findById(course.getId()).isPresent());
    }

    @Test
    void testFindByCourseIdWithPagination() {
        // 准备测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase = courseBaseRepository.save(courseBase);

        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setPosition("讲师");
        teacher.setDescription("测试教师简介");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.getCourses().add(courseBase);
        teacher.setCreateTime(LocalDateTime.now());
        teacher.setUpdateTime(LocalDateTime.now());
        courseTeacherRepository.save(teacher);

        // 执行分页查询
        Page<CourseTeacher> page = courseTeacherRepository.findByCourseId(
                courseBase.getId(),
                PageRequest.of(0, 10));

        // 验证
        assertNotNull(page);
        assertEquals(1, page.getTotalElements());
        assertEquals("测试教师", page.getContent().get(0).getName());
        assertEquals("讲师", page.getContent().get(0).getPosition());
    }

    @Test
    void testFindByOrganizationIdWithPagination() {
        // 清理已有数据
        courseTeacherRepository.deleteAll();

        // 创建测试教师
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.setCreateTime(LocalDateTime.now());
        teacher.setUpdateTime(LocalDateTime.now());
        courseTeacherRepository.save(teacher);

        // 测试分页查询
        Page<CourseTeacher> page = courseTeacherRepository.findByOrganizationId(
                TEST_ORG_ID,
                PageRequest.of(0, 10));

        // 验证
        assertEquals(1, page.getTotalElements());
        assertEquals("测试教师", page.getContent().get(0).getName());
    }
}