package com.double2and9.base.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

import java.util.List;

@Data
@ToString
@AllArgsConstructor
@NoArgsConstructor
public class PageResult<T> {
    // 数据列表
    private List<T> items;
    //总记录数
    private long counts;
    //当前⻚码
    private long page;
    //每⻚记录数
    private long pageSize;
}
