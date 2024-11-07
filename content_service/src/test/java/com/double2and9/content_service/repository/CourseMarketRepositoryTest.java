package com.double2and9.content_service.repository;

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
@Transactional
@Rollback
public class CourseMarketRepositoryTest {

    @Autowired
    private CourseMarketRepository courseMarketRepository;

    @Test
    void testSaveCourseMarket() {
        // 创建测试数据
        CourseMarket courseMarket = new CourseMarket();
        courseMarket.setId(1L); // 注意这里需要设置ID，因为它是与CourseBase关联的
        courseMarket.setCharge("201001");
        courseMarket.setPrice(new BigDecimal("99.99"));
        courseMarket.setPriceOld(new BigDecimal("199.99"));
        courseMarket.setDiscounts("限时优惠");
        courseMarket.setValid(true);
        courseMarket.setCreateTime(new Date());
        courseMarket.setUpdateTime(new Date());

        // 保存
        CourseMarket saved = courseMarketRepository.save(courseMarket);
        
        // 验证
        assertNotNull(saved);
        assertEquals(new BigDecimal("99.99"), saved.getPrice());
        assertTrue(saved.getValid());
    }

    @Test
    void testFindByPriceBetween() {
        // 创建测试数据
        CourseMarket courseMarket = new CourseMarket();
        courseMarket.setId(1L);
        courseMarket.setPrice(new BigDecimal("150.00"));
        courseMarket.setValid(true);
        courseMarket.setCreateTime(new Date());
        courseMarket.setUpdateTime(new Date());
        courseMarketRepository.save(courseMarket);

        // 测试查询
        List<CourseMarket> markets = courseMarketRepository.findByPriceBetween(
            new BigDecimal("100.00"), 
            new BigDecimal("200.00")
        );
        
        // 验证
        assertFalse(markets.isEmpty());
        assertTrue(markets.stream().anyMatch(market -> 
            market.getPrice().compareTo(new BigDecimal("150.00")) == 0));
    }
} 