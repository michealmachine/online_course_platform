package com.double2and9.media.entity;

import jakarta.persistence.*;
import lombok.Data;
import lombok.ToString;

import java.util.Date;

/**
 * 媒资处理实体
 */
@Data
@ToString
@Entity
@Table(name = "media_process")
public class MediaProcess {
    
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;
    
    @Column(name = "file_id", length = 120)
    private String fileId;               // 文件标识
    
    @Column(name = "file_name", length = 255)
    private String fileName;             // 文件名称
    
    @Column(name = "bucket", length = 255)
    private String bucket;               // 存储桶
    
    @Column(name = "file_path", length = 512)
    private String filePath;             // 存储路径
    
    @Column(length = 12)
    private String status;               // 处理状态
    
    @Column(name = "create_time")
    private Date createTime;             // 创建时间
    
    @Column(name = "finish_time")
    private Date finishTime;             // 完成时间
    
    @Column(name = "url", length = 1024)
    private String url;                  // 处理后的文件访问地址
    
    @Column(name = "error_msg", length = 1024)
    private String errorMsg;             // 错误信息

    @PrePersist
    public void prePersist() {
        if (createTime == null) {
            createTime = new Date();
        }
    }
} 