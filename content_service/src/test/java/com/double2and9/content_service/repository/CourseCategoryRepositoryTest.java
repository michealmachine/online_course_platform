package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseCategory;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
public class CourseCategoryRepositoryTest {

    @Autowired
    private CourseCategoryRepository courseCategoryRepository;

    @Test
    public void testSaveAndQueryCategory() {
        // 创建父分类
        CourseCategory parent = new CourseCategory();
        parent.setName("后端开发");
        parent.setParentId(0L);
        parent.setLevel(1);
        parent.setCreateTime(new Date());
        parent.setUpdateTime(new Date());
        courseCategoryRepository.save(parent);

        // 创建子分类
        CourseCategory child = new CourseCategory();
        child.setName("Java开发");
        child.setParentId(parent.getId());
        child.setLevel(2);
        child.setCreateTime(new Date());
        child.setUpdateTime(new Date());
        courseCategoryRepository.save(child);

        // 测试查询
        List<CourseCategory> children = courseCategoryRepository.findByParentId(parent.getId());
        assertFalse(children.isEmpty(), "应该能找到子分类");
        assertEquals("Java开发", children.get(0).getName());
    }
} 