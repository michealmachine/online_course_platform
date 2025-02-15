package com.double2and9.media.service;

import com.double2and9.media.dto.GetPresignedUrlRequestDTO;
import com.double2and9.media.dto.GetPresignedUrlResponseDTO;
import com.double2and9.media.dto.InitiateMultipartUploadRequestDTO;
import com.double2and9.media.dto.InitiateMultipartUploadResponseDTO;

/**
 * 媒体上传服务接口
 */
public interface MediaUploadService {
    
    /**
     * 初始化分片上传
     * 
     * @param initiateMultipartUploadRequestDTO 初始化请求参数
     * @return 初始化响应信息，包含uploadId等信息
     */
    InitiateMultipartUploadResponseDTO initiateMultipartUpload(
            InitiateMultipartUploadRequestDTO initiateMultipartUploadRequestDTO);

    GetPresignedUrlResponseDTO getPresignedUrl(GetPresignedUrlRequestDTO request);
}