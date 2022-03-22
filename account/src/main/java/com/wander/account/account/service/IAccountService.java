package com.wander.account.account.service;

import com.google.gson.JsonObject;
import com.wander.account.account.entity.CommodityOrder;
import com.wander.account.account.entity.User;
import com.wander.account.account.entity.VipOrder;
import com.wander.cloud.net.exception.ParamsException2;


import javax.validation.constraints.NotNull;
import java.util.List;
import java.util.Map;

/**
 * @author linlongxin
 * @date 2021/6/8 4:28 下午
 */
public interface IAccountService {

    /**
     * 验证token
     *
     * @param token
     * @return
     */
    boolean verifyToken(String token);

    /**
     * 通过 token 获取 user
     *
     * @param token
     * @return
     */
    JsonObject queryUserWithTokenByToken(@NotNull String token) throws Exception;

    /**
     * 通过 token 获取 user
     *
     * @param token
     * @return
     */
    User queryUserByToken(@NotNull String token) throws Exception;


    /**
     * 查账号
     *
     * @return
     */
    User queryUserByUserId(String userId);

    /**
     * 查账号
     *
     * @return
     */
    JsonObject queryUserByPhoneNum(String phoneNum) throws Exception;

    /**
     * 给 token 续期
     *
     * @param token
     * @return
     */
    JsonObject updateTokenLife(String token, String deviceId, int productCode) throws Exception;

    /**
     * 注销token
     */
    void deleteToken(String token);

    /**
     * 发送短信验证码
     *
     * @param phoneNum
     */
    void sendSmsCode(int productId, String phoneNum, String codeTemplateId) throws Exception;

    /**
     * 注册账号
     *
     * @param phoneNum
     * @param password
     * @param smsCode
     * @return
     * @throws Exception
     */
    JsonObject register(String phoneNum, String password, String deviceId, String smsCode, int productCode) throws Exception;

    /**
     * 注册账号
     *
     * @param phoneNum
     * @param password
     * @throws Exception
     */
    User register(String phoneNum, String password, int vipType, long vipTime) throws Exception;

    /**
     * 登录
     *
     * @param phoneNum
     * @param password
     * @param smsCode  可选参数
     */
    JsonObject login(String phoneNum, String password, String deviceId, String smsCode, int productCode) throws Exception;

    /**
     * 检查账户是否存在
     *
     * @param phoneNum
     */
    boolean checkAccountExist(String phoneNum);

    /**
     * 重置密码
     *
     * @param phoneNum
     * @param password
     * @param smsCode
     * @throws Exception
     */
    void resetPassword(String phoneNum, String password, String smsCode) throws Exception;

    /**
     * 修改密码
     *
     * @throws Exception
     */
    void modifyPassword(String token, String oldPassword, String newPassword) throws Exception;


    /**
     * 更换手机号
     *
     * @param oldPhoneNum
     * @param newPhoneNum
     * @param password
     */
    void modifyPhoneNumHack(String oldPhoneNum, String newPhoneNum, String password, int productCode) throws Exception;

    void modifyPhoneNum(String token, String smsCode, String oldPhoneNum, String newPhoneNum, int productCode) throws Exception;


    void continueLifeHack(String phoneNum, int vipType, long vipTime) throws Exception;

    /**
     * 重置账号信息
     */
    void resetAccount(String phoneNum, String password) throws Exception;

    /**
     * 清除账号会员信息
     */
    void clearVip(int vipType, String phoneNum) throws Exception;

    /**
     * 注销账号
     *
     * @param phoneNum
     * @param password
     * @throws Exception
     */
    boolean deleteAccount(String phoneNum, String password) throws Exception;

    /**
     * 更新会员信息
     */
    void updateVipInfo(VipOrder order) throws Exception;


    JsonObject updateUserInfo(String token, Map<String, String> params) throws Exception;

    JsonObject exchangeLabel(String code, String token) throws Exception;

    List<CommodityOrder> getUserOrder(int page, int pageSize, String token) throws Exception;

    void resetAccountAll(String phoneNum, String password) throws ParamsException2;

    void deleteAccountAll(String phoneNum, String password, String token, String smsCode) throws Exception;

    /**
     * 获取会员订单
     *
     * @param page
     * @param pageSize
     * @param token
     * @return
     * @throws Exception
     */
    List<VipOrder> queryVipOrder(int page, int pageSize, String token, int productId) throws Exception;

    JsonObject quickLogin(String openId, int idType, String smsCode, String phoneNum, String deviceId,
                          int productId, String accessToken, String avatarUrl, String nickName, String wxOpenId) throws Exception;

    boolean queryOpenIdExist(String openId);

    JsonObject phoneQuickLogin(String phoneToken, String deviceId, int productCode) throws Exception;

    JsonObject loginByToken(String Token, String deviceId, int productCode) throws Exception;

    User quickBind(String openId, int idType, String token, String accessToken, String wxOpenId) throws Exception;


}