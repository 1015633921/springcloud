package com.wander.config.service;

import com.google.gson.JsonArray;
import com.wander.cloud.net.exception.BusinessException2;
import com.wander.cloud.net.exception.ParamsException2;
import com.wander.cloud.utils.BitOperationUtils;
import com.wander.cloud.utils.CollectionUtil;
import com.wander.cloud.utils.TextUtils;
import com.wander.cloud.utils.json.GsonUtil;
import com.wander.config.controller.IConfigRepository;
import com.wander.config.entity.ConfigEntity;
import com.wander.config.entity.dto.ConfigDTO;
import com.wander.config.entity.dto.ConfigEntityDTO;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * @author DongYu
 * @description:
 * @date 2021-05-19-43
 **/
@Service
public class ConfigServiceImpl implements IConfigService {

    private static final String REDIS_PREFIX = "cm_config_cache_";
    @Autowired
    private StringRedisTemplate mRedis;

    @Autowired
    private IConfigRepository mConfigRepository;

    /**
     * 更新一个配置信息
     *
     * @param configKey
     */
    @Override
    public void updateConfig(String configKey, String configValue) {
        ConfigEntity entity = mConfigRepository.queryOneConfig(configKey);
        if (entity == null) {
            entity = new ConfigEntity();
            entity.configKey = configKey;
            entity.value = configValue;
        } else {
            entity.value = configValue;
        }
        mConfigRepository.save(entity);
        String cacheKey = getCacheKey(configKey);
        mRedis.opsForValue().set(cacheKey, configValue);
    }

    /**
     * 查询一个配置信息
     *
     * @param configKey
     */
    @Override
    public String getConfigInfo(String configKey) throws Exception {
        if (mRedis.hasKey(getCacheKey(configKey))) {
            return mRedis.opsForValue().get(getCacheKey(configKey));
        } else {
            ConfigEntity entity = mConfigRepository.queryOneConfig(configKey);
            if (entity == null) {
                throw ParamsException2.buildBusinessParamsError("该配置信息不存在");
            }
            return entity.value;
        }
    }

    @Override
    public void loadConfigToRedis() {
        List<ConfigEntityDTO> dtoList = mConfigRepository.queryAll();
        if (CollectionUtil.isEmpty(dtoList)) {
            return;
        }
        Set<String> keys = mRedis.keys(REDIS_PREFIX + "*");
        if (!CollectionUtil.isEmpty(keys)) {
            mRedis.delete(keys);
        }
        // 存到 redis
        for (ConfigEntityDTO dto : dtoList) {
            String key = dto.getConfigKey();
            String value = dto.getValue();
            if (TextUtils.isEmpty(key) || TextUtils.isEmpty(value)) {
                continue;
            }
            mRedis.opsForValue().set(getCacheKey(key), value);
        }
    }

    private String getCacheKey(String configKey) {
        return REDIS_PREFIX + configKey;
    }

    /**
     * 运营端配置列表下发
     *
     * @param title
     * @return
     */
    @Override
    public List<ConfigEntity> queryConfigList(String title, int page, int pageSize, long productCode) {
        int start = page * pageSize;
        List<ConfigDTO> configDTOS;
        if (productCode > -1) {
            configDTOS = mConfigRepository.queryList(title, start, pageSize, productCode);
        } else {
            configDTOS = mConfigRepository.queryListAll(title, start, pageSize);
        }
        List<ConfigEntity> res = new LinkedList<>();
        for (ConfigDTO dto : configDTOS) {
            ConfigEntity entity = new ConfigEntity();
            entity.configKey = dto.getConfigKey();
            entity.value = dto.getValue();
            entity.title = dto.getTitle();
            entity.productCode = dto.getProductCode();
            List<Integer> list = new ArrayList<>();
            for (int i = 1; i < 7; i++) {
                if (BitOperationUtils.hasFlag(entity.productCode, (int) (Math.pow(2, i)))) {
                    list.add((int) (Math.pow(2, i)));
                }
            }
            entity.productList = list;
            res.add(entity);
        }
        return res;
    }

    /**
     * 修改配置
     *
     * @param configKey
     * @param configValue
     * @param title
     * @return
     */
    @Override
    public ConfigEntity updateConfigV2(String configKey, String configValue, String title, String productList) throws BusinessException2 {
        ConfigEntity entity = mConfigRepository.queryOneConfig(configKey);
        if (entity == null) {
            throw BusinessException2.buildError("该配置信息不存在");
        }
        if (!TextUtils.isEmpty(productList)) {
            JsonArray jsonArray = GsonUtil.toJsonArray(productList);
            long productCode = jsonArray.get(0).getAsLong();
            for (int i = 1; i < jsonArray.size(); i++) {
                productCode = BitOperationUtils.setFlag(productCode, jsonArray.get(i).getAsLong());
            }
            entity.productCode = productCode;
        }
        entity.value = configValue;
        if (!TextUtils.isEmpty(title)) {
            entity.title = title;
        }
        mConfigRepository.save(entity);
        String cacheKey = getCacheKey(configKey);
        mRedis.opsForValue().set(cacheKey, configValue);
        return entity;
    }

    /**
     * 增加一个配置
     *
     * @param configKey
     * @param configValue
     * @param title
     * @return
     */
    @Override
    public ConfigEntity addConfig(String configKey, String configValue, String title, String productList) throws BusinessException2 {
        if (mConfigRepository.queryOneConfig(configKey) != null) {
            throw BusinessException2.buildError("该key已经存在!");
        }
        JsonArray jsonArray = GsonUtil.toJsonArray(productList);
        long productCode = jsonArray.get(0).getAsLong();
        for (int i = 1; i < jsonArray.size(); i++) {
            productCode = BitOperationUtils.setFlag(productCode, jsonArray.get(i).getAsLong());
        }
        ConfigEntity entity = new ConfigEntity();
        entity.productCode = productCode;
        entity.configKey = configKey;
        entity.value = configValue;
        entity.title = title;
        mConfigRepository.save(entity);
        String cacheKey = getCacheKey(configKey);
        mRedis.opsForValue().set(cacheKey, configValue);
        return entity;
    }
}
