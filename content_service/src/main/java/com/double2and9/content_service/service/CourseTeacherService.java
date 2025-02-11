package com.double2and9.content_service.service;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.CourseBaseDTO;
import com.double2and9.content_service.dto.CourseTeacherDTO;
import com.double2and9.content_service.dto.SaveCourseTeacherDTO;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface CourseTeacherService {
    /**
     * 查询课程教师列表
     * 
     * @param courseId 课程ID
     * @return 教师列表
     */
    List<CourseTeacherDTO> listByCourseId(Long courseId);

    @Transactional
    void deleteCourseTeacher(Long courseId, Long teacherId);

    /**
     * 查询机构教师列表
     * 
     * @param organizationId 机构ID
     * @return 教师列表
     */
    List<CourseTeacherDTO> listByOrganizationId(Long organizationId);

    /**
     * 根据教师ID查询其关联的所有课程
     * 
     * @param teacherId 教师ID
     * @return 课程列表
     */
    List<CourseBaseDTO> listCoursesByTeacherId(Long teacherId);

    /**
     * 根据机构ID和教师ID查询教师信息
     * 
     * @param organizationId 机构ID
     * @param teacherId      教师ID
     * @return 教师信息
     */
    CourseTeacherDTO getTeacherDetail(Long organizationId, Long teacherId);

    /**
     * 保存课程教师信息
     * 
     * @param teacherDTO 教师信息
     */
    void saveCourseTeacher(SaveCourseTeacherDTO teacherDTO);

    /**
     * 关联教师到课程
     * 
     * @param organizationId 机构ID
     * @param courseId       课程ID
     * @param teacherId      教师ID
     * @throws ContentException 当教师或课程不存在，或不属于同一机构时
     */
    void associateTeacherToCourse(Long organizationId, Long courseId, Long teacherId);

    /**
     * 解除教师与课程的关联
     * 
     * @param organizationId 机构ID
     * @param courseId       课程ID
     * @param teacherId      教师ID
     * @throws ContentException 当教师或课程不存在，或不属于同一机构时
     */
    void dissociateTeacherFromCourse(Long organizationId, Long courseId, Long teacherId);

    /**
     * 上传教师头像到临时存储
     *
     * @param teacherId 教师ID
     * @param file      头像图片文件
     * @return 临时存储的key
     * @throws ContentException 当教师不存在或上传失败时
     */
    String uploadTeacherAvatarTemp(Long teacherId, MultipartFile file);

    /**
     * 确认并保存临时头像
     *
     * @param teacherId 教师ID
     * @param tempKey   临时存储key
     * @throws ContentException 当教师不存在、临时文件不存在或保存失败时
     */
    void confirmTeacherAvatar(Long teacherId, String tempKey);

    /**
     * 删除教师头像
     *
     * @param teacherId 教师ID
     * @throws ContentException 当教师不存在或删除失败时
     */
    void deleteTeacherAvatar(Long teacherId);

    /**
     * 保存教师信息
     * 
     * @param teacherDTO 教师信息
     * @return 教师ID
     * @throws ContentException 当保存失败时
     */
    Long saveTeacher(SaveCourseTeacherDTO teacherDTO);

    /**
     * 删除教师
     * 
     * @param organizationId 机构ID
     * @param teacherId      教师ID
     * @throws ContentException 当教师不存在或不属于该机构时
     */
    void deleteTeacher(Long organizationId, Long teacherId);

    PageResult<CourseTeacherDTO> listByOrganizationId(Long organizationId, PageParams pageParams);

    PageResult<CourseTeacherDTO> listByCourseId(Long courseId, PageParams pageParams);

    PageResult<CourseBaseDTO> listCoursesByTeacherId(Long teacherId, PageParams pageParams);

}