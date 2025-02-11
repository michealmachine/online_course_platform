package com.double2and9.content_service.service;

import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.base.enums.CourseAuditStatusEnum;
import com.double2and9.base.enums.CourseStatusEnum;
import com.double2and9.base.model.PageParams;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.CourseAuditDTO;
import com.double2and9.content_service.dto.SaveCourseTeacherDTO;
import com.double2and9.content_service.dto.SaveTeachplanDTO;
import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.CoursePublishPre;
import com.double2and9.content_service.entity.CourseTeacher;
import com.double2and9.content_service.entity.Teachplan;
import com.double2and9.content_service.repository.CourseBaseRepository;
import com.double2and9.content_service.repository.CourseTeacherRepository;
import com.double2and9.content_service.repository.TeachplanRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
class CourseAuditServiceTest {

    @Autowired
    private CourseAuditService courseAuditService;

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Autowired
    private TeachplanRepository teachplanRepository;

    @Autowired
    private CourseTeacherRepository courseTeacherRepository;

    @Autowired
    private TeachplanService teachplanService;

    @Autowired
    private CourseTeacherService courseTeacherService;

    private static final Long TEST_ORG_ID = 1234L;

    private Long createTestCourse() {
        // 1. 创建课程基本信息
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setBrief("测试简介");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase.setStatus(CourseStatusEnum.DRAFT.getCode());
        courseBase.setCreateTime(LocalDateTime.now());
        courseBase.setUpdateTime(LocalDateTime.now());
        courseBaseRepository.save(courseBase);

        Long courseId = courseBase.getId();

        // 2. 添加课程计划（章节）
        SaveTeachplanDTO chapterDTO = new SaveTeachplanDTO();
        chapterDTO.setCourseId(courseId);
        chapterDTO.setParentId(0L);
        chapterDTO.setLevel(1);
        chapterDTO.setName("第一章");
        chapterDTO.setOrderBy(1);
        Long chapterId = teachplanService.saveTeachplan(chapterDTO);

        // 3. 添加课程计划（小节）
        SaveTeachplanDTO sectionDTO = new SaveTeachplanDTO();
        sectionDTO.setCourseId(courseId);
        sectionDTO.setParentId(chapterId);
        sectionDTO.setLevel(2);
        sectionDTO.setName("第一节");
        sectionDTO.setOrderBy(1);
        teachplanService.saveTeachplan(sectionDTO);

        // 4. 添加教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setDescription("测试教师简介");
        Long teacherId = courseTeacherService.saveTeacher(teacherDTO);

        // 5. 关联教师到课程
        courseTeacherService.associateTeacherToCourse(TEST_ORG_ID, courseId, teacherId);

        return courseId;
    }

    @Test
    void testGetPendingAuditCourses() {
        // 1. 准备测试数据
        Long courseId = createTestCourse();
        CourseBase courseBase = courseBaseRepository.findById(courseId).orElseThrow();

        // 2. 提交审核
        courseAuditService.submitForAudit(courseId);

        // 3. 查询待审核课程
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(10L);
        var result = courseAuditService.getPendingAuditCourses(pageParams);

        // 4. 验证结果
        assertNotNull(result);
        assertTrue(result.getCounts() > 0, "应当有待审核课程");
        assertTrue(result.getItems().size() > 0, "应当有待审核课程记录");
        var firstCourse = result.getItems().get(0);
        assertEquals(CourseAuditStatusEnum.SUBMITTED.getCode(), firstCourse.getStatus());
        assertEquals(courseBase.getName(), firstCourse.getName(), "课程名称应与原课程一致");
    }

    @Test
    void testAuditCourse() {
        // 1. 准备测试数据
        Long courseId = createTestCourse();
        courseAuditService.submitForAudit(courseId);

        // 2. 审核通过
        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus(CourseAuditStatusEnum.APPROVED.getCode());
        auditDTO.setAuditorId(1L);
        courseAuditService.auditCourse(auditDTO);

        // 3. 验证结果
        CourseBase courseBase = courseBaseRepository.findById(courseId).orElseThrow();
        assertEquals(CourseAuditStatusEnum.APPROVED.getCode(), courseBase.getCoursePublishPre().getStatus());
    }

    @Test
    void testGetAuditorHistory() {
        // 1. 准备测试数据
        Long courseId = createTestCourse();
        courseAuditService.submitForAudit(courseId);

        // 2. 审核
        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus("pass");
        auditDTO.setAuditMessage("审核通过");
        courseAuditService.auditCourse(auditDTO);

        // 3. 查询审核历史
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(10L);
        var result = courseAuditService.getAuditHistory(courseId, pageParams);

        // 4. 验证结果
        assertNotNull(result);
        assertTrue(result.getCounts() > 0);
        assertTrue(result.getItems().size() > 0);
        assertEquals("审核通过", result.getItems().get(0).getAuditMessage());
    }

