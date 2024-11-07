package com.double2and9.content_service.service;

import com.double2and9.content_service.dto.AddCourseDTO;
import com.double2and9.content_service.dto.CourseAuditDTO;
import com.double2and9.content_service.dto.SaveTeachplanDTO;
import com.double2and9.content_service.dto.SaveCourseTeacherDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CourseAuditServiceTests {

    @Autowired
    private CourseBaseService courseBaseService;
    
    @Autowired
    private TeachplanService teachplanService;
    
    @Autowired
    private CourseTeacherService courseTeacherService;
    
    private Long courseId;

    @BeforeEach
    @Transactional
    public void setUp() {
        // 1. 创建课程
        AddCourseDTO courseDTO = new AddCourseDTO();
        courseDTO.setName("测试课程");
        courseDTO.setBrief("这是一个测试课程");
        courseDTO.setMt(1L);
        courseDTO.setSt(2L);
        courseDTO.setCharge("201001");
        courseDTO.setPrice(BigDecimal.ZERO);
        courseDTO.setValid(true);

        courseId = courseBaseService.createCourse(courseDTO);
        assertNotNull(courseId, "课程创建失败");

        // 2. 添加课程计划
        SaveTeachplanDTO teachplanDTO = new SaveTeachplanDTO();
        teachplanDTO.setCourseId(courseId);
        teachplanDTO.setParentId(0L);
        teachplanDTO.setLevel(1);
        teachplanDTO.setName("第一章");
        teachplanDTO.setOrderBy(1);
        teachplanService.saveTeachplan(teachplanDTO);

        // 3. 添加课程教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setCourseId(courseId);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setDescription("测试教师简介");
        courseTeacherService.saveCourseTeacher(teacherDTO);
    }

    @Test
    @Transactional
    public void testAuditProcess() {
        // 1. 提交审核
        courseBaseService.submitForAudit(courseId);
        String status = courseBaseService.getAuditStatus(courseId);
        assertEquals("202301", status, "提交审核后状态应为'已提交'");

        // 2. 审核通过
        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus("202303");  // 通过
        auditDTO.setAuditMind("课程内容符合要求");
        
        courseBaseService.auditCourse(auditDTO);
        status = courseBaseService.getAuditStatus(courseId);
        assertEquals("202303", status, "审核后状态应为'通过'");

        // 3. 审核不通过
        auditDTO.setAuditStatus("202304");  // 不通过
        auditDTO.setAuditMind("课程内容需要完善");
        
        courseBaseService.auditCourse(auditDTO);
        status = courseBaseService.getAuditStatus(courseId);
        assertEquals("202304", status, "审核后状态应为'不通过'");
    }

    @Test
    @Transactional
    public void testSubmitAuditWithoutTeacher() {
        // 删除教师信息
        courseTeacherService.listByCourseId(courseId)
            .forEach(teacher -> courseTeacherService.deleteCourseTeacher(courseId, teacher.getId()));

        // 提交审核应该失败
        assertThrows(RuntimeException.class, () -> courseBaseService.submitForAudit(courseId),
            "没有教师信息时不应该能提交审核");
    }

    @Test
    @Transactional
    public void testSubmitAuditWithoutTeachplan() {
        // 删除课程计划
        teachplanService.findTeachplanTree(courseId)
            .forEach(chapter -> teachplanService.deleteTeachplan(chapter.getId()));

        // 提交审核应该失败
        assertThrows(RuntimeException.class, () -> courseBaseService.submitForAudit(courseId),
            "没有课程计划时不应该能提交审核");
    }
} 