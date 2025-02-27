package com.double2and9.auth_service.dto.response;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;

@Data
@NoArgsConstructor
@AllArgsConstructor
public class ResourceMeta {
    private String name;
    private String description;
    private int weight;
    private int sort;

    public ResourceMeta(String name, int weight) {
        this.name = name;
        this.weight = weight;
        this.sort = weight;  // 默认将 sort 设置为与 weight 相同
    }
} 