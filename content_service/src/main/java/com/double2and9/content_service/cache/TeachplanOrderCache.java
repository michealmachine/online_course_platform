package com.double2and9.content_service.cache;

import com.double2and9.content_service.entity.Teachplan;
import com.double2and9.content_service.repository.TeachplanRepository;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Slf4j
@Component
public class TeachplanOrderCache {
    // 使用ConcurrentHashMap保证线程安全
    private final Map<Long, Integer> orderCache = new ConcurrentHashMap<>();
    private final TeachplanRepository teachplanRepository;

    public TeachplanOrderCache(TeachplanRepository teachplanRepository) {
        this.teachplanRepository = teachplanRepository;
    }

    /**
     * 缓存排序变更(仅内存操作)
     */
    public void cacheOrderChange(Long teachplanId1, Integer order1, Long teachplanId2, Integer order2) {
        orderCache.put(teachplanId1, order1);
        orderCache.put(teachplanId2, order2);
        log.debug("缓存排序变更: [{},{}] <-> [{},{}]", teachplanId1, order1, teachplanId2, order2);
    }

    /**
     * 获取当前排序号(优先从缓存获取)
     */
    public Integer getCurrentOrder(Long teachplanId, Integer defaultOrder) {
        return orderCache.getOrDefault(teachplanId, defaultOrder);
    }

    /**
     * 保存所有排序变更到数据库
     */
    @Transactional
    public void saveAllChanges() {
        if (orderCache.isEmpty()) {
            return;
        }

        try {
            List<Teachplan> teachplans = teachplanRepository.findAllById(orderCache.keySet());
            teachplans.forEach(teachplan -> {
                Integer newOrder = orderCache.get(teachplan.getId());
                teachplan.setOrderBy(newOrder);
            });
            teachplanRepository.saveAll(teachplans);
            log.info("保存排序变更成功，更新数量：{}", teachplans.size());
        } finally {
            orderCache.clear();
        }
    }

    /**
     * 丢弃所有未保存的变更
     */
    public void discardChanges() {
        orderCache.clear();
        log.info("丢弃未保存的排序变更");
    }
} 