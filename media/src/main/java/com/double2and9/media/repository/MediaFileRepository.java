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
     * 根据文件ID查找文件信息
     * @param fileId 文件ID（MD5值）
     * @return 文件信息
     */
    Optional<MediaFile> findByFileId(String fileId);
    
    /**
     * 根据文件类型查询文件列表
     * @param fileType 文件类型
     * @return 文件列表
     */
    List<MediaFile> findByFileType(String fileType);
    
    /**
     * 根据状态查询文件列表
     * @param status 文件状态
     * @return 文件列表
     */
    List<MediaFile> findByStatus(String status);
    
    /**
     * 根据文件名模糊查询
     * @param filename 文件名
     * @return 文件列表
     */
    @Query("SELECT m FROM MediaFile m WHERE m.fileName LIKE %:filename%")
    List<MediaFile> findByFileNameLike(@Param("filename") String filename);
    
    /**
     * 检查文件是否存在
     * @param fileId 文件ID
     * @return 是否存在
     */
    boolean existsByFileId(String fileId);
    
    /**
     * 根据文件MD5查询
     */
    Optional<MediaFile> findByFileMd5(String fileMd5);
    
    /**
     * 根据文件名模糊查询
     */
    List<MediaFile> findByFileNameContaining(String fileName);
} 