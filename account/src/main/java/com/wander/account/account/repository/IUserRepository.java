package com.wander.account.account.repository;

import com.wander.account.account.entity.User;
import com.wander.account.account.entity.UserDTO;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

/**
 * @author linlongxin
 * @date 2021/6/8 4:27 下午
 */
@Repository
public interface IUserRepository extends JpaRepository<User, Long> {


    /**
     * 由于getOne 返回的数据里面没有 id 字段。这里就自己写SQL
     *
     * @param userId
     * @return
     */
    @Query(nativeQuery = true, value = "select * from w_user u where u.user_id = :user_id limit 1")
    User queryOneByUserId(@Param("user_id") String userId);

    /**
     * 由于getOne 返回的数据里面没有 id 字段。这里就自己写SQL
     *
     * @param phoneNum
     * @return
     */
    @Query(nativeQuery = true, value = "select * from w_user u where u.phone_num = :phone_num limit 1")
    User queryOneByPhoneNum(@Param("phone_num") String phoneNum);

    /**
     * 根据手机号，密码查询用户
     *
     * @param phoneNum
     * @return
     */
    @Query(nativeQuery = true, value = "select * from w_user u where u.phone_num = :phone_num and u.password = :password limit 1")
    User queryOneByPhoneNumAndPassword(@Param("phone_num") String phoneNum, @Param("password") String password);


    @Query(nativeQuery = true, value = "select * from w_user u where u.open_id=:open_id or u.union_id=:open_id limit 1")
    User queryByOpenId(@Param("open_id") String openId);

    @Query(nativeQuery = true, value = "select u.password as password from w_user u where u.phone_num = :phone_num  limit 1")
    UserDTO queryAllByUser(@Param("phone_num") String phoneNum);

}
