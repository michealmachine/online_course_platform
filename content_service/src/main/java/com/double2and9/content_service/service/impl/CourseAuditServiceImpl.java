package com.double2and9.content_service.service.impl;

import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.base.enums.CourseAuditStatusEnum;
import com.double2and9.base.enums.CourseStatusEnum;
import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.CourseAuditDTO;
import com.double2and9.content_service.dto.CourseAuditHistoryDTO;
import com.double2and9.content_service.dto.CourseBaseDTO;
import com.double2and9.content_service.dto.CourseAuditInfoDTO;
import com.double2and9.content_service.dto.QueryCourseParamsDTO;
import com.double2and9.content_service.entity.*;
import com.double2and9.content_service.event.CourseAuditEvent;
import com.double2and9.content_service.repository.CourseAuditHistoryRepository;
import com.double2and9.content_service.repository.CourseBaseRepository;
import com.double2and9.content_service.repository.CourseTeacherRepository;
import com.double2and9.content_service.repository.TeachplanRepository;
import com.double2and9.content_service.service.CourseAuditService;
import com.double2and9.content_service.service.CourseBaseService;
import org.modelmapper.ModelMapper;
import org.springframework.context.ApplicationEventPublisher;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.StringUtils;

import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

@Service
@Transactional(readOnly = true)
public class CourseAuditServiceImpl implements CourseAuditService {

        private final CourseTeacherRepository courseTeacherRepository;
        private final TeachplanRepository teachplanRepository;
        private final CourseBaseRepository courseBaseRepository;
        private final CourseAuditHistoryRepository auditHistoryRepository;
        private final ModelMapper modelMapper;
        private final ApplicationEventPublisher eventPublisher;
        private final CourseBaseService courseBaseService;

        public CourseAuditServiceImpl(CourseTeacherRepository courseTeacherRepository,
                        TeachplanRepository teachplanRepository,
                        CourseBaseRepository courseBaseRepository,
                        CourseAuditHistoryRepository auditHistoryRepository,
                        ModelMapper modelMapper,
                        ApplicationEventPublisher eventPublisher,
                        CourseBaseService courseBaseService) {
                this.courseTeacherRepository = courseTeacherRepository;
                this.teachplanRepository = teachplanRepository;
                this.courseBaseRepository = courseBaseRepository;
                this.auditHistoryRepository = auditHistoryRepository;
                this.modelMapper = modelMapper;
                this.eventPublisher = eventPublisher;
                this.courseBaseService = courseBaseService;
        }

        private void validateCourseForAudit(CourseBase courseBase) {
                // 1. 检查基本信息
                if (!StringUtils.hasText(courseBase.getName()) || !StringUtils.hasText(courseBase.getBrief())) {
                        throw new ContentException(ContentErrorCode.COURSE_AUDIT_STATUS_ERROR, "课程基本信息不完整");
                }

                // 2. 检查课程计划
                List<Teachplan> teachplans = teachplanRepository.findByCourseBaseIdOrderByOrderBy(courseBase.getId());
                if (teachplans == null || teachplans.isEmpty()) {
                        throw new ContentException(ContentErrorCode.TEACHPLAN_NOT_EXISTS, "未配置课程计划");
                }

                // 3. 检查教师信息 - 使用分页查询，只需要第一页数据
                Page<CourseTeacher> teacherPage = courseTeacherRepository.findByCourseId(
                                courseBase.getId(),
                                PageRequest.of(0, 1));
                if (teacherPage == null || teacherPage.isEmpty()) {
                        throw new ContentException(ContentErrorCode.TEACHER_NOT_EXISTS, "未关联教师信息");
                }
        }

