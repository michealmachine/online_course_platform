package com.double2and9.content_service.service;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.dto.CourseAuditDTO;
import com.double2and9.content_service.dto.CoursePublishPreDTO;
import com.double2and9.content_service.dto.CourseAuditHistoryDTO;
import com.double2and9.content_service.dto.CourseBaseDTO;

public interface CourseAuditService {
    /**
     * 提交课程审核
     * 
     * @param courseId 课程ID
     */
    void submitForAudit(Long courseId);

    /**
     * 审核课程
     * 
     * @param auditDTO 审核信息
     */
    void auditCourse(CourseAuditDTO auditDTO);

    /**
     * 获取待审核课程列表
     * 
     * @param pageParams 分页参数
     * @return 待审核课程列表
     */
    PageResult<CourseBaseDTO> getPendingAuditCourses(PageParams pageParams);

    /**
     * 获取课程审核历史
     */
    PageResult<CourseAuditHistoryDTO> getAuditHistory(Long courseId, PageParams pageParams);

    /**
     * 获取审核人的审核历史
     */
    PageResult<CourseAuditHistoryDTO> getAuditorHistory(Long auditorId, PageParams pageParams);
}