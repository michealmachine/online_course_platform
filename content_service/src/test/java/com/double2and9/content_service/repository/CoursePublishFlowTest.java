package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.CoursePublish;
import com.double2and9.content_service.entity.CoursePublishPre;
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
public class CoursePublishFlowTest {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Autowired
    private CoursePublishPreRepository coursePublishPreRepository;

    @Autowired
    private CoursePublishRepository coursePublishRepository;

    @Test
    void testCoursePublishFlow() {
        // 1. 创建课程基本信息
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setBrief("测试课程简介");
        courseBase.setMt(1L);
        courseBase.setSt(1L);
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase.setCharge("201001");
        courseBase.setValid(true);
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        courseBase = courseBaseRepository.save(courseBase);

        assertNotNull(courseBase.getId(), "CourseBase ID should not be null");

        // 2. 提交审核，创建预发布记录
        CoursePublishPre publishPre = new CoursePublishPre();
        publishPre.setCourseBase(courseBase);
        publishPre.setName("测试课程预发布");
        publishPre.setStatus("审核中");
        publishPre.setPreviewTime(new Date());
        publishPre.setCreateTime(new Date());
        publishPre.setUpdateTime(new Date());
        
        CoursePublishPre savedPre = coursePublishPreRepository.save(publishPre);
        assertNotNull(savedPre);
        assertEquals(courseBase.getId(), savedPre.getId());
        assertEquals("审核中", savedPre.getStatus());

        // 3. 审核通过，创建发布记录
        CoursePublish coursePublish = new CoursePublish();
        coursePublish.setCourseBase(courseBase);
        coursePublish.setName("测试课程发布");
        coursePublish.setStatus("已发布");
        coursePublish.setPublishTime(new Date());
        coursePublish.setCreateTime(new Date());
        coursePublish.setUpdateTime(new Date());
        
        CoursePublish savedPublish = coursePublishRepository.save(coursePublish);
        assertNotNull(savedPublish);
        assertEquals(courseBase.getId(), savedPublish.getId());
        assertEquals("已发布", savedPublish.getStatus());

        // 4. 验证查询功能
        List<CoursePublishPre> preList = coursePublishPreRepository.findByStatus("审核中");
        assertFalse(preList.isEmpty());
        assertEquals(courseBase.getId(), preList.get(0).getId());

        List<CoursePublish> publishList = coursePublishRepository.findByStatus("已发布");
        assertFalse(publishList.isEmpty());
        assertEquals(courseBase.getId(), publishList.get(0).getId());
    }

    @Test
    void testCoursePublishFlowWithRejection() {
        // 1. 创建课程基本信息
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setBrief("测试课程简介");
        courseBase.setMt(1L);
        courseBase.setSt(1L);
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase.setCharge("201001");
        courseBase.setValid(true);
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        courseBase = courseBaseRepository.save(courseBase);

        // 2. 提交审核
        CoursePublishPre publishPre = new CoursePublishPre();
        publishPre.setCourseBase(courseBase);
        publishPre.setName("测试课程预发布");
        publishPre.setStatus("审核中");
        publishPre.setPreviewTime(new Date());
        publishPre.setCreateTime(new Date());
        publishPre.setUpdateTime(new Date());
        
        CoursePublishPre savedPre = coursePublishPreRepository.save(publishPre);
        assertNotNull(savedPre);

        // 3. 审核不通过
        savedPre.setStatus("审核不通过");
        savedPre = coursePublishPreRepository.save(savedPre);
        assertEquals("审核不通过", savedPre.getStatus());

        // 4. 验证没有创建发布记录
        List<CoursePublish> publishList = coursePublishRepository.findByStatus("已发布");
        assertTrue(publishList.isEmpty());
    }
} 