        @Override
        @Transactional
        public void submitForAudit(Long courseId) {
                // 1. 获取课程信息
                CourseBase courseBase = courseBaseRepository.findById(courseId)
                                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

                // 2. 验证课程信息
                validateCourseForAudit(courseBase);

                // 3. 获取或创建预发布记录
                CoursePublishPre publishPre = courseBase.getCoursePublishPre();
                if (publishPre == null) {
                        publishPre = new CoursePublishPre();
                        publishPre.setCourseBase(courseBase);
                } else {
                        // 如果已存在预发布记录，检查状态
                        if (!CourseAuditStatusEnum.REJECTED.getCode().equals(publishPre.getStatus())) {
                                throw new ContentException(ContentErrorCode.COURSE_AUDIT_STATUS_ERROR, "课程已提交审核");
                        }
                }

                // 4. 设置预发布记录信息
                publishPre.setName(courseBase.getName());
                publishPre.setStatus(CourseAuditStatusEnum.SUBMITTED.getCode());
                courseBase.setCoursePublishPre(publishPre);

                // 5. 更新课程状态为已发布
                courseBase.setStatus(CourseStatusEnum.PUBLISHED.getCode());  // 202002
                courseBase.setUpdateTime(LocalDateTime.now());

                // 6. 保存更新
                courseBaseRepository.save(courseBase);

                // 7. 发布审核事件
                eventPublisher.publishEvent(new CourseAuditEvent(
                                CourseAuditStatusEnum.SUBMITTED.getCode(),
                                courseBase,
                                null));
        }

        @Override
        public PageResult<CourseBaseDTO> getPendingAuditCourses(PageParams pageParams) {
                // 修改查询逻辑，确保返回的是待审核状态的课程
                Page<CourseBase> page = courseBaseRepository.findByPublishPreStatus(
                                CourseAuditStatusEnum.SUBMITTED.getCode(),
                                PageRequest.of(pageParams.getPageNo().intValue() - 1,
                                                pageParams.getPageSize().intValue()));

                List<CourseBaseDTO> items = page.getContent().stream()
                                .map(course -> {
                                        CourseBaseDTO dto = modelMapper.map(course, CourseBaseDTO.class);
                                        // 确保DTO中的状态是预发布记录的状态
                                        if (course.getCoursePublishPre() != null) {
                                                dto.setStatus(course.getCoursePublishPre().getStatus());
                                        }
                                        return dto;
                                })
                                .collect(Collectors.toList());

                return new PageResult<>(items, page.getTotalElements(),
                                pageParams.getPageNo(), pageParams.getPageSize());
        }

        @Override
        public PageResult<CourseAuditHistoryDTO> getAuditorHistory(Long courseId, PageParams pageParams) {
                // 使用已有的方法 findByCourseIdOrderByAuditTimeDesc 而不是创建新方法
                Page<CourseAuditHistory> page = auditHistoryRepository.findByCourseIdOrderByAuditTimeDesc(
                                courseId,
                                PageRequest.of(pageParams.getPageNo().intValue() - 1,
                                                pageParams.getPageSize().intValue()));

                List<CourseAuditHistoryDTO> items = page.getContent().stream()
                                .map(history -> modelMapper.map(history, CourseAuditHistoryDTO.class))
                                .collect(Collectors.toList());

                return new PageResult<>(items, page.getTotalElements(),
                                pageParams.getPageNo(), pageParams.getPageSize());
        }

        @Override
        public PageResult<CourseAuditHistoryDTO> getAuditHistory(Long courseId, PageParams pageParams) {
                // 使用已有的方法 findByCourseIdOrderByAuditTimeDesc
                Page<CourseAuditHistory> page = auditHistoryRepository.findByCourseIdOrderByAuditTimeDesc(
                                courseId,
                                PageRequest.of(pageParams.getPageNo().intValue() - 1,
                                                pageParams.getPageSize().intValue()));

                List<CourseAuditHistoryDTO> items = page.getContent().stream()
                                .map(history -> modelMapper.map(history, CourseAuditHistoryDTO.class))
                                .collect(Collectors.toList());

                return new PageResult<>(items, page.getTotalElements(),
                                pageParams.getPageNo(), pageParams.getPageSize());
        }

