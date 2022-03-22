package com.wander.account.account.util;

/**
 * 数据库配置的 会员购买信息
 *
 * @author linlongxin
 * @date 2020/12/7 6:12 下午
 */
public class VipConfigInfo {

    // 会员的类型，二进制表示法
    // 普通会员
    public static final int VIP_TYPE_COMMON = 0b0000000001;
    // 永久会员
    public static final int VIP_TYPE_FOREVER = 0b10000000000;
    // 这个用来判断是否是会员身份
    public static final int VIP_TYPE_VALID = 0b10000000001;

    // 这个用来判断是否是元气壁纸会员身份
    public static final int VIP_TYPE_VALID_YUANQIBIZI = 0b0100000010;
    // 普通会员（元气壁纸）
    public static final int VIP_TYPE_COMMON_YUANQIBIZI = 0b0000000010;
    // 永久会员（元气壁纸
    public static final int VIP_TYPE_FOREVER_YUANQIBIZI  = 0b0100000000;

    // 订单类型，每个会员购买对应一个订单类型
    public int orderType;
    // 会员类型
    public int vipType;
    // 标题
    public String title;
    // 描述信息
    public String desc;
    // 售卖价格
    public int salePrice;
    // 原始价格
    public int originPrice;
    // 会员时长，单位 ms
    public long effectiveTime;
    // 默认选中价格
    public boolean defaultSelected;

}
