package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.Teachplan;
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
public class TeachplanRepositoryTest {

    @Autowired
    private TeachplanRepository teachplanRepository;
    
    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Test
    void testSaveTeachplan() {
        // 准备测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase = courseBaseRepository.save(courseBase);

        Teachplan teachplan = new Teachplan();
        teachplan.setName("第一章");
        teachplan.setParentId(0L);
        teachplan.setLevel(1);
        teachplan.setOrderBy(1);
        teachplan.setCourseBase(courseBase);
        teachplan.setCreateTime(new Date());
        teachplan.setUpdateTime(new Date());

        // 执行保存
        Teachplan saved = teachplanRepository.save(teachplan);

        // 验证
        assertNotNull(saved.getId());
        assertEquals("第一章", saved.getName());
        assertEquals(1, saved.getLevel());
    }

    @Test
    void testFindByCourseBaseIdOrderByOrderBy() {
        // 准备测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase = courseBaseRepository.save(courseBase);

        Teachplan chapter = new Teachplan();
        chapter.setName("第一章");
        chapter.setParentId(0L);
        chapter.setLevel(1);
        chapter.setOrderBy(1);
        chapter.setCourseBase(courseBase);
        chapter.setCreateTime(new Date());
        chapter.setUpdateTime(new Date());
        teachplanRepository.save(chapter);

        // 执行查询
        List<Teachplan> teachplans = teachplanRepository.findByCourseBaseIdOrderByOrderBy(courseBase.getId());

        // 验证
        assertFalse(teachplans.isEmpty());
        assertEquals("第一章", teachplans.get(0).getName());
    }
} 