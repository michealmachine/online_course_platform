package com.double2and9.content_service.utils;

import com.double2and9.content_service.dto.base.TreeNodeDTO;
import org.junit.jupiter.api.Test;
import lombok.Data;
import lombok.EqualsAndHashCode;

import java.util.Arrays;
import java.util.List;

import static org.junit.jupiter.api.Assertions.*;

class TreeUtilsTest {

    @Data
    @EqualsAndHashCode(callSuper = true)
    static class TestNode extends TreeNodeDTO<TestNode> {
        private String value;
    }

    @Test
    void buildTree_WithValidData_ShouldCreateCorrectTree() {
        // 1. 准备测试数据
        TestNode root = createNode(1L, null, "Root");
        TestNode child1 = createNode(2L, 1L, "Child1");
        TestNode child2 = createNode(3L, 1L, "Child2");
        TestNode grandChild1 = createNode(4L, 2L, "GrandChild1");
        
        List<TestNode> nodes = Arrays.asList(root, child1, child2, grandChild1);

        // 2. 执行测试
        List<TestNode> result = TreeUtils.buildTree(
                nodes,
                TestNode::getId,
                TestNode::getParentId
        );

        // 3. 验证结果
        // 验证根节点
        assertEquals(1, result.size());
        TestNode resultRoot = result.get(0);
        assertEquals("Root", resultRoot.getValue());
        
        // 验证子节点
        List<TestNode> children = resultRoot.getChildren();
        assertEquals(2, children.size());
        assertTrue(children.stream().anyMatch(node -> "Child1".equals(node.getValue())));
        assertTrue(children.stream().anyMatch(node -> "Child2".equals(node.getValue())));
        
        // 验证孙节点
        TestNode child = children.stream()
                .filter(node -> "Child1".equals(node.getValue()))
                .findFirst()
                .orElse(null);
        assertNotNull(child);
        assertNotNull(child.getChildren());
        assertEquals(1, child.getChildren().size());
        assertEquals("GrandChild1", child.getChildren().get(0).getValue());
    }

    @Test
    void buildTree_WithEmptyList_ShouldReturnEmptyList() {
        List<TestNode> result = TreeUtils.buildTree(
                List.of(),
                TestNode::getId,
                TestNode::getParentId
        );
        assertTrue(result.isEmpty());
    }

    @Test
    void buildTree_WithOnlyRootNode_ShouldReturnSingleNode() {
        TestNode root = createNode(1L, null, "Root");
        
        List<TestNode> result = TreeUtils.buildTree(
                List.of(root),
                TestNode::getId,
                TestNode::getParentId
        );
        
        assertEquals(1, result.size());
        assertNull(result.get(0).getChildren());
    }

    @Test
    void buildTree_WithMissingParent_ShouldHandleGracefully() {
        TestNode orphanNode = createNode(1L, 999L, "Orphan");
        
        List<TestNode> result = TreeUtils.buildTree(
                List.of(orphanNode),
                TestNode::getId,
                TestNode::getParentId
        );
        
        assertTrue(result.isEmpty());
    }

    @Test
    void buildTree_WithMultipleRoots_ShouldCreateMultipleTrees() {
        TestNode root1 = createNode(1L, null, "Root1");
        TestNode root2 = createNode(2L, null, "Root2");
        TestNode child1 = createNode(3L, 1L, "Child1");
        
        List<TestNode> result = TreeUtils.buildTree(
                Arrays.asList(root1, root2, child1),
                TestNode::getId,
                TestNode::getParentId
        );
        
        assertEquals(2, result.size());
        assertTrue(result.stream().anyMatch(node -> "Root1".equals(node.getValue())));
        assertTrue(result.stream().anyMatch(node -> "Root2".equals(node.getValue())));
    }

    private TestNode createNode(Long id, Long parentId, String value) {
        TestNode node = new TestNode();
        node.setId(id);
        node.setParentId(parentId);
        node.setValue(value);
        return node;
    }
} 