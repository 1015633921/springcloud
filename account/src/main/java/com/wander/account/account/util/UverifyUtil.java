package com.wander.account.account.util;

import com.alibaba.fastjson.JSONObject;
import com.alipay.api.internal.util.codec.Base64;
import org.apache.commons.codec.digest.DigestUtils;
import org.apache.http.Header;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.client.methods.HttpPost;
import org.apache.http.entity.StringEntity;
import org.apache.http.impl.client.CloseableHttpClient;
import org.apache.http.impl.client.HttpClients;

import javax.crypto.Mac;
import javax.crypto.spec.SecretKeySpec;
import java.nio.charset.StandardCharsets;
import java.security.InvalidKeyException;
import java.security.NoSuchAlgorithmException;
import java.util.HashMap;
import java.util.Map;
import java.util.UUID;

public class UverifyUtil {
    public static final String UM_APP_KEY = "5f73360580455950e49c5fda";
    public static final String APP_KEY = "204039629";
    public static final String APP_SECRET = "ahbRKMXoR92xUIdfI4eMoANx2IA3FYdb";

    public static CloseableHttpResponse queryPhoneNumFromUM(String PhoneToken) throws InvalidKeyException, NoSuchAlgorithmException {
        String umAppkey = UM_APP_KEY;
        String appKey = APP_KEY;
        String appSecret = APP_SECRET;
        String token = PhoneToken;
        // 下面的url要和阿里云云市场购买的商品对应
        String url = "https://verify5.market.alicloudapi.com/api/v1/mobile/info?appkey=" + umAppkey;
        HttpPost httpPost = new HttpPost(url);
        /**
         * body
         */
        JSONObject object = new JSONObject();
        object.put("token", token);
        StringEntity stringEntity = new StringEntity(object.toJSONString(), StandardCharsets.UTF_8);
        httpPost.setEntity(stringEntity);
        /**
         * header
         */
        httpPost.setHeader("Content-Type", "application/json; charset=UTF-8");
        httpPost.setHeader("Accept", "application/json");
        httpPost.setHeader("X-Ca-Version", "1");
        httpPost.setHeader("X-Ca-Signature-Headers", "X-Ca-Version,X-Ca-Stage,X-Ca-Key,X-Ca-Timestamp");
        httpPost.setHeader("X-Ca-Stage", "RELEASE");
        httpPost.setHeader("X-Ca-Key", appKey);
        httpPost.setHeader("X-Ca-Timestamp", String.valueOf(System.currentTimeMillis()));
        httpPost.setHeader("X-Ca-Nonce", UUID.randomUUID().toString());
        httpPost.setHeader("Content-MD5", Base64.encodeBase64String(DigestUtils.md5(object.toJSONString())));
        /**
         * sign
         */
        String stringToSign = getSignString(httpPost);
        Mac hmacSha256 = Mac.getInstance("HmacSHA256");
        byte[] keyBytes = appSecret.getBytes(StandardCharsets.UTF_8);
        hmacSha256.init(new SecretKeySpec(keyBytes, 0, keyBytes.length, "HmacSHA256"));
        String sign = new String(Base64.encodeBase64(hmacSha256.doFinal(stringToSign.getBytes(StandardCharsets.UTF_8))));//appSecret
        httpPost.setHeader("X-Ca-Signature", sign);
        /**
         * execute
         */
        CloseableHttpClient httpclient = HttpClients.createDefault();
        try {
            CloseableHttpResponse response = httpclient.execute(httpPost);
            return response;
        } catch (Exception e) {
            e.printStackTrace();
        }
        return null;
    }

    private static String getSignString(HttpPost httpPost) {
        Header[] headers = httpPost.getAllHeaders();
        Map<String, String> map = new HashMap<>();
        for (Header header : headers) {
            map.put(header.getName(), header.getValue());
        }
        return httpPost.getMethod() + "\n" +
                map.get("Accept") + "\n" +
                map.get("Content-MD5") + "\n" +
                map.get("Content-Type") + "\n\n" +
                "X-Ca-Key:" + map.get("X-Ca-Key") + "\n" +
                "X-Ca-Stage:" + map.get("X-Ca-Stage") + "\n" +
                "X-Ca-Timestamp:" + map.get("X-Ca-Timestamp") + "\n" +
                "X-Ca-Version:" + map.get("X-Ca-Version") + "\n" +
                httpPost.getURI().getPath() + "?" + httpPost.getURI().getQuery();
    }
}
