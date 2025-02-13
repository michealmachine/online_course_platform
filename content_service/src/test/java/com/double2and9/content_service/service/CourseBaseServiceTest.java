package com.double2and9.content_service.service;

import com.double2and9.base.dto.CommonResponse;

import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.enums.CourseStatusEnum;
import com.double2and9.base.enums.CourseAuditStatusEnum;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.client.MediaFeignClient;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.*;
import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.CourseCategory;
import com.double2and9.content_service.entity.CoursePublish;
import com.double2and9.content_service.entity.CoursePublishPre;
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
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Optional;
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
                // 1. 准备测试数据
                CourseBase courseBase = new CourseBase();
                courseBase.setName("测试课程");
                courseBase.setBrief("测试简介");
                courseBase.setStatus(CourseStatusEnum.DRAFT.getCode());
                courseBase.setOrganizationId(TEST_ORG_ID);
                courseBaseRepository.save(courseBase);

                // 2. 添加预发布记录
                CoursePublishPre publishPre = new CoursePublishPre();
                publishPre.setId(courseBase.getId());
                publishPre.setCourseBase(courseBase);
                publishPre.setStatus(CourseAuditStatusEnum.APPROVED.getCode());
                publishPre.setName(courseBase.getName());
                courseBase.setCoursePublishPre(publishPre);

                // 3. 添加发布记录
                CoursePublish coursePublish = new CoursePublish();
                coursePublish.setId(courseBase.getId());
                coursePublish.setCourseBase(courseBase);
                coursePublish.setStatus(CourseStatusEnum.PUBLISHED.getCode());
                coursePublish.setName(courseBase.getName());
                courseBase.setCoursePublish(coursePublish);

                courseBaseRepository.save(courseBase);

                // 4. 执行删除
                courseBaseService.deleteCourse(courseBase.getId());

                // 5. 验证结果
                Optional<CourseBase> deletedCourse = courseBaseRepository.findById(courseBase.getId());
                assertFalse(deletedCourse.isPresent(), "课程应该被删除");
        }

        @Test
        @Transactional
        void testDeletePublishedCourse() {
                // 1. 准备已发布的课程
                CourseBase courseBase = new CourseBase();
                courseBase.setName("已发布课程");
                courseBase.setBrief("测试简介");
                courseBase.setStatus(CourseStatusEnum.PUBLISHED.getCode());
                courseBase.setOrganizationId(TEST_ORG_ID);
                courseBaseRepository.save(courseBase);

                // 2. 验证删除失败
                ContentException exception = assertThrows(ContentException.class, 
                                () -> courseBaseService.deleteCourse(courseBase.getId()));
                assertEquals(ContentErrorCode.COURSE_STATUS_ERROR, exception.getErrorCode());
        }

        @Test
        @Transactional
        void testDeleteCourseUnderReview() {
                // 1. 准备审核中的课程
                CourseBase courseBase = new CourseBase();
                courseBase.setName("审核中课程");
                courseBase.setBrief("测试简介");
                courseBase.setStatus(CourseStatusEnum.DRAFT.getCode());
                courseBase.setOrganizationId(TEST_ORG_ID);
                courseBaseRepository.save(courseBase);

                // 2. 添加审核中的预发布记录
                CoursePublishPre publishPre = new CoursePublishPre();
                publishPre.setId(courseBase.getId());
                publishPre.setCourseBase(courseBase);
                publishPre.setStatus(CourseAuditStatusEnum.SUBMITTED.getCode());
                publishPre.setName(courseBase.getName());
                courseBase.setCoursePublishPre(publishPre);

                courseBaseRepository.save(courseBase);

                // 3. 验证删除失败
                ContentException exception = assertThrows(ContentException.class, 
                                () -> courseBaseService.deleteCourse(courseBase.getId()));
                assertEquals(ContentErrorCode.COURSE_STATUS_ERROR, exception.getErrorCode());
        }

        @Test
        @Transactional
        void testDeleteCourseWithInvalidLogo() {
                // 1. 准备带无效logo的课程
                CourseBase courseBase = new CourseBase();
                courseBase.setName("测试课程");
                courseBase.setBrief("测试简介");
                courseBase.setStatus(CourseStatusEnum.DRAFT.getCode());
                courseBase.setLogo("invalid/path/logo.jpg");
                courseBase.setOrganizationId(TEST_ORG_ID);
                courseBaseRepository.save(courseBase);

                // 2. 执行删除
                courseBaseService.deleteCourse(courseBase.getId());

                // 3. 验证课程被删除
                Optional<CourseBase> deletedCourse = courseBaseRepository.findById(courseBase.getId());
                assertFalse(deletedCourse.isPresent(), "即使logo删除失败，课程也应该被删除");
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
        @Transactional
        void testQueryCourseCategoryTree() {
                // 1. 准备测试数据
                // 创建父分类
                CourseCategory parent = new CourseCategory();
                parent.setName("后端开发");
                parent.setParentId(0L);
                parent.setLevel(1);
                parent.setCreateTime(LocalDateTime.now());
                parent.setUpdateTime(LocalDateTime.now());
                courseCategoryRepository.save(parent);

                // 创建子分类1
                CourseCategory child1 = new CourseCategory();
                child1.setName("Java开发");
                child1.setParentId(parent.getId());
                child1.setLevel(2);
                child1.setCreateTime(LocalDateTime.now());
                child1.setUpdateTime(LocalDateTime.now());
                courseCategoryRepository.save(child1);

                // 创建子分类2
                CourseCategory child2 = new CourseCategory();
                child2.setName("Python开发");
                child2.setParentId(parent.getId());
                child2.setLevel(2);
                child2.setCreateTime(LocalDateTime.now());
                child2.setUpdateTime(LocalDateTime.now());
                courseCategoryRepository.save(child2);

                // 2. 获取课程分类树
                List<CourseCategoryTreeDTO> categoryTree = courseBaseService.queryCourseCategoryTree();

                // 3. 验证根节点
                assertNotNull(categoryTree);
                assertFalse(categoryTree.isEmpty());

                // 4. 验证子节点
                CourseCategoryTreeDTO parentNode = categoryTree.get(0);
                assertNotNull(parentNode.getChildren());
                assertFalse(parentNode.getChildren().isEmpty());
                assertEquals(2, parentNode.getChildren().size());

                // 5. 验证节点内容
                assertEquals("后端开发", parentNode.getName());
                List<CourseCategoryTreeDTO> children = parentNode.getChildren();
                assertTrue(children.stream().anyMatch(child -> "Java开发".equals(child.getName())));
                assertTrue(children.stream().anyMatch(child -> "Python开发".equals(child.getName())));
                
                // 6. 验证层级
                children.forEach(child -> {
                        assertEquals(2, child.getLevel());
                        assertEquals(parent.getId(), child.getParentId());
                });
        }

        @Test
        @Transactional
        void testPreview() {
                // ... 保持不变
        }

        @Test
        void testSubmitForAudit() {
                CourseBase courseBase = new CourseBase();
                courseBase.setName("测试课程");
                courseBase.setBrief("测试课程简介");
                courseBase.setStatus(CourseStatusEnum.DRAFT.getCode());
                courseBase.setOrganizationId(TEST_ORG_ID);
                courseBase.setCreateTime(LocalDateTime.now());
                courseBase.setUpdateTime(LocalDateTime.now());
                courseBaseRepository.save(courseBase);

                // ... 其他代码保持不变
        }

        @Test
        @Transactional
        void testDeleteCourse_WithTeacherAssociation() {
                // 1. 创建课程
                Long courseId = courseBaseService.createCourse(createTestCourseDTO());

                // 2. 创建并关联教师
                SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
                teacherDTO.setOrganizationId(TEST_ORG_ID);
                teacherDTO.setName("测试教师");
                teacherDTO.setPosition("讲师");
                Long teacherId = courseTeacherService.saveTeacher(teacherDTO);
                courseTeacherService.associateTeacherToCourse(TEST_ORG_ID, courseId, teacherId);

                // 3. 尝试普通删除，应该抛出异常
                ContentException exception = assertThrows(ContentException.class,
                        () -> courseBaseService.deleteCourse(courseId));
                assertEquals(ContentErrorCode.COURSE_HAS_TEACHER, exception.getErrorCode());

                // 4. 使用强制删除
                courseBaseService.deleteCourseWithRelations(courseId, true);

                // 5. 验证课程和关联都已被删除
                assertFalse(courseBaseRepository.findById(courseId).isPresent(), "课程应该已被删除");
                
                // 验证教师与课程的关联已解除
                List<CourseTeacherDTO> teachers = courseTeacherService.listByCourseId(courseId);
                assertTrue(teachers.isEmpty(), "课程的教师关联应该已被解除");
        }

        @Test
        @Transactional
        void testDeleteCourseWithRelations_WhenPublished() {
                // 1. 创建课程
                Long courseId = courseBaseService.createCourse(createTestCourseDTO());

                // 2. 设置课程为已发布状态
                CourseBase courseBase = courseBaseRepository.findById(courseId)
                        .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));
                courseBase.setStatus(CourseStatusEnum.PUBLISHED.getCode());
                courseBaseRepository.save(courseBase);

                // 3. 尝试强制删除，应该抛出异常
                ContentException exception = assertThrows(ContentException.class,
                        () -> courseBaseService.deleteCourseWithRelations(courseId, true));
                assertEquals(ContentErrorCode.COURSE_STATUS_ERROR, exception.getErrorCode());
        }

}