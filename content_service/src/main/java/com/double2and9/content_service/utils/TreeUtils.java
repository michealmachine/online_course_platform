package com.double2and9.content_service.utils;

import com.double2and9.content_service.dto.base.TreeNodeDTO;
import java.util.*;
import java.util.function.Function;

public class TreeUtils {

    /**
     * 构建树形结构
     * @param items 要构建树的节点列表
     * @param idGetter ID获取函数
     * @param parentIdGetter 父ID获取函数
     * @param <T> 节点类型
     * @param <ID> ID类型
     * @return 树形结构的根节点列表
     */
    public static <T extends TreeNodeDTO<T>, ID> List<T> buildTree(
            Collection<T> items,
            Function<T, ID> idGetter,
            Function<T, ID> parentIdGetter) {
        
        // 1. 创建ID到节点的映射
        Map<ID, T> nodeMap = new HashMap<>();
        for (T item : items) {
            nodeMap.put(idGetter.apply(item), item);
        }

        List<T> roots = new ArrayList<>();
        
        // 2. 构建树形结构
        for (T node : items) {
            ID parentId = parentIdGetter.apply(node);
            
            // 如果是根节点
            if (parentId == null || parentId.equals(0L)) {
                roots.add(node);
                continue;
            }
            
            // 找到父节点并添加子节点
            T parent = nodeMap.get(parentId);
            if (parent != null) {
                if (parent.getChildren() == null) {
                    parent.setChildren(new ArrayList<>());
                }
                parent.getChildren().add(node);
            }
        }

        return roots;
    }
} 