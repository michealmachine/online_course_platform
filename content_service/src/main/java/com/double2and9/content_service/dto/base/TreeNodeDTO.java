package com.double2and9.content_service.dto.base;

import io.swagger.v3.oas.annotations.media.Schema;
import lombok.Data;
import java.util.List;

@Data
public class TreeNodeDTO<T> {

    @Schema(description = "节点ID")
    private Long id;

    @Schema(description = "节点名称")
    private String name;

    @Schema(description = "父节点ID")
    private Long parentId;

    @Schema(description = "子节点列表")
    private List<T> children;
}