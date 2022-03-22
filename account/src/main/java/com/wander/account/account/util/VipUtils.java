package com.wander.account.account.util;


import com.wander.cloud.net.exception.ParamsException;
import com.wander.cloud.utils.BitOperationUtils;
import com.wander.cloud.utils.CollectionUtil;
import com.wander.cloud.utils.LogUtils;
import com.wander.cloud.utils.TextUtils;
import com.wander.cloud.utils.json.GsonUtil;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * 会员工具
 *
 * @author linlongxin
 * @date 2020/4/3 4:08 下午
 */
public class VipUtils {


    private static List<VipConfigInfo> sVipInfos = new ArrayList<>();

    /**
     * 获取支付宝异步通知地址
     *
     * @return
     */
    public static String getNotifyUrlAlipay() {
        String path = "/api/v1/vip/notifyAlipay";
        return AppConfig.getServerDomain() + path;
    }

    /**
     * 获取支付宝异步通知地址
     *
     * @return
     */
    public static String getNotifyUrlWX() {
        String path = "/api/v1/vip/notifyWx";
        return AppConfig.getServerDomain() + path;
    }

    /**
     * 更新会员售卖信息
     *
     * @param jsonStr
     */
    public static void updateVipConfigInfos(String jsonStr) {
        List<VipConfigInfo> list = GsonUtil.parseList(jsonStr, VipConfigInfo.class);
        if (CollectionUtil.isEmpty(list)) {
            LogUtils.error("会员配置信息无效");
            return;
        }
        sVipInfos.clear();
        sVipInfos.addAll(list);
        LogUtils.info("会员配置信息：" + GsonUtil.toJsonStr(list));
    }


    /**
     * 根据 vipType 获取 vipPrice
     *
     * @param orderType
     * @return
     */
    public static long getVipPrice(int orderType) throws Exception {
        for (VipConfigInfo info : sVipInfos) {
            if (info.orderType == orderType) {
                return info.salePrice;
            }
        }
        throw ParamsException.buildBusinessParamsError();
    }

    public static String getVipPayTitle(int orderType) throws Exception {
        for (VipConfigInfo info : sVipInfos) {
            if (info.orderType == orderType) {
                return info.title;
            }
        }
        throw ParamsException.buildBusinessParamsError();
    }

    public static int getVipType(int orderType) throws Exception {
        for (VipConfigInfo info : sVipInfos) {
            if (info.orderType == orderType) {
                return info.vipType;
            }
        }
        throw ParamsException.buildBusinessParamsError();
    }

    /**
     * 根据 orderType 获取会员时长
     * Note： 只能通过 orderType 获取会员时长
     *
     * @param orderType
     * @return
     */
    public static long getVipTime(int orderType) {
        for (VipConfigInfo info : sVipInfos) {
            if (info.orderType == orderType) {
                return info.effectiveTime;
            }
        }
        return 0;
    }

    /**
     * 是否是元气壁纸会员：永久 或者 普通会员
     *
     * @param type
     * @return
     */
    public static boolean isVitalityVip(int type) {
        return (type & VipConfigInfo.VIP_TYPE_VALID_YUANQIBIZI) > 0;
    }

    public static boolean isVitalityCommonVip(int type) {
        return BitOperationUtils.hasFlag(type, VipConfigInfo.VIP_TYPE_COMMON_YUANQIBIZI);
    }

    public static boolean isVitalityForeverVip(int type) {
        return BitOperationUtils.hasFlag(type, VipConfigInfo.VIP_TYPE_FOREVER_YUANQIBIZI);
    }


    /**
     * 是否是搜图神器会员：永久 或者 普通会员
     *
     * @param type
     * @return
     */
    public static boolean isSoutuVip(int type) {
        return (type & VipConfigInfo.VIP_TYPE_VALID) > 0;
    }

    public static boolean isSoutuCommonVip(int type) {
        return BitOperationUtils.hasFlag(type, VipConfigInfo.VIP_TYPE_COMMON);
    }

    public static boolean isSoutuForeverVip(int type) {
        return BitOperationUtils.hasFlag(type, VipConfigInfo.VIP_TYPE_FOREVER);
    }

    /**
     * 添加会员
     * @param userVipType
     * @param addTargetType
     * @return
     */
    public static int addVip(int userVipType, int addTargetType){
        return BitOperationUtils.setFlag(userVipType, addTargetType);
    }
    /**
     * 清除会员
     */
    public static int clearVip(int userVipType, int clearTargetType){
        return BitOperationUtils.clearFlag(userVipType, clearTargetType);
    }

    public interface VipLevel {
        int DEFAULT_USER = 0;
        int NEW_VIP = 1;            // 萌新会员
        int BRONZE_VIP = 2;         // 青铜会员
        int GLORY_VIP = 5;          // 荣耀捐赠会员
        int VETERAN_VIP = 10;       // 元老捐赠会员
        int EXPERIENCE_PAVILION_VIP = 11;       // 元老体验官
    }

    public interface VipLabel {
        String DEFAULT_USER = "普通用户";
        String NEW_VIP = "萌新会员";            // 萌新会员
        String BRONZE_VIP = "青铜会员";         // 青铜会员
        String GLORY_VIP = "荣耀捐赠会员";          // 荣耀捐赠会员
        String VETERAN_VIP = "元老捐赠会员";       // 元老捐赠会员
        String EXPERIENCE_PAVILION_VIP = "元老体验官";       // 元老体验官
    }

    private static final Map<Integer, String> sLabelMap = new HashMap<>();

    static {
        sLabelMap.put(VipLevel.DEFAULT_USER, VipLabel.DEFAULT_USER);
        sLabelMap.put(VipLevel.NEW_VIP, VipLabel.NEW_VIP);
        sLabelMap.put(VipLevel.BRONZE_VIP, VipLabel.BRONZE_VIP);
        sLabelMap.put(VipLevel.GLORY_VIP, VipLabel.GLORY_VIP);
        sLabelMap.put(VipLevel.VETERAN_VIP, VipLabel.VETERAN_VIP);
        sLabelMap.put(VipLevel.EXPERIENCE_PAVILION_VIP, VipLabel.EXPERIENCE_PAVILION_VIP);
    }

    public static String getVipLabel(int level) {
        String label = sLabelMap.get(level);
        if (TextUtils.isEmpty(label)) {
            return VipLabel.DEFAULT_USER;
        }
        return label;
    }

}
