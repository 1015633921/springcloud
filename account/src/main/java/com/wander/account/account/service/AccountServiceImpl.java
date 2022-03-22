package com.wander.account.account.service;

import com.google.gson.JsonObject;
import com.google.gson.JsonParser;
import com.wander.account.account.AccountUtils;
import com.wander.account.account.TokenInfo;
import com.wander.account.account.TokenUtils;
import com.wander.account.account.entity.*;
import com.wander.account.account.repository.IChangePhoneRecordRepository;
import com.wander.account.account.repository.IUserRepository;
import com.wander.account.account.util.UverifyUtil;
import com.wander.account.account.util.VipUtils;
import com.wander.cloud.base.BasisConfig;
import com.wander.cloud.base.config.IConfigRepository;
import com.wander.cloud.base.config.service.IConfigService;
import com.wander.cloud.base.sms.SMSUtils;
import com.wander.cloud.net.exception.BusinessException2;
import com.wander.cloud.net.exception.ParamsException2;
import com.wander.cloud.net.reponse.ResponseCode;
import com.wander.cloud.net.request.NetRequestUtils;
import com.wander.cloud.net.request.RequestManager;
import com.wander.cloud.net.security.JwtUtil;
import com.wander.cloud.utils.*;
import com.wander.cloud.utils.json.GsonUtil;
import okhttp3.ResponseBody;
import org.apache.http.client.methods.CloseableHttpResponse;
import org.apache.http.util.EntityUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.StringRedisTemplate;
import org.springframework.stereotype.Service;


import javax.validation.constraints.NotNull;
import java.util.*;
import java.util.concurrent.TimeUnit;

/**
 * @author linlongxin
 * @date 2021/6/8 4:29 下午
 */
@Service
public class AccountServiceImpl implements IAccountService {

    private static final String API_BMOB_LOGIN = "http://javacloud.bmob.cn/7c0d04782a169f6a/login";
    private static final String API_BMOB_QUERY_USER = "http://javacloud.bmob.cn/7c0d04782a169f6a/queryuser";
    private static final String API_BMOB_MODIFY_PHONE = "http://javacloud.bmob.cn/7c0d04782a169f6a/modifyphone";
    private static final String API_BMOB_MODIFY_PASSWORD = "http://javacloud.bmob.cn/7c0d04782a169f6a/modifypassword";
    private static final String API_BMOB_DELETE_ACCOUNT = "http://pycloud.bmob.cn/7c0d04782a169f6a/DeleteAccount";
    private static final String API_U_VERIFY_QUERY_MOBILE_INFO = "https://verify5.market.alicloudapi.com/api/v1/mobile/info";
    public static final String PREFIX_SMS_CODE_TOKEN = "wallpaper_sms_code_";
    private static final String REDIS_CACHE_KEY_VIP_LABEL_INFOS = "wp_vip_label_infos";
    private static final String CONFIG_KEY_ACCOUNT = "account_config_infos";
    private static final String U_VERIFY_YZBZ_APP_KEY = "5f73360580455950e49c5fda";
    // 登录token过期时间 30 天
    public static final int TOKEN_CACHE_TIME_DAY = 30;

    @Autowired
    private StringRedisTemplate mStrRedisTemplate;


    @Autowired
    private IUserRepository mAccountRepository;

    @Autowired
    private IChangePhoneRecordRepository mChangePhoneRecordRepository;

    @Autowired
    private FeignConfig iConfigService;
    @Override
    public boolean verifyToken(String token) {
        if (TextUtils.isEmpty(token)) {
            LogUtils.error("token 异常为空 == " + token);
            return false;
        }
        String redisKey = JwtUtil.getValueByToken(token);
        String tokenInfoStr = mStrRedisTemplate.opsForValue().get(redisKey);
        TokenInfo info = GsonUtil.parseObject(tokenInfoStr, TokenInfo.class);
        boolean isOk = info != null && TextUtils.equals(token, info.token);
        if (!isOk) {
            LogUtils.error("token 校验异常：" + token + " , redisKey : " + redisKey + " , tokenInfoStr : " + tokenInfoStr);
        }
        return isOk;
    }

    @Override
    public JsonObject queryUserWithTokenByToken(@NotNull String token) throws Exception {
        String userId = TokenUtils.getUserIdFromToken(token);
        if (TextUtils.isEmpty(userId)) {
            LogUtils.error("queryUserWithTokenByToken 异常：token : " + token + " , userId : " + userId);
            throw BusinessException2.buildLoginOverdueError();
        } else {
            User user = mAccountRepository.queryOneByUserId(userId);
            return buildUserJsonWithToken(user, token);
        }
    }

    @Override
    public User queryUserByToken(@NotNull String token) throws Exception {
        String userId = TokenUtils.getUserIdFromToken(token);
        if (TextUtils.isEmpty(userId)) {
            throw BusinessException2.buildLoginOverdueError();
        } else {
            return mAccountRepository.queryOneByUserId(userId);
        }
    }

    @Override
    public JsonObject updateTokenLife(String token, String deviceId, int productCode) throws Exception {
        if (!verifyToken(token)) {
            throw BusinessException2.buildLoginOverdueError();
        }
        int productId = TokenUtils.getProductIdFromToken(token);
        // 新 token
        if (productId > 0) {
            saveTokenToRedis(token, deviceId, productCode);
            return queryUserWithTokenByToken(token);
        } else {
            // 删除旧token，NOTE: 先后顺序不能反
            deleteToken(token);
            // 新版本token生成
            String userId = TokenUtils.getUserIdFromToken(token);
            String newToken = TokenUtils.createToken(userId, deviceId, productCode);
            saveTokenToRedis(newToken, deviceId, productCode);
            return queryUserWithTokenByToken(newToken);
        }
    }

