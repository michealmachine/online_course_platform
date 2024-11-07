package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.TeachplanMedia;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface TeachplanMediaRepository extends JpaRepository<TeachplanMedia, Long> {
    
    // 根据教学计划ID查询媒资
    List<TeachplanMedia> findByTeachplanId(Long teachplanId);
    
    // 根据媒资文件ID查询
    List<TeachplanMedia> findByMediaFileId(Long mediaFileId);
    
    // 根据教学计划ID和媒资文件ID查询
    Optional<TeachplanMedia> findByTeachplanIdAndMediaFileId(Long teachplanId, Long mediaFileId);
} 