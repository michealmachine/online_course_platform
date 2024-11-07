package com.double2and9.base.model;

import lombok.AllArgsConstructor;
import lombok.Data;
import lombok.NoArgsConstructor;
import lombok.ToString;

@Data
@NoArgsConstructor
@AllArgsConstructor
@ToString
public class PageParams {
    //当前页码
    private Long pageNo = 1L;

    //每页记录数默认值
    private Long pageSize =10L;

}
