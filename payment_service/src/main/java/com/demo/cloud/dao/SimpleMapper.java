package com.demo.cloud.dao;

import org.apache.ibatis.annotations.Select;

/**
 * @author lqq
 * @date 2020/4/20
 */
public interface SimpleMapper {

    @Select("select 1")
    int count();
}
