package com.double2and9.auth_service.utils;

import com.fasterxml.jackson.databind.ObjectMapper;
import lombok.extern.slf4j.Slf4j;

@Slf4j
public class JsonUtils {
    private static final ObjectMapper objectMapper = new ObjectMapper();

    public static <T> T fromJson(String json, Class<T> clazz) {
        try {
            return objectMapper.readValue(json, clazz);
        } catch (Exception e) {
            log.error("JSON反序列化失败: {}", e.getMessage());
            throw new RuntimeException("JSON反序列化失败", e);
        }
    }

    public static String toJson(Object object) {
        try {
            return objectMapper.writeValueAsString(object);
        } catch (Exception e) {
            log.error("JSON序列化失败: {}", e.getMessage());
            throw new RuntimeException("JSON序列化失败", e);
        }
    }
} 