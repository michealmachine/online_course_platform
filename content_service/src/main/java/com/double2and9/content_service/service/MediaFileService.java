package com.double2and9.content_service.service;

import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.entity.MediaFile;
import org.springframework.transaction.annotation.Transactional;

public interface MediaFileService {
    
    /**
     * 保存媒资文件信息
     */
    MediaFile saveMediaFile(Long organizationId, MediaFileDTO mediaFileDTO);

    /**
     * 分页查询媒资文件
     */
    PageResult<MediaFileDTO> queryMediaFiles(
        Long organizationId, 
        String mediaType,
        String purpose,
        PageParams pageParams
    );


    /**
     * 获取媒资文件访问地址
     */
    String getMediaFileUrl(Long organizationId, String mediaFileId);
    
    /**
     * 更新媒资文件审核状态
     */
    void updateAuditStatus(String mediaFileId, String auditStatus, String auditMessage);
} 