package com.double2and9.content_service.service;

import com.double2and9.content_service.dto.AddCourseDTO;
import com.double2and9.content_service.dto.SaveTeachplanDTO;
import com.double2and9.content_service.dto.TeachplanDTO;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

/**
 * 课程计划服务测试类
 * 测试课程计划的增删改查功能
 * 包括：
 * 1. 创建章节和小节
 * 2. 树形结构的查询
 * 3. 级联删除功能
 */
@SpringBootTest
public class TeachplanServiceTests {

    @Autowired
    private TeachplanService teachplanService;
    
    @Autowired
    private CourseBaseService courseBaseService;
    
    private Long courseId;

    /**
     * 测试前置准备
     * 创建一个测试课程，用于后续的课程计划测试
     */
    @BeforeEach
    @Transactional
    public void setUp() {
        // 创建测试课程数据
        AddCourseDTO courseDTO = new AddCourseDTO();
        courseDTO.setName("测试课程");
        courseDTO.setBrief("这是一个测试课程");
        courseDTO.setMt(1L);
        courseDTO.setSt(2L);
        courseDTO.setCharge("201001");  // 免费课程
        courseDTO.setPrice(BigDecimal.ZERO);
        courseDTO.setValid(true);

        // 保存课程并验证
        courseId = courseBaseService.createCourse(courseDTO);
        assertNotNull(courseId, "课程创建失败");
    }

    /**
     * 测试课程计划的级联删除功能
     * 验证删除章节时，其下的小节也会被删除
     */
    @Test
    @Transactional
    public void testTeachplanCascadeDelete() {
        // 1. 创建章节
        SaveTeachplanDTO chapterDTO = new SaveTeachplanDTO();
        chapterDTO.setCourseId(courseId);
        chapterDTO.setParentId(0L);     // 顶级节点
        chapterDTO.setLevel(1);         // 章节级别
        chapterDTO.setName("测试章节");
        chapterDTO.setOrderBy(1);       // 排序号

        teachplanService.saveTeachplan(chapterDTO);

        // 2. 查询并获取章节ID
        List<TeachplanDTO> teachplanTree = teachplanService.findTeachplanTree(courseId);
        TeachplanDTO chapter = teachplanTree.stream()
                .filter(t -> "测试章节".equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("章节创建失败"));
        Long chapterId = chapter.getId();

        // 3. 创建多个小节
        for (int i = 1; i <= 3; i++) {
            SaveTeachplanDTO sectionDTO = new SaveTeachplanDTO();
            sectionDTO.setCourseId(courseId);
            sectionDTO.setParentId(chapterId);  // 关联到章节
            sectionDTO.setLevel(2);             // 小节级别
            sectionDTO.setName("测试小节" + i);
            sectionDTO.setOrderBy(i);           // 按序号排序
            teachplanService.saveTeachplan(sectionDTO);
        }

        // 4. 验证小节创建成功
        teachplanTree = teachplanService.findTeachplanTree(courseId);
        chapter = teachplanTree.stream()
                .filter(t -> "测试章节".equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("章节不存在"));
        assertEquals(3, chapter.getTeachPlanTreeNodes().size(), "应该有3个小节");

        // 5. 测试级联删除功能
        teachplanService.deleteTeachplan(chapterId);
        
        // 6. 验证章节和所有小节都被删除
        teachplanTree = teachplanService.findTeachplanTree(courseId);
        assertTrue(teachplanTree.stream().noneMatch(t -> "测试章节".equals(t.getName())), 
            "章节应该被删除");
    }

    /**
     * 测试手动删除课程计划的功能
     * 先删除小节，再删除章节
     */
    @Test
    @Transactional
    public void testTeachplanManualDelete() {
        // 1. 创建章节和小节
        SaveTeachplanDTO chapterDTO = new SaveTeachplanDTO();
        chapterDTO.setCourseId(courseId);
        chapterDTO.setParentId(0L);
        chapterDTO.setLevel(1);
        chapterDTO.setName("测试章节");
        chapterDTO.setOrderBy(1);

        teachplanService.saveTeachplan(chapterDTO);

        // 查询章节信息
        List<TeachplanDTO> teachplanTree = teachplanService.findTeachplanTree(courseId);
        TeachplanDTO chapter = teachplanTree.stream()
                .filter(t -> "测试章节".equals(t.getName()))
                .findFirst()
                .orElseThrow(() -> new RuntimeException("章节创建失败"));

        // 创建小节
        SaveTeachplanDTO sectionDTO = new SaveTeachplanDTO();
        sectionDTO.setCourseId(courseId);
        sectionDTO.setParentId(chapter.getId());
        sectionDTO.setLevel(2);
        sectionDTO.setName("测试小节");
        sectionDTO.setOrderBy(1);

        teachplanService.saveTeachplan(sectionDTO);

        // 2. 先删除小节
        teachplanTree = teachplanService.findTeachplanTree(courseId);
        TeachplanDTO section = teachplanTree.stream()
                .filter(t -> "测试章节".equals(t.getName()))
                .flatMap(t -> t.getTeachPlanTreeNodes().stream())
                .findFirst()
                .orElseThrow(() -> new RuntimeException("小节不存在"));

        teachplanService.deleteTeachplan(section.getId());

        // 3. 再删除章节
        teachplanService.deleteTeachplan(chapter.getId());

        // 4. 验证都被删除
        teachplanTree = teachplanService.findTeachplanTree(courseId);
        assertTrue(teachplanTree.stream().noneMatch(t -> "测试章节".equals(t.getName())), 
            "章节应该被删除");
    }
} 