package com.double2and9.base.enums;

import lombok.Getter;
import lombok.AllArgsConstructor;

/**
 * 认证服务错误码
 */
@Getter
@AllArgsConstructor
public enum AuthErrorCode {
    // 用户相关错误 3001xx
    USERNAME_ALREADY_EXISTS(300101, "用户名已存在"),
    EMAIL_ALREADY_EXISTS(300102, "邮箱已存在"),
    USER_NOT_FOUND(300103, "用户不存在"),
    PASSWORD_ERROR(300104, "用户名或密码错误"),
    ACCOUNT_DISABLED(300105, "账号已被禁用"),
    ACCOUNT_LOCKED(300106, "账号已被锁定"),
    ACCOUNT_EXPIRED(300107, "账号已过期"),
    CREDENTIALS_EXPIRED(300108, "凭证已过期"),
    USERNAME_EXISTS(300109, "用户名已存在"),
    EMAIL_EXISTS(300110, "邮箱已被注册"),
    PASSWORD_MISMATCH(300111, "两次输入的密码不一致"),

    // 角色权限相关错误 3002xx
    ROLE_NOT_EXISTS(300201, "角色不存在"),
    PERMISSION_DENIED(300202, "没有操作权限"),
    ROLE_ALREADY_EXISTS(300203, "角色已存在"),
    PERMISSION_ALREADY_EXISTS(300204, "权限已存在"),
    PERMISSION_NOT_FOUND(300205, "权限不存在"),
    PERMISSION_IN_USE(300207, "权限正在被使用，无法删除"),
    ROLE_IN_USE(300208, "角色正在使用中，无法删除"),
    INVALID_ROLE(300210, "无效的角色"),

    // Token相关错误 3003xx
    TOKEN_EXPIRED(300301, "Token已过期"),
    TOKEN_INVALID(300302, "无效的Token"),
    TOKEN_SIGNATURE_INVALID(300303, "Token签名无效"),
    TOKEN_UNSUPPORTED(300304, "不支持的Token格式"),
    TOKEN_CLAIMS_EMPTY(300305, "Token信息为空"),
    TOKEN_REVOKED(300510, "Token已被撤销"),

    // 认证相关错误 3004xx
    AUTHENTICATION_FAILED(300401, "认证失败"),
    UNAUTHORIZED(300402, "未授权访问"),
    LOGIN_FAILED(300403, "登录失败"),
    LOGOUT_FAILED(300404, "登出失败"),
    INVALID_CAPTCHA(300405, "验证码错误或已过期"),

    // 参数验证错误 3005xx
    PARAMETER_VALIDATION_FAILED(300501, "参数验证失败"),
    PARAMETER_MISSING(300502, "缺少必要参数"),
    PARAMETER_TYPE_MISMATCH(300503, "参数类型不匹配"),
    INVALID_REQUEST(300504, "无效的请求参数"),

    // 用户管理相关错误 3006xx
    USER_UPDATE_FAILED(300601, "用户信息更新失败"),
    USER_QUERY_FAILED(300602, "用户查询失败"),
    USER_NOT_AUTHORIZED(300603, "无权操作此用户"),
    PKCE_REQUIRED(300601, "PKCE参数缺失"),
    INVALID_CODE_CHALLENGE_METHOD(300602, "无效的code_challenge_method"),
    CODE_VERIFIER_REQUIRED(300603, "code_verifier不能为空"),
    INVALID_CODE_VERIFIER(300604, "无效的code_verifier"),

    // 系统错误 3999xx
    SYSTEM_ERROR(399999, "系统内部错误"),

    // 客户端相关错误 3007xx
    CLIENT_ID_EXISTS(300701, "客户端ID已存在"),
    CLIENT_NOT_FOUND(300702, "客户端不存在"),
    CLIENT_SECRET_INVALID(300703, "客户端密钥无效"),
    CLIENT_DISABLED(300704, "客户端已被禁用"),
    CLIENT_SCOPE_INVALID(300705, "无效的授权范围"),
    CLIENT_REDIRECT_URI_INVALID(300706, "无效的重定向URI"),
    CLIENT_AUTH_METHOD_INVALID(300707, "不支持的客户端认证方式"),
    CLIENT_GRANT_TYPE_INVALID(300708, "不支持的授权类型"),
    INVALID_CLIENT(300709, "无效的客户端"),

    RESPONSE_TYPE_INVALID(300004, "响应类型必须是 code"),

    // OAuth2 授权相关错误码 3008xx
    AUTHORIZATION_REQUEST_NOT_FOUND(300801, "授权请求不存在或已过期"),
    INVALID_APPROVED_SCOPES(300802, "无效的授权范围"),
    AUTHORIZATION_CODE_GENERATE_ERROR(300803, "授权码生成失败"),
    AUTHORIZATION_CODE_GENERATION_FAILED(300804, "授权码生成失败"),

    // 授权码相关错误码 3009xx
    INVALID_AUTHORIZATION_CODE(300901, "无效的授权码"),
    AUTHORIZATION_CODE_EXPIRED(300902, "授权码已过期"),
    AUTHORIZATION_CODE_USED(300903, "授权码已被使用"),

    // 令牌相关错误码 3010xx
    INVALID_GRANT_TYPE(301001, "不支持的授权类型"),
    INVALID_CLIENT_CREDENTIALS(301002, "客户端认证失败"),
    TOKEN_GENERATE_ERROR(301004, "令牌生成失败"),
    INVALID_REFRESH_TOKEN(301005, "无效的刷新令牌"),

    // PKCE 相关错误码
    INVALID_SCOPE(301101, "无效的授权范围"),
    INVALID_CODE_CHALLENGE(301102, "无效的 code_challenge"),
    CODE_VERIFIER_MISMATCH(301105, "code_verifier 不匹配"),

    INVALID_CODE(301003, "无效的授权码"); // 添加这个

    private final int code;
    private final String message;

    public static AuthErrorCode getByCode(int code) {
        for (AuthErrorCode errorCode : values()) {
            if (errorCode.getCode() == code) {
                return errorCode;
            }
        }
        return null;
    }

    @Override
    public String toString() {
        return String.format("AuthErrorCode{code=%d, message='%s'}", code, message);
    }
} 