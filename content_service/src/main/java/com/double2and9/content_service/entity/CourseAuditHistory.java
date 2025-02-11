package com.double2and9.content_service.entity;

import jakarta.persistence.*;
import lombok.Data;

import java.util.Date;
import java.time.LocalDateTime;

@Entity
@Table(name = "course_audit_history")
@Data
public class CourseAuditHistory {
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(name = "course_id", nullable = false)
    private Long courseId;

    @Column(name = "audit_status", nullable = false)
    private String auditStatus;

    @Column(name = "audit_message")
    private String auditMessage;

    @Column(name = "auditor_id")
    private Long auditorId;

    @Column(name = "audit_time", nullable = false)
    private LocalDateTime auditTime;

    @PrePersist
    public void prePersist() {
        if (auditTime == null) {
            auditTime = LocalDateTime.now();
        }
    }
}