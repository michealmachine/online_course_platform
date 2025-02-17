package com.double2and9.media.controller;

import com.double2and9.media.dto.request.InitiateMultipartUploadRequestDTO;
import com.double2and9.media.dto.InitiateMultipartUploadResponseDTO;
import com.double2and9.media.service.MediaUploadService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
public class MediaUploadControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private ObjectMapper objectMapper;

    @MockBean
    private MediaUploadService mediaUploadService;

    @Test
    public void testInitiateMultipartUpload() throws Exception {
        // 1. 准备请求数据
        InitiateMultipartUploadRequestDTO request = new InitiateMultipartUploadRequestDTO();
        request.setFileName("test-video.mp4");
        request.setFileSize(10L * 1024 * 1024); // 10MB
        request.setMediaType("VIDEO");
        request.setMimeType("video/mp4");
        request.setPurpose("TEST");
        // 不再在请求DTO中设置organizationId

        // 2. 准备模拟响应
        InitiateMultipartUploadResponseDTO mockResponse = InitiateMultipartUploadResponseDTO.builder()
                .uploadId("test-upload-id")
                .mediaFileId("test-media-id")
                .bucket("test-bucket")
                .filePath("video/1/test-media-id.mp4")
                .chunkSize(5242880)
                .totalChunks(2)
                .build();

        when(mediaUploadService.initiateMultipartUpload(any()))
                .thenReturn(mockResponse);

        // 3. 执行测试请求，添加organizationId作为请求参数
        mockMvc.perform(post("/api/media/upload/initiate")
                .param("organizationId", "1")  // 添加organizationId作为请求参数
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.code").value(200))
                .andExpect(jsonPath("$.data.uploadId").value("test-upload-id"))
                .andExpect(jsonPath("$.data.mediaFileId").value("test-media-id"))
                .andExpect(jsonPath("$.data.bucket").value("test-bucket"))
                .andExpect(jsonPath("$.data.filePath").value("video/1/test-media-id.mp4"))
                .andExpect(jsonPath("$.data.chunkSize").value(5242880))
                .andExpect(jsonPath("$.data.totalChunks").value(2));
    }

    @Test
    public void testInitiateMultipartUploadWithInvalidRequest() throws Exception {
        // 1. 准备无效的请求数据（缺少必填字段）
        InitiateMultipartUploadRequestDTO request = new InitiateMultipartUploadRequestDTO();
        request.setFileName("test-video.mp4");
        // 缺少其他必填字段

        // 2. 执行测试请求，添加organizationId作为请求参数
        mockMvc.perform(post("/api/media/upload/initiate")
                .param("organizationId", "1")  // 添加organizationId作为请求参数
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.code").value(400));
    }

    @Test
    public void testInitiateMultipartUploadWithoutOrganizationId() throws Exception {
        // 1. 准备请求数据
        InitiateMultipartUploadRequestDTO request = new InitiateMultipartUploadRequestDTO();
        request.setFileName("test-video.mp4");
        request.setFileSize(10L * 1024 * 1024);
        request.setMediaType("VIDEO");
        request.setMimeType("video/mp4");
        request.setPurpose("TEST");

        // 2. 执行测试请求，不添加organizationId参数
        mockMvc.perform(post("/api/media/upload/initiate")
                .contentType(MediaType.APPLICATION_JSON)
                .content(objectMapper.writeValueAsString(request)))
                .andExpect(status().isBadRequest()); // 应该返回400错误，因为缺少必需的organizationId参数
    }
} 