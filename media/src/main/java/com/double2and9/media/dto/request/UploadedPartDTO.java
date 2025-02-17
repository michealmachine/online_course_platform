package com.double2and9.media.dto.request;


import lombok.Builder;
import lombok.Data;

@Data
@Builder
public class UploadedPartDTO {
    private Integer partNumber;
    private String eTag;
    private Long size;
}