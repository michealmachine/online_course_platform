package com.double2and9.content_service.service;

import com.double2and9.base.dto.CommonResponse;
import com.double2and9.base.dto.MediaFileDTO;
import com.double2and9.content_service.client.MediaFeignClient;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.AddCourseDTO;
import com.double2and9.content_service.dto.CourseBaseDTO;
import com.double2and9.content_service.dto.CourseTeacherDTO;
import com.double2and9.content_service.dto.SaveCourseTeacherDTO;
import com.double2and9.content_service.entity.CourseTeacher;
import com.double2and9.content_service.repository.CourseTeacherRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

import com.double2and9.base.model.PageParams;
import com.double2and9.base.model.PageResult;
import org.springframework.data.domain.Page;

@SpringBootTest
@Transactional
@Rollback
public class CourseTeacherServiceTests {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private CourseTeacherService courseTeacherService;

    @Autowired
    private CourseBaseService courseBaseService;

    @Autowired
    private CourseTeacherRepository courseTeacherRepository;

    @MockBean
    private MediaFeignClient mediaFeignClient;

    private Long courseId;

    @BeforeEach
    @Transactional
    public void setUp() {
        // 创建一个测试课程
        AddCourseDTO courseDTO = new AddCourseDTO();
        courseDTO.setName("测试课程");
        courseDTO.setBrief("这是一个测试课程");
        courseDTO.setMt(1L);
        courseDTO.setSt(2L);
        courseDTO.setCharge("201001");
        courseDTO.setPrice(BigDecimal.ZERO);
        courseDTO.setValid(true);
        courseDTO.setOrganizationId(TEST_ORG_ID);

        courseId = courseBaseService.createCourse(courseDTO);
        assertNotNull(courseId);
    }

    @Test
    @Transactional
    public void testTeacherCRUD() {
        // 清理已有数据
        courseTeacherRepository.deleteAll();

        // 1. 添加教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setDescription("测试教师简介");

        // 保存教师信息
        Long teacherId = courseTeacherService.saveTeacher(teacherDTO);
        assertNotNull(teacherId);

        // 2. 关联课程
        courseTeacherService.associateTeacherToCourse(TEST_ORG_ID, courseId, teacherId);

        // 3. 查询教师列表
        List<CourseTeacherDTO> teachers = courseTeacherService.listByCourseId(courseId);
        assertNotNull(teachers);
        assertFalse(teachers.isEmpty());

        CourseTeacherDTO teacher = teachers.get(0);
        assertEquals("测试教师", teacher.getName());
        assertEquals("讲师", teacher.getPosition());
        assertTrue(teacher.getCourseIds().contains(courseId));

        // 4. 修改教师信息
        teacherDTO.setId(teacher.getId());
        teacherDTO.setPosition("高级讲师");
        courseTeacherService.saveTeacher(teacherDTO);

        // 5. 再次查询验证
        teachers = courseTeacherService.listByCourseId(courseId);
        teacher = teachers.get(0);
        assertEquals("高级讲师", teacher.getPosition());

        // 6. 解除课程关联
        courseTeacherService.dissociateTeacherFromCourse(TEST_ORG_ID, courseId, teacher.getId());
        teachers = courseTeacherService.listByCourseId(courseId);
        assertTrue(teachers.isEmpty());

        // 7. 删除教师
        courseTeacherService.deleteTeacher(TEST_ORG_ID, teacher.getId());
        teachers = courseTeacherService.listByOrganizationId(TEST_ORG_ID);
        assertTrue(teachers.isEmpty());
    }

    @Test
    @Transactional
    public void testTeacherWithWrongOrg() {
        // 1. 创建教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        Long teacherId = courseTeacherService.saveTeacher(teacherDTO);

        // 2. 使用错误的机构ID查询，应该抛出异常
        Long wrongOrgId = 9999L;
        assertThrows(ContentException.class,
                () -> courseTeacherService.getTeacherDetail(wrongOrgId, teacherId));
    }

    @Test
    void testTeacherAvatar_TwoPhase() throws IOException {
        // 1. 创建测试教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        Long teacherId = courseTeacherService.saveTeacher(teacherDTO);

        // 2. 准备测试文件
        MultipartFile file = new MockMultipartFile(
                "file",
                "test.jpg",
                "image/jpeg",
                "test image content".getBytes());

        // 3. Mock临时上传响应
        when(mediaFeignClient.uploadImageTemp(any()))
                .thenReturn(CommonResponse.success("temp-key-123"));

        // 4. Mock永久保存响应
        MediaFileDTO mediaFileDTO = new MediaFileDTO();
        mediaFileDTO.setMediaFileId("test-file-id");
        mediaFileDTO.setUrl("/test/url");
        when(mediaFeignClient.saveTempFile(any()))
                .thenReturn(CommonResponse.success(mediaFileDTO));

        // 5. 执行临时上传
        String tempKey = courseTeacherService.uploadTeacherAvatarTemp(teacherId, file);
        assertEquals("temp-key-123", tempKey);

        // 6. 执行确认保存
        courseTeacherService.confirmTeacherAvatar(teacherId, tempKey);

        // 7. 验证调用和结果
        verify(mediaFeignClient).uploadImageTemp(any());
        verify(mediaFeignClient).saveTempFile(argThat(params -> "temp-key-123".equals(params.get("tempKey"))));

        CourseTeacherDTO updatedTeacher = courseTeacherService.getTeacherDetail(TEST_ORG_ID, teacherId);
        assertEquals("/test/url", updatedTeacher.getAvatar());
    }

    @Test
    @Transactional
    void testListTeachers() {
        // 清理已有数据
        courseTeacherRepository.deleteAll();

        // 创建测试数据
        CourseTeacher teacher = new CourseTeacher();
        teacher.setName("测试教师");
        teacher.setOrganizationId(TEST_ORG_ID);
        teacher.setCreateTime(LocalDateTime.now());
        teacher.setUpdateTime(LocalDateTime.now());
        courseTeacherRepository.save(teacher);

        // 执行测试
        PageResult<CourseTeacherDTO> result = courseTeacherService.listByOrganizationId(
                TEST_ORG_ID,
                new PageParams(1L, 10L));

        // 验证
        assertEquals(1, result.getCounts());
        assertEquals("测试教师", result.getItems().get(0).getName());
    }
}