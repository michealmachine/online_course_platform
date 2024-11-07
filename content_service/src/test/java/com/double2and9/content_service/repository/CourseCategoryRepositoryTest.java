package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
public class CourseCategoryRepositoryTest {

    @Autowired
    private CourseCategoryRepository courseCategoryRepository;

    @Test
    void testSaveCategory() {
        // 准备测试数据
        CourseCategory category = new CourseCategory();
        category.setName("测试分类");
        category.setParentId(0L);
        category.setLevel(1);
        category.setCreateTime(new Date());
        category.setUpdateTime(new Date());

        // 执行保存
        CourseCategory saved = courseCategoryRepository.save(category);

        // 验证
        assertNotNull(saved.getId());
        assertEquals("测试分类", saved.getName());
        assertEquals(1, saved.getLevel());
    }

    @Test
    void testFindByParentId() {
        // 准备测试数据
        CourseCategory parent = new CourseCategory();
        parent.setName("父分类");
        parent.setParentId(0L);
        parent.setLevel(1);
        parent.setCreateTime(new Date());
        parent.setUpdateTime(new Date());
        parent = courseCategoryRepository.save(parent);

        CourseCategory child = new CourseCategory();
        child.setName("子分类");
        child.setParentId(parent.getId());
        child.setLevel(2);
        child.setCreateTime(new Date());
        child.setUpdateTime(new Date());
        courseCategoryRepository.save(child);

        // 执行查询
        List<CourseCategory> children = courseCategoryRepository.findByParentId(parent.getId());

        // 验证
        assertFalse(children.isEmpty());
        assertEquals("子分类", children.get(0).getName());
    }
} 