        @Override
        @Transactional
        public void auditCourse(CourseAuditDTO auditDTO) {
                // 1. 获取课程信息
                CourseBase courseBase = courseBaseRepository.findById(auditDTO.getCourseId())
                                .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));

                // 2. 检查预发布记录状态
                CoursePublishPre publishPre = courseBase.getCoursePublishPre();
                if (publishPre == null || !CourseAuditStatusEnum.SUBMITTED.getCode().equals(publishPre.getStatus())) {
                        throw new ContentException(ContentErrorCode.COURSE_AUDIT_STATUS_ERROR, "课程未处于待审核状态");
                }

                // 3. 设置审核状态 - 修改状态判断逻辑
                String status;
                if ("pass".equals(auditDTO.getAuditStatus()) ||
                                CourseAuditStatusEnum.APPROVED.getCode().equals(auditDTO.getAuditStatus())) {
                        status = CourseAuditStatusEnum.APPROVED.getCode();
                } else {
                        status = CourseAuditStatusEnum.REJECTED.getCode();
                }
                publishPre.setStatus(status);

                // 4. 保存审核历史
                CourseAuditHistory auditHistory = new CourseAuditHistory();
                auditHistory.setCourseId(auditDTO.getCourseId());
                auditHistory.setAuditStatus(status);
                auditHistory.setAuditMessage(auditDTO.getAuditMessage());
                auditHistory.setAuditorId(auditDTO.getAuditorId());
                auditHistory.setAuditTime(LocalDateTime.now());

                CourseAuditHistory savedHistory = auditHistoryRepository.save(auditHistory);

                // 验证审核时间是否被正确保存
                if (savedHistory.getAuditTime() == null) {
                        throw new ContentException(ContentErrorCode.SYSTEM_ERROR, "审核时间保存失败");
                }

                // 同时更新预发布记录的时间
                publishPre.setUpdateTime(LocalDateTime.now());

                // 5. 保存课程更新
                courseBaseRepository.save(courseBase);

                // 6. 发布审核事件
                eventPublisher.publishEvent(new CourseAuditEvent(
                                status,
                                courseBase,
                                auditDTO.getAuditMessage()));
        }

        @Override
        public PageResult<CourseAuditInfoDTO> queryCourseAuditList(PageParams params, QueryCourseParamsDTO queryParams) {
                // 1. 先获取课程基本信息
                PageResult<CourseBaseDTO> courseResult = courseBaseService.queryCourseList(params, queryParams);
                
                // 2. 转换为带审核信息的DTO
                List<CourseAuditInfoDTO> auditInfoList = courseResult.getItems().stream()
                        .map(courseBase -> {
                                CourseAuditInfoDTO auditInfo = new CourseAuditInfoDTO();
                                auditInfo.setCourseBase(courseBase);
                                
                                // 从CourseBase获取预发布记录中的审核信息
                                CourseBase course = courseBaseRepository.findById(courseBase.getId())
                                    .orElseThrow(() -> new ContentException(ContentErrorCode.COURSE_NOT_EXISTS));
                                    
                                CoursePublishPre publishPre = course.getCoursePublishPre();
                                if (publishPre != null) {
                                    auditInfo.setAuditStatus(publishPre.getStatus());
                                    auditInfo.setAuditMessage(publishPre.getAuditMessage());
                                    auditInfo.setLastAuditTime(publishPre.getUpdateTime());
                                }
                                    
                                return auditInfo;
                        })
                        .collect(Collectors.toList());
                
                // 3. 返回分页结果，使用正确的字段名
                return new PageResult<>(
                    auditInfoList,
                    courseResult.getCounts(),    // 使用counts而不是total
                    courseResult.getPage(),      // 使用page而不是pageNo
                    courseResult.getPageSize()
                );
        }
}