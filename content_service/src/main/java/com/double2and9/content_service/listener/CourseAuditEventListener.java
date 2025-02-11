package com.double2and9.content_service.listener;

import com.double2and9.content_service.event.CourseAuditEvent;
import lombok.extern.slf4j.Slf4j;
import org.springframework.context.event.EventListener;
import org.springframework.stereotype.Component;

@Slf4j
@Component
public class CourseAuditEventListener {

    @EventListener
    public void onCourseAudit(CourseAuditEvent event) {
        log.info("收到课程审核事件，courseId：{}，审核状态：{}",
                event.getCourseBase().getId(), event.getAuditStatus());

        // TODO: 发送消息通知
        // 1. 审核通过，通知机构
        // 2. 审核不通过，通知机构修改
    }
}