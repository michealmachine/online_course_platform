package com.double2and9.auth_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;

@Data
@Builder
@JsonInclude(JsonInclude.Include.NON_NULL)
public class IDTokenResponse {
    // 必需的claims
    private String iss;    // 发行者
    private String sub;    // 主题（用户ID）
    private String aud;    // 受众（客户端ID）
    private long exp;      // 过期时间
    private long iat;      // 签发时间
    private String nonce;  // 防重放随机数
    
    @JsonProperty("auth_time")
    private Long authTime; // 用户认证时间
    
    // 用户信息claims
    private String name;
    
    @JsonProperty("given_name")
    private String givenName;
    
    @JsonProperty("family_name")
    private String familyName;
    
    @JsonProperty("middle_name")
    private String middleName;
    
    private String nickname;
    
    @JsonProperty("preferred_username")
    private String preferredUsername;
    
    private String profile;
    private String picture;
    private String website;
    private String email;
    
    @JsonProperty("email_verified")
    private Boolean emailVerified;
    
    private String gender;
    private String birthdate;
    private String zoneinfo;
    private String locale;
    
    @JsonProperty("phone_number")
    private String phoneNumber;
    
    @JsonProperty("phone_number_verified")
    private Boolean phoneNumberVerified;
    
    @JsonProperty("updated_at")
    private Long updatedAt;
    
    // 如果是机构用户，添加机构ID
    @JsonProperty("organization_id")
    private Long organizationId;
} 