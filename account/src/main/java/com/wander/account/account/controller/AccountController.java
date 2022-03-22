package com.wander.account.account.controller;

import com.google.gson.JsonObject;
import com.wander.account.account.service.IAccountService;
import com.wander.cloud.net.exception.BusinessException2;
import com.wander.cloud.net.exception.ParamsException2;
import com.wander.cloud.net.reponse.ResponseData;
import com.wander.cloud.net.request.NetRequestUtils;
import com.wander.cloud.utils.TextUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import javax.servlet.http.HttpServletRequest;
import java.util.Map;

/**
 * @author linlongxin
 * @date 2021/6/8 4:27 下午
 */
@RestController
@RequestMapping("/api/v1/account")
public class AccountController {

    @Autowired
    private IAccountService mAccountService;


    @PostMapping("/register")
    public ResponseData register(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        String password = params.get("password");
        String smsCode = params.get("sms_code");
        String deviceId = params.get("device_id");
        int productCode = Integer.parseInt(params.get("product_id"));
        if (TextUtils.isEmpty(phoneNum) || TextUtils.isEmpty(password) || TextUtils.isEmpty(deviceId)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        if (password.length() < 6 || password.length() > 18) {
            throw BusinessException2.buildError("密码长度必须6~18位");
        }
        JsonObject json = mAccountService.register(phoneNum, password, deviceId, smsCode, productCode);
        return ResponseData.buildSuccessData(json);
    }

    @PostMapping("/login")
    public ResponseData login(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        String password = params.get("password");
        String deviceId = params.get("device_id");
        String smsCode = params.get("sms_code");
        int productCode = Integer.parseInt(params.get("product_id"));
        if (TextUtils.isEmpty(phoneNum) || TextUtils.isEmpty(password)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        JsonObject json = mAccountService.login(phoneNum, password, deviceId, smsCode, productCode);
        return ResponseData.buildSuccessData(json);
    }

    /**
     * 个人信息
     */
    @PostMapping("/info")
    public ResponseData info(HttpServletRequest request) throws Exception {
        String token = NetRequestUtils.getTokenFromHeader(request);
        if (!mAccountService.verifyToken(token)) {
            throw BusinessException2.buildLoginOverdueError();
        }
        JsonObject jsonObject = mAccountService.queryUserWithTokenByToken(token);
        return ResponseData.buildSuccessData(jsonObject);
    }

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ResponseData logout(HttpServletRequest request) throws Exception {
        String token = NetRequestUtils.getTokenFromHeader(request);
        if (!mAccountService.verifyToken(token)) {
            throw BusinessException2.buildLoginOverdueError();
        }
        mAccountService.deleteToken(token);
        return ResponseData.buildSimpleSuccess();
    }

    /**
     * 重置密码，用户未登录的状态下
     */
    @PostMapping("/resetPassword")
    public ResponseData resetPassword(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        String password = params.get("password");
        String smsCode = params.get("sms_code");
        mAccountService.resetPassword(phoneNum, password, smsCode);
        return ResponseData.buildSimpleSuccess();
    }

    /**
     * 修改密码，用户登录的状态下
     */
    @PostMapping("/modifyPassword")
    public ResponseData modifyPassword(HttpServletRequest request) throws Exception {
        String token = NetRequestUtils.getTokenFromHeader(request);
        if (!mAccountService.verifyToken(token)) {
            throw BusinessException2.buildLoginOverdueError();
        }
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String oldPassword = params.get("old_password");
        String newPassword = params.get("new_password");
        mAccountService.modifyPassword(token, oldPassword, newPassword);
        return ResponseData.buildSimpleSuccess();
    }

    /**
     * 发送验证码
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/smscode")
    public ResponseData sendSMSCode(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        int productId = Integer.parseInt(params.get("product_id"));
        int sceneType = 0;
        if (params.containsKey("scene_type")) {
            sceneType = Integer.parseInt(params.get("scene_type"));
        }
        if (TextUtils.isEmpty(phoneNum)) {
            throw ParamsException2.buildBusinessParamsError("手机号不能为空");
        }
        String codeTemplateId = null;
        switch (sceneType) {
            // 注册场景
            case 0:
                boolean isExist = mAccountService.checkAccountExist(phoneNum);
                if (isExist) {
                    throw BusinessException2.buildError("该手机号已注册，请直接登录");
                }
                codeTemplateId = "SMS_206010193";
                break;
            // 重置密码
            case 1:
                codeTemplateId = "SMS_206010192";
                break;
            // 登录多设备安全校验
            case 2:
                codeTemplateId = "SMS_215337478";
                break;
            // 修改手机号
            case 3:
                codeTemplateId = "SMS_218286005";
                break;
            // 绑定微信或QQ验证
            case 4:
                codeTemplateId = "SMS_206010191";
                break;
            //注销验证
            case 5:
                codeTemplateId = "SMS_236980029";
                break;
            default:
                break;
        }
        if (TextUtils.isEmpty(codeTemplateId)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        mAccountService.sendSmsCode(productId, phoneNum, codeTemplateId);
        return ResponseData.buildSuccessData("验证码发送成功");
    }

    @PostMapping("/modifyPhone")
    public ResponseData modifyPhone(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String oldPhoneNum = params.get("old_phone_num");
        String newPhoneNum = params.get("new_phone_num");
        String smsCode = params.get("sms_code");
        int productCode = Integer.parseInt(params.get("product_id"));
        if (TextUtils.isEmpty(oldPhoneNum) || TextUtils.isEmpty(newPhoneNum) || TextUtils.isEmpty(smsCode)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        String token = NetRequestUtils.getTokenFromHeader(request);
        mAccountService.modifyPhoneNum(token, smsCode, oldPhoneNum, newPhoneNum, productCode);
        return ResponseData.buildSuccessData("修改成功");
    }

    /**
     * 检查账号是否存在
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/existAccount")
    public ResponseData checkAccountExist(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        if (TextUtils.isEmpty(phoneNum)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        boolean isExist = mAccountService.checkAccountExist(phoneNum);
        return ResponseData.buildSuccessData(isExist ? "账号存在" : "账号不存在");
    }

    /**
     * 更新token
     *
     * @param request
     * @return
     */
    @PostMapping("/token")
    public ResponseData updateToken(HttpServletRequest request) throws Exception {
        String token = NetRequestUtils.getTokenFromHeader(request);
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String deviceId = params.get("device_id");
        int productCode = Integer.parseInt(params.get("product_id"));
        JsonObject json = mAccountService.updateTokenLife(token, deviceId, productCode);
        return ResponseData.buildSuccessData(json);
    }

    /**
     * 更新用户信息
     *
     * @param request
     * @return
     */
    @PostMapping("/updateInfo")
    public ResponseData updateInfo(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String token = NetRequestUtils.getTokenFromHeader(request);
        JsonObject jsonObject = mAccountService.updateUserInfo(token, params);
        return ResponseData.buildSuccessData(jsonObject);
    }

    /**
     * 兑换会员称号
     *
     * @param request
     * @return
     */
    @PostMapping("/exchangeLabel")
    public ResponseData exchangeLabel(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String token = NetRequestUtils.getTokenFromHeader(request);
        String code = params.get("code");
        JsonObject jsonObject = mAccountService.exchangeLabel(code, token);
        return ResponseData.buildSuccessData(jsonObject);
    }

    /**
     * 个人中心获取订单
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/userOrder")
    public ResponseData userOrder(HttpServletRequest request) throws Exception {
        Map<String, String> param = NetRequestUtils.parseRequest(request);
        int page = 0;
        if (param.containsKey("page")) {
            page = Integer.parseInt(param.get("page"));
        }
        int pageSize = 30;
        if (param.containsKey("page_size")) {
            pageSize = Integer.parseInt(param.get("page_size"));
        }
        String token = NetRequestUtils.getTokenFromHeader(request);
        return ResponseData.buildSuccessData(mAccountService.getUserOrder(page, pageSize, token));
    }

    /**
     * 重置账号
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/resetAccount")
    public ResponseData resetAccount(HttpServletRequest request) throws Exception {
        Map<String, String> param = NetRequestUtils.parseRequest(request);
        String phoneNum = param.get("phone_num");
        String password = param.get("password");
        if (TextUtils.isEmpty(phoneNum) || TextUtils.isEmpty(password)) {
            throw ParamsException2.buildBusinessParamsError();
        }

        mAccountService.resetAccountAll(phoneNum, password);
        return ResponseData.buildSuccessData("重置成功");
    }

    /**
     * 注销账号
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/deleteAccount")
    public ResponseData deleteAccount(HttpServletRequest request) throws Exception {
        Map<String, String> param = NetRequestUtils.parseRequest(request);
        String phoneNum = param.get("phone_num");
        String password = null;
        if (param.containsKey("password")) {
            password = param.get("password");
        }
        String smsCode = null;
        if (param.containsKey("sms_code")) {
            smsCode = param.get("sms_code");
        }
        String token = NetRequestUtils.getTokenFromHeader(request);
        if (TextUtils.isEmpty(phoneNum)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        mAccountService.deleteAccountAll(phoneNum, password, token, smsCode);
        return ResponseData.buildSuccessData("删除成功");
    }

    /**
     * 获取会员订单
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/vipOrder")
    public ResponseData queryVipOrder(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String token = NetRequestUtils.getTokenFromHeader(request);
        int page = 0;
        int pageSize = 30;
        int productId = Integer.parseInt(params.get("product_id"));
        if (params.containsKey("page")) {
            page = Integer.parseInt(params.get("page"));
        }
        if (params.containsKey("page_size")) {
            pageSize = Integer.parseInt(params.get("page_size"));
        }
        return ResponseData.buildSuccessData(mAccountService.queryVipOrder(page, pageSize, token, productId));
    }

    /**
     * 微信或QQ快捷登录
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/quickLogin")
    public ResponseData quickLogin(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String openId = params.get("open_id");
        String wxOpenId = null;
        if (params.containsKey("wx_openid")) {
            wxOpenId = params.get("wx_openid");
        }
        int idType = Integer.parseInt(params.get("id_type"));
        String smsCode = null;
        String deviceId = params.get("device_id");
        int productCode = Integer.parseInt(params.get("product_id"));
        String phoneNum = null;
        String accessToken = params.get("access_token");
        String avatarUrl = params.get("avatar_url");
        String nickName = params.get("nick_name");
        if (params.containsKey("phone_num")) {
            phoneNum = params.get("phone_num");
        }
        if (params.containsKey("sms_code")) {
            smsCode = params.get("sms_code");
        }
        JsonObject json = mAccountService.quickLogin(openId, idType, smsCode, phoneNum, deviceId, productCode,
                accessToken, avatarUrl, nickName, wxOpenId);
        return ResponseData.buildSuccessData(json);
    }

    /**
     * 微信或者QQ快捷登录验证openid是否存在
     *
     * @param request
     * @return
     */
    @PostMapping("/queryOpenId")
    public ResponseData queryOpenId(HttpServletRequest request) {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String openId = params.get("open_id");
        return ResponseData.buildSuccessData(mAccountService.queryOpenIdExist(openId));
    }

    /**
     * 手机号码快捷登录
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/phoneQuickLogin")
    public ResponseData phoneQuickLogin(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneToken = params.get("phone_token");
        String deviceId = params.get("device_id");
        int productCode = Integer.parseInt(params.get("product_id"));
        return ResponseData.buildSuccessData(mAccountService.phoneQuickLogin(phoneToken, deviceId, productCode));
    }


    /**
     * 通过同一个账号不同token登录不同端
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/loginByToken")
    public ResponseData loginByToken(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String token = params.get("other_token");
        int productCode = Integer.parseInt(params.get("product_id"));
        String deviceId = params.get("device_id");
        return ResponseData.buildSuccessData(mAccountService.loginByToken(token, deviceId, productCode));
    }


    @PostMapping("/quickBind")
    public ResponseData quickBind(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String token = NetRequestUtils.getTokenFromHeader(request);
        String accessToken = params.get("access_token");
        String wxOpenId = null;
        if (params.containsKey("wx_openid")) {
            wxOpenId = params.get("wx_openid");
        }
        String openId = params.get("open_id");
        int idType = Integer.parseInt(params.get("id_type"));
        return ResponseData.buildSuccessData(mAccountService.quickBind(openId, idType, token, accessToken, wxOpenId));
    }
}