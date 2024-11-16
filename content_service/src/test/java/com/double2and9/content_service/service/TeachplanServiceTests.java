package com.double2and9.content_service.service;

import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.Teachplan;
import com.double2and9.content_service.BaseTest;
import com.double2and9.content_service.repository.CourseBaseRepository;
import com.double2and9.content_service.repository.TeachplanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class TeachplanServiceTests {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private TeachplanService teachplanService;
    
    @Autowired
    private TeachplanRepository teachplanRepository;
    
    @Autowired
    private CourseBaseRepository courseBaseRepository;

    private Long courseId;
    private Long chapter1Id;
    private Long chapter2Id;
    private Long section1Id;
    private Long section2Id;

    @BeforeEach
    public void setUp() {
        // 创建测试课程
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase = courseBaseRepository.save(courseBase);
        courseId = courseBase.getId();

        // 创建第一章
        Teachplan chapter1 = new Teachplan();
        chapter1.setName("第一章");
        chapter1.setParentId(0L);
        chapter1.setLevel(1);
        chapter1.setOrderBy(1);
        chapter1.setCourseBase(courseBase);
        chapter1.setCreateTime(new Date());
        chapter1.setUpdateTime(new Date());
        chapter1 = teachplanRepository.save(chapter1);
        chapter1Id = chapter1.getId();

        // 创建第二章
        Teachplan chapter2 = new Teachplan();
        chapter2.setName("第二章");
        chapter2.setParentId(0L);
        chapter2.setLevel(1);
        chapter2.setOrderBy(2);
        chapter2.setCourseBase(courseBase);
        chapter2.setCreateTime(new Date());
        chapter2.setUpdateTime(new Date());
        chapter2 = teachplanRepository.save(chapter2);
        chapter2Id = chapter2.getId();

        // 创建第一节
        Teachplan section1 = new Teachplan();
        section1.setName("第一节");
        section1.setParentId(chapter1Id);
        section1.setLevel(2);
        section1.setOrderBy(1);
        section1.setCourseBase(courseBase);
        section1.setCreateTime(new Date());
        section1.setUpdateTime(new Date());
        section1 = teachplanRepository.save(section1);
        section1Id = section1.getId();

        // 创建第二节
        Teachplan section2 = new Teachplan();
        section2.setName("第二节");
        section2.setParentId(chapter1Id);
        section2.setLevel(2);
        section2.setOrderBy(2);
        section2.setCourseBase(courseBase);
        section2.setCreateTime(new Date());
        section2.setUpdateTime(new Date());
        section2 = teachplanRepository.save(section2);
        section2Id = section2.getId();
    }

    @Test
    public void testMoveUpChapter() {
        // 移动第二章向上
        teachplanService.moveUp(chapter2Id);
        
        // 验证顺序已经交换
        Teachplan chapter1 = teachplanRepository.findById(chapter1Id).orElseThrow();
        Teachplan chapter2 = teachplanRepository.findById(chapter2Id).orElseThrow();
        
        assertEquals(2, chapter1.getOrderBy());
        assertEquals(1, chapter2.getOrderBy());
    }

    @Test
    public void testMoveDownChapter() {
        // 移动第一章向下
        teachplanService.moveDown(chapter1Id);
        
        // 验证顺序已经交换
        Teachplan chapter1 = teachplanRepository.findById(chapter1Id).orElseThrow();
        Teachplan chapter2 = teachplanRepository.findById(chapter2Id).orElseThrow();
        
        assertEquals(2, chapter1.getOrderBy());
        assertEquals(1, chapter2.getOrderBy());
    }

    @Test
    public void testMoveUpSection() {
        // 移动第二节向上
        teachplanService.moveUp(section2Id);
        
        // 验证顺序已经交换
        Teachplan section1 = teachplanRepository.findById(section1Id).orElseThrow();
        Teachplan section2 = teachplanRepository.findById(section2Id).orElseThrow();
        
        assertEquals(2, section1.getOrderBy());
        assertEquals(1, section2.getOrderBy());
    }

    @Test
    public void testMoveDownSection() {
        // 移动第一节向下
        teachplanService.moveDown(section1Id);
        
        // 验证顺序已经交换
        Teachplan section1 = teachplanRepository.findById(section1Id).orElseThrow();
        Teachplan section2 = teachplanRepository.findById(section2Id).orElseThrow();
        
        assertEquals(2, section1.getOrderBy());
        assertEquals(1, section2.getOrderBy());
    }

    @Test
    public void testMoveUpFirstNode() {
        // 尝试移动第一个节点向上，应该抛出异常
        assertThrows(ContentException.class, () -> teachplanService.moveUp(chapter1Id));
    }

    @Test
    public void testMoveDownLastNode() {
        // 尝试移动最后一个节点向下，应该抛出异常
        assertThrows(ContentException.class, () -> teachplanService.moveDown(chapter2Id));
    }
} 