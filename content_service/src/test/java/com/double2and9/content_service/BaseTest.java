package com.double2and9.content_service;

import com.double2and9.content_service.entity.*;
import com.double2and9.content_service.repository.*;
import org.junit.jupiter.api.BeforeEach;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.util.Date;

/**
 * 测试基类，提供通用的测试数据准备
 */
@SpringBootTest
@Transactional
public abstract class BaseTest {

    @Autowired
    protected CourseBaseRepository courseBaseRepository;
    
    @Autowired
    protected TeachplanRepository teachplanRepository;
    
    @Autowired
    protected CourseCategoryRepository courseCategoryRepository;

    protected Long courseId;
    protected Long chapter1Id;
    protected Long chapter2Id;
    protected Long section1Id;
    protected Long section2Id;

    @BeforeEach
    public void setUp() {
        // 创建课程
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setBrief("这是一个测试课程");
        courseBase.setStatus("202001");
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        courseBaseRepository.save(courseBase);
        courseId = courseBase.getId();

        // 创建第一个章节
        Teachplan chapter1 = new Teachplan();
        chapter1.setName("第一章");
        chapter1.setParentId(0L);
        chapter1.setLevel(1);
        chapter1.setOrderBy(1);
        chapter1.setCourseBase(courseBase);
        chapter1.setCreateTime(new Date());
        chapter1.setUpdateTime(new Date());
        teachplanRepository.save(chapter1);
        chapter1Id = chapter1.getId();

        // 创建第二个章节
        Teachplan chapter2 = new Teachplan();
        chapter2.setName("第二章");
        chapter2.setParentId(0L);
        chapter2.setLevel(1);
        chapter2.setOrderBy(2);
        chapter2.setCourseBase(courseBase);
        chapter2.setCreateTime(new Date());
        chapter2.setUpdateTime(new Date());
        teachplanRepository.save(chapter2);
        chapter2Id = chapter2.getId();

        // 创建第一章下的小节
        Teachplan section1 = new Teachplan();
        section1.setName("第一节");
        section1.setParentId(chapter1Id);
        section1.setLevel(2);
        section1.setOrderBy(1);
        section1.setCourseBase(courseBase);
        section1.setCreateTime(new Date());
        section1.setUpdateTime(new Date());
        teachplanRepository.save(section1);
        section1Id = section1.getId();

        // 创建第一章下的第二个小节
        Teachplan section2 = new Teachplan();
        section2.setName("第二节");
        section2.setParentId(chapter1Id);
        section2.setLevel(2);
        section2.setOrderBy(2);
        section2.setCourseBase(courseBase);
        section2.setCreateTime(new Date());
        section2.setUpdateTime(new Date());
        teachplanRepository.save(section2);
        section2Id = section2.getId();
    }
} 