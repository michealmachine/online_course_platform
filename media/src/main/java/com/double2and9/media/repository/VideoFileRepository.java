package com.double2and9.media.repository;

import com.double2and9.media.entity.VideoFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface VideoFileRepository extends JpaRepository<VideoFile, Long> {
    
    /**
     * 根据媒体文件ID查询视频文件
     * @param mediaFileId 媒体文件ID
     * @return 视频文件实体
     */
    VideoFile findByMediaFileId(String mediaFileId);
    
    /**
     * 根据组织ID和文件名查询视频文件
     * @param organizationId 组织ID
     * @param fileName 文件名
     * @return 视频文件实体
     */
    VideoFile findByOrganizationIdAndFileName(Long organizationId, String fileName);
} 