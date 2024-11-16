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
        // 1. 先创建并保存CourseBase
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase.setCreateTime(new Date());
        courseBase.setUpdateTime(new Date());
        courseBase = courseBaseRepository.save(courseBase);

        // 2. 创建CourseMarket
        CourseMarket courseMarket = new CourseMarket();
        courseMarket.setId(courseBase.getId());  // 先设置ID
        courseMarket.setPrice(new BigDecimal("150.00"));
        courseMarket.setValid(true);
        courseMarket.setCreateTime(new Date());
        courseMarket.setUpdateTime(new Date());
        
        // 3. 建立双向关联
        courseMarket.setCourseBase(courseBase);
        courseBase.setCourseMarket(courseMarket);
        
        // 4. 通过CourseBase保存（利用级联保存）
        courseBase = courseBaseRepository.save(courseBase);

        // 5. 测试查询
        List<CourseMarket> markets = courseMarketRepository.findByPriceBetween(
            new BigDecimal("100.00"), 
            new BigDecimal("200.00")
        );
        
        // 6. 验证
        assertFalse(markets.isEmpty());
        assertTrue(markets.stream().anyMatch(market -> 
            market.getPrice().compareTo(new BigDecimal("150.00")) == 0));
        
        // 7. 验证关联关系
        CourseMarket foundMarket = markets.get(0);
        assertNotNull(foundMarket.getCourseBase());
        assertEquals(courseBase.getName(), foundMarket.getCourseBase().getName());
    }
} 