package com.double2and9.content_service.service.impl;

import com.double2and9.content_service.common.enums.ContentErrorCode;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.SaveTeachplanDTO;
import com.double2and9.content_service.dto.TeachplanDTO;
import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.Teachplan;
import com.double2and9.content_service.repository.CourseBaseRepository;
import com.double2and9.content_service.repository.TeachplanRepository;
import com.double2and9.content_service.service.TeachplanService;
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
        // 查询所有课程计划
        List<Teachplan> teachplans = teachplanRepository.findByCourseBaseIdOrderByOrderBy(courseId);
        
        // 将课程计划转换为树形结构
        List<TeachplanDTO> chapters = new ArrayList<>();
        Map<Long, TeachplanDTO> teachplanMap = new HashMap<>();
        
        teachplans.forEach(teachplan -> {
            TeachplanDTO dto = modelMapper.map(teachplan, TeachplanDTO.class);
            teachplanMap.put(dto.getId(), dto);
            
            if (teachplan.getParentId() == 0L) {
                chapters.add(dto);
            } else {
                TeachplanDTO parentNode = teachplanMap.get(teachplan.getParentId());
                if (parentNode != null) {
                    if (parentNode.getTeachPlanTreeNodes() == null) {
                        parentNode.setTeachPlanTreeNodes(new ArrayList<>());
                    }
                    parentNode.getTeachPlanTreeNodes().add(dto);
                }
            }
        });
        
        return chapters;
    }

    @Override
    @Transactional
    public void saveTeachplan(SaveTeachplanDTO teachplanDTO) {
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
        
        teachplanRepository.save(teachplan);
        log.info("保存课程计划成功，课程ID：{}，课程计划ID：{}", courseBase.getId(), teachplan.getId());
    }

    @Override
    @Transactional
    public void deleteTeachplan(Long teachplanId) {
        // 查询课程计划
        Teachplan teachplan = teachplanRepository.findById(teachplanId)
            .orElseThrow(() -> new ContentException(ContentErrorCode.TEACHPLAN_NOT_EXISTS));
        
        // 如果是章节，先删除其下的所有小节
        if (teachplan.getLevel() == 1) {
            teachplanRepository.deleteByParentId(teachplanId);
        }
        
        // 删除当前课程计划
        teachplanRepository.delete(teachplan);
        
        log.info("课程计划删除成功，课程计划ID：{}", teachplanId);
    }
} 