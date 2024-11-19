package com.double2and9.content_service.service.impl;
import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.entity.MediaFile;
import com.double2and9.content_service.repository.MediaFileRepository;
import com.double2and9.content_service.service.MediaFileService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.util.ArrayList;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class MediaFileServiceImpl implements MediaFileService {
    
    private final MediaFileRepository mediaFileRepository;
    private final ModelMapper modelMapper;
    
    public MediaFileServiceImpl(MediaFileRepository mediaFileRepository, ModelMapper modelMapper) {
        this.mediaFileRepository = mediaFileRepository;
        this.modelMapper = modelMapper;
    }
    
    @Override
    @Transactional
    public MediaFile saveMediaFile(Long organizationId, MediaFileDTO mediaFileDTO) {
        // 检查mediaFileId是否已存在
        if (mediaFileRepository.findById(mediaFileDTO.getMediaFileId()).isPresent()) {
            throw new ContentException(ContentErrorCode.MEDIA_ALREADY_EXISTS);
        }

        MediaFile mediaFile = new MediaFile();
        modelMapper.map(mediaFileDTO, mediaFile);
        mediaFile.setOrganizationId(organizationId);
        
        // 设置审核状态
        if ("IMAGE".equals(mediaFileDTO.getMediaType())) {
            mediaFile.setAuditStatus("202003"); // 图片默认通过
        } else {
            mediaFile.setAuditStatus("202001"); // 其他类型默认待审核
        }
        
        return mediaFileRepository.save(mediaFile);
    }
    
    @Override
    public PageResult<MediaFileDTO> queryMediaFiles(
            Long organizationId, 
            String mediaType,
            String purpose,
            PageParams pageParams) {
        // 使用Specification构建动态查询条件
        Specification<MediaFile> spec = (root, query, builder) -> {
            List<jakarta.persistence.criteria.Predicate> predicates = new ArrayList<>();
            
            // 添加机构ID条件
            predicates.add(builder.equal(root.get("organizationId"), organizationId));
            
            // 添加媒体类型条件
            if (StringUtils.hasText(mediaType)) {
                predicates.add(builder.equal(root.get("mediaType"), mediaType));
            }
            
            // 添加用途条件
            if (StringUtils.hasText(purpose)) {
                predicates.add(builder.equal(root.get("purpose"), purpose));
            }
            
            return builder.and(predicates.toArray(new jakarta.persistence.criteria.Predicate[0]));
        };
        
        // 执行查询
        Page<MediaFile> page = mediaFileRepository.findAll(
            spec, 
            PageRequest.of(pageParams.getPageNo().intValue() - 1, pageParams.getPageSize().intValue())
        );
        
        // 转换结果
        List<MediaFileDTO> items = page.getContent().stream()
            .map(file -> modelMapper.map(file, MediaFileDTO.class))
            .collect(Collectors.toList());
            
        return new PageResult<>(items, page.getTotalElements(), pageParams.getPageNo(), pageParams.getPageSize());
    }
    
    @Override
    public String getMediaFileUrl(Long organizationId, String mediaFileId) {
        MediaFile mediaFile = mediaFileRepository.findByMediaFileId(mediaFileId)
            .orElseThrow(() -> new ContentException(ContentErrorCode.MEDIA_NOT_EXISTS));
            
        // 验证机构ID
        if (!mediaFile.getOrganizationId().equals(organizationId)) {
            throw new ContentException(ContentErrorCode.MEDIA_ORG_NOT_MATCH);
        }
        
        // 图片类型直接返回url
        if ("IMAGE".equals(mediaFile.getMediaType())) {
            return mediaFile.getUrl();
        }
        
        // 视频类型暂不支持访问
        throw new ContentException(ContentErrorCode.MEDIA_TYPE_NOT_SUPPORT);
    }
    
    @Override
    @Transactional
    public void updateAuditStatus(String mediaFileId, String auditStatus, String auditMessage) {
        MediaFile mediaFile = mediaFileRepository.findByMediaFileId(mediaFileId)
            .orElseThrow(() -> new ContentException(ContentErrorCode.MEDIA_NOT_EXISTS));
            
        mediaFile.setAuditStatus(auditStatus);
        mediaFile.setAuditMessage(auditMessage);
        mediaFileRepository.save(mediaFile);
        
        log.info("更新媒资文件审核状态：mediaFileId={}, status={}", mediaFileId, auditStatus);
    }
} 