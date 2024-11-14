package com.double2and9.media.dto;

import lombok.Data;
import lombok.ToString;
import java.io.Serializable;

@Data
@ToString
public class TempFileDTO implements Serializable {
    private String fileName;       // 原始文件名
    private String contentType;    // 文件类型
    private byte[] fileData;       // 文件数据
    private Long fileSize;         // 文件大小
    private String uploadUser;     // 上传用户
} 