    /**
     * 缓存 token 到 redis
     *
     * @param token
     * @param deviceId
     * @throws Exception
     */
    private void saveTokenToRedis(String token, String deviceId, int productCode) throws Exception {
        String cacheKey = JwtUtil.getValueByToken(token);
        if (TextUtils.isEmpty(cacheKey)) {
            throw BusinessException2.buildLoginOverdueError();
        }
        TokenInfo tokenInfo = new TokenInfo(token, deviceId, productCode);
        mStrRedisTemplate.opsForValue().set(cacheKey, GsonUtil.toJsonStr(tokenInfo), TOKEN_CACHE_TIME_DAY, TimeUnit.DAYS);
    }

    @Override
    public void deleteToken(String token) {
        String cacheKey = JwtUtil.getValueByToken(token);
        mStrRedisTemplate.delete(cacheKey);
    }

    @Override
    public void sendSmsCode(int productId, String phoneNum, String codeTemplateId) throws Exception {
        String key = getSmsCodeCacheKey(phoneNum);
        String smsCode = SMSUtils.createSmsCode();
        JsonObject codeJson = new JsonObject();
        String signName = "搜图神器";
        switch (productId) {
            case BasisConfig.PRODUCT_ID_SOUTU:
                signName = "搜图神器";
                break;
            case BasisConfig.PRODUCT_ID_YUANQI_TUKU:
                signName = "元气图库";
                break;
            case BasisConfig.PRODUCT_ID_YUANQI_BIZHI:
                signName = "元气壁纸";
                break;
            case BasisConfig.PRODUCT_ID_QINGMANG_BIZHI:
                signName = "青芒壁纸";
                break;
        }
        codeJson.addProperty("code", smsCode);
        SMSUtils.sendCode(phoneNum, signName, codeTemplateId, codeJson.toString());
        // 10 分钟过期
        mStrRedisTemplate.opsForValue().set(key, smsCode, 10, TimeUnit.MINUTES);
    }

    @Override
    public JsonObject register(String phoneNum, String password, String deviceId, String smsCode, int productCode) throws Exception {
        // 校验验证码
        String key = getSmsCodeCacheKey(phoneNum);
        if (!mStrRedisTemplate.hasKey(key)) {
            throw BusinessException2.buildError("验证码错误");
        }
        String cacheSmsCode = mStrRedisTemplate.opsForValue().get(key);
        if (!TextUtils.equals(smsCode, cacheSmsCode)) {
            throw BusinessException2.buildError("验证码错误");
        }
        // 废弃验证码
        mStrRedisTemplate.delete(key);
        // 注册账号
        User user = register(phoneNum, password, 0, 0L);
        return buildLoginData(user, user.phoneNum, deviceId, smsCode, productCode);
    }

    @Override
    public User register(String phoneNum, String password, int vipType, long vipTime) throws Exception {
        // 检查 阿里云数据库是否 已存在账号
        boolean isExist = checkAccountExist(phoneNum);
        if (isExist) {
            throw BusinessException2.buildError("该手机号已注册，请直接登录");
        }

        boolean isCheckBmob = isCheckBmob();
        if (isCheckBmob) {
            // 检查 bmob 是否已存在账号
            Map<String, String> params = new HashMap<>();
            params.put("phone_num", phoneNum);
            String responseStr = NetRequestUtils.postSyncBmob(API_BMOB_QUERY_USER, params);
            JsonObject resJson = JsonParser.parseString(responseStr).getAsJsonObject();
            int responseCode = resJson.get("code").getAsInt();
            // 说明 bmob 有账号
            if (responseCode == 200) {
                String objectId = resJson.get("data").getAsJsonObject().get("objectId").getAsString();
                if (!TextUtils.isEmpty(objectId)) {
                    throw BusinessException2.buildError("该手机号已注册，请直接登录");
                }
            }
        }

        // 生成一个有效的 userId
        String userId = createUserId();
        User user = new User();
        // 注册账号
        user.userId = userId;
        user.phoneNum = phoneNum;
        user.password = password;

        // 线上用户不会走这里的逻辑，这里主要为了处理 管理App 注册新用户
        if (VipUtils.isSoutuVip(vipType)) {
            user.vipType = vipType;
            if (VipUtils.isSoutuCommonVip(vipType)) {
                user.vipPastDueTime = System.currentTimeMillis() + vipTime;
            }
            if (user.vipLabelLevel <= VipUtils.VipLevel.DEFAULT_USER) {
                user.vipLabelLevel = VipUtils.VipLevel.NEW_VIP;
            }
        } else if (VipUtils.isVitalityVip(vipType)) {
            user.vipType = vipType;
            if (VipUtils.isVitalityCommonVip(vipType)) {
                user.vitalityVipPastDueTime = System.currentTimeMillis() + vipTime;
            }
        }
        return saveUserToDb(user);
    }

