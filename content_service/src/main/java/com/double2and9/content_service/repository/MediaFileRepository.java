package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.MediaFile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, Long> {
    
    // 根据文件名模糊查询
    List<MediaFile> findByFileNameContaining(String fileName);
    
    // 根据文件类型查询
    List<MediaFile> findByFileType(String fileType);
    
    // 根据文件路径查询
    List<MediaFile> findByFilePath(String filePath);
} 