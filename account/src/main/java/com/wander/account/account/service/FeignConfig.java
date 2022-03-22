package com.wander.account.account.service;

import com.wander.cloud.net.reponse.ResponseData;
import org.springframework.cloud.openfeign.FeignClient;
import org.springframework.web.bind.annotation.PostMapping;

@FeignClient(name = "config")
public interface FeignConfig {
    @PostMapping("/feign/getconfig")
    public ResponseData queryConfig();
}
