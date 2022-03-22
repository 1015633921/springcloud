package com.wander.account.account.util;

import com.wander.cloud.net.NetConfig;

/**
 * @author linlongxin
 * @date 2020/4/3 4:12 下午
 */
public class AppConfig {

    private static final String DOMAIN_TEST = "http://doubi.soutushenqi.com";
    private static final String DOMAIN_PRODUCT = "http://wallpaper.soutushenqi.com";

    public static final int PLATFORM_ANDROID = 0;
    public static final int PLATFORM_IOS = 1;


    public static String getServerDomain() {
        if (NetConfig.isTestEnv()) {
            return DOMAIN_TEST;
        } else {
            return DOMAIN_PRODUCT;
        }
    }
}
