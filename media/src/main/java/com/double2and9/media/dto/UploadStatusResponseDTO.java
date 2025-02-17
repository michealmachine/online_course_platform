package com.double2and9.media.dto;

import com.double2and9.media.dto.request.UploadedPartDTO;
import lombok.Builder;
import lombok.Data;

import java.util.List;

@Data
@Builder
public class UploadStatusResponseDTO {
    private String uploadId;
    private String status;
    private Integer totalChunks;
    private List<UploadedPartDTO> uploadedParts;
    private Long expirationTime;
}
