package com.double2and9.media.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.DiscriminatorValue;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;
import lombok.EqualsAndHashCode;

/**
 * 视频文件实体
 */
@Getter
@Setter
@ToString(callSuper = true)
@EqualsAndHashCode(callSuper = true)
@Entity
@DiscriminatorValue("VIDEO")  // 用于区分不同类型的媒体文件
public class VideoFile extends MediaFile {
    
    /**
     * 视频时长（秒）
     */
    @Column(name = "duration")
    private Long duration;
    
    /**
     * 视频分辨率-宽
     */
    @Column(name = "width")
    private Integer width;
    
    /**
     * 视频分辨率-高
     */
    @Column(name = "height")
    private Integer height;
    
    /**
     * 视频编码格式
     */
    @Column(name = "codec", length = 32)
    private String codec;
    
    /**
     * 视频比特率
     */
    @Column(name = "bitrate")
    private Long bitrate;
} 