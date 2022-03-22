package com.wander.account.account;

import com.wander.cloud.net.security.JwtUtil;
import com.wander.cloud.utils.TextUtils;
import com.wander.cloud.utils.Utils;

/**
 * @author linlongxin
 * @date 2020/12/2 6:38 下午
 */
public class TokenUtils {

    private static final String PREFIX_TOKEN = "wallpaper_token_";
    // 一年过期
    public static final long TOKEN_CACHE_TIME_MS = 365 * 24 * 60 * 60 * 1000L;

    /**
     * NOTE：使用该方法前，请确认校验过 token 是否有效（redis 中是否存在token）
     * 根据 token 取出对应的 userId
     *
     * @param token
     * @return
     */
    public static String getUserIdFromToken(String token) {
        if (TextUtils.isEmpty(token)) {
            return null;
        }
        String[] infos = parseToken(token);
        if (infos != null && infos.length > 0) {
            return infos[0];
        }
        return null;
    }

    /**
     * NOTE：使用该方法前，请确认校验过 token 是否有效（redis 中是否存在token）
     * 根据 token 取出对应的 device_id
     *
     * @param token
     * @return
     */
    public static String getDeviceIdFromToken(String token) {
        if (TextUtils.isEmpty(token)) {
            return null;
        }
        String[] infos = parseToken(token);
        if (infos != null && infos.length > 2) {
            return infos[2];
        }
        return null;
    }

    /**
     * 获取 product_id
     * @param token
     * @return
     */
    public static int getProductIdFromToken(String token) {
        if (TextUtils.isEmpty(token)) {
            return -1;
        }
        String[] infos = parseToken(token);
        try {
            if (infos != null && infos.length > 1) {
                return Integer.parseInt(infos[1]);
            }
        } catch (Throwable e){
            return -1;
        }
        return -1;
    }

    private static String[] parseToken(String token) {
        String value = JwtUtil.getValueByToken(token);
        if (!TextUtils.isEmpty(value) && value.startsWith(PREFIX_TOKEN)) {
            value = value.replace(PREFIX_TOKEN, "");
            return value.split("_");
        }
        return null;
    }

    /**
     * token 格式：wallpaper_token_ + userId + "_" + deviceId
     *
     * @param userId
     * @return
     */
    public static String createToken(String userId, String deviceId, int productCode) {
        String secret = Utils.createRandomStr(8);
        String value = getTokenCacheKey(userId, deviceId, productCode);
        return JwtUtil.createToken(value, secret, TOKEN_CACHE_TIME_MS);
    }

    /**
     * token 会被缓存在 redis 中
     * key：wallpaper_token_ + userId + "_" productCode + "_" + deviceId
     * value：token
     *
     * @param userId
     * @return
     */
    public static String getTokenCacheKey(String userId, String deviceId, int productCode) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        return PREFIX_TOKEN + userId + "_" + productCode + "_" + deviceId;
    }

    /**
     * token 会被缓存在 redis 中
     * key：wallpaper_token_ + userId + "_"
     * value：token
     *
     * @param userId
     * @return
     */
    public static String getTokenCacheKeyPrefix(String userId, int productCode) {
        if (TextUtils.isEmpty(userId)) {
            return null;
        }
        return PREFIX_TOKEN + userId + "_" + productCode + "_";
    }

    /**
     * 根据token 获取 token 的 value
     * @param token
     * @return
     */
    public static String getValueFromToken(String token){
        if (TextUtils.isEmpty(token)) {
            return null;
        }
        return JwtUtil.getValueByToken(token);
    }
}
