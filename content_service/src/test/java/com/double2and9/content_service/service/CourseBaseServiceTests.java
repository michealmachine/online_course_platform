package com.double2and9.content_service.service;


import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.dto.*;
import com.double2and9.content_service.entity.CourseCategory;
import com.double2and9.content_service.repository.CourseCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CourseBaseServiceTests {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private CourseBaseService courseBaseService;

    @Autowired
    private CourseCategoryRepository courseCategoryRepository;

    @BeforeEach
    public void setUp() {
        // 创建测试用的分类数据
        CourseCategory parent = new CourseCategory();
        parent.setName("后端开发");
        parent.setParentId(0L);
        parent.setLevel(1);
        parent.setCreateTime(new Date());
        parent.setUpdateTime(new Date());
        courseCategoryRepository.save(parent);
        
        CourseCategory child = new CourseCategory();
        child.setName("Java开发");
        child.setParentId(parent.getId());
        child.setLevel(2);
        child.setCreateTime(new Date());
        child.setUpdateTime(new Date());
        courseCategoryRepository.save(child);
    }

    @Test
    public void testQueryCourseCategoryTree() {
        List<CourseCategoryTreeDTO> categoryTree = courseBaseService.queryCourseCategoryTree();
        assertNotNull(categoryTree, "课程分类树不能为空");
        assertFalse(categoryTree.isEmpty(), "课程分类树不能为空列表");
        
        // 验证树形结构
        CourseCategoryTreeDTO parent = categoryTree.get(0);
        assertEquals("后端开发", parent.getName());
        assertNotNull(parent.getChildrenTreeNodes());
        assertFalse(parent.getChildrenTreeNodes().isEmpty());
        assertEquals("Java开发", parent.getChildrenTreeNodes().get(0).getName());
    }

    @Test
    public void testCoursePreview() {
        // 创建测试课程
        AddCourseDTO courseDTO = new AddCourseDTO();
        courseDTO.setName("测试课程");
        courseDTO.setBrief("测试课程简介");
        courseDTO.setMt(1L);
        courseDTO.setSt(1L);
        courseDTO.setOrganizationId(TEST_ORG_ID);
        courseDTO.setCharge("201001");
        courseDTO.setPrice(BigDecimal.ZERO);
        courseDTO.setValid(true);

        Long courseId = courseBaseService.createCourse(courseDTO);
        
        CoursePreviewDTO preview = courseBaseService.preview(courseId);
        assertNotNull(preview);
        assertEquals(TEST_ORG_ID, preview.getCourseBase().getOrganizationId());
    }
} 