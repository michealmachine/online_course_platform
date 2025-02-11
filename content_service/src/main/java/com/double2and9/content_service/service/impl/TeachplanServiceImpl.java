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
import com.double2and9.content_service.cache.TeachplanOrderCache;
import lombok.extern.slf4j.Slf4j;
import org.modelmapper.ModelMapper;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.*;
import java.util.stream.Collectors;
import java.time.LocalDateTime;

@Slf4j
@Service
public class TeachplanServiceImpl implements TeachplanService {

    private final TeachplanRepository teachplanRepository;
    private final CourseBaseRepository courseBaseRepository;
    private final ModelMapper modelMapper;
    private final TeachplanOrderCache orderCache;

    public TeachplanServiceImpl(TeachplanRepository teachplanRepository,
            CourseBaseRepository courseBaseRepository,
            ModelMapper modelMapper,
            TeachplanOrderCache orderCache) {
        this.teachplanRepository = teachplanRepository;
        this.courseBaseRepository = courseBaseRepository;
        this.modelMapper = modelMapper;
        this.orderCache = orderCache;
    }

    @Override
    public List<TeachplanDTO> findTeachplanTree(Long courseId) {
        // 1. 查询所有课程计划
        List<Teachplan> teachplans = teachplanRepository.findByCourseBaseIdOrderByOrderBy(courseId);
        
        // 2. 转换为DTO，使用缓存中的临时排序
        List<TeachplanDTO> teachplanDTOs = teachplans.stream()
                .map(teachplan -> {
                    TeachplanDTO dto = modelMapper.map(teachplan, TeachplanDTO.class);
                    // 如果缓存中有临时排序，使用临时排序
                    Integer tempOrder = orderCache.getCurrentOrder(teachplan.getId(), teachplan.getOrderBy());
                    dto.setOrderBy(tempOrder);
                    return dto;
                })
                .collect(Collectors.toList());
        
        // 3. 构建树形结构
        return TreeUtils.buildTree(
                teachplanDTOs,
                TeachplanDTO::getId,
                TeachplanDTO::getParentId
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
            teachplan.setCreateTime(LocalDateTime.now());
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
        teachplan.setUpdateTime(LocalDateTime.now());

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
    public void moveUp(Long teachplanId) {
        // 1. 获取当前节点
        Teachplan current = teachplanRepository.findById(teachplanId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHPLAN_NOT_EXISTS));

        // 2. 获取上一个节点
        Optional<Teachplan> previous = teachplanRepository.findPreviousNode(
                current.getParentId(), current.getOrderBy());
        if (previous.isEmpty()) {
            throw new ContentException(ContentErrorCode.TEACHPLAN_MOVE_ERROR, "已经是第一个，无法上移");
        }

        // 3. 只在缓存中交换顺序
        orderCache.cacheOrderChange(
            current.getId(), previous.get().getOrderBy(),
            previous.get().getId(), current.getOrderBy()
        );
        
        log.debug("课程计划上移(临时)：teachplanId={}", teachplanId);
    }

    @Override
    public void moveDown(Long teachplanId) {
        // 1. 获取当前节点
        Teachplan current = teachplanRepository.findById(teachplanId)
                .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHPLAN_NOT_EXISTS));

        // 2. 获取下一个节点
        Optional<Teachplan> next = teachplanRepository.findNextNode(
                current.getParentId(), current.getOrderBy());
        if (next.isEmpty()) {
            throw new ContentException(ContentErrorCode.TEACHPLAN_MOVE_ERROR, "已经是最后一个，无法下移");
        }

        // 3. 只在缓存中交换顺序
        orderCache.cacheOrderChange(
            current.getId(), next.get().getOrderBy(),
            next.get().getId(), current.getOrderBy()
        );
        
        log.debug("课程计划下移(临时)：teachplanId={}", teachplanId);
    }

    @Override
    @Transactional
    public void saveOrderChanges() {
        try {
            orderCache.saveAllChanges();
            log.info("保存课程计划排序变更成功");
        } catch (Exception e) {
            log.error("保存课程计划排序变更失败", e);
            throw new ContentException(ContentErrorCode.TEACHPLAN_SAVE_ERROR, "保存排序变更失败");
        }
    }

    @Override
    public void discardOrderChanges() {
        orderCache.discardChanges();
        log.info("丢弃课程计划排序变更");
    }
}