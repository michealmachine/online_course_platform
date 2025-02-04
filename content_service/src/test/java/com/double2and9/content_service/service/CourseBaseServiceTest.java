package com.double2and9.content_service.service;

import com.double2and9.base.dto.CommonResponse;

import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.client.MediaFeignClient;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.*;
import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.CourseCategory;
import com.double2and9.content_service.repository.CourseBaseRepository;
import com.double2and9.content_service.repository.CourseCategoryRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.io.IOException;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;
import static org.mockito.ArgumentMatchers.argThat;

@SpringBootTest
@Transactional
@Rollback
class CourseBaseServiceTest {

    @Autowired
    private CourseBaseService courseBaseService;

    @Autowired
    private TeachplanService teachplanService;

    @Autowired
    private CourseTeacherService courseTeacherService;

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Autowired
    private CourseCategoryRepository courseCategoryRepository;

    @MockBean
    private MediaFeignClient mediaFeignClient;

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
        queryParams.setCourseName("测试课程"); // 添加课程名称条件

        // 3. 执行查询
        PageResult<CourseBaseDTO> result = courseBaseService.queryCourseList(pageParams, queryParams);

        // 4. 验证结果
        assertNotNull(result, "查询结果不能为null");
        assertNotNull(result.getItems(), "查询结果列表不能为null");
        assertFalse(result.getItems().isEmpty(), "查询结果不能为空");

        // 5. 验证查询到的课程包含我们刚创建的课程
        boolean found = result.getItems().stream()
                .anyMatch(course -> course.getId().equals(courseId) &&
                        course.getName().equals("测试课程"));
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

    @Test
    @Transactional
    void testGetCourseById() {
        // 1. 先创建一个测试课程
        AddCourseDTO addCourseDTO = createTestCourseDTO();
        Long courseId = courseBaseService.createCourse(addCourseDTO);

        // 2. 获取课程信息
        CourseBaseDTO courseBaseDTO = courseBaseService.getCourseById(courseId);

        // 3. 验证结果
        assertNotNull(courseBaseDTO);
        assertEquals(courseId, courseBaseDTO.getId());
        assertEquals(addCourseDTO.getName(), courseBaseDTO.getName());
        assertEquals(TEST_ORG_ID, courseBaseDTO.getOrganizationId());
    }

    @Test
    @Transactional
    void testGetCourseById_NotFound() {
        // 1. 使用一个不存在的课程ID
        Long nonExistentCourseId = 999999L;

        // 2. 验证抛出异常
        ContentException exception = assertThrows(ContentException.class,
                () -> courseBaseService.getCourseById(nonExistentCourseId));

        // 3. 验证异常信息
        assertEquals(ContentErrorCode.COURSE_NOT_EXISTS, exception.getErrorCode());
    }

    @Test
    @Transactional
    void testDeleteCourse() {
        // 1. 创建测试课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());

        // 2. 删除课程
        courseBaseService.deleteCourse(courseId);

        // 3. 验证课程已被删除
        ContentException exception = assertThrows(ContentException.class,
                () -> courseBaseService.getCourseById(courseId));
        assertEquals(ContentErrorCode.COURSE_NOT_EXISTS, exception.getErrorCode());
    }

