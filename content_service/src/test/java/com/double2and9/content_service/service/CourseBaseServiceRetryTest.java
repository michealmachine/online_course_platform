package com.double2and9.content_service.service;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.client.MediaFeignClient;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.repository.CourseBaseRepository;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;


import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.*;

import java.util.Date;

@SpringBootTest
@Transactional
@Rollback
public class CourseBaseServiceRetryTest {

    @Autowired
    private CourseBaseService courseBaseService;

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @MockBean
    private MediaFeignClient mediaFeignClient;

    private static final Long TEST_ORG_ID = 1234L;

    private CourseBase createTestCourse() {
        CourseBase courseBase = new CourseBase();
        courseBase.setName("Test Course");
        courseBase.setMt(101L);
        courseBase.setSt(10101L);
        courseBase.setOrganizationId(TEST_ORG_ID); // 设置机构ID
        return courseBaseRepository.save(courseBase);
    }

    @Test
    void testFallback_WhenServiceUnavailable() {
        // 准备测试数据
        CourseBase courseBase = createTestCourse();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test".getBytes());

        // 模拟服务不可用的情况
        when(mediaFeignClient.uploadCourseLogo(any(), any(), any()))
                .thenReturn(CommonResponse.error(
                        String.valueOf(ContentErrorCode.UPLOAD_LOGO_FAILED.getCode()),
                        ContentErrorCode.UPLOAD_LOGO_FAILED.getMessage()));

        // 执行测试
        ContentException exception = assertThrows(ContentException.class,
                () -> courseBaseService.updateCourseLogo(courseBase.getId(), file));

        // 验证异常信息
        assertEquals(ContentErrorCode.UPLOAD_LOGO_FAILED.getMessage(), exception.getMessage());

        // 验证调用次数
        verify(mediaFeignClient, times(1)).uploadCourseLogo(any(), any(), any());
    }

    @Test
    void testFallback_WhenServiceSuccess() {
        // 准备测试数据
        CourseBase courseBase = createTestCourse();

        MockMultipartFile file = new MockMultipartFile(
                "file", "test.jpg", "image/jpeg", "test".getBytes());

        // 模拟服务调用成功
        MediaFileDTO mediaFileDTO = new MediaFileDTO();
        mediaFileDTO.setMediaFileId("test-file-id"); // 必须设置ID
        mediaFileDTO.setUrl("/test/url");
        mediaFileDTO.setFileName("test.jpg");
        mediaFileDTO.setFileSize(1024L);
        mediaFileDTO.setMimeType("image/jpeg");
        mediaFileDTO.setOrganizationId(TEST_ORG_ID);

        when(mediaFeignClient.uploadCourseLogo(any(), any(), any()))
                .thenReturn(CommonResponse.success(mediaFileDTO));

        // 执行测试
        assertDoesNotThrow(() -> courseBaseService.updateCourseLogo(courseBase.getId(), file));

        // 验证调用次数
        verify(mediaFeignClient, times(1)).uploadCourseLogo(any(), any(), any());

        // 验证课程封面URL已更新
        CourseBase updatedCourse = courseBaseRepository.findById(courseBase.getId()).orElseThrow();
        assertEquals("/test/url", updatedCourse.getLogo());
    }

    @Test
    void testDeleteMediaFile_WhenServiceUnavailable() {
        // 模拟服务不可用的情况
        when(mediaFeignClient.deleteMediaFile(any()))
                .thenReturn(CommonResponse.error("500", "媒体服务不可用: Service Unavailable"));

        // 执行测试
        CommonResponse<?> response = mediaFeignClient.deleteMediaFile("test/url");

        // 验证响应
        assertFalse(response.isSuccess());
        assertEquals("500", response.getCode());
        assertTrue(response.getMessage().contains("媒体服务不可用"));

        // 验证调用次数
        verify(mediaFeignClient, times(1)).deleteMediaFile(any());
    }
}