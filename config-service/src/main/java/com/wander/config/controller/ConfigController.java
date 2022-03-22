package com.wander.config.controller;

import com.wander.cloud.net.exception.ParamsException2;
import com.wander.cloud.net.reponse.ResponseData;
import com.wander.cloud.net.request.NetRequestUtils;
import com.wander.cloud.utils.TextUtils;
import com.wander.config.service.IConfigService;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author DongYu
 * @description:
 * @date 2021-05-19-35
 **/
@RestController
@RequestMapping("/v1/config")
@FeignClient(name = "config-service")
public class ConfigController {

    private final IConfigService configService;

    public ConfigController(IConfigService configService) {
        this.configService = configService;
        configService.loadConfigToRedis();
    }

    /**
     * 添加一个配置信息
     *
     * @param request
     */
    @PostMapping(value = "/addConfig")
    public ResponseData addConfig(HttpServletRequest request) throws Exception {
        Map<String, String> map = NetRequestUtils.parseRequest(request);
        String configKey = map.get("config_key");
        String configValue = map.get("config_value");
        String title = map.get("title");
        String productList = map.get("product_list");
        if (TextUtils.isEmpty(configKey)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        return ResponseData.buildSuccessData(configService.addConfig(configKey, configValue, title, productList));
    }

    /**
     * 更新一个配置信息
     *
     * @param request
     */
    @PostMapping(value = "/updateConfig")
    public ResponseData updateConfig(HttpServletRequest request) throws Exception {
        Map<String, String> map = NetRequestUtils.parseRequest(request);
        String configKey = map.get("config_key");
        String configValue = map.get("config_value");
        String title = null;
        String productList = null;
        if (map.containsKey("title")) {
            title = map.get("title");
        }
        if (map.containsKey("product_list")) {
            productList = map.get("product_list");
        }
        if (TextUtils.isEmpty(configKey)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        return ResponseData.buildSuccessData(configService.updateConfigV2(configKey, configValue, title, productList));
    }

    /**
     * 查询一个配置信息
     *
     * @param request
     */
    @PostMapping(value = "/queryConfig")
    public ResponseData queryConfig(HttpServletRequest request) throws Exception {
        Map<String, String> map = NetRequestUtils.parseRequest(request);
        String configKey = map.get("config_key");
        if (TextUtils.isEmpty(configKey)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        String configInfo = configService.getConfigInfo(configKey);
        return ResponseData.buildSuccessData(configInfo);
    }

    /**
     * 下发配置列表到运营端
     *
     * @param request
     * @return
     */
    @PostMapping(value = "/configList")
    public ResponseData queryConfigList(HttpServletRequest request) {
        Map<String, String> param = NetRequestUtils.parseRequest(request);
        int page = 0;
        int pageSize = 30;
        String title = null;
        long productCode = -1;
        if (param.containsKey("product_code")) {
            productCode = Long.parseLong(param.get("product_code"));
        }
        if (param.containsKey("title")) {
            title = param.get("title");
            if (TextUtils.isEmpty(title)) {
                title = null;
            }
        }
        if (param.containsKey("page")) {
            page = Integer.parseInt(param.get("page"));
        }
        if (param.containsKey("page_size")) {
            pageSize = Integer.parseInt(param.get("page_size"));
        }
        return ResponseData.buildSuccessData(configService.queryConfigList(title, page, pageSize, productCode));
    }
}
