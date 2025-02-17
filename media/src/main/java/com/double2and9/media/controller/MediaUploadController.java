package com.double2and9.media.controller;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.media.dto.request.InitiateMultipartUploadRequestDTO;
import com.double2and9.media.dto.InitiateMultipartUploadResponseDTO;
import com.double2and9.media.dto.request.GetPresignedUrlRequestDTO;
import com.double2and9.media.dto.GetPresignedUrlResponseDTO;
import com.double2and9.media.dto.request.CompleteMultipartUploadRequestDTO;
import com.double2and9.media.dto.CompleteMultipartUploadResponseDTO;
import com.double2and9.media.service.MediaUploadService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;

@Tag(name = "媒体文件上传接口", description = "处理媒体文件的分片上传相关操作")
@Slf4j
@RestController
@RequestMapping("/api/media/upload")
@RequiredArgsConstructor
public class MediaUploadController {

    private final MediaUploadService mediaUploadService;

    @Operation(summary = "初始化分片上传", description = "创建分片上传会话，返回uploadId等信息")
    @PostMapping("/initiate")
    public CommonResponse<InitiateMultipartUploadResponseDTO> initiateMultipartUpload(
            @RequestParam Long organizationId,
            @RequestBody @Valid InitiateMultipartUploadRequestDTO request) {
        
        request.setOrganizationId(organizationId);
        
        log.info("初始化分片上传请求: fileName={}, fileSize={}, mediaType={}, organizationId={}",
                request.getFileName(),
                request.getFileSize(),
                request.getMediaType(),
                organizationId);
        
        InitiateMultipartUploadResponseDTO response = mediaUploadService.initiateMultipartUpload(request);
        
        log.info("初始化分片上传成功: uploadId={}, mediaFileId={}", 
                response.getUploadId(),
                response.getMediaFileId());
        
        return CommonResponse.success(response);
    }

    @Operation(summary = "获取分片上传预签名URL", description = "为客户端提供上传文件分片的预签名URL")
    @GetMapping("/presigned-url")
    public CommonResponse<GetPresignedUrlResponseDTO> getPresignedUrl(
            @Valid GetPresignedUrlRequestDTO request) {
        
        log.info("接收到获取预签名URL请求: uploadId={}, chunkIndex={}, request={}", 
                request.getUploadId(), 
                request.getChunkIndex(),
                request);
        
        GetPresignedUrlResponseDTO response = mediaUploadService.getPresignedUrl(request);
        
        log.info("预签名URL生成成功: uploadId={}, chunkIndex={}, expirationTime={}, presignedUrl={}", 
                request.getUploadId(), 
                request.getChunkIndex(),
                response.getExpirationTime(),
                response.getPresignedUrl());
        
        return CommonResponse.success(response);
    }

    @Operation(summary = "完成分片上传")
    @PostMapping("/complete")
    public CommonResponse<CompleteMultipartUploadResponseDTO> completeMultipartUpload(
            @Parameter(description = "完成分片上传请求", required = true)
            @Valid @RequestBody CompleteMultipartUploadRequestDTO request) {
        
        log.info("接收到完成分片上传请求: uploadId={}", request.getUploadId());
        
        CompleteMultipartUploadResponseDTO response = mediaUploadService.completeMultipartUpload(request);
        
        log.info("完成分片上传成功: uploadId={}, mediaFileId={}, fileUrl={}", 
                request.getUploadId(),
                response.getMediaFileId(),
                response.getFileUrl());
        
        return CommonResponse.success(response);
    }
} 