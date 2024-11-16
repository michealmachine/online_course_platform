package com.double2and9.content_service.service;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
class CourseBaseServiceTest {

    @Autowired
    private CourseBaseService courseBaseService;

    @Autowired
    private TeachplanService teachplanService;

    @Autowired
    private CourseTeacherService courseTeacherService;

    private static final Long TEST_ORG_ID = 1234L;

    private AddCourseDTO createTestCourseDTO() {
        AddCourseDTO dto = new AddCourseDTO();
        dto.setName("测试课程");
        dto.setBrief("测试课程简介");
        dto.setMt(1L);
        dto.setSt(1L);
        dto.setOrganizationId(TEST_ORG_ID);
        dto.setCharge("201001");
        dto.setPrice(BigDecimal.ZERO);
        dto.setValid(true);
        return dto;
    }

    private EditCourseDTO createTestEditCourseDTO(Long courseId) {
        EditCourseDTO dto = new EditCourseDTO();
        dto.setId(courseId);
        dto.setName("更新后的课程名称");
        dto.setBrief("更新后的简介");
        dto.setMt(1L);
        dto.setSt(1L);
        dto.setCharge("201001");
        return dto;
    }

    @Test
    @Transactional
    void testCreateCourse() {
        AddCourseDTO dto = createTestCourseDTO();
        Long courseId = courseBaseService.createCourse(dto);
        
        assertNotNull(courseId);
        
        // 验证查询结果包含机构ID
        CoursePreviewDTO preview = courseBaseService.preview(courseId);
        assertEquals(TEST_ORG_ID, preview.getCourseBase().getOrganizationId());
    }

    @Test
    @Transactional
    void testQueryCourseList() {
        // 1. 先创建一个测试课程
        AddCourseDTO addCourseDTO = createTestCourseDTO();
        Long courseId = courseBaseService.createCourse(addCourseDTO);
        assertNotNull(courseId, "课程创建失败");

        // 2. 设置查询参数
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(10L);

        QueryCourseParamsDTO queryParams = new QueryCourseParamsDTO();
        queryParams.setOrganizationId(TEST_ORG_ID);
        queryParams.setCourseName("测试课程");  // 添加课程名称条件

        // 3. 执行查询
        PageResult<CourseBaseDTO> result = courseBaseService.queryCourseList(pageParams, queryParams);
        
        // 4. 验证结果
        assertNotNull(result, "查询结果不能为null");
        assertNotNull(result.getItems(), "查询结果列表不能为null");
        assertFalse(result.getItems().isEmpty(), "查询结果不能为空");
        
        // 5. 验证查询到的课程包含我们刚创建的课程
        boolean found = result.getItems().stream()
            .anyMatch(course -> 
                course.getId().equals(courseId) &&
                course.getName().equals("测试课程")
            );
        assertTrue(found, "未找到刚创建的测试课程");
    }

    @Test
    @Transactional
    void testUpdateCourse() {
        // 先创建一个课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());
        
        // 更新课程信息
        EditCourseDTO editDTO = createTestEditCourseDTO(courseId);
        courseBaseService.updateCourse(editDTO);
        
        // 验证更新结果
        CoursePreviewDTO preview = courseBaseService.preview(courseId);
        assertEquals("更新后的课程名称", preview.getCourseBase().getName());
        assertEquals(TEST_ORG_ID, preview.getCourseBase().getOrganizationId());
    }

    @Test
    @Transactional
    void testSubmitForAudit() {
        // 1. 创建课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());
        
        // 2. 添加课程计划
        SaveTeachplanDTO chapterDTO = new SaveTeachplanDTO();
        chapterDTO.setCourseId(courseId);
        chapterDTO.setParentId(0L);
        chapterDTO.setLevel(1);
        chapterDTO.setName("第一章");
        chapterDTO.setOrderBy(1);
        teachplanService.saveTeachplan(chapterDTO);

        // 添加小节
        SaveTeachplanDTO sectionDTO = new SaveTeachplanDTO();
        sectionDTO.setCourseId(courseId);
        sectionDTO.setParentId(chapterDTO.getId());
        sectionDTO.setLevel(2);
        sectionDTO.setName("第一节");
        sectionDTO.setOrderBy(1);
        teachplanService.saveTeachplan(sectionDTO);

        // 3. 添加课程教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setDescription("测试教师简介");
        teacherDTO.setCourseIds(Set.of(courseId));
        courseTeacherService.saveCourseTeacher(teacherDTO);
        
        // 4. 提交审核
        courseBaseService.submitForAudit(courseId);
        
        // 5. 验证审核状态
        String auditStatus = courseBaseService.getAuditStatus(courseId);
        assertEquals("202301", auditStatus);
        
        // 6. 验证机构ID未被修改
        CoursePreviewDTO preview = courseBaseService.preview(courseId);
        assertEquals(TEST_ORG_ID, preview.getCourseBase().getOrganizationId());
    }
} 