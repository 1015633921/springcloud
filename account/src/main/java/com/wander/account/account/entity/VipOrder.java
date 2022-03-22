package com.wander.account.account.entity;

import com.google.gson.annotations.Expose;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

/**
 * 购买会员订单
 */
@Entity
@Table(name = "vip_order")
@EntityListeners(AuditingEntityListener.class)
public class VipOrder {

    // 服务器生成信息
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;
    // 支付类型，1,微信：wx，2,支付宝：alipay
    @Expose
    public int payType;
    // 微信/支付宝 平台分配的 app_id
    public String appId;
    // 支付宝 or 微信支付 商品的标题/交易标题/订单标题/订单关键字等
    // eg : 天天爱消除-游戏充值
    @Expose
    public String title;
    // 商户唯一订单号，我们自己随机生成
    @Expose
    public String outTradeNo;
    // 支付宝/微信订单金额，单元分，eg：100
    @Expose
    public long totalAmount = 0;
    // 订单交易是否支付成功
    @Expose
    public boolean isPayed = false;
    // 支付成功后，请求 payResult 接口会自动同步修改 user 的身份
    // 该字段标记该支付订单是否已经同步过身份信息
    @Expose
    public boolean isUsed = false;

    // 客户端上传信息
    public int orderType;
    // 会员类型
    public int vipType;
    public int versionCode = 0;
    public int productId = 0;

    // 手机号，用户注册成功后录入
    public String userId;
    public String phoneNum;

    // 支付平台回调后的信息
    // 支付宝/微信平台生成的唯一交易号
    @Expose
    public String platformTradeNo;
    // 实际支付金额，单位分
    @Expose
    public long receiptAmount = 0;
    // 买家支付宝用户号/微信号
    public String buyerId;
    // 目前交易状态
    @Expose
    public String tradeStatus;
    // 交易付款时间
    @Expose
    public Date gmtPayment;

    // 订单创建时间
    @CreatedDate
    public Date createTime;
    // 数据被更新时间
    @LastModifiedDate
    public Date updateTime;
}