package com.double2and9.content_service.repository;

import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.CourseMarket;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.annotation.Rollback;
import org.springframework.transaction.annotation.Transactional;

import java.math.BigDecimal;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

@SpringBootTest
public class CourseMarketRepositoryTest {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private CourseMarketRepository courseMarketRepository;

    @Autowired
    private CourseBaseRepository courseBaseRepository;

    @Test
    @Transactional
    void testSaveCourseMarket() {
        // 1. 创建并保存CourseBase
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setBrief("测试课程简介");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        courseBase = courseBaseRepository.save(courseBase);
        
        // 2. 创建CourseMarket
        CourseMarket courseMarket = new CourseMarket();
        courseMarket.setId(courseBase.getId());
        courseMarket.setCharge("201001");
        courseMarket.setPrice(new BigDecimal("99.99"));
        courseMarket.setPriceOld(new BigDecimal("199.99"));
        courseMarket.setDiscounts("限时优惠");
        courseMarket.setValid(true);
        courseMarket.setCreateTime(new Date());
        courseMarket.setUpdateTime(new Date());
        
        // 3. 建立双向关联
        courseMarket.setCourseBase(courseBase);
        courseBase.setCourseMarket(courseMarket);
        
        // 4. 保存CourseBase（会级联保存CourseMarket）
        courseBase = courseBaseRepository.save(courseBase);
        
        // 5. 验证
        assertNotNull(courseBase.getCourseMarket());
        assertEquals(courseBase.getId(), courseBase.getCourseMarket().getId());
        assertEquals(new BigDecimal("99.99"), courseBase.getCourseMarket().getPrice());
    }

    @Test
    @Transactional
    void testFindByPriceBetween() {
        // 1. 创建并保存CourseBase
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setBrief("测试课程简介");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        courseBase = courseBaseRepository.save(courseBase);
        
        // 2. 创建并保存CourseMarket
        CourseMarket courseMarket = new CourseMarket();
        courseMarket.setId(courseBase.getId());  // 设置ID，与CourseBase共享ID
        courseMarket.setCourseBase(courseBase);  // 设置关联关系
        courseMarket.setPrice(BigDecimal.valueOf(100));
        courseMarket.setCharge("201001");       // 设置收费规则
        courseMarket.setValid(true);
        courseMarket.setCreateTime(new Date());
        courseMarket.setUpdateTime(new Date());
        
        // 建立双向关联
        courseBase.setCourseMarket(courseMarket);
        courseBaseRepository.save(courseBase);   // 通过级联保存CourseMarket
        
        // 3. 执行测试查询
        List<CourseMarket> result = courseMarketRepository.findByPriceBetween(
            BigDecimal.valueOf(50), 
            BigDecimal.valueOf(150)
        );

        // 4. 验证结果
        assertFalse(result.isEmpty(), "查询结果不应为空");
        assertEquals("测试课程", result.get(0).getCourseBase().getName(), "课程名称不匹配");
        assertEquals(BigDecimal.valueOf(100), result.get(0).getPrice(), "课程价格不匹配");
    }
} 