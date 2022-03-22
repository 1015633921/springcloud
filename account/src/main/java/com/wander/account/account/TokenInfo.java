package com.wander.account.account;

import java.io.Serializable;
import java.util.Objects;

/**
 * @author linlongxin
 * @date 2021/4/13 5:27 下午
 */
public class TokenInfo implements Serializable {

    private static final long serialVersionUID = 8234328654029308871L;

    // 登录的 token
    public String token;
    // 设备id
    public String deviceId;
    // 上次登录时间
    public long lastActiveTime;
    // 产品id
    public int productcCode;

    public TokenInfo(String token, String deviceId, int productcCode) {
        this.token = token;
        this.deviceId = deviceId;
        this.productcCode = productcCode;
        lastActiveTime = System.currentTimeMillis();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (o == null || getClass() != o.getClass()) return false;
        TokenInfo tokenInfo = (TokenInfo) o;
        return Objects.equals(token, tokenInfo.token) && Objects.equals(deviceId, tokenInfo.deviceId) && Objects.equals(productcCode, tokenInfo.productcCode);
    }

    @Override
    public int hashCode() {
        return Objects.hash(token, deviceId, productcCode);
    }
}
