package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.Teachplan;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.PageRequest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
@Transactional
@Rollback
public class TeachplanRepositoryTest {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private TeachplanRepository teachplanRepository;

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Test
    void testSaveTeachplan() {
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setOrganizationId(TEST_ORG_ID);
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
        courseBase.setOrganizationId(TEST_ORG_ID);
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

    @Test
    void testFindPageByCourseId() {
        // 准备测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase = courseBaseRepository.save(courseBase);

        // 创建两个课程计划
        Teachplan plan1 = new Teachplan();
        plan1.setName("第一章");
        plan1.setCourseBase(courseBase);
        plan1.setLevel(1);
        plan1.setOrderBy(1);
        plan1.setCreateTime(new Date());
        plan1.setUpdateTime(new Date());
        teachplanRepository.save(plan1);

        Teachplan plan2 = new Teachplan();
        plan2.setName("第二章");
        plan2.setCourseBase(courseBase);
        plan2.setLevel(1);
        plan2.setOrderBy(2);
        plan2.setCreateTime(new Date());
        plan2.setUpdateTime(new Date());
        teachplanRepository.save(plan2);

        // 测试分页查询
        Page<Teachplan> result = teachplanRepository.findPageByCourseId(
                courseBase.getId(),
                PageRequest.of(0, 10));

        // 验证结果
        assertEquals(2, result.getTotalElements());
        assertEquals("第一章", result.getContent().get(0).getName());
        assertEquals("第二章", result.getContent().get(1).getName());
    }

    @Test
    void testFindByCourseBaseIdAndLevel() {
        // 准备测试数据
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase = courseBaseRepository.save(courseBase);

        // 创建一级和二级课程计划
        Teachplan chapter = new Teachplan();
        chapter.setName("第一章");
        chapter.setCourseBase(courseBase);
        chapter.setLevel(1);
        chapter.setOrderBy(1);
        chapter.setCreateTime(new Date());
        chapter.setUpdateTime(new Date());
        teachplanRepository.save(chapter);

        Teachplan section = new Teachplan();
        section.setName("第一节");
        section.setCourseBase(courseBase);
        section.setParentId(chapter.getId());
        section.setLevel(2);
        section.setOrderBy(1);
        section.setCreateTime(new Date());
        section.setUpdateTime(new Date());
        teachplanRepository.save(section);

        // 测试分页查询一级课程计划
        Page<Teachplan> result = teachplanRepository.findByCourseBaseIdAndLevel(
                courseBase.getId(),
                1,
                PageRequest.of(0, 10));

        // 验证结果
        assertEquals(1, result.getTotalElements());
        assertEquals("第一章", result.getContent().get(0).getName());
    }
}