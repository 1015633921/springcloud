package com.wander.account.account.repository;

import com.wander.account.account.entity.ChangePhoneRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author linlongxin
 * @date 2021/7/2 11:56 上午
 */
public interface IChangePhoneRecordRepository extends JpaRepository<ChangePhoneRecord, Long> {

    @Query(nativeQuery = true, value = "select * from w_change_phone_record p where p.new_phone_num = :phone order by create_time desc limit 1")
    ChangePhoneRecord queryChangeRecord(@Param("phone") String oldPhoneNum);

    @Query(nativeQuery = true, value = "select * from w_change_phone_record p where p.new_phone_num = :phone or p.old_phone_num = :phone limit 1")
    ChangePhoneRecord queryPhoneRecord(@Param("phone") String phoneNum);
}