package com.wander.account.account.entity;



import com.google.gson.annotations.Expose;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * @author linlongxin
 * @date 2021/6/7 11:02 上午
 */
@Entity
@Table(name = "w_user")
@EntityListeners(AuditingEntityListener.class)
public class User implements Serializable {

    private static final long serialVersionUID = -4924996655406307339L;

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;
    // 用户id，唯一，8位随机数
    @Expose
    @Column(nullable = false, unique = true)
    public String userId;
    // 手机号
    @Expose
    @Column(nullable = false, unique = true)
    public String phoneNum;
    // 密码
    public String password;
    // 用户名
    @Expose
    public String username;
    // 头像
    @Expose
    @Column(length = 512)
    public String avatarUrl;
    @Expose
    @Column(length = 512)
    public String coverUrl;
    // 性别，0：男，1：女
    @Expose
    public int gender;
    // 会员类型
    @Expose
    public int vipType = 0;
    // 会员过期时间戳，单位 ms
    @Expose
    public long vipPastDueTime = 0;
    // 会员称号等级，这里主要是为了映射成对应的称号
    @Expose
    public int vipLabelLevel;

    // 非数据库字段：会员称号，由 vipLevel 映射过来
    @Transient
    @Expose
    public String vipLabel;

    // 创建时间
    @Expose
    @CreatedDate
    public Date createTime;
    // 更新时间
    @Expose
    @LastModifiedDate
    public Date updateTime;
    @Expose
    // 元气壁纸过期时间
    public long vitalityVipPastDueTime;
    // 个人签名
    @Expose
    public String accountSign;
    // web版本背景墙
    @Expose
    public String webBackground;
    // QQ唯一标识
    @Expose
    public String openId;
    // 微信唯一标识
    @Expose
    public String unionId;
}

