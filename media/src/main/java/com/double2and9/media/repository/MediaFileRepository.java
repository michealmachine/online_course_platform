package com.double2and9.media.repository;

import com.double2and9.media.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

/**
 * 媒资文件数据访问层
 */
@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {

    /**
     * 根据媒体文件ID查找文件信息
     * 
     * @param mediaFileId 媒体文件ID（MD5值）
     * @return 文件信息
     */
    Optional<MediaFile> findByMediaFileId(String mediaFileId);

    /**
     * 根据媒体类型查询文件列表
     * 
     * @param mediaType 媒体类型
     * @return 文件列表
     */
    List<MediaFile> findByMediaType(String mediaType);

    /**
     * 根据状态查询文件列表
     * 
     * @param status 文件状态
     * @return 文件列表
     */
    List<MediaFile> findByStatus(String status);

    /**
     * 根据文件名模糊查询
     * 
     * @param filename 文件名
     * @return 文件列表
     */
    @Query("SELECT m FROM MediaFile m WHERE m.fileName LIKE %:filename%")
    List<MediaFile> findByFileNameLike(@Param("filename") String filename);

    /**
     * 检查文件是否存在
     * 
     * @param mediaFileId 媒体文件ID
     * @return 是否存在
     */
    boolean existsByMediaFileId(String mediaFileId);

    /**
     * 根据URL查询
     */
    Optional<MediaFile> findByUrl(String url);

    /**
     * 根据文件名模糊查询
     */
    List<MediaFile> findByFileNameContaining(String fileName);
}