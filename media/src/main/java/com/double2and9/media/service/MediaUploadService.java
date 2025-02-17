package com.double2and9.media.service;

import com.double2and9.media.dto.request.GetPresignedUrlRequestDTO;
import com.double2and9.media.dto.GetPresignedUrlResponseDTO;
import com.double2and9.media.dto.request.InitiateMultipartUploadRequestDTO;
import com.double2and9.media.dto.InitiateMultipartUploadResponseDTO;
import com.double2and9.media.dto.request.CompleteMultipartUploadRequestDTO;
import com.double2and9.media.dto.CompleteMultipartUploadResponseDTO;

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
    /**
     * 获取分片上传预签名URL
     *
     * @param request 获取预签名URL请求参数
     * @return 预签名URL响应信息
     */
    GetPresignedUrlResponseDTO getPresignedUrl(GetPresignedUrlRequestDTO request);

    /**
     * 完成分片上传
     * @param request 完成分片上传请求
     * @return 完成分片上传响应
     */
    CompleteMultipartUploadResponseDTO completeMultipartUpload(CompleteMultipartUploadRequestDTO request);
}