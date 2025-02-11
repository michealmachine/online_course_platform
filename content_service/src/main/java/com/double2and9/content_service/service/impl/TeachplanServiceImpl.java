package com.double2and9.content_service.service.impl;

import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.SaveTeachplanDTO;
import com.double2and9.content_service.dto.TeachplanDTO;
import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.Teachplan;
import com.double2and9.content_service.repository.CourseBaseRepository;
import com.double2and9.content_service.repository.TeachplanRepository;
import com.double2and9.content_service.service.TeachplanService;
import com.double2and9.content_service.utils.TreeUtils;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;

@Slf4j
@Service
public class TeachplanServiceImpl implements TeachplanService {

    private final TeachplanRepository teachplanRepository;
    private final CourseBaseRepository courseBaseRepository;
    private final ModelMapper modelMapper;

    public TeachplanServiceImpl(TeachplanRepository teachplanRepository,
            CourseBaseRepository courseBaseRepository,
            ModelMapper modelMapper) {
        this.teachplanRepository = teachplanRepository;
        this.courseBaseRepository = courseBaseRepository;
        this.modelMapper = modelMapper;
    }

    @Override
    public List<TeachplanDTO> findTeachplanTree(Long courseId) {
        // 1. 查询所有课程计划
        List<Teachplan> teachplans = teachplanRepository.findByCourseBaseIdOrderByOrderBy(courseId);
        
        // 2. 转换为DTO
        List<TeachplanDTO> teachplanDTOs = teachplans.stream()
                .map(teachplan -> modelMapper.map(teachplan, TeachplanDTO.class))
                .collect(Collectors.toList());
        
        // 3. 使用工具类构建树形结构
        return TreeUtils.buildTree(
                teachplanDTOs,
                TeachplanDTO::getId,  // ID获取函数
                TeachplanDTO::getParentId  // 父ID获取函数
        );
    }

    @Override
    @Transactional
    public Long saveTeachplan(SaveTeachplanDTO teachplanDTO) {
        // 获取课程信息
        CourseBase courseBase = courseBaseRepository.findById(teachplanDTO.getCourseId())
                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

        Long teachplanId = teachplanDTO.getId();
        Teachplan teachplan;

        if (teachplanId == null) {
            teachplan = new Teachplan();
            // 设置新增课程计划的排序号
            Integer count = teachplanRepository.countByParentId(teachplanDTO.getParentId());
            teachplan.setOrderBy(count + 1);
            teachplan.setCreateTime(new Date());
        } else {
            teachplan = teachplanRepository.findById(teachplanId)
                    .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHPLAN_NOT_EXISTS));
        }

        // 验证层级
        if (teachplanDTO.getLevel() != 1 && teachplanDTO.getLevel() != 2) {
            throw new ContentException(ContentErrorCode.TEACHPLAN_LEVEL_ERROR);
        }

        // 更新课程计划信息
        modelMapper.map(teachplanDTO, teachplan);
        teachplan.setCourseBase(courseBase);
        teachplan.setUpdateTime(new Date());

        // 保存课程计划
        Teachplan savedTeachplan = teachplanRepository.save(teachplan);

        log.info("保存课程计划成功，课程ID：{}，课程计划ID：{}", courseBase.getId(), savedTeachplan.getId());
        return savedTeachplan.getId(); // 返回ID
    }

    @Override
    @Transactional
    public void deleteTeachplan(Long teachplanId) {
        // 查询课程计划
        Teachplan teachplan = teachplanRepository.findById(teachplanId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHPLAN_NOT_EXISTS));

        // 检查是否有子节点
        if (teachplanRepository.countByParentId(teachplanId) > 0) {
            throw new ContentException(ContentErrorCode.TEACHPLAN_DELETE_ERROR);
        }

        // 如果是章节，先删除其下的所有小节
        if (teachplan.getLevel() == 1) {
            teachplanRepository.deleteByParentId(teachplanId);
        }

        // 删除当前课程计划
        teachplanRepository.delete(teachplan);

        log.info("课程计划删除成功，课程计划ID：{}", teachplanId);
    }

    @Override
    @Transactional
    public void moveUp(Long teachplanId) {
        // 获取当前课程计划
        Teachplan current = teachplanRepository.findById(teachplanId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHPLAN_NOT_EXISTS));

        // 获取上一个课程计划
        Optional<Teachplan> previous = teachplanRepository.findPreviousNode(current.getParentId(),
                current.getOrderBy());
        if (previous.isEmpty()) {
            // 已经是第一个，抛出异常
            throw new ContentException(ContentErrorCode.TEACHPLAN_MOVE_ERROR, "已经是第一个，无法上移");
        }

        // 交换orderBy
        int tempOrderBy = current.getOrderBy();
        current.setOrderBy(previous.get().getOrderBy());
        previous.get().setOrderBy(tempOrderBy);

        // 保存更改
        teachplanRepository.save(current);
        teachplanRepository.save(previous.get());
    }

    @Override
    @Transactional
    public void moveDown(Long teachplanId) {
        // 获取当前课程计划
        Teachplan current = teachplanRepository.findById(teachplanId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHPLAN_NOT_EXISTS));

        // 获取下一个课程计划
        Optional<Teachplan> next = teachplanRepository.findNextNode(current.getParentId(), current.getOrderBy());
        if (next.isEmpty()) {
            // 已经是最后一个，抛出异常
            throw new ContentException(ContentErrorCode.TEACHPLAN_MOVE_ERROR, "已经是最后一个，无法下移");
        }

        // 交换orderBy
        int tempOrderBy = current.getOrderBy();
        current.setOrderBy(next.get().getOrderBy());
        next.get().setOrderBy(tempOrderBy);

        // 保存更改
        teachplanRepository.save(current);
        teachplanRepository.save(next.get());
    }
}