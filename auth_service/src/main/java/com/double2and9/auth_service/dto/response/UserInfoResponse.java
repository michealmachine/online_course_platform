package com.double2and9.auth_service.dto.response;

import com.fasterxml.jackson.annotation.JsonInclude;
import com.fasterxml.jackson.annotation.JsonProperty;
import lombok.Data;
import lombok.Builder;
import lombok.NoArgsConstructor;
import lombok.AllArgsConstructor;

@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
@JsonInclude(JsonInclude.Include.NON_NULL)
public class UserInfoResponse {
    private String sub;    // 用户标识符
    private String name;   // 完整名称
    
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