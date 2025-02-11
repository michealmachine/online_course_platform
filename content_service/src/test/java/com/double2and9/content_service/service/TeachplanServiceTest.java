package com.double2and9.content_service.service;

import com.double2and9.base.enums.ContentErrorCode;
import com.double2and9.base.enums.CourseStatusEnum;
import com.double2and9.content_service.cache.TeachplanOrderCache;
import com.double2and9.content_service.common.exception.ContentException;
import com.double2and9.content_service.dto.SaveTeachplanDTO;
import com.double2and9.content_service.dto.TeachplanDTO;
import com.double2and9.content_service.entity.CourseBase;
import com.double2and9.content_service.entity.Teachplan;
import com.double2and9.content_service.repository.CourseBaseRepository;
import com.double2and9.content_service.repository.TeachplanRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Date;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

@SpringBootTest
@Transactional
class TeachplanServiceTest {

    private static final Long TEST_ORG_ID = 1234L;

    @Autowired
    private TeachplanService teachplanService;
    
    @Autowired
    private TeachplanRepository teachplanRepository;
    
    @Autowired
    private CourseBaseRepository courseBaseRepository;
    
    @Autowired
    private TeachplanOrderCache orderCache;

    private Long courseId;
    private Long chapter1Id;
    private Long chapter2Id;
    private Long section1Id;
    private Long section2Id;

    @BeforeEach
    void setUp() {
        // 1. 创建课程
        CourseBase courseBase = new CourseBase();
        courseBase.setName("测试课程");
        courseBase.setBrief("测试简介");
        courseBase.setOrganizationId(TEST_ORG_ID);
        courseBase.setStatus(CourseStatusEnum.DRAFT.getCode());
        courseBase.setCreateTime(LocalDateTime.now());
        courseBase.setUpdateTime(LocalDateTime.now());
        courseBaseRepository.save(courseBase);
        courseId = courseBase.getId();

        // 2. 创建章节
        SaveTeachplanDTO chapter1 = new SaveTeachplanDTO();
        chapter1.setCourseId(courseId);
        chapter1.setParentId(0L);
        chapter1.setLevel(1);
        chapter1.setName("第一章");
        chapter1.setOrderBy(1);
        chapter1Id = teachplanService.saveTeachplan(chapter1);

        SaveTeachplanDTO chapter2 = new SaveTeachplanDTO();
        chapter2.setCourseId(courseId);
        chapter2.setParentId(0L);
        chapter2.setLevel(1);
        chapter2.setName("第二章");
        chapter2.setOrderBy(2);
        chapter2Id = teachplanService.saveTeachplan(chapter2);

        // 3. 创建小节
        SaveTeachplanDTO section1 = new SaveTeachplanDTO();
        section1.setCourseId(courseId);
        section1.setParentId(chapter1Id);
        section1.setLevel(2);
        section1.setName("第一节");
        section1.setOrderBy(1);
        section1Id = teachplanService.saveTeachplan(section1);

        SaveTeachplanDTO section2 = new SaveTeachplanDTO();
        section2.setCourseId(courseId);
        section2.setParentId(chapter1Id);
        section2.setLevel(2);
        section2.setName("第二节");
        section2.setOrderBy(2);
        section2Id = teachplanService.saveTeachplan(section2);
    }

    @Test
    void findTeachplanTree_ShouldReturnCorrectStructure() {
        // 1. 获取课程计划树
        List<TeachplanDTO> tree = teachplanService.findTeachplanTree(courseId);

        // 2. 验证树结构
        assertEquals(2, tree.size(), "应该有两个章节");
        assertEquals("第一章", tree.get(0).getName());
        assertEquals("第二章", tree.get(1).getName());
        assertEquals(2, tree.get(0).getChildren().size(), "第一章应该有两个小节");
        assertEquals("第一节", tree.get(0).getChildren().get(0).getName());
        assertEquals("第二节", tree.get(0).getChildren().get(1).getName());
    }

    @Test
    void saveTeachplan_ShouldCreateAndUpdate() {
        // 1. 创建新的章节
        SaveTeachplanDTO newChapter = new SaveTeachplanDTO();
        newChapter.setCourseId(courseId);
        newChapter.setParentId(0L);
        newChapter.setLevel(1);
        newChapter.setName("第三章");
        newChapter.setOrderBy(3);
        Long newChapterId = teachplanService.saveTeachplan(newChapter);

        // 2. 验证创建结果
        Teachplan saved = teachplanRepository.findById(newChapterId).orElseThrow();
        assertEquals("第三章", saved.getName());
        assertEquals(3, saved.getOrderBy());

        // 3. 更新章节
        newChapter.setId(newChapterId);
        newChapter.setName("第三章(修改)");
        teachplanService.saveTeachplan(newChapter);

        // 4. 验证更新结果
        Teachplan updated = teachplanRepository.findById(newChapterId).orElseThrow();
        assertEquals("第三章(修改)", updated.getName());
    }

