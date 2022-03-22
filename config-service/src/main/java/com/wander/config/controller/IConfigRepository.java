package com.wander.config.controller;


import com.wander.config.entity.ConfigEntity;
import com.wander.config.entity.dto.ConfigDTO;
import com.wander.config.entity.dto.ConfigEntityDTO;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.CrudRepository;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;


/**
 * describe：所有业务配置相关的 DAO
 * create time：2020/7/10 9:42 上午
 * author：linlongxin
 */
@Repository
public interface IConfigRepository extends CrudRepository<ConfigEntity, Long> {

    /**
     * 查询一个配置信息，比如：获取一个App的下载链接
     */
    @Query(nativeQuery = true, value = "select c.config_key as configKey, c.value from config c")
    List<ConfigEntityDTO> queryAll();

    /**
     * 查询一个配置信息，比如：获取一个App的下载链接
     *
     * @param key
     * @return
     */
    @Query(nativeQuery = true, value = "select * from config c where config_key = :key limit 1")
    ConfigEntity queryOneConfig(@Param("key") String key);

    /**
     * @param value
     * @param configKey
     */
    @Transactional
    @Modifying
    @Query(nativeQuery = true, value = "update config c set c.value = :value where c.config_key = :config_key")
    void updateOneConfig(@Param("config_key") String configKey, @Param("value") String value);


    @Query(nativeQuery = true, value = "select c.config_key as configKey, c.value, c.title,c.product_code as " +
            "productCode from config c where if(:title IS NULL, 1=1, (c.title like %:title% or c.title like %:title%)" +
            ")  and if(:product_code " +
            ">0,(:product_code & product_code) = " +
            ":product_code,product_code =0)  order by update_time desc limit :start,:page_size")
    List<ConfigDTO> queryList(@Param("title") String title, @Param("start") int start, @Param("page_size") int pageSize, @Param("product_code") long product_code);

    @Query(nativeQuery = true, value = "select c.config_key as configKey, c.value, c.title,c.product_code as " +
            "productCode from config c where if(:title IS NULL, 1=1, c.title like %:title% or c.config_key like " +
            "%:title%)  order by " +
            "update_time " +
            "desc limit :start,:page_size")
    List<ConfigDTO> queryListAll(@Param("title") String title, @Param("start") int start, @Param("page_size") int pageSize);

}