    private User register(String phoneNum, String password, int vipType, long vipTime, String openId, int idType, String avatarUrl, String nickName) throws Exception {
        // 检查 阿里云数据库是否 已存在账号
        boolean isExist = checkAccountExist(phoneNum);
        if (isExist) {
            throw BusinessException2.buildError("该手机号已注册，请直接登录");
        }

        boolean isCheckBmob = isCheckBmob();
        if (isCheckBmob) {
            // 检查 bmob 是否已存在账号
            Map<String, String> params = new HashMap<>();
            params.put("phone_num", phoneNum);
            String responseStr = NetRequestUtils.postSyncBmob(API_BMOB_QUERY_USER, params);
            JsonObject resJson = JsonParser.parseString(responseStr).getAsJsonObject();
            int responseCode = resJson.get("code").getAsInt();
            // 说明 bmob 有账号
            if (responseCode == 200) {
                String objectId = resJson.get("data").getAsJsonObject().get("objectId").getAsString();
                if (!TextUtils.isEmpty(objectId)) {
                    throw BusinessException2.buildError("该手机号已注册，请直接登录");
                }
            }
        }

        // 生成一个有效的 userId
        String userId = createUserId();
        User user = new User();
        // 注册账号
        user.userId = userId;
        user.phoneNum = phoneNum;
        user.password = password;
        user.avatarUrl = avatarUrl;
        user.username = nickName;
        if (idType == 1) {
            user.unionId = openId;
        } else {
            user.openId = openId;
        }

        // 线上用户不会走这里的逻辑，这里主要为了处理 管理App 注册新用户
        if (VipUtils.isSoutuVip(vipType)) {
            user.vipType = vipType;
            if (VipUtils.isSoutuCommonVip(vipType)) {
                user.vipPastDueTime = System.currentTimeMillis() + vipTime;
            }
            if (user.vipLabelLevel <= VipUtils.VipLevel.DEFAULT_USER) {
                user.vipLabelLevel = VipUtils.VipLevel.NEW_VIP;
            }
        } else if (VipUtils.isVitalityVip(vipType)) {
            user.vipType = vipType;
            if (VipUtils.isVitalityCommonVip(vipType)) {
                user.vitalityVipPastDueTime = System.currentTimeMillis() + vipTime;
            }
        }
        return saveUserToDb(user);
    }