    @Test
    void deleteTeachplan_ShouldDeleteCorrectly() {
        // 1. 删除小节
        teachplanService.deleteTeachplan(section1Id);
        
        // 2. 验证小节已删除
        assertFalse(teachplanRepository.findById(section1Id).isPresent());
        
        // 3. 验证章节仍存在
        assertTrue(teachplanRepository.findById(chapter1Id).isPresent());
    }

    @Test
    void moveUp_ShouldOnlyUpdateCache() {
        // 1. 移动第二章向上(临时操作)
        teachplanService.moveUp(chapter2Id);

        // 2. 验证数据库中的顺序未变
        Teachplan chapter1 = teachplanRepository.findById(chapter1Id).orElseThrow();
        Teachplan chapter2 = teachplanRepository.findById(chapter2Id).orElseThrow();
        assertEquals(1, chapter1.getOrderBy(), "数据库中chapter1顺序不应改变");
        assertEquals(2, chapter2.getOrderBy(), "数据库中chapter2顺序不应改变");

        // 3. 验证缓存中的顺序已变
        assertEquals(2, orderCache.getCurrentOrder(chapter1Id, 1));
        assertEquals(1, orderCache.getCurrentOrder(chapter2Id, 2));
    }

    @Test
    void moveDown_ShouldOnlyUpdateCache() {
        // 1. 移动第一章向下(临时操作)
        teachplanService.moveDown(chapter1Id);

        // 2. 验证数据库中的顺序未变
        Teachplan chapter1 = teachplanRepository.findById(chapter1Id).orElseThrow();
        Teachplan chapter2 = teachplanRepository.findById(chapter2Id).orElseThrow();
        assertEquals(1, chapter1.getOrderBy(), "数据库中chapter1顺序不应改变");
        assertEquals(2, chapter2.getOrderBy(), "数据库中chapter2顺序不应改变");

        // 3. 验证缓存中的顺序已变
        assertEquals(2, orderCache.getCurrentOrder(chapter1Id, 1));
        assertEquals(1, orderCache.getCurrentOrder(chapter2Id, 2));
    }

    @Test
    void saveOrderChanges_ShouldUpdateDatabase() {
        // 1. 移动第二章向上(临时操作)
        teachplanService.moveUp(chapter2Id);

        // 2. 保存变更
        teachplanService.saveOrderChanges();

        // 3. 验证数据库中的顺序已更新
        Teachplan chapter1 = teachplanRepository.findById(chapter1Id).orElseThrow();
        Teachplan chapter2 = teachplanRepository.findById(chapter2Id).orElseThrow();
        assertEquals(2, chapter1.getOrderBy(), "数据库中chapter1顺序应该更新");
        assertEquals(1, chapter2.getOrderBy(), "数据库中chapter2顺序应该更新");
    }

    @Test
    void discardOrderChanges_ShouldNotUpdateDatabase() {
        // 1. 移动第二章向上(临时操作)
        teachplanService.moveUp(chapter2Id);

        // 2. 丢弃变更
        teachplanService.discardOrderChanges();

        // 3. 验证数据库中的顺序未变
        Teachplan chapter1 = teachplanRepository.findById(chapter1Id).orElseThrow();
        Teachplan chapter2 = teachplanRepository.findById(chapter2Id).orElseThrow();
        assertEquals(1, chapter1.getOrderBy(), "数据库中chapter1顺序不应改变");
        assertEquals(2, chapter2.getOrderBy(), "数据库中chapter2顺序不应改变");

        // 4. 验证缓存已清空
        assertEquals(1, orderCache.getCurrentOrder(chapter1Id, 1));
        assertEquals(2, orderCache.getCurrentOrder(chapter2Id, 2));
    }

    @Test
    void multipleMovesBeforeSave() {
        // 1. 执行多次移动操作
        teachplanService.moveUp(chapter2Id);  // 第二章上移
        teachplanService.moveDown(section1Id); // 第一节下移

        // 2. 保存所有变更
        teachplanService.saveOrderChanges();

        // 3. 验证最终顺序
        Teachplan chapter1 = teachplanRepository.findById(chapter1Id).orElseThrow();
        Teachplan chapter2 = teachplanRepository.findById(chapter2Id).orElseThrow();
        Teachplan section1 = teachplanRepository.findById(section1Id).orElseThrow();
        Teachplan section2 = teachplanRepository.findById(section2Id).orElseThrow();

        assertEquals(2, chapter1.getOrderBy());
        assertEquals(1, chapter2.getOrderBy());
        assertEquals(2, section1.getOrderBy());
        assertEquals(1, section2.getOrderBy());
    }

    @Test
    void moveUp_ShouldThrowException_WhenFirstNode() {
        // 尝试上移第一个节点
        ContentException exception = assertThrows(ContentException.class,
                () -> teachplanService.moveUp(chapter1Id));
        assertEquals(ContentErrorCode.TEACHPLAN_MOVE_ERROR, exception.getErrorCode());
    }

    @Test
    void moveDown_ShouldThrowException_WhenLastNode() {
        // 尝试下移最后一个节点
        ContentException exception = assertThrows(ContentException.class,
                () -> teachplanService.moveDown(chapter2Id));
        assertEquals(ContentErrorCode.TEACHPLAN_MOVE_ERROR, exception.getErrorCode());
    }
} 