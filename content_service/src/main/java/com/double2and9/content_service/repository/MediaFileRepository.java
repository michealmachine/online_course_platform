package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.MediaFile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.JpaSpecificationExecutor;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface MediaFileRepository extends JpaRepository<MediaFile, String>, JpaSpecificationExecutor<MediaFile> {
    
    // 根据机构ID查询
    Page<MediaFile> findByOrganizationId(Long organizationId, Pageable pageable);
    
    // 根据机构ID和审核状态查询
    Page<MediaFile> findByOrganizationIdAndAuditStatus(
        Long organizationId, 
        String auditStatus, 
        Pageable pageable
    );
    
    // 根据mediaFileId查询
    Optional<MediaFile> findByMediaFileId(String mediaFileId);
    
    // 根据机构ID和用途查询
    List<MediaFile> findByOrganizationIdAndPurpose(Long organizationId, String purpose);
    
    // 根据机构ID和媒体类型查询
    List<MediaFile> findByOrganizationIdAndMediaType(Long organizationId, String mediaType);
}