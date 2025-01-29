package com.double2and9.content_service.controller;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.model.ContentResponse;
import com.double2and9.content_service.service.MediaFileService;
import com.double2and9.base.model.PageParams;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.extern.slf4j.Slf4j;
import org.springframework.validation.annotation.Validated;
import org.springframework.web.bind.annotation.*;

@Slf4j
@RestController
@RequestMapping("/media-files")
@Tag(name = "媒资文件管理", description = "提供媒资文件的增删改查接口")
public class MediaFileController {
    
    private final MediaFileService mediaFileService;
    
    public MediaFileController(MediaFileService mediaFileService) {
        this.mediaFileService = mediaFileService;
    }
    
    @Operation(summary = "保存媒资文件信息")
    @PostMapping
    public ContentResponse<Void> saveMediaFile(
            @Parameter(description = "媒资文件信息") @RequestBody @Validated MediaFileDTO mediaFileDTO) {
        mediaFileService.saveMediaFile(mediaFileDTO.getOrganizationId(), mediaFileDTO);
        return ContentResponse.success(null);
    }
    
    @Operation(summary = "查询媒资文件列表")
    @GetMapping("/list/{organizationId}")
    public ContentResponse<PageResult<MediaFileDTO>> queryMediaFiles(
            @Parameter(description = "机构ID") @PathVariable Long organizationId,
            @Parameter(description = "媒体类型") @RequestParam(required = false) String mediaType,
            @Parameter(description = "文件用途") @RequestParam(required = false) String purpose,
            @Parameter(description = "分页参数") PageParams pageParams) {
        return ContentResponse.success(
            mediaFileService.queryMediaFiles(organizationId, mediaType, purpose, pageParams)
        );
    }
    
    @Operation(summary = "获取媒资文件访问地址")
    @GetMapping("/url/{organizationId}/{mediaFileId}")
    public ContentResponse<String> getMediaFileUrl(
            @Parameter(description = "机构ID") @PathVariable Long organizationId,
            @Parameter(description = "媒资文件ID") @PathVariable String mediaFileId) {
        return ContentResponse.success(
            mediaFileService.getMediaFileUrl(organizationId, mediaFileId)
        );
    }
} 