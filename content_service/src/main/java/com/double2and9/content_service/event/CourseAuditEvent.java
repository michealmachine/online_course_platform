package com.double2and9.content_service.event;

import com.double2and9.content_service.entity.CourseBase;
import lombok.Getter;
import org.springframework.context.ApplicationEvent;

@Getter
public class CourseAuditEvent extends ApplicationEvent {

    private final CourseBase courseBase;
    private final String auditStatus;

    public CourseAuditEvent(Object source, CourseBase courseBase, String auditStatus) {
        super(source);
        this.courseBase = courseBase;
        this.auditStatus = auditStatus;
    }
}