package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.TeachplanMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeachplanMediaRepository extends JpaRepository<TeachplanMedia, Long> {
    
    // 根据课程计划ID查询
    List<TeachplanMedia> findByTeachplanId(Long teachplanId);
    
    // 根据课程计划ID和媒资文件ID查询
    Optional<TeachplanMedia> findByTeachplanIdAndMediaFile_MediaFileId(Long teachplanId, String mediaFileId);
    
    // 根据媒资文件ID查询
    List<TeachplanMedia> findByMediaFile_MediaFileId(String mediaFileId);
    
    // 删除课程计划的所有媒资关联
    void deleteByTeachplanId(Long teachplanId);
}