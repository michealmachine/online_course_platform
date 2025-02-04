package com.double2and9.content_service.service;

import com.double2and9.content_service.dto.TeachplanMediaDTO;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;

public interface TeachplanMediaService {
    /**
     * 绑定课程计划与媒资文件
     * 
     * @param teachplanMediaDTO 媒资绑定信息
     */
    void associateMedia(TeachplanMediaDTO teachplanMediaDTO);

    /**
     * 解除课程计划与媒资的绑定
     * 
     * @param teachplanId 课程计划ID
     * @param mediaFileId 媒资文件ID（media服务的fileId）
     */

    @Transactional
    void dissociateMedia(Long teachplanId, String mediaFileId);

    /**
     * 获取课程计划关联的媒资列表
     * 
     * @param teachplanId 课程计划ID
     * @return 媒资列表
     */
    List<TeachplanMediaDTO> getMediaByTeachplanId(Long teachplanId);
}