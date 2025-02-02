package com.double2and9.content_service.service;

import com.double2and9.content_service.dto.*;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.test.annotation.Rollback;

import java.math.BigDecimal;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

import com.double2and9.content_service.common.exception.ContentException;

@SpringBootTest
@Transactional
@Rollback
public class CourseAuditServiceTests {

    private static final Long TEST_ORG_ID = 1234L;

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
        courseDTO.setOrganizationId(TEST_ORG_ID);

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

        // 添加小节
        SaveTeachplanDTO sectionDTO = new SaveTeachplanDTO();
        sectionDTO.setCourseId(courseId);
        sectionDTO.setParentId(teachplanDTO.getId());
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
        auditDTO.setAuditStatus("202303"); // 通过
        auditDTO.setAuditMessage("课程内容符合要求");

        courseBaseService.auditCourse(auditDTO);
        status = courseBaseService.getAuditStatus(courseId);
        assertEquals("202303", status, "审核后状态应为'通过'");

        // 3. 验证CoursePublishPre记录
        CoursePreviewDTO preview = courseBaseService.preview(courseId);
        assertNotNull(preview.getCourseBase());
        assertEquals(courseId, preview.getCourseBase().getId());
        assertEquals("测试课程", preview.getCourseBase().getName());
    }

    @Test
    @Transactional
    public void testAuditReject() {
        // 1. 提交审核
        courseBaseService.submitForAudit(courseId);

        // 2. 审核不通过
        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus("202302"); // 不通过
        auditDTO.setAuditMessage("课程内容需要完善");

        courseBaseService.auditCourse(auditDTO);
        String status = courseBaseService.getAuditStatus(courseId);
        assertEquals("202302", status, "审核后状态应为'不通过'");
    }

    @Test
    @Transactional
    public void testSubmitAuditWithoutTeachplan() {
        // 删除课程计划
        List<TeachplanDTO> chapters = teachplanService.findTeachplanTree(courseId);

        // 1. 先删除所有小节
        for (TeachplanDTO chapter : chapters) {
            if (chapter.getTeachPlanTreeNodes() != null) {
                for (TeachplanDTO section : chapter.getTeachPlanTreeNodes()) {
                    if (section != null && section.getId() != null) {
                        teachplanService.deleteTeachplan(section.getId());
                    }
                }
            }
        }

        // 2. 再删除所有章节
        for (TeachplanDTO chapter : chapters) {
            if (chapter != null && chapter.getId() != null) {
                teachplanService.deleteTeachplan(chapter.getId());
            }
        }

        // 提交审核应该失败
        assertThrows(ContentException.class,
                () -> courseBaseService.submitForAudit(courseId));
    }

    @Test
    @Transactional
    public void testSubmitAuditWithoutTeacher() {
        // 删除教师信息
        List<CourseTeacherDTO> teachers = courseTeacherService.listByCourseId(courseId);
        for (CourseTeacherDTO teacher : teachers) {
            courseTeacherService.deleteCourseTeacher(courseId, teacher.getId());
        }

        // 提交审核应该失败
        assertThrows(ContentException.class,
                () -> courseBaseService.submitForAudit(courseId));
    }
}