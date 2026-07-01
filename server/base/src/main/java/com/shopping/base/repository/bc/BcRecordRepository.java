package com.shopping.base.repository.bc;

import com.shopping.base.domain.bc.BcRecord;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

/**
 * @anthor bin
 * @data 2019/7/6 21:56
 */
public interface BcRecordRepository extends JpaRepository<BcRecord,Long>{

    /**
     * 通过userId 和  AddTime  查询报餐记录
     * @param appId
     * @param userId
     * @param dinTime
     * @return
     */
    @Query("select obj from BcRecord obj where obj.appId=:appId and obj.userId=:userId and obj.dinTime like concat('%',:dinTime,'%') ")
    BcRecord getByUserIdAndDinTime(@Param("appId") String appId,@Param("userId")Long userId, @Param("dinTime")String dinTime);

    /**
     * 按 appId + userId + id 精确获取一条报餐记录(取消报餐前校验用)
     */
    @Query("select obj from BcRecord obj where obj.appId=:appId and obj.userId=:userId and obj.id=:id")
    BcRecord getByAppIdAndUserIdAndId(@Param("appId") String appId,@Param("userId") Long userId,@Param("id") Long id);

    /**
     * 查询某天的总报餐人数
     * @param appId
     * @param dinTime
     * @return
     */
    @Query("select count(r.id) from BcRecord  r join BcUser u on (r.appId=u.appId and r.userId =u.id) where r.appId=:appId and  r.dinTime like concat('%',:dinTime,'%')")
    int  getTotalRecordByDinTime(@Param("appId")String appId,@Param("dinTime")String dinTime);

    /**
     * 撤销报餐删除报餐记录
     * @param appId
     * @param userId
     * @param id
     * @return
     */
    @Modifying
    @Query("delete from BcRecord obj where obj.appId=:appId and obj.userId=:userId and obj.id =:id")
    Integer  deleteBcRecordByAppIdAndUserIdAndId(@Param("appId") String appId,@Param("userId") Long userId,@Param("id") Long id);

    /**
     * 根据就餐时间获取报餐记录
     * @param appId
     * @param dinTime
     * @return
     */
    @Query("from BcRecord  obj  where obj.appId=:appId and obj.dinTime like concat('%',:dinTime,'%') ")
    List<BcRecord>  getBcRecordListByDinTime(@Param("appId")String appId,@Param("dinTime")String dinTime);

    @Modifying
    @Query("update BcRecord  set hadEat =:hadEat where id=:id and appId=:appId")
    int updateHadEatById(@Param("hadEat")Integer hadEat,@Param("id")Long id,@Param("appId")String appId);

    /**
     * 查询某用户"未来"(严格晚于给定日期)的所有报餐记录, 按就餐时间升序(预订页"已订列表"用;
     * 预约现在直接写入 bc_record, 不再单独维护预约表)
     */
    @Query("select obj from BcRecord obj where obj.appId=:appId and obj.userId=:userId and obj.dinTime > :today order by obj.dinTime asc")
    List<BcRecord> findFutureByUser(@Param("appId") String appId, @Param("userId") Long userId, @Param("today") java.util.Date today);

    /**
     * 查询某用户某年某月的所有报餐记录(预订页日历"已订/已报"标记用)
     */
    @Query("select obj from BcRecord obj where obj.appId=:appId and obj.userId=:userId and obj.dinTime like concat(:yearMonth,'%')")
    List<BcRecord> findByUserAndYearMonth(@Param("appId") String appId, @Param("userId") Long userId, @Param("yearMonth") String yearMonth);
}
