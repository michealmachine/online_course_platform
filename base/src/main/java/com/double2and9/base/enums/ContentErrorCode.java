package com.double2and9.base.enums;

import lombok.Getter;

/**
 * 内容服务错误码
 */
@Getter
public enum ContentErrorCode {
    // 课程相关错误 1001xx
    COURSE_NOT_EXISTS(100101, "课程不存在"),
    COURSE_NAME_EMPTY(100102, "课程名称不能为空"),
    COURSE_CATEGORY_NOT_EXISTS(100103, "课程分类不存在"),
    COURSE_AUDIT_STATUS_ERROR(100106, "课程审核状态错误"),
    COURSE_STATUS_ERROR(100105, "课程状态错误"),
    COURSE_PUBLISH_ERROR(100106, "课程发布失败"),
    COURSE_ORG_NOT_MATCH(100106, "课程与机构不匹配"),

    // 课程计划相关错误 1002xx
    TEACHPLAN_NOT_EXISTS(100201, "课程计划不存在"),
    TEACHPLAN_LEVEL_ERROR(100202, "课程计划层级错误"),
    TEACHPLAN_DELETE_ERROR(100203, "课程计划包含子节点，无法删除"),
    TEACHPLAN_MOVE_ERROR(100204, "课程计划移动失败"),

    // 教师相关错误 1003xx
    TEACHER_NOT_EXISTS(100301, "教师不存在"),
    TEACHER_COURSE_NOT_MATCH(100302, "教师与课程不匹配"),

    // 媒资相关错误 1004xx
    MEDIA_NOT_EXISTS(100401, "媒资文件不存在"),
    MEDIA_BIND_ERROR(100402, "媒资绑定失败"),
    MEDIA_ORG_NOT_MATCH(100403, "媒资文件不属于该机构"),
    MEDIA_TYPE_NOT_SUPPORT(100404, "不支持的媒体类型"),
    MEDIA_ALREADY_EXISTS(100405, "媒资文件已存在"),
    MEDIA_DELETE_ERROR(100406, "删除媒资文件失败"),
    MEDIA_SERVICE_ERROR(100407, "媒体服务不可用"),

    // 系统错误 1999xx
    SYSTEM_ERROR(199999, "系统内部错误"),

    // 课程封面相关错误 1005xx
    UPLOAD_LOGO_FAILED(100501, "上传课程封面失败"),
    DELETE_LOGO_FAILED(100502, "删除课程封面失败"),
    LOGO_NOT_EXISTS(100503, "课程封面不存在"),

    /**
     * 参数错误 (1001xx)
     */
    PARAMS_ERROR(100100, "参数错误"),
    PARAMS_EMPTY(100101, "必填参数为空"),
    PARAMS_INVALID(100102, "参数格式不正确"),

    PARAM_ERROR(400001, "参数错误"),
    FILE_UPLOAD_ERROR(400007, "文件上传失败"),
    FILE_DELETE_ERROR(400008, "文件删除失败");

    private final int code;
    private final String message;

    ContentErrorCode(int code, String message) {
        this.code = code;
        this.message = message;
    }

}