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
@RequestMapping("/api/v1/accountHack")
public class AccountHackController {

    @Autowired
    private IAccountService mAccountService;

    /**
     * 退出登录
     */
    @PostMapping("/logout")
    public ResponseData logout(HttpServletRequest request) throws Exception {
        String token = NetRequestUtils.getTokenFromHeader(request);
        if (TextUtils.isEmpty(token)) {
            throw BusinessException2.buildError("你还未登录，无效操作");
        }
        mAccountService.deleteToken(token);
        return ResponseData.buildSimpleSuccess();
    }

    /**
     * 查用户信息
     */
    @PostMapping("/info")
    public ResponseData queryUser(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        JsonObject json = mAccountService.queryUserByPhoneNum(phoneNum);
        return ResponseData.buildSuccessData(json);
    }

    @PostMapping("/register")
    public ResponseData registerHack(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        String password = params.get("password");
        int vipType = Integer.parseInt(params.get("vip_type"));
        long vipTime = Long.parseLong(params.get("vip_time"));
        if (TextUtils.isEmpty(phoneNum) || TextUtils.isEmpty(password)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        if (password.length() < 6 || password.length() > 18) {
            throw BusinessException2.buildError("密码长度必须6~18位");
        }
        mAccountService.register(phoneNum, password, vipType, vipTime);
        return ResponseData.buildSuccessData("注册成功");
    }

    @PostMapping("/modifyPhoneNum")
    public ResponseData modifyPhoneHack(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String oldPhoneNum = params.get("old_phone_num");
        String newPhoneNum = params.get("new_phone_num");
        String password = params.get("password");
        int productCode = Integer.parseInt(params.get("product_id"));
        if (TextUtils.isEmpty(oldPhoneNum) || TextUtils.isEmpty(password) || TextUtils.isEmpty(newPhoneNum)) {
            throw ParamsException2.buildBusinessParamsError();
        }

        mAccountService.modifyPhoneNumHack(oldPhoneNum, newPhoneNum, password,productCode);
        return ResponseData.buildSuccessData("修改成功");
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
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        String password = params.get("password");
        if (TextUtils.isEmpty(phoneNum) || TextUtils.isEmpty(password)) {
            throw ParamsException2.buildBusinessParamsError();
        }

        mAccountService.resetAccount(phoneNum, password);
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
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        String password = params.get("password");
        if (TextUtils.isEmpty(phoneNum) || TextUtils.isEmpty(password)) {
            throw ParamsException2.buildBusinessParamsError();
        }

        boolean isSuccess = mAccountService.deleteAccount(phoneNum, password);
        return ResponseData.buildSuccessData(isSuccess ? "注销成功" : "账号或密码错误");
    }

    /**
     * 注销会员
     *
     * @param request
     * @return
     * @throws Exception
     */
    @PostMapping("/clearVip")
    public ResponseData clearVip(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        int vipType = 0;
        if (params.containsKey("vip_type")) {
            vipType = Integer.parseInt(params.get("vip_type"));
        }
        if (vipType == 0 || TextUtils.isEmpty(phoneNum)) {
            throw ParamsException2.buildBusinessParamsError();
        }

        mAccountService.clearVip(vipType, phoneNum);
        return ResponseData.buildSuccessData("重置成功");
    }

    @PostMapping("/continueLife")
    public ResponseData continueLife(HttpServletRequest request) throws Exception {
        Map<String, String> params = NetRequestUtils.parseRequest(request);
        String phoneNum = params.get("phone_num");
        int vipType = Integer.parseInt(params.get("vip_type"));
        long vipTime = 0;
        if (params.containsKey("vip_time")) {
            vipTime = Long.parseLong(params.get("vip_time"));
        }
        if (TextUtils.isEmpty(phoneNum)) {
            throw ParamsException2.buildBusinessParamsError();
        }
        mAccountService.continueLifeHack(phoneNum, vipType, vipTime);
        return ResponseData.buildSuccessData("续命成功");
    }
}