    @Test
    @Transactional
    void testDeleteCourse_WhenPublished() {
        // 1. 创建课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());

        // 2. 直接修改课程状态为已发布
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));
        courseBase.setStatus("202002"); // 设置为已发布状态
        courseBaseRepository.save(courseBase);

        // 3. 尝试删除已发布的课程
        ContentException exception = assertThrows(ContentException.class,
                () -> courseBaseService.deleteCourse(courseId));
        assertEquals(ContentErrorCode.COURSE_STATUS_ERROR, exception.getErrorCode());
    }

    @Test
    @Transactional
    void testAuditCourse() {
        // 1. 创建课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());
        prepareForAudit(courseId); // 添加必要的课程计划和教师

        // 2. 提交审核
        courseBaseService.submitForAudit(courseId);

        // 3. 验证提交审核后的状态
        String preAuditStatus = courseBaseService.getAuditStatus(courseId);
        assertEquals("202301", preAuditStatus, "提交审核后状态应为'已提交'");

        // 4. 审核通过
        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus("202303"); // 审核通过
        auditDTO.setAuditMessage("审核通过");
        courseBaseService.auditCourse(auditDTO);

        // 5. 验证审核后的状态
        CourseBaseDTO courseBaseDTO = courseBaseService.getCourseById(courseId);
        assertEquals("202001", courseBaseDTO.getStatus(), "审核通过后课程状态应为'未发布'");

        String postAuditStatus = courseBaseService.getAuditStatus(courseId);
        assertEquals("202303", postAuditStatus, "审核状态应为'审核通过'");

        // 6. 发布课程
        courseBaseService.publishCourse(courseId);

        // 7. 验证发布后的状态
        courseBaseDTO = courseBaseService.getCourseById(courseId);
        assertEquals("202002", courseBaseDTO.getStatus(), "发布后课程状态应为'已发布'");
    }

    @Test
    @Transactional
    void testAuditCourse_Reject() {
        // 1. 创建并提交审核
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());
        prepareForAudit(courseId); // 添加必要的课程计划和教师
        courseBaseService.submitForAudit(courseId);

        // 2. 审核拒绝
        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus("202304"); // 审核拒绝
        auditDTO.setAuditMessage("审核拒绝");
        courseBaseService.auditCourse(auditDTO);

        // 3. 验证状态
        CourseBaseDTO courseBaseDTO = courseBaseService.getCourseById(courseId);
        assertEquals("202001", courseBaseDTO.getStatus(), "审核拒绝后课程状态应为'未发布'");

        String auditStatus = courseBaseService.getAuditStatus(courseId);
        assertEquals("202304", auditStatus, "审核状态应为'审核拒绝'");
    }

    @Test
    @Transactional
    void testOfflineCourse() {
        // 1. 创建课程并发布
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());
        prepareForAudit(courseId);
        courseBaseService.submitForAudit(courseId);

        CourseAuditDTO auditDTO = new CourseAuditDTO();
        auditDTO.setCourseId(courseId);
        auditDTO.setAuditStatus("202303");
        courseBaseService.auditCourse(auditDTO);

        courseBaseService.publishCourse(courseId);

        // 2. 下架课程
        courseBaseService.offlineCourse(courseId);

        // 3. 验证状态
        CourseBaseDTO courseBaseDTO = courseBaseService.getCourseById(courseId);
        assertEquals("202003", courseBaseDTO.getStatus(), "课程状态应为'已下架'");
    }

    @Test
    @Transactional
    void testOfflineCourse_WhenNotPublished() {
        // 1. 创建课程（未发布状态）
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());

        // 2. 尝试下架未发布的课程
        ContentException exception = assertThrows(ContentException.class,
                () -> courseBaseService.offlineCourse(courseId));
        assertEquals(ContentErrorCode.COURSE_STATUS_ERROR, exception.getErrorCode());
    }

    @Test
    @Transactional
    void testUploadCourseLogo_TwoPhase() throws IOException {
        // 1. 创建测试课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());

        // 2. 准备测试文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test".getBytes());

        // 3. Mock临时上传响应
        when(mediaFeignClient.uploadImageTemp(any()))
                .thenReturn(CommonResponse.success("temp-key-123"));

        // 4. Mock永久保存响应
        MediaFileDTO mediaFileDTO = new MediaFileDTO();
        mediaFileDTO.setMediaFileId("test-file-id");
        mediaFileDTO.setUrl("/test/url");
        when(mediaFeignClient.saveTempFile(any()))
                .thenReturn(CommonResponse.success(mediaFileDTO));

        // 5. 执行临时上传
        String tempKey = courseBaseService.uploadCourseLogoTemp(courseId, file);
        assertEquals("temp-key-123", tempKey);

        // 6. 执行确认保存
        courseBaseService.confirmCourseLogo(courseId, tempKey);

        // 7. 验证调用和结果
        verify(mediaFeignClient).uploadImageTemp(any());
        verify(mediaFeignClient).saveTempFile(argThat(params -> "temp-key-123".equals(params.get("tempKey"))));

        CourseBase updatedCourse = courseBaseRepository.findById(courseId).orElseThrow();
        assertEquals("/test/url", updatedCourse.getLogo());
    }

    @Test
    @Transactional
    void testUploadCourseLogo_TempUploadFailed() {
        // 1. 创建测试课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());

        // 2. 准备测试文件
        MockMultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test".getBytes());

        // 3. Mock临时上传失败
        when(mediaFeignClient.uploadImageTemp(any()))
                .thenReturn(CommonResponse.error("500", "上传失败"));

        // 4. 验证异常
        ContentException exception = assertThrows(ContentException.class,
                () -> courseBaseService.uploadCourseLogoTemp(courseId, file));
        assertEquals(ContentErrorCode.UPLOAD_LOGO_FAILED, exception.getErrorCode());
    }

    @Test
    @Transactional
    void testUploadCourseLogo_ConfirmFailed() {
        // 1. 创建测试课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());

        // 2. Mock永久保存失败
        when(mediaFeignClient.saveTempFile(any()))
                .thenReturn(CommonResponse.error("500", "保存失败"));

        // 3. 验证异常
        ContentException exception = assertThrows(ContentException.class,
                () -> courseBaseService.confirmCourseLogo(courseId, "temp-key-123"));
        assertEquals(ContentErrorCode.UPLOAD_LOGO_FAILED, exception.getErrorCode());
    }

    @Test
    @Transactional
    void testDeleteCourseLogo() {
        // 1. 先创建一个课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());

        // 2. 设置logo
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));
        String logoUrl = "/test/url";
        courseBase.setLogo(logoUrl);
        courseBaseRepository.save(courseBase);

        // 3. Mock媒体服务响应
        when(mediaFeignClient.deleteMediaFile(logoUrl))
                .thenReturn(CommonResponse.success(null));

        // 4. 执行测试
        courseBaseService.deleteCourseLogo(courseId);

        // 5. 验证结果
        courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));
        assertNull(courseBase.getLogo());
    }

    @Test
    @Transactional
    void testDeleteCourse_WithLogo() {
        // 1. 创建带logo的课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());

        // 直接设置logo URL
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));
        courseBase.setLogo("http://test.com/logo.jpg");
        courseBaseRepository.save(courseBase);

        // 2. 模拟媒体服务删除成功
        when(mediaFeignClient.deleteMediaFile(courseBase.getLogo()))
                .thenReturn(CommonResponse.success(null));

        // 3. 删除课程
        courseBaseService.deleteCourse(courseId);

        // 4. 验证课程已删除
        assertFalse(courseBaseRepository.findById(courseId).isPresent(), "课程应该已被删除");
    }

    @Test
    @Transactional
    void testDeleteCourse_WithLogoDeleteError() {
        // 1. 创建带logo的课程
        Long courseId = courseBaseService.createCourse(createTestCourseDTO());

        // 直接设置logo URL
        CourseBase courseBase = courseBaseRepository.findById(courseId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));
        courseBase.setLogo("http://test.com/logo.jpg");
        courseBaseRepository.save(courseBase);

        // 2. 模拟媒体服务删除失败
        when(mediaFeignClient.deleteMediaFile(courseBase.getLogo()))
                .thenReturn(CommonResponse.error("500", "删除失败"));

        // 3. 删除课程 - 应该成功，即使logo删除失败
        courseBaseService.deleteCourse(courseId);

        // 4. 验证课程已删除
        assertFalse(courseBaseRepository.findById(courseId).isPresent(), "课程应该已被删除");
    }

    @Test
    void testQueryCourseCategoryTree() {
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

        // 执行测试
        List<CourseCategoryTreeDTO> categoryTree = courseBaseService.queryCourseCategoryTree();

        // 验证结果
        assertNotNull(categoryTree);
        assertFalse(categoryTree.isEmpty());
        CourseCategoryTreeDTO parentNode = categoryTree.get(0);
        assertEquals("后端开发", parentNode.getName());
        assertNotNull(parentNode.getChildrenTreeNodes());
        assertFalse(parentNode.getChildrenTreeNodes().isEmpty());
        assertEquals("Java开发", parentNode.getChildrenTreeNodes().get(0).getName());
    }

    // 辅助方法：准备审核所需的课程计划和教师
    private void prepareForAudit(Long courseId) {
        // 添加课程计划
        SaveTeachplanDTO chapterDTO = new SaveTeachplanDTO();
        chapterDTO.setCourseId(courseId);
        chapterDTO.setParentId(0L);
        chapterDTO.setLevel(1);
        chapterDTO.setName("第一章");
        chapterDTO.setOrderBy(1);
        teachplanService.saveTeachplan(chapterDTO);

        SaveTeachplanDTO sectionDTO = new SaveTeachplanDTO();
        sectionDTO.setCourseId(courseId);
        sectionDTO.setParentId(chapterDTO.getId());
        sectionDTO.setLevel(2);
        sectionDTO.setName("第一节");
        sectionDTO.setOrderBy(1);
        teachplanService.saveTeachplan(sectionDTO);

        // 添加教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setDescription("测试教师简介");
        teacherDTO.setCourseIds(Set.of(courseId));
        courseTeacherService.saveCourseTeacher(teacherDTO);
    }
}