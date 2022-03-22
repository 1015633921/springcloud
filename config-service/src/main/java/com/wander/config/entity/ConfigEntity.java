package com.wander.config.entity;

import com.google.gson.annotations.Expose;
import org.springframework.data.annotation.CreatedDate;
import org.springframework.data.annotation.LastModifiedDate;
import org.springframework.data.jpa.domain.support.AuditingEntityListener;

import javax.persistence.*;
import java.util.Date;
import java.util.List;

/**
 * describe：配置表，所有业务公用表
 * create time：2020/7/9 5:21 下午
 * author：linlongxin
 */
@Entity
@Table(name = "config")
@EntityListeners(AuditingEntityListener.class)
public class ConfigEntity {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    public long id;

    @Expose
    public String title;
    /**
     * 产品列表二进制码
     */
    @Expose
    public long productCode;
    // 客户端支持的最小版本号
    public int minVersionCode;

    // 客户端支持的最大版本号
    public int maxVersionCode;

    // 排序
    public int sort;


    // 配置的业务 key
    @Expose
    @Column(unique = true, nullable = false)
    public String configKey;

    // 配置的 值
    @Column(columnDefinition = "text")
    @Expose
    public String value;

    // 创建时间
    @CreatedDate
    public Date createTime;
    // 更新时间
    @LastModifiedDate
    public Date updateTime;

    @Transient
    @Expose
    public List<Integer> productList;

    public static ConfigEntity createConfigEntity(String key, String value) {
        ConfigEntity entity = new ConfigEntity();
        entity.configKey = key;
        entity.value = value;
        return entity;
    }
}
