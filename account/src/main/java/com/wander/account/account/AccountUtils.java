package com.wander.account.account;

import com.google.gson.JsonObject;
import com.wander.account.account.entity.User;
import com.wander.account.account.util.VipConfigInfo;
import com.wander.cloud.net.exception.BusinessException2;
import org.apache.http.util.TextUtils;


/**
 * 会员工具类
 *
 * @author linlongxin
 * @date 2021/6/8 3:23 下午
 */
public class AccountUtils {

    /**
     * 从 bmob 解析用户数据
     *
     * @param phoneNum
     * @param password
     * @param userJson
     * @return
     */
    public static User parseFromBomob(String phoneNum, String password, JsonObject userJson) throws Exception {
        String objectId = userJson.get("objectId").getAsString();
        if (TextUtils.isEmpty(objectId)) {
            throw BusinessException2.buildError("账号异常，请联系客服");
        }
        String apkSign = null;
        if (userJson.has("apkSign")) {
            apkSign = userJson.get("apkSign").getAsString();
        }
        long vipTime= 0;
        if (userJson.has("vipExpireTime")) {
            vipTime = userJson.get("vipExpireTime").getAsLong();
        }
        String mobilePhoneNumber = null;
        if (userJson.has("mobilePhoneNumber")) {
            mobilePhoneNumber = userJson.get("mobilePhoneNumber").getAsString();
        }
        boolean mobilePhoneNumberVerified = false;
        if (userJson.has("mobilePhoneNumberVerified")) {
            mobilePhoneNumberVerified = userJson.get("mobilePhoneNumberVerified").getAsBoolean();
        }
        String dateStr = userJson.get("createdAt").getAsString();
        // 2020 之后的这种账号是盗版账号
        if ((dateStr.trim().startsWith("202")) && vipTime == 0 && TextUtils.isEmpty(apkSign)) {
            // 有两种情况是客服手动注册的会员，需要排除掉
            if (TextUtils.isEmpty(mobilePhoneNumber) || !mobilePhoneNumberVerified) {
                return buildUser(phoneNum, password, userJson);
            } else {
                throw BusinessException2.buildError("账号异常，请联系客服");
            }
        }
        return buildUser(phoneNum, password, userJson);
    }

    private static User buildUser(String phoneNum, String password, JsonObject userJson){
        User user = new User();
        user.phoneNum = phoneNum;
        user.password = password;
        user.userId = userJson.get("objectId").getAsString();
        if (userJson.get("nickname") != null) {
            user.username = userJson.get("nickname").getAsString();
        }
        if (userJson.get("vipLevel") != null) {
            user.vipLabelLevel = userJson.get("vipLevel").getAsInt();
        }
        if (userJson.get("avatar") != null) {
            user.avatarUrl = userJson.get("avatar").getAsString();
        }
        if (userJson.get("cover") != null) {
            user.coverUrl = userJson.get("cover").getAsString();
        }
        if (userJson.get("gender") != null) {
            user.gender = userJson.get("gender").getAsInt();
        }
        if(userJson.get("account_sign")!=null){
            user.accountSign = userJson.get("account_sign").getAsString();
        }
        if (userJson.get("vipExpireTime") != null) {
            long vipExpireTime = userJson.get("vipExpireTime").getAsLong();
            user.vipPastDueTime = vipExpireTime;
            if (vipExpireTime <= 0) {
                // 永久会员
                user.vipType = VipConfigInfo.VIP_TYPE_FOREVER;
            } else {
                // 普通会员
                user.vipType = VipConfigInfo.VIP_TYPE_COMMON;
            }
        }
        return user;
    }
}
