package com.double2and9.content_service.service;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.dto.*;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CourseBaseServiceTests {

    @Autowired
    private CourseBaseService courseBaseService;

    @Test
    @Transactional
    public void testCourseBaseFullProcess() {
        // 1. 创建课程
        AddCourseDTO addCourseDTO = new AddCourseDTO();
        addCourseDTO.setName("测试课程");
        addCourseDTO.setBrief("这是一个测试课程");
        addCourseDTO.setMt(1L);
        addCourseDTO.setSt(2L);
        addCourseDTO.setCharge("201001"); // 免费课程
        addCourseDTO.setPrice(BigDecimal.ZERO);
        addCourseDTO.setValid(true);

        Long courseId = courseBaseService.createCourse(addCourseDTO);
        assertNotNull(courseId, "课程创建失败");

        // 2. 修改课程
        EditCourseDTO editCourseDTO = new EditCourseDTO();
        editCourseDTO.setId(courseId);
        editCourseDTO.setName("修改后的课程名称");
        editCourseDTO.setBrief("修改后的课程简介");
        editCourseDTO.setMt(1L);
        editCourseDTO.setSt(2L);
        editCourseDTO.setCharge("201001");
        editCourseDTO.setPrice(BigDecimal.ZERO);

        courseBaseService.updateCourse(editCourseDTO);

        // 3. 查询课程
        PageParams pageParams = new PageParams(1L, 10L);
        QueryCourseParamsDTO queryParams = new QueryCourseParamsDTO();
        queryParams.setCourseName("修改后的课程名称");

        PageResult<CourseBaseDTO> result = courseBaseService.queryCourseList(pageParams, queryParams);
        assertNotNull(result, "查询结果不能为空");
        assertTrue(result.getCounts() > 0, "应该能查询到课程");
        assertEquals("修改后的课程名称", result.getItems().get(0).getName(), "课程名称应该已更新");
    }

    @Test
    public void testQueryCourseCategoryTree() {
        List<CourseCategoryTreeDTO> categoryTree = courseBaseService.queryCourseCategoryTree();
        assertNotNull(categoryTree, "课程分类树不能为空");
        assertFalse(categoryTree.isEmpty(), "课程分类树不能为空列表");
        
        // 验证树形结构
        categoryTree.forEach(category -> {
            assertNotNull(category.getId(), "分类ID不能为空");
            assertNotNull(category.getName(), "分类名称不能为空");
            assertEquals(0L, category.getParentId(), "顶级分类的父ID应该为0");
            
            // 如果有子节点，验证子节点
            if (category.getChildrenTreeNodes() != null && !category.getChildrenTreeNodes().isEmpty()) {
                category.getChildrenTreeNodes().forEach(child -> {
                    assertEquals(category.getId(), child.getParentId(), 
                            "子节点的父ID应该等于父节点的ID");
                });
            }
        });
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