    @Override
    public JsonObject login(String phoneNum, String password, String deviceId, String smsCode, int productCode) throws Exception {
        if (TextUtils.isEmpty(deviceId)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        // 先尝试 阿里云数据库 登录
        User user = mAccountRepository.queryOneByPhoneNumAndPassword(phoneNum, password);
        if (user == null) {
            // 再查下阿里云是否存在该账号
            user = mAccountRepository.queryOneByPhoneNum(phoneNum);
            if (user != null) {
                // 密码错误
                throw ParamsException2.buildBusinessParamsError("账号或密码错误");
            }
            // 再次尝试 bmob 登录
            boolean isCheckBmob = isCheckBmob();
            if (!isCheckBmob) {
                throw ParamsException2.buildBusinessParamsError("账号或密码错误");
            }
            // 在尝试 bmob 登录前，检查下是否手机号发生过换绑，如果换绑过，则不允许登录（因为说明这个账号已经在阿里云使用过了）
            ChangePhoneRecord record = mChangePhoneRecordRepository.queryPhoneRecord(phoneNum);
            if (record != null) {
                throw BusinessException2.buildError("账号或密码错误");
            }
            Map<String, String> params = new HashMap<>();
            params.put("phone_num", phoneNum);
            params.put("password", password);
            String responseStr = NetRequestUtils.postSyncBmob(API_BMOB_LOGIN, params);
            if (TextUtils.isEmpty(responseStr)) {
                throw ParamsException2.buildBusinessParamsError("账号或密码错误");
            } else {
                JsonObject resJson = JsonParser.parseString(responseStr).getAsJsonObject();
                int responseCode = resJson.get("code").getAsInt();
                // 说明 bmob 有账号，需把账号同步到阿里云数据库
                if (responseCode == 200) {
                    JsonObject userInfoJson = resJson.get("data").getAsJsonObject();
                    user = AccountUtils.parseFromBomob(phoneNum, password, userInfoJson);
                    // 把 bmob 数据导入阿里云的数据库
                    saveUserToDb(user);
                    return buildLoginData(user, phoneNum, deviceId, smsCode, productCode);
                } else {
                    throw ParamsException2.buildBusinessParamsError("账号或密码错误");
                }
            }
        } else {
            return buildLoginData(user, phoneNum, deviceId, smsCode, productCode);
        }
    }

    private User saveUserToDb(User user) {
        if (user == null) {
            return null;
        }
        user.username = TextUtils.replaceUtf8mb4(user.username);
        return mAccountRepository.save(user);
    }

    /**
     * 检查设备安全
     *
     * @param userId
     * @param phoneNum
     * @param smsCode
     * @throws Exception
     */
    private void checkDeviceSecurity(String tokenCacheKey, String userId, String phoneNum, String smsCode,
                                     int productCode) throws Exception {
        if (!TextUtils.isEmpty(tokenCacheKey)) {
            Set<String> keys = mStrRedisTemplate.keys(TokenUtils.getTokenCacheKeyPrefix(userId, productCode) + "*");
            long size;
            if (CollectionUtil.isEmpty(keys)) {
                size = 0;
            } else {
                size = keys.size();
            }
            // 不在设备列表中，且设备数大于等于 3
            if (!mStrRedisTemplate.hasKey(tokenCacheKey) && size >= 3) {
                if (TextUtils.isEmpty(smsCode)) {
                    // 提示用户需要发送验证码
                    throw BusinessException2.buildError(ResponseCode.CLIENT_ACCOUNT_SECURITY_LOGIN, "请使用验证码安全登录");
                }
                // 验证验证码
                String smsCodeKey = getSmsCodeCacheKey(phoneNum);
                String cacheSmsCode = mStrRedisTemplate.opsForValue().get(smsCodeKey);
                if (!TextUtils.equals(smsCode, cacheSmsCode)) {
                    throw BusinessException2.buildError("验证码错误");
                }
                // 废弃验证码
                mStrRedisTemplate.delete(cacheSmsCode);
                // 找出最不活跃的设备，并删除
                long miniTime = System.currentTimeMillis();
                String inactiveKey = null;
                for (String key : keys) {
                    String infoStr = mStrRedisTemplate.opsForValue().get(key);
                    TokenInfo info = GsonUtil.parseObject(infoStr, TokenInfo.class);
                    if (info == null) {
                        continue;
                    }
                    if (miniTime > info.lastActiveTime) {
                        miniTime = info.lastActiveTime;
                        inactiveKey = key;
                    }
                }
                if (!TextUtils.isEmpty(inactiveKey)) {
                    mStrRedisTemplate.delete(inactiveKey);
                }
            }
        }
    }

    /**
     * 检查设备安全
     *
     * @param userId
     * @param phoneNum
     * @throws Exception
     */
    private void checkQuickLoginDeviceSecurity(String tokenCacheKey, String userId, String phoneNum,
                                               int productCode) throws Exception {
        if (!TextUtils.isEmpty(tokenCacheKey)) {
            Set<String> keys = mStrRedisTemplate.keys(TokenUtils.getTokenCacheKeyPrefix(userId, productCode) + "*");
            long size;
            if (CollectionUtil.isEmpty(keys)) {
                size = 0;
            } else {
                size = keys.size();
            }
            // 不在设备列表中，且设备数大于等于 3
            if (!mStrRedisTemplate.hasKey(tokenCacheKey) && size >= 3) {
                // 找出最不活跃的设备，并删除
                long miniTime = System.currentTimeMillis();
                String inactiveKey = null;
                for (String key : keys) {
                    String infoStr = mStrRedisTemplate.opsForValue().get(key);
                    TokenInfo info = GsonUtil.parseObject(infoStr, TokenInfo.class);
                    if (info == null) {
                        continue;
                    }
                    if (miniTime > info.lastActiveTime) {
                        miniTime = info.lastActiveTime;
                        inactiveKey = key;
                    }
                }
                if (!TextUtils.isEmpty(inactiveKey)) {
                    mStrRedisTemplate.delete(inactiveKey);
                }
            }
        }
    }


    private JsonObject buildLoginData(@NotNull User user, String phoneNum, String deviceId, String smsCode,
                                      int productCode) throws Exception {
        String userId = user.userId;
        String tokenCacheKey = TokenUtils.getTokenCacheKey(userId, deviceId, productCode);
        if (TextUtils.isEmpty(tokenCacheKey)) {
            throw BusinessException2.buildError("服务异常");
        }
        checkDeviceSecurity(tokenCacheKey, userId, phoneNum, smsCode, productCode);
        // 生成新 token
        String token = TokenUtils.createToken(userId, deviceId, productCode);
        saveTokenToRedis(token, deviceId, productCode);
        return buildUserJsonWithToken(user, token);
    }

    private String createUserId() {
        String userId;
        User user;
        do {
            userId = Utils.createRandomStr(8);
            user = mAccountRepository.queryOneByUserId(userId);
        } while (user != null);
        return userId;
    }

    @Override
    public boolean checkAccountExist(String phoneNum) {
        User user = mAccountRepository.queryOneByPhoneNum(phoneNum);
        return user != null;
    }

    @Override
    public void resetPassword(String phoneNum, String password, String smsCode) throws Exception {
        // 1，校验验证码
        String key = getSmsCodeCacheKey(phoneNum);
        if (!mStrRedisTemplate.hasKey(key)) {
            throw BusinessException2.buildError("验证码错误");
        }
        String cacheSmsCode = mStrRedisTemplate.opsForValue().get(key);
        if (!TextUtils.equals(smsCode, cacheSmsCode)) {
            throw BusinessException2.buildError("验证码错误");
        }
        // 废弃验证码
        mStrRedisTemplate.delete(key);
        // 2，先查阿里云账户
        User user = mAccountRepository.queryOneByPhoneNum(phoneNum);
        if (user == null) {
            // 再查下 bmob 是否已存在账号
            Map<String, String> params = new HashMap<>();
            params.put("phone_num", phoneNum);
            String responseStr = NetRequestUtils.postSyncBmob(API_BMOB_QUERY_USER, params);
            JsonObject resJson = JsonParser.parseString(responseStr).getAsJsonObject();
            int responseCode = resJson.get("code").getAsInt();
            if (responseCode != 200) {
                throw BusinessException2.buildError("账号不存在，请先注册");
            }
            // 说明 bmob 有账号
            JsonObject userInfoJson = resJson.get("data").getAsJsonObject();
            // 把 bmob 数据导入阿里云的数据库
            user = AccountUtils.parseFromBomob(phoneNum, password, userInfoJson);
        } else {
            user.password = password;
        }
        // 3，更新
        mAccountRepository.save(user);
    }

    @Override
    public void modifyPassword(String token, String oldPassword, String newPassword) throws Exception {
        User user = queryUserByToken(token);
        if (user == null) {
            throw ParamsException2.buildBusinessParamsError("状态异常，请重新登录");
        }
        if (!TextUtils.equals(oldPassword, user.password)) {
            throw BusinessException2.buildError("原密码错误，请仔细检查");
        }
        user.password = newPassword;
        mAccountRepository.save(user);
        modifyPasswordFromBmob(user.phoneNum, oldPassword, newPassword);
    }

    /**
     * 同步修改 Bmob 账号的密码
     */
    private void modifyPasswordFromBmob(String phoneNum, String oldPassword, String newPassword) {
        Map<String, String> params = new HashMap<>();
        params.put("phone_num", phoneNum);
        params.put("old_password", oldPassword);
        params.put("new_password", newPassword);
        NetRequestUtils.postSyncBmob(API_BMOB_MODIFY_PASSWORD, params);
    }

    @Override
    public void modifyPhoneNumHack(String oldPhoneNum, String newPhoneNum, String password, int productCode) throws Exception {
        User user = mAccountRepository.queryOneByPhoneNum(oldPhoneNum);
        if (user == null) {
            throw BusinessException2.buildError("该手机号未注册账号");
        }
        user = mAccountRepository.queryOneByPhoneNumAndPassword(oldPhoneNum, password);
        if (user == null) {
            throw BusinessException2.buildError("手机号或密码错误");
        }
        user.phoneNum = newPhoneNum;
        mAccountRepository.save(user);

        // 换绑记录
        ChangePhoneRecord record = new ChangePhoneRecord();
        record.oldPhoneNum = oldPhoneNum;
        record.newPhoneNum = newPhoneNum;
        record.userId = user.userId;
        mChangePhoneRecordRepository.save(record);

        // 删除 redis 缓存中的 token，使其登录失效，需用户重新登录
        Set<String> keys = mStrRedisTemplate.keys(TokenUtils.getTokenCacheKeyPrefix(user.userId, productCode) + "*");
        if (CollectionUtil.isEmpty(keys)) {
            return;
        }
        for (String cacheKey : keys) {
            mStrRedisTemplate.delete(cacheKey);
        }
        modifyPhoneFromBmob(oldPhoneNum, newPhoneNum, user.password);
    }

    @Override
    public void modifyPhoneNum(String token, String smsCode, String oldPhoneNum, String newPhoneNum, int productCode) throws Exception {
        // 检查 阿里云数据库是否 已存在账号
        boolean isExist = checkAccountExist(newPhoneNum);
        if (isExist) {
            throw BusinessException2.buildError("新手机号已注册无法换绑");
        }
        // 1，校验验证码
        String key = getSmsCodeCacheKey(newPhoneNum);
        if (!mStrRedisTemplate.hasKey(key)) {
            throw BusinessException2.buildError("验证码错误");
        }
        String cacheSmsCode = mStrRedisTemplate.opsForValue().get(key);
        if (!TextUtils.equals(smsCode, cacheSmsCode)) {
            throw BusinessException2.buildError("验证码错误");
        }
        // 废弃验证码
        mStrRedisTemplate.delete(key);
        User user = mAccountRepository.queryOneByPhoneNum(oldPhoneNum);
        if (user == null) {
            throw BusinessException2.buildError("该手机号未注册账号");
        }
        ChangePhoneRecord record = mChangePhoneRecordRepository.queryChangeRecord(oldPhoneNum);
        if (record != null && record.createTime.getTime() + TimeUtils.DAY_TIME * 30 > System.currentTimeMillis()) {
            throw BusinessException2.buildError("抱歉，您近期已更换过手机号或者注册账号未满一个月，无法频繁更换~");
        }
        // 如果没有记录，说明还没有更换过手机号，新注册账号一个月内也不允许更换
        if (record == null && user.createTime.getTime() + TimeUtils.DAY_TIME * 30 > System.currentTimeMillis()) {
            throw BusinessException2.buildError("抱歉，您近期已更换过手机号或者注册账号未满一个月，无法频繁更换~");
        }
        user.phoneNum = newPhoneNum;
        mAccountRepository.save(user);
        // 换绑记录
        record = new ChangePhoneRecord();
        record.oldPhoneNum = oldPhoneNum;
        record.newPhoneNum = newPhoneNum;
        record.userId = user.userId;
        mChangePhoneRecordRepository.save(record);

        // 删除 redis 缓存中的 token，使其登录失效，需用户重新登录
        Set<String> keys = mStrRedisTemplate.keys(TokenUtils.getTokenCacheKeyPrefix(user.userId, productCode) + "*");
        if (CollectionUtil.isEmpty(keys)) {
            return;
        }
        for (String cacheKey : keys) {
            String itemToken = mStrRedisTemplate.opsForValue().get(cacheKey);
            if (TextUtils.equals(itemToken, token)) {
                continue;
            }
            mStrRedisTemplate.delete(cacheKey);
        }
        modifyPhoneFromBmob(oldPhoneNum, newPhoneNum, user.password);
    }

    /**
     * 同步修改 Bmob 账号的手机号
     *
     * @param oldPhoneNum
     * @param newPhoneNum
     * @param password
     */
    private void modifyPhoneFromBmob(String oldPhoneNum, String newPhoneNum, String password) {
        Map<String, String> params = new HashMap<>();
        params.put("old_phone_num", oldPhoneNum);
        params.put("new_phone_num", newPhoneNum);
        params.put("password", password);
        NetRequestUtils.postSyncBmob(API_BMOB_MODIFY_PHONE, params);
    }

    @Override
    public void resetAccount(String phoneNum, String password) throws Exception {
        User user = mAccountRepository.queryOneByPhoneNumAndPassword(phoneNum, password);
        if (user == null) {
            throw ParamsException2.buildBusinessParamsError("账号不存在");
        }
        user.vipType = 0;
        user.vipPastDueTime = 0;
        user.vitalityVipPastDueTime = 0;
        // 重置会员身份
        mAccountRepository.save(user);
    }

    @Override
    public void clearVip(int vipType, String phoneNum) throws Exception {
        User user = mAccountRepository.queryOneByPhoneNum(phoneNum);
        if (user == null) {
            throw ParamsException2.buildBusinessParamsError("账号不存在");
        }
        user.vipType = VipUtils.clearVip(user.vipType, vipType);
        if (VipUtils.isSoutuVip(vipType)) {
            user.vipPastDueTime = 0;
            user.vipLabelLevel = 0;
        } else if (VipUtils.isVitalityVip(vipType)) {
            user.vitalityVipPastDueTime = 0;
        }
        mAccountRepository.save(user);
    }

    @Override
    public boolean deleteAccount(String phoneNum, String password) {
        User user = mAccountRepository.queryOneByPhoneNumAndPassword(phoneNum, password);
        if (user == null) {
            return false;
        } else {
            mAccountRepository.delete(user);
            return true;
        }
    }

    /**
     * 支付完成后，更新用户的会员身份信息
     *
     * @param order
     * @throws Exception
     */
    @Override
    public void updateVipInfo(VipOrder order) throws Exception {
        if (!order.isPayed) {
            throw BusinessException2.buildError("订单未支付，请先完成支付");
        }
        User user = mAccountRepository.queryOneByUserId(order.userId);
        if (user == null) {
            LogUtils.error("updateVipInfo user == null");
            return;
        }
        // 废弃订单
//        mVipService.abandonOrder(order);

        // 根据用户当前会员身份 和 订单的会员类型判断
        user.vipType = BitOperationUtils.setFlag(user.vipType, VipUtils.getVipType(order.orderType));
        if (order.orderType < 1000) {
            if (user.vipLabelLevel < VipUtils.VipLevel.NEW_VIP) {
                // 默认开通会员后是萌新会员
                user.vipLabelLevel = VipUtils.VipLevel.NEW_VIP;
            }
        }
        long time = VipUtils.getVipTime(order.orderType);
        updateVipTime(user, time, order.orderType);
        mAccountRepository.save(user);
    }

    @Override
    public JsonObject updateUserInfo(String token, Map<String, String> params) throws Exception {
        User user = queryUserByToken(token);
        if (user == null) {
            throw ParamsException2.buildBusinessParamsError();
        }
        if (params.containsKey("vip_label_level")) {
            user.vipLabelLevel = Integer.parseInt(params.get("vip_label_level"));
        }
        if (params.containsKey("username")) {
            user.username = params.get("username");
        }
        if (params.containsKey("avatar_url")) {
            user.avatarUrl = params.get("avatar_url");
        }
        if (params.containsKey("cover_url")) {
            user.coverUrl = params.get("cover_url");
        }
        if (params.containsKey("gender")) {
            user.gender = Integer.parseInt(params.get("gender"));
        }
        if (params.containsKey("account_sign")) {
            user.accountSign = params.get("account_sign");
        }
        if (params.containsKey("web_background")) {
            user.webBackground = params.get("web_background");
        }
        saveUserToDb(user);
        return buildUserJsonWithToken(user, token);
    }

    @Override
    public JsonObject exchangeLabel(String code, String token) throws Exception {
        String infos = iConfigService.queryConfig(REDIS_CACHE_KEY_VIP_LABEL_INFOS);
        Map<String, String> map = GsonUtil.parseMap(infos, String.class, String.class);
        if (!map.containsKey(code)) {
            throw BusinessException2.buildError("兑换码错误, 请仔细检查");
        }
        int targetLevel = Integer.parseInt(map.get(code));
        User user = queryUserByToken(token);

        if (user.vipLabelLevel >= targetLevel) {
            throw BusinessException2.buildError("当前会员称号更高级，无需兑换");
        }
        user.vipLabelLevel = targetLevel;
        mAccountRepository.save(user);
        return buildUserJsonWithToken(user, token);
    }

    private JsonObject buildUserJsonWithToken(User user, String token) {
        user.vipLabel = VipUtils.getVipLabel(user.vipLabelLevel);
        JsonObject json = GsonUtil.toJsonObjectOnlyExpose(user);
        json.addProperty("token", token);
        return json;
    }

    /**
     * 仅仅内部使用
     *
     * @param phoneNum
     * @param vipType
     * @param vipTime
     * @throws Exception
     */
    @Override
    public void continueLifeHack(String phoneNum, int vipType, long vipTime) throws Exception {
        // 检查 阿里云数据库是否 已存在账号
        User user = mAccountRepository.queryOneByPhoneNum(phoneNum);
        if (user == null) {
            throw BusinessException2.buildError("账号不存在，请先注册");
        }
        // 添加会员
        user.vipType = VipUtils.addVip(user.vipType, vipType);
        if (VipUtils.isSoutuVip(vipType)) {
            if (VipUtils.isSoutuCommonVip(vipType)) {
                if (user.vipPastDueTime > System.currentTimeMillis()) {
                    user.vipPastDueTime += vipTime;
                } else {
                    user.vipPastDueTime = System.currentTimeMillis() + vipTime;
                }
                if (user.vipLabelLevel <= VipUtils.VipLevel.DEFAULT_USER) {
                    user.vipLabelLevel = VipUtils.VipLevel.NEW_VIP;
                }
            }
        } else if (VipUtils.isVitalityVip(vipType)) {
            if (user.vitalityVipPastDueTime > System.currentTimeMillis()) {
                user.vitalityVipPastDueTime += vipTime;
            } else {
                user.vitalityVipPastDueTime = System.currentTimeMillis() + vipTime;
            }
        }
        saveUserToDb(user);
    }

    @Override
    public User queryUserByUserId(String userId) {
        return mAccountRepository.queryOneByUserId(userId);
    }

    @Override
    public JsonObject queryUserByPhoneNum(String phoneNum) throws Exception {
        User user = mAccountRepository.queryOneByPhoneNum(phoneNum);
        if (user == null) {
            throw BusinessException2.buildError("该账号不存在，请先注册");
        }
        return buildUserJsonWithToken(user, null);
    }

    private String getSmsCodeCacheKey(String phoneNum) {
        return PREFIX_SMS_CODE_TOKEN + phoneNum;
    }

    private void updateVipTime(User user, long addTime, int orderType) {
        if (addTime <= 0) {
            return;
        }
        long currentTime = System.currentTimeMillis();
        // orderType <1000是更新搜图神器会员过期时间，大于1000是元气壁纸会员过期时间
        if (orderType < 1000) {
            if (user.vipPastDueTime > currentTime) {
                user.vipPastDueTime += addTime;
            } else {
                user.vipPastDueTime = currentTime + addTime;
            }
        } else {
            if (user.vitalityVipPastDueTime > currentTime) {
                user.vitalityVipPastDueTime += addTime;
            } else {
                user.vitalityVipPastDueTime = currentTime + addTime;
            }
        }
    }

    /**
     * 获取用户订单
     *
     * @param page
     * @param pageSize
     * @param token
     * @return
     */
    @Override
    public List<CommodityOrder> getUserOrder(int page, int pageSize, String token) throws Exception {
        User user = queryUserByToken(token);
        if(user == null){
            throw BusinessException2.buildLoginOverdueError();
        }
        int start = page * pageSize;
//        List<CommodityOrderDTO> dtos = mCommodityOrderRepository.queryByUserId(user.userId, start, pageSize);
//        List<CommodityOrder> res = new ArrayList<>();
//        for (CommodityOrderDTO dto : dtos) {
//            CommodityOrder order = new CommodityOrder();
//            order.buyType = dto.getBuyType();
//            order.commodityId = dto.getCommodityId();
//            order.outTradeNo = dto.getOutTradeNo();
//            order.title = dto.getTitle();
//            order.isPayed = dto.getIsPayed();
//            order.imageList = dto.getImageList();
//            order.gmtPayment = dto.getGmtPayment();
//            order.createTime = dto.getCreateTime();
//            order.originalPrice = dto.getOriginalPrice();
//            order.tradeStatus = dto.getTradeStatus();
//            order.receiptAmount = dto.getReceiptAmount();
//            order.payType = dto.getPayType();
//            order.platformTradeNo = dto.getPlatformTradeNo();
//            res.add(order);
//        }
        return null;
    }

    /**
     * 重置账号
     *
     * @param phoneNum
     * @param password
     * @throws ParamsException2
     */
    @Override
    public void resetAccountAll(String phoneNum, String password) throws ParamsException2 {
        User user = mAccountRepository.queryOneByPhoneNumAndPassword(phoneNum, password);
        if (user == null) {
            throw ParamsException2.buildBusinessParamsError();
        }
        user.vipType = 0;
        user.vipPastDueTime = 0;
        user.vipLabelLevel = 0;
        // 重置会员身份
        mAccountRepository.save(user);
        // 删除购买记录
//        mCommodityOrderRepository.deleteByUserId(user.userId);
    }

    /**
     * 注销账号账号
     *
     * @param phoneNum
     * @param password
     * @throws ParamsException2
     */
    @Override
    public void deleteAccountAll(String phoneNum, String password, String token, String smsCode) throws Exception {
        User user;
        if (!TextUtils.isEmpty(smsCode)) {
            user = mAccountRepository.queryOneByPhoneNum(phoneNum);
            if (user != null) {
                // 1，校验验证码
                String key = getSmsCodeCacheKey(phoneNum);
                if (!mStrRedisTemplate.hasKey(key)) {
                    throw BusinessException2.buildError("验证码错误");
                }
                String cacheSmsCode = mStrRedisTemplate.opsForValue().get(key);
                if (!TextUtils.equals(smsCode, cacheSmsCode)) {
                    throw BusinessException2.buildError("验证码错误");
                }
                // 废弃验证码
                mStrRedisTemplate.delete(key);
            } else {
                throw BusinessException2.buildError("当前账号未注册");
            }
        } else {
            user = mAccountRepository.queryOneByPhoneNumAndPassword(phoneNum, password);
            if (!TextUtils.isEmpty(token)) {
                User tmp = queryUserByToken(token);
                if (tmp == null || !TextUtils.equals(tmp.userId, user.userId)) {
                    throw ParamsException2.buildBusinessParamsError("注销账号与现登录账号不符合！");
                }
            }
            if (user == null) {
                throw ParamsException2.buildBusinessParamsError();
            }
        }
        // 删除 bmob 账号
        UserDTO userDTO = mAccountRepository.queryAllByUser(phoneNum);
        Map<String, String> params = new HashMap<>();
        params.put("phone_num", phoneNum);
        params.put("password", userDTO.getPassword());
        NetRequestUtils.getSyncBmob(API_BMOB_DELETE_ACCOUNT, params);
        // 删除会员身份
        mAccountRepository.delete(user);
        // 删除购买记录
//        mCommodityOrderRepository.deleteByUserId(user.userId);
    }

    /**
     * 获取会员订单
     *
     * @param page
     * @param pageSize
     * @param token
     * @return
     * @throws Exception
     */
    @Override
    public List<VipOrder> queryVipOrder(int page, int pageSize, String token, int productId) throws Exception {
        if (TextUtils.isEmpty(token)) {
            throw BusinessException2.buildLoginOverdueError();
        }
        User user = queryUserByToken(token);
        if (user == null) {
            throw BusinessException2.buildLoginOverdueError();
        }
//        List<VipOrderDTO> dtos = mVipOrderRepository.getUserVipOrder(page * pageSize, pageSize, user.userId, productId);
//        List<VipOrder> data = new ArrayList<>();
//        for (VipOrderDTO orderDto : dtos) {
//            VipOrder order = new VipOrder();
//            order.isPayed = orderDto.getIsPayed();
//            order.title = orderDto.getTitle();
//            order.payType = orderDto.getPayType();
//            order.receiptAmount = orderDto.getPrice();
//            order.outTradeNo = orderDto.getOutTradeNo();
//            order.gmtPayment = orderDto.getGmtPayment();
//            data.add(order);
//        }
        return null;
    }

    /**
     * 注册和登录账号的时候是否检查bmob的账号系统，默认 true
     * 用途：bmob 服务不稳定，如果bmob服务宕机了，会导致所有用户的注册登录出现异常
     *
     * @return
     */
    public boolean isCheckBmob() {
        try {
            String configInfo = .getConfigInfo(CONFIG_KEY_ACCOUNT);
            JsonObject json = GsonUtil.toJsonObject(configInfo);
            return json.get("is_check_bmob").getAsBoolean();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return true;
    }

    @Override
    public JsonObject quickLogin(String openId, int idType, String smsCode, String phoneNum, String deviceId,
                                 int productId, String accessToken, String avatarUrl, String nickName, String wxOpenid) throws Exception {
        if (idType == 1) {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", accessToken);
            params.put("openid", wxOpenid);
            ResponseBody responseBody = RequestManager.getSync("https://api.weixin.qq.com/sns/auth", params).body();
            String data = responseBody.source().readUtf8();
            int code = GsonUtil.toJsonObject(data).get("errcode").getAsInt();
            if (code != 0) {
                throw BusinessException2.buildError("快捷登录失败");
            }
        }
        User user;
        user = mAccountRepository.queryByOpenId(openId);
        if (!TextUtils.isEmpty(phoneNum)) {
            user = mAccountRepository.queryOneByPhoneNum(phoneNum);
            if (user != null) {
                if (idType == 1) {
                    user.unionId = openId;
                } else {
                    user.openId = openId;
                }
                mAccountRepository.save(user);
            } else {
                // 校验验证码
                String key = getSmsCodeCacheKey(phoneNum);
                if (!mStrRedisTemplate.hasKey(key)) {
                    throw BusinessException2.buildError("验证码错误");
                }
                String cacheSmsCode = mStrRedisTemplate.opsForValue().get(key);
                if (!TextUtils.equals(smsCode, cacheSmsCode)) {
                    throw BusinessException2.buildError("验证码错误");
                }
                // 废弃验证码
                mStrRedisTemplate.delete(key);
                // 注册账号
                user = register(phoneNum, "", 0, 0L, openId, idType, avatarUrl, nickName);
            }
        }

        return buildLoginData(user, user.phoneNum, deviceId, smsCode, productId);
    }


    @Override
    public boolean queryOpenIdExist(String openId) {
        return mAccountRepository.queryByOpenId(openId) == null ? false : true;
    }

    /**
     * 电话号码快捷登录
     *
     * @param phoneToken
     * @param deviceId
     * @return
     * @throws Exception
     */
    @Override
    public JsonObject phoneQuickLogin(String phoneToken, String deviceId, int productCode) throws Exception {
        CloseableHttpResponse response = UverifyUtil.queryPhoneNumFromUM(phoneToken);
        String resStr = EntityUtils.toString(response.getEntity(), "utf-8");
        int statusCode = response.getStatusLine().getStatusCode();
        if (statusCode != 200) {
            throw BusinessException2.buildError("请求出错");
        }
        JsonObject res = GsonUtil.toJsonObject(resStr);
        String phoneNum = res.getAsJsonObject("data").get("mobile").getAsString();
        if (TextUtils.isEmpty(phoneNum)) {
            throw BusinessException2.buildError("快捷登录失败");
        }
        User user = mAccountRepository.queryOneByPhoneNum(phoneNum);
        if (user == null) {
            user = register(phoneNum, "", 0, 0);
            return buildLoginData(user, phoneNum, deviceId, "", productCode);
        } else {
            String tokenCacheKey = TokenUtils.getTokenCacheKey(user.userId, deviceId, productCode);
            checkQuickLoginDeviceSecurity(tokenCacheKey, user.userId, phoneNum, productCode);
            // 生成新 token
            String token = TokenUtils.createToken(user.userId, deviceId, productCode);
            saveTokenToRedis(token, deviceId, productCode);
            return buildUserJsonWithToken(user, token);
        }
    }

    /**
     * 通过同一个账号不同token登录不同端
     *
     * @param token
     * @param deviceId
     * @param productCode
     * @return
     * @throws Exception
     */
    @Override
    public JsonObject loginByToken(String token, String deviceId, int productCode) throws Exception {
        if (TextUtils.isEmpty(token)) {
            throw BusinessException2.buildError("当前token失效或不存在");
        }
        User user = queryUserByToken(token);
        if (user == null) {
            throw BusinessException2.buildLoginOverdueError();
        }
        return buildLoginData(user, user.phoneNum, deviceId, "", productCode);
    }

    @Override
    public User quickBind(String openId, int idType, String token, String accessToken, String wxOpenId) throws Exception {
        if (idType == 1) {
            Map<String, String> params = new HashMap<>();
            params.put("access_token", accessToken);
            params.put("openid", wxOpenId);
            ResponseBody responseBody = RequestManager.getSync("https://api.weixin.qq.com/sns/auth", params).body();
            String data = responseBody.source().readUtf8();
            int code = GsonUtil.toJsonObject(data).get("errcode").getAsInt();
            if (code != 0) {
                throw BusinessException2.buildError("快捷绑定失败");
            }
        }
        if (TextUtils.isEmpty(token)) {
            throw BusinessException2.buildError("当前token失效或不存在");
        }
        User user = queryUserByToken(token);
        if (user == null) {
            throw BusinessException2.buildLoginOverdueError();
        }
        if(mAccountRepository.queryByOpenId(openId)!=null){
            if(idType==1) {
                throw BusinessException2.buildError("当前微信已绑定其他账号");
            }else if(idType==2){
                throw BusinessException2.buildError("当前QQ已绑定其他账号");
            }
            throw BusinessException2.buildError("绑定失败");
        }
        if (idType == 1) {
            user.unionId = openId;
        } else {
            user.openId = openId;
        }
        mAccountRepository.save(user);
        return user;
    }
}