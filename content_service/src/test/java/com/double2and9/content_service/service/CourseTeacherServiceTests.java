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
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.argThat;
import static org.mockito.Mockito.when;
import static org.mockito.Mockito.verify;

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
        // 1. 添加教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setDescription("测试教师简介");
        teacherDTO.setCourseIds(Set.of(courseId)); // 使用Set设置课程ID

        courseTeacherService.saveCourseTeacher(teacherDTO);

        // 2. 查询教师列表
        List<CourseTeacherDTO> teachers = courseTeacherService.listByCourseId(courseId);
        assertNotNull(teachers);
        assertFalse(teachers.isEmpty());

        // 验证新增的教师
        CourseTeacherDTO teacher = teachers.stream()
                .filter(t -> "测试教师".equals(t.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(teacher);
        assertEquals("讲师", teacher.getPosition());
        assertTrue(teacher.getCourseIds().contains(courseId));

        // 3. 修改教师信息
        teacherDTO.setId(teacher.getId());
        teacherDTO.setPosition("高级讲师");
        courseTeacherService.saveCourseTeacher(teacherDTO);

        // 4. 再次查询验证
        teachers = courseTeacherService.listByCourseId(courseId);
        teacher = teachers.stream()
                .filter(t -> "测试教师".equals(t.getName()))
                .findFirst()
                .orElse(null);
        assertNotNull(teacher);
        assertEquals("高级讲师", teacher.getPosition());
        assertTrue(teacher.getCourseIds().contains(courseId));

        // 5. 删除测试
        courseTeacherService.deleteCourseTeacher(courseId, teacher.getId());
        teachers = courseTeacherService.listByCourseId(courseId);
        assertTrue(teachers.stream().noneMatch(t -> "测试教师".equals(t.getName())));
    }

    @Test
    @Transactional
    public void testListByOrganizationId() {
        // 1. 创建教师并关联课程
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setCourseIds(Set.of(courseId));
        courseTeacherService.saveCourseTeacher(teacherDTO);

        // 2. 查询机构教师列表
        List<CourseTeacherDTO> teachers = courseTeacherService.listByOrganizationId(TEST_ORG_ID);
        assertNotNull(teachers);
        assertFalse(teachers.isEmpty());
        assertEquals(TEST_ORG_ID, teachers.get(0).getOrganizationId());
    }

    @Test
    @Transactional
    public void testListCoursesByTeacherId() {
        // 1. 创建教师并关联课程
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setCourseIds(Set.of(courseId));
        courseTeacherService.saveCourseTeacher(teacherDTO);

        // 2. 获取教师ID
        List<CourseTeacherDTO> teachers = courseTeacherService.listByCourseId(courseId);
        Long teacherId = teachers.get(0).getId();

        // 3. 查询教师关联的课程
        List<CourseBaseDTO> courses = courseTeacherService.listCoursesByTeacherId(teacherId);
        assertNotNull(courses);
        assertFalse(courses.isEmpty());
        assertEquals(courseId, courses.get(0).getId());
    }

    @Test
    @Transactional
    public void testGetTeacherDetail() {
        // 1. 创建教师并关联课程
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setCourseIds(Set.of(courseId));
        courseTeacherService.saveCourseTeacher(teacherDTO);

        // 2. 获取教师ID
        List<CourseTeacherDTO> teachers = courseTeacherService.listByCourseId(courseId);
        Long teacherId = teachers.get(0).getId();

        // 3. 查询教师详情
        CourseTeacherDTO teacherDetail = courseTeacherService.getTeacherDetail(TEST_ORG_ID, teacherId);
        assertNotNull(teacherDetail);
        assertEquals("测试教师", teacherDetail.getName());
        assertEquals(TEST_ORG_ID, teacherDetail.getOrganizationId());
        assertTrue(teacherDetail.getCourseIds().contains(courseId));
    }

    @Test
    @Transactional
    public void testGetTeacherDetailWithWrongOrg() {
        // 1. 创建教师并关联课程
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setPosition("讲师");
        teacherDTO.setCourseIds(Set.of(courseId));
        courseTeacherService.saveCourseTeacher(teacherDTO);

        // 2. 获取教师ID
        List<CourseTeacherDTO> teachers = courseTeacherService.listByCourseId(courseId);
        Long teacherId = teachers.get(0).getId();

        // 3. 使用错误的机构ID查询，应该抛出异常
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
        teacherDTO.setCourseIds(Set.of(courseId));
        courseTeacherService.saveCourseTeacher(teacherDTO);

        List<CourseTeacherDTO> teachers = courseTeacherService.listByCourseId(courseId);
        Long teacherId = teachers.get(0).getId();

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
    void testDeleteTeacherAvatar() {
        // 1. 创建带头像的教师
        SaveCourseTeacherDTO teacherDTO = new SaveCourseTeacherDTO();
        teacherDTO.setOrganizationId(TEST_ORG_ID);
        teacherDTO.setName("测试教师");
        teacherDTO.setCourseIds(Set.of(courseId));
        courseTeacherService.saveCourseTeacher(teacherDTO);

        List<CourseTeacherDTO> teachers = courseTeacherService.listByCourseId(courseId);
        Long teacherId = teachers.get(0).getId();

        // 2. 设置头像URL
        CourseTeacher teacher = courseTeacherRepository.findById(teacherId).orElseThrow();
        teacher.setAvatar("/test/url");
        courseTeacherRepository.save(teacher);

        // 3. Mock删除响应
        when(mediaFeignClient.deleteMediaFile(any()))
                .thenReturn(CommonResponse.success(null));

        // 4. 执行删除
        courseTeacherService.deleteTeacherAvatar(teacherId);

        // 5. 验证结果
        verify(mediaFeignClient).deleteMediaFile("/test/url");
        CourseTeacherDTO updatedTeacher = courseTeacherService.getTeacherDetail(TEST_ORG_ID, teacherId);
        assertNull(updatedTeacher.getAvatar());
    }
}