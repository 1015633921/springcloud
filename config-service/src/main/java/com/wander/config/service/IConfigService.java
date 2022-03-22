package com.wander.config.service;


import com.wander.cloud.net.exception.BusinessException2;
import com.wander.config.entity.ConfigEntity;

import java.util.List;

/**
 * @author DongYu
 * @description:
 * @date 2021-05-19-41
 **/
public interface IConfigService {

    /**
     * 更新一个配置信息
     *
     * @param configValue
     * @param configKey
     */
    void updateConfig(String configKey, String configValue);

    /**
     * 查询一个配置信息
     *
     * @param configKey
     */
    String getConfigInfo(String configKey) throws Exception;

    /**
     * 加载 config 表的配置到 redis 缓存
     */
    void loadConfigToRedis();

    /**
     * 配置列表下发
     *
     * @param title
     * @param page
     * @param pageSize
     * @return
     */
    List<ConfigEntity> queryConfigList(String title, int page, int pageSize, long productList);

    /**
     * 更新配置
     *
     * @param configKey
     * @param configValue
     * @param title
     * @return
     */
    ConfigEntity updateConfigV2(String configKey, String configValue, String title, String productList) throws BusinessException2;

    /***
     * 增加一个配置
     * @param configKey
     * @param configValue
     * @param title
     * @return
     */
    ConfigEntity addConfig(String configKey, String configValue, String title, String productList) throws BusinessException2;
}
