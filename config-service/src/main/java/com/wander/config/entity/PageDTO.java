package com.wander.config.entity;

import com.google.gson.annotations.Expose;

import java.util.List;

/**
 * @author DongYu
 * @description: 分页DTO
 * @date 2021-09-17-18
 **/
public class PageDTO<T> {
    @Expose
    public List<T> data;
    //总数
    @Expose
    public long count;

}
