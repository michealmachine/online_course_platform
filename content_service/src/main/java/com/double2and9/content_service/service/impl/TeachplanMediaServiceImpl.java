package com.double2and9.content_service.service.impl;

import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.TeachplanMediaDTO;
import com.double2and9.content_service.entity.MediaFile;
import com.double2and9.content_service.entity.Teachplan;
import com.double2and9.content_service.entity.TeachplanMedia;
import com.double2and9.content_service.repository.MediaFileRepository;
import com.double2and9.content_service.repository.TeachplanMediaRepository;
import com.double2and9.content_service.repository.TeachplanRepository;
import com.double2and9.content_service.service.TeachplanMediaService;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TeachplanMediaServiceImpl implements TeachplanMediaService {

    private final TeachplanMediaRepository teachplanMediaRepository;
    private final TeachplanRepository teachplanRepository;
    private final MediaFileRepository mediaFileRepository;
    private final ModelMapper modelMapper;
    public TeachplanMediaServiceImpl(TeachplanMediaRepository teachplanMediaRepository,
                                   TeachplanRepository teachplanRepository,
                                   MediaFileRepository mediaFileRepository,
                                   ModelMapper modelMapper) {
        this.teachplanMediaRepository = teachplanMediaRepository;
        this.teachplanRepository = teachplanRepository;
        this.mediaFileRepository = mediaFileRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    @Transactional
    public void associateMedia(TeachplanMediaDTO teachplanMediaDTO) {
        // 验证课程计划是否存在
        Teachplan teachplan = teachplanRepository.findById(teachplanMediaDTO.getTeachplanId())
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHPLAN_NOT_EXISTS));
        
        // 验证媒资是否存在 - 使用mediaFileId查询
        MediaFile mediaFile = mediaFileRepository.findByMediaFileId(teachplanMediaDTO.getMediaId())
                .orElseThrow(() -> new ContentException(ContentErrorCode.MEDIA_NOT_EXISTS));

        try {
            // 检查是否已经存在关联
            TeachplanMedia existingMedia = teachplanMediaRepository
                    .findByTeachplanIdAndMediaFile_MediaFileId(teachplan.getId(), mediaFile.getMediaFileId())
                    .orElse(null);

            if (existingMedia == null) {
                // 创建新的关联
                TeachplanMedia teachplanMedia = new TeachplanMedia();
                teachplanMedia.setTeachplan(teachplan);
                teachplanMedia.setMediaFile(mediaFile);
                teachplanMedia.setCreateTime(new Date());
                teachplanMedia.setUpdateTime(new Date());
                teachplanMediaRepository.save(teachplanMedia);
            }

            log.info("课程计划与媒资关联成功，课程计划ID：{}，媒资ID：{}", teachplan.getId(), mediaFile.getMediaFileId());
        } catch (Exception e) {
            throw new ContentException(ContentErrorCode.MEDIA_BIND_ERROR);
        }
    }

    @Override
    @Transactional
    public void dissociateMedia(Long teachplanId, String mediaFileId) {
        TeachplanMedia teachplanMedia = teachplanMediaRepository
                .findByTeachplanIdAndMediaFile_MediaFileId(teachplanId, mediaFileId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.MEDIA_NOT_EXISTS));

        teachplanMediaRepository.delete(teachplanMedia);
        log.info("解除课程计划与媒资的关联成功，课程计划ID：{}，媒资ID：{}", teachplanId, mediaFileId);
    }

    @Override
    public List<TeachplanMediaDTO> getMediaByTeachplanId(Long teachplanId) {
        List<TeachplanMedia> teachplanMediaList = teachplanMediaRepository.findByTeachplanId(teachplanId);
        return teachplanMediaList.stream()
                .map(this::convertToDTO)
                .collect(Collectors.toList());
    }

    private TeachplanMediaDTO convertToDTO(TeachplanMedia teachplanMedia) {
        TeachplanMediaDTO dto = new TeachplanMediaDTO();
        dto.setTeachplanId(teachplanMedia.getTeachplan().getId());
        dto.setMediaId(teachplanMedia.getMediaFile().getMediaFileId());
        dto.setMediaFileName(teachplanMedia.getMediaFile().getFileName());
        dto.setMediaType(teachplanMedia.getMediaFile().getMediaType());
        dto.setUrl(teachplanMedia.getMediaFile().getUrl());
        return dto;
    }
} 