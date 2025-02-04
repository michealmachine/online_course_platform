package com.double2and9.content_service.service;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.client.MediaFeignClient;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.repository.CourseBaseRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.*;

/**
 * 课程封面管理测试类
 * 测试课程封面的上传、确认和删除功能
 * 包括正常流程和异常情况的测试
 */
@SpringBootTest
@Transactional
@Rollback
class CourseLogoTest {

        @Autowired
        private CourseBaseService courseBaseService;

        @Autowired
        private CourseBaseRepository courseBaseRepository;

        @MockBean
        private MediaFeignClient mediaFeignClient;

        private static final Long TEST_ORG_ID = 1234L;
        private CourseBase testCourse;
        private MockMultipartFile testFile;

        @BeforeEach
        void setUp() {
                // 准备测试课程
                testCourse = new CourseBase();
                testCourse.setName("测试课程");
                testCourse.setOrganizationId(TEST_ORG_ID);
                testCourse = courseBaseRepository.save(testCourse);

                // 准备测试文件
                testFile = new MockMultipartFile(
                                "file",
                                "test.jpg",
                                "image/jpeg",
                                "test image content".getBytes());
        }

        /**
         * 测试两步式上传的完整流程
         * 1. 上传到临时存储
         * 2. 确认并保存到永久存储
         */
        @Test
        void testUploadCourseLogo_TwoPhase() {
                // 1. Mock临时上传响应
                when(mediaFeignClient.uploadImageTemp(any()))
                                .thenReturn(CommonResponse.success("temp-key-123"));

                // 2. Mock永久保存响应
                MediaFileDTO mediaFileDTO = new MediaFileDTO();
                mediaFileDTO.setMediaFileId("test-file-id");
                mediaFileDTO.setUrl("/test/url");
                when(mediaFeignClient.saveTempFile(any()))
                                .thenReturn(CommonResponse.success(mediaFileDTO));

                // 3. 执行临时上传
                String tempKey = courseBaseService.uploadCourseLogoTemp(testCourse.getId(), testFile);
                assertEquals("temp-key-123", tempKey);

                // 4. 执行确认保存
                courseBaseService.confirmCourseLogo(testCourse.getId(), tempKey);

                // 5. 验证调用
                verify(mediaFeignClient).uploadImageTemp(any());
                verify(mediaFeignClient).saveTempFile(argThat(params -> "temp-key-123".equals(params.get("tempKey"))));

                // 6. 验证课程封面已更新
                CourseBase updatedCourse = courseBaseRepository.findById(testCourse.getId()).orElseThrow();
                assertEquals("/test/url", updatedCourse.getLogo());
        }

        @Test
        void testDeleteCourseLogo() {
                // 设置初始logo
                testCourse.setLogo("/test/url");
                courseBaseRepository.save(testCourse);

                // 模拟删除响应
                when(mediaFeignClient.deleteMediaFile(any()))
                                .thenReturn(CommonResponse.success(null));

                // 执行删除
                courseBaseService.deleteCourseLogo(testCourse.getId());

                // 验证调用
                verify(mediaFeignClient).deleteMediaFile("/test/url");

                // 验证logo已清除
                CourseBase updatedCourse = courseBaseRepository.findById(testCourse.getId()).orElseThrow();
                assertNull(updatedCourse.getLogo());
        }

        /**
         * 测试临时上传失败的情况
         * 验证异常抛出和错误信息
         */
        @Test
        void testUploadCourseLogo_TempUploadFailed() {
                // 1. Mock临时上传失败
                when(mediaFeignClient.uploadImageTemp(any()))
                                .thenReturn(CommonResponse.error("500", "临时上传失败"));

                // 2. 验证异常
                ContentException exception = assertThrows(ContentException.class,
                                () -> courseBaseService.uploadCourseLogoTemp(testCourse.getId(), testFile));
                assertEquals(ContentErrorCode.UPLOAD_LOGO_FAILED, exception.getErrorCode());
                assertTrue(exception.getMessage().contains("临时上传失败"));
        }

        /**
         * 测试确认保存失败的情况
         * 验证异常抛出和错误信息
         */
        @Test
        void testUploadCourseLogo_ConfirmFailed() {
                // 1. Mock永久保存失败
                when(mediaFeignClient.saveTempFile(any()))
                                .thenReturn(CommonResponse.error("500", "永久保存失败"));

                // 2. 验证异常
                ContentException exception = assertThrows(ContentException.class,
                                () -> courseBaseService.confirmCourseLogo(testCourse.getId(), "temp-key-123"));
                assertEquals(ContentErrorCode.UPLOAD_LOGO_FAILED, exception.getErrorCode());
                assertTrue(exception.getMessage().contains("永久保存失败"));
        }

        @Test
        void testDeleteCourseLogo_DeleteFailed() {
                // 1. 设置初始logo
                testCourse.setLogo("/test/url");
                courseBaseRepository.save(testCourse);

                // 2. Mock删除失败
                when(mediaFeignClient.deleteMediaFile(any()))
                                .thenReturn(CommonResponse.error("500", "删除失败"));

                // 3. 验证异常
                ContentException exception = assertThrows(ContentException.class,
                                () -> courseBaseService.deleteCourseLogo(testCourse.getId()));
                assertEquals(ContentErrorCode.DELETE_LOGO_FAILED, exception.getErrorCode());
                assertTrue(exception.getMessage().contains("删除失败"));

                // 4. 验证logo未被清除
                CourseBase updatedCourse = courseBaseRepository.findById(testCourse.getId()).orElseThrow();
                assertEquals("/test/url", updatedCourse.getLogo());
        }

        @Test
        void testUploadCourseLogo_CourseNotExists() {
                // 1. 使用不存在的课程ID
                Long nonExistentCourseId = 999999L;

                // 2. 验证临时上传异常
                ContentException exception = assertThrows(ContentException.class,
                                () -> courseBaseService.uploadCourseLogoTemp(nonExistentCourseId, testFile));
                assertEquals(ContentErrorCode.COURSE_NOT_EXISTS, exception.getErrorCode());

                // 3. 验证确认保存异常
                exception = assertThrows(ContentException.class,
                                () -> courseBaseService.confirmCourseLogo(nonExistentCourseId, "temp-key-123"));
                assertEquals(ContentErrorCode.COURSE_NOT_EXISTS, exception.getErrorCode());
        }

        @Test
        void testDeleteCourseLogo_CourseNotExists() {
                // 1. 使用不存在的课程ID
                Long nonExistentCourseId = 999999L;

                // 2. 验证异常
                ContentException exception = assertThrows(ContentException.class,
                                () -> courseBaseService.deleteCourseLogo(nonExistentCourseId));
                assertEquals(ContentErrorCode.COURSE_NOT_EXISTS, exception.getErrorCode());
        }
}