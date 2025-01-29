package com.double2and9.content_service.service;

import com.double2and9.content_service.dto.SaveTeachplanDTO;
import com.double2and9.content_service.dto.TeachplanDTO;
import java.util.List;

public interface TeachplanService {
    /**
     * 查询课程计划树形结构
     * @param courseId 课程id
     * @return 课程计划树形结构
     */
    List<TeachplanDTO> findTeachplanTree(Long courseId);

    /**
     * 创建或修改课程计划
     * @param teachplanDTO 课程计划信息
     * @return 课程计划ID
     */
    Long saveTeachplan(SaveTeachplanDTO teachplanDTO);

    /**
     * 删除课程计划
     * @param teachplanId 课程计划id
     */
    void deleteTeachplan(Long teachplanId);

    /**
     * 向上移动课程计划
     * @param teachplanId 课程计划ID
     */
    void moveUp(Long teachplanId);

    /**
     * 向下移动课程计划
     * @param teachplanId 课程计划ID
     */
    void moveDown(Long teachplanId);
} 