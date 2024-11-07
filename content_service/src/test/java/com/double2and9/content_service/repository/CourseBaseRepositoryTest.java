package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
public class CourseBaseRepositoryTest {

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Test
    void testSaveCourseBase() {
        // 创建测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setBrief("这是一个测试课程");
        courseBase.setLogo("test.jpg");
        courseBase.setMt(1L);
        courseBase.setSt(1L);
        courseBase.setPrice(new BigDecimal("99.99"));
        courseBase.setPriceOld(new BigDecimal("199.99"));
        courseBase.setCharge("201001");  // 假设这是收费课程的代码
        courseBase.setValid(true);
        courseBase.setQq("12345678");
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());

        // 保存
        CourseBase saved = courseBaseRepository.save(courseBase);
        
        // 验证
        assertNotNull(saved.getId());
        assertEquals("测试课程", saved.getName());
        assertTrue(saved.getValid());
    }

    @Test
    void testFindByNameContaining() {
        // 创建测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("Java高级课程");
        courseBase.setValid(true);
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        courseBaseRepository.save(courseBase);

        // 测试查询
        List<CourseBase> courses = courseBaseRepository.findByNameContaining("Java");
        
        // 验证
        assertFalse(courses.isEmpty());
        assertTrue(courses.stream().anyMatch(course -> 
            course.getName().equals("Java高级课程")));
    }

    @Test
    void testFindByMtAndSt() {
        // 创建测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setMt(1L);
        courseBase.setSt(2L);
        courseBase.setValid(true);
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        courseBaseRepository.save(courseBase);

        // 测试查询
        List<CourseBase> courses = courseBaseRepository.findByMtAndSt(1L, 2L);
        
        // 验证
        assertFalse(courses.isEmpty());
        CourseBase found = courses.get(0);
        assertEquals(1L, found.getMt());
        assertEquals(2L, found.getSt());
    }

    @Test
    void testFindLatestCoursesByMt() {
        // 创建测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("最新测试课程");
        courseBase.setMt(1L);
        courseBase.setValid(true);
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        courseBaseRepository.save(courseBase);

        // 测试查询
        List<CourseBase> courses = courseBaseRepository.findLatestCoursesByMt(1L);
        
        // 验证
        assertFalse(courses.isEmpty());
        assertTrue(courses.stream().anyMatch(course -> 
            course.getName().equals("最新测试课程")));
    }
} 