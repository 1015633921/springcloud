package com.wander.account.account.entity;

import com.google.gson.annotations.Expose;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.io.Serializable;
import java.util.Date;

/**
 * 手机号换绑记录
 * @author linlongxin
 * @date 2021/7/2 11:42 上午
 */
@Entity
@Table(name = "w_change_phone_record")
@EntityListeners(AuditingEntityListener.class)
public class ChangePhoneRecord implements Serializable {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;

    public String userId;
    public String oldPhoneNum;
    @Column(nullable = false)
    public String newPhoneNum;

    // 创建时间
    @Expose
    @CreatedDate
    public Date createTime;
    // 更新时间
    @Expose
    @LastModifiedDate
    public Date updateTime;
}
