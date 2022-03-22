package com.wander.account.account.entity;

import com.google.gson.annotations.Expose;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;

/**
 * @author DongYu
 * @description:电商订单实体类
 * @date 2021-06-10-40
 **/
@Entity
@Table(name = "ds_commodity_order")
@EntityListeners(AuditingEntityListener.class)
public class CommodityOrder {
    @Id
    @Expose
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;
    /**
     * 支付类型，1,微信：wx，2,支付宝：alipay
     */
    @Expose
    public int payType;
    /**
     * 微信平台分配的 app_id
     */
    @Expose
    public String wxappId;
    /**
     * 支付宝 平台分配的 app_id
     */
    @Expose
    public String alipayappId;
    /**
     * 支付宝 or 微信支付 商品的标题/交易标题/订单标题/订单关键字等
     */
    @Expose
    public String title;
    /**
     * 商户唯一订单号，我们自己随机生成
     */
    @Expose
    @Column(nullable = false)
    public String outTradeNo;
    /**
     * 订单交易是否支付成功
     */
    @Expose
    public boolean isPayed;

    @Expose
    @CreatedDate
    public Date createTime;
    @Expose
    @LastModifiedDate
    public Date updateTime;

    /**
     * 用户id
     */
    @Expose
    @Column(nullable = false)
    public String userId;
    /**
     * 付宝/微信平台生成的唯一交易号
     */
    @Expose
    public String platformTradeNo;
    /**
     * 实际支付金额，单位分
     */
    @Expose
    @Column(nullable = false)
    public int receiptAmount;
    /**
     * 该商品原价
     */
    @Expose
    public int originalPrice;

    /**
     * 买家支付宝用户号/微信号
     */
    @Expose
    public String buyerId;

    /**
     * 目前交易状态
     */
    @Expose
    public String tradeStatus;
    /**
     * 交易付款时间
     */
    @Expose
    @CreatedDate
    public Date gmtPayment;


    /**
     * app的id
     */
    @Expose
    public int productId;

    /**
     * 商品id
     */
    @Expose
    public long commodityId;
    @Expose
    @Transient
    public String imageList;
    @Expose
    public int buyType;
}
