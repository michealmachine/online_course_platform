package com.double2and9.content_service.service;

import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.entity.Teachplan;
import com.double2and9.content_service.BaseTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;

import static org.junit.jupiter.api.Assertions.*;

public class TeachplanServiceTests extends BaseTest {

    @Autowired
    private TeachplanService teachplanService;

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