    @Test
    void testSubmitForAudit() {
        // 1. 准备测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setBrief("测试课程简介");
        courseBase.setStatus(CourseStatusEnum.DRAFT.getCode());
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase.setCreateTime(LocalDateTime.now());
        courseBase.setUpdateTime(LocalDateTime.now());
        courseBaseRepository.save(courseBase);

        // 2. 添加课程计划
        SaveTeachplanDTO chapterDTO = new SaveTeachplanDTO();
        chapterDTO.setCourseId(courseBase.getId());
        chapterDTO.setParentId(0L);
        chapterDTO.setLevel(1);
        chapterDTO.setName("第一章");
        chapterDTO.setOrderBy(1);
        Long chapterId = teachplanService.saveTeachplan(chapterDTO);

        SaveTeachplanDTO sectionDTO = new SaveTeachplanDTO();
        sectionDTO.setCourseId(courseBase.getId());
        sectionDTO.setParentId(chapterId);
        sectionDTO.setLevel(2);
        sectionDTO.setName("第一节");
        sectionDTO.setOrderBy(1);
        teachplanService.saveTeachplan(sectionDTO);

        // 如果有排序操作，需要保存
        teachplanService.saveOrderChanges();

        // 3. 添加教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setDescription("测试教师简介");
        Long teacherId = courseTeacherService.saveTeacher(teacherDTO);

        // 4. 关联教师到课程
        courseTeacherService.associateTeacherToCourse(TEST_ORG_ID, courseBase.getId(), teacherId);

        String courseName = courseBase.getName();

        // 5. 执行测试前先验证课程基本信息
        assertNotNull(courseBase.getName(), "课程名称不能为空");
        courseAuditService.submitForAudit(courseBase.getId());

        // 6. 验证结果
        CourseBase updatedCourse = courseBaseRepository.findById(courseBase.getId()).orElseThrow();
        CoursePublishPre publishPre = updatedCourse.getCoursePublishPre();
        assertNotNull(publishPre, "预发布记录不应为空");
        assertEquals(courseBase.getName(), publishPre.getName(), "预发布记录应包含课程名称");
        assertEquals(CourseAuditStatusEnum.SUBMITTED.getCode(), publishPre.getStatus(),
                "预发布记录状态应为已提交");

        // 7. 验证课程计划和教师关联
        List<Teachplan> teachplans = teachplanRepository.findByCourseBaseIdOrderByOrderBy(courseBase.getId());
        assertFalse(teachplans.isEmpty(), "课程计划不应为空");
        assertEquals("第一章", teachplans.get(0).getName());

        Page<CourseTeacher> teachers = courseTeacherRepository.findByCourseId(
                courseBase.getId(),
                PageRequest.of(0, 10));
        assertTrue(teachers.getTotalElements() > 0, "课程教师不应为空");
        assertEquals("测试教师", teachers.getContent().get(0).getName());
    }

    @Test
    void testInvalidStatusTransitions() {
        // 1. 创建课程并提交审核
        Long courseId = createTestCourse();

        // 2. 先验证课程状态
        CourseBase courseBase = courseBaseRepository.findById(courseId).orElseThrow();
        assertEquals(CourseStatusEnum.DRAFT.getCode(), courseBase.getStatus(), "初始状态应为草稿");

        courseAuditService.submitForAudit(courseId);

        // 3. 验证提交后状态
        courseBase = courseBaseRepository.findById(courseId).orElseThrow();
        assertNotNull(courseBase.getCoursePublishPre(), "应该有预发布记录");
        assertEquals(CourseAuditStatusEnum.SUBMITTED.getCode(),
                courseBase.getCoursePublishPre().getStatus(), "状态应为已提交");

        // 4. 验证重复提交时抛出异常
        ContentException exception = assertThrows(ContentException.class, () -> {
            courseAuditService.submitForAudit(courseId);
        });
        assertEquals(ContentErrorCode.COURSE_AUDIT_STATUS_ERROR, exception.getErrorCode());
    }

    @Test
    void testResubmitAfterRejection() {
        // 1. 创建课程并首次提交
        Long courseId = createTestCourse();
        courseAuditService.submitForAudit(courseId);

        // 2. 审核拒绝
        CourseAuditDTO rejectDTO = new CourseAuditDTO();
        rejectDTO.setCourseId(courseId);
        rejectDTO.setAuditStatus(CourseAuditStatusEnum.REJECTED.getCode());
        rejectDTO.setAuditMessage("需要修改");
        rejectDTO.setAuditorId(1L);
        courseAuditService.auditCourse(rejectDTO);

        // 3. 验证拒绝状态
        CourseBase rejectedCourse = courseBaseRepository.findById(courseId).orElseThrow();
        assertEquals(CourseAuditStatusEnum.REJECTED.getCode(),
                rejectedCourse.getCoursePublishPre().getStatus(),
                "审核状态应为已拒绝");

        // 4. 重新提交
        courseAuditService.submitForAudit(courseId);

        // 5. 验证重新提交后状态
        CourseBase updatedCourse = courseBaseRepository.findById(courseId).orElseThrow();
        CoursePublishPre publishPre = updatedCourse.getCoursePublishPre();
        assertNotNull(publishPre, "预发布记录不应为空");
        assertEquals(CourseAuditStatusEnum.SUBMITTED.getCode(), publishPre.getStatus(),
                "重新提交后状态应为已提交");
    }

    @Test
    void testCourseStatusFlow() {
        // 1. 创建课程
        Long courseId = createTestCourse();

        // 2. 提交审核
        courseAuditService.submitForAudit(courseId);
        CourseBase courseBase = courseBaseRepository.findById(courseId).orElseThrow();
        assertEquals(CourseAuditStatusEnum.SUBMITTED.getCode(),
                courseBase.getCoursePublishPre().getStatus());

        // 3. 审核通过
        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus("pass");
        courseAuditService.auditCourse(auditDTO);

        courseBase = courseBaseRepository.findById(courseId).orElseThrow();
        assertEquals(CourseAuditStatusEnum.APPROVED.getCode(),
                courseBase.getCoursePublishPre().getStatus());
    }

    @Test
    void testAuditCourseWithoutMessage() {
        // 1. 创建课程
        Long courseId = createTestCourse();
        courseAuditService.submitForAudit(courseId);

        // 2. 审核通过，不带消息
        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus(CourseAuditStatusEnum.APPROVED.getCode());
        auditDTO.setAuditorId(1L);
        courseAuditService.auditCourse(auditDTO);

        // 3. 验证结果
        CourseBase courseBase = courseBaseRepository.findById(courseId).orElseThrow();
        assertEquals(CourseAuditStatusEnum.APPROVED.getCode(),
                courseBase.getCoursePublishPre().getStatus());
    }

    @Test
    void testGetAuditHistory() {
        // 建立 课程
        Long courseId = createTestCourse();
        courseAuditService.submitForAudit(courseId);

        // 2. 审核操作
        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus("reject");
        auditDTO.setAuditMessage("内容需要完善");
        courseAuditService.auditCourse(auditDTO);

        // 3. 查询历史
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(10L);
        var result = courseAuditService.getAuditHistory(courseId, pageParams);

        // 4. 验证结果
        assertNotNull(result);
        assertTrue(result.getCounts() > 0);
        assertTrue(result.getItems().size() > 0);
        assertEquals("内容需要完善", result.getItems().get(0).getAuditMessage());
        assertEquals(CourseAuditStatusEnum.REJECTED.getCode(),
                result.getItems().get(0).getAuditStatus());
    }

    @Test
    void testAuditHistory() {
        // 1. 准备测试数据
        Long courseId = createTestCourse();
        courseAuditService.submitForAudit(courseId);

        // 2. 审核
        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus(CourseAuditStatusEnum.APPROVED.getCode());
        auditDTO.setAuditMessage("审核通过");
        auditDTO.setAuditorId(1L); // 设置审核人ID
        courseAuditService.auditCourse(auditDTO);

        // 3. 查询审核历史
        PageParams pageParams = new PageParams();
        pageParams.setPageNo(1L);
        pageParams.setPageSize(10L);
        var result = courseAuditService.getAuditHistory(courseId, pageParams);

        // 4. 验证结果
        assertNotNull(result, "审核历史不应为空");
        assertTrue(result.getCounts() > 0, "审核历史记录数应大于0");
        assertTrue(result.getItems().size() > 0, "应当有审核历史记录");
        var firstHistory = result.getItems().get(0);
        assertEquals("审核通过", firstHistory.getAuditMessage(), "审核消息应匹配");
        assertEquals(CourseAuditStatusEnum.APPROVED.getCode(), firstHistory.getAuditStatus(),
                "审核状态应匹配");
        assertNotNull(firstHistory.getAuditTime(), "审核时间不应为空");
    }
}