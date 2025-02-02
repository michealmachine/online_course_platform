package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
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
class CourseBaseRepositoryTest {

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    private static final Long TEST_ORG_ID = 1234L; // 测试用机构ID

    @Test
    @Transactional
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
}