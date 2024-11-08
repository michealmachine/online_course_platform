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

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CourseBaseServiceTests {

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
        // 假设数据库中已有课程数据
        PageParams pageParams = new PageParams(1L, 1L);
        QueryCourseParamsDTO queryParams = new QueryCourseParamsDTO();
        PageResult<CourseBaseDTO> courses = courseBaseService.queryCourseList(pageParams, queryParams);
        
        if (courses.getCounts() > 0) {
            Long courseId = courses.getItems().get(0).getId();
            CoursePreviewDTO preview = courseBaseService.preview(courseId);
            
            assertNotNull(preview, "预览数据不能为空");
            assertNotNull(preview.getCourseBase(), "课程基本信息不能为空");
            assertEquals(courseId, preview.getCourseBase().getId(), "课程ID应该匹配");
        }
    }
} 