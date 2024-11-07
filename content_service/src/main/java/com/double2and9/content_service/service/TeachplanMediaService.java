package com.double2and9.content_service.service;

import com.double2and9.content_service.dto.TeachplanMediaDTO;
import java.util.List;

public interface TeachplanMediaService {
    /**
     * 绑定课程计划与媒资文件
     */
    void associateMedia(TeachplanMediaDTO teachplanMediaDTO);

    /**
     * 解除课程计划与媒资的绑定
     */
    void dissociateMedia(Long teachplanId, Long mediaId);

    /**
     * 获取课程计划关联的媒资列表
     */
    List<TeachplanMediaDTO> getMediaByTeachplanId(Long teachplanId);
} 