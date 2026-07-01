package com.shopping.wx.service.bc;

import com.shopping.base.domain.bc.BcReserveRecord;
import com.shopping.base.foundation.base.service.IBaseService;
import com.shopping.base.foundation.result.ActionResult;
import com.shopping.wx.form.bc.BcReserveRecordAddForm;

import java.util.Date;
import java.util.List;
import java.util.Map;

/**
 * @anthor bin
 * @data 2019/7/9 15:00
 * 类描述：预定就餐接口
 * <p>
 * 说明: 预约现在直接写入 bc_record(与"手动报餐"同一张表, 渠道标记为"预约报餐"), 不再经过
 * bc_reserve_record 这张中间表 + 批处理转换。原因: 之前预约记录需要靠 bctch 批处理才能
 * 转成正式报餐记录, 但这个批处理从未被自动调度触发过, 导致预约了却永远不出现在报餐明细/
 * 统计里。查询类方法因此返回 Map 而不是 BcReserveRecord 实体(数据源变成了 BcRecord)。
 */
public interface BcReserveRecordService extends IBaseService<BcReserveRecord,Long>{
    /**
     * 预约就餐记录保存
     * @param  appid
     * @param id
     * @param bcReserveRecordAddForm
     * @return
     * @throws Exception
     */
    ActionResult bcReserveRecordSave(String appid,Long id,BcReserveRecordAddForm bcReserveRecordAddForm,int status)throws Exception;
    /**
     * 根据用户id和appid查询"未来"的报餐记录(即预约中的记录), 每条含 id/reserveTime/reserveTimeWeek
     * @param id
     * @param appid
     * @return
     * @throws Exception
     */
    List<Map<String,Object>> getBcReserveRecordList(Long id, String appid) throws Exception;

    /**
     * 根据选中的记录删除数据库的预约记录
     * @param appId
     * @param id
     * @param bcUserId
     * @return
     * @throws Exception
     */
    ActionResult deleteBcReserveRecordById(String appId,Long bcUserId,Long id)throws Exception;

    /**
     * @deprecated 预约已直接写入 bc_record, 不再需要"转换"这一步; bc_reserve_record 表
     * 不再有新数据写入, 此方法保留仅为兼容旧接口签名, 不建议再调用
     */
    @Deprecated
    List<BcReserveRecord> getBcReserveRecordListByReserveTime(String appId,String reserveRecordTime) throws  Exception;

    /**
     * @deprecated 同上, 预约不再需要批量转换
     */
    @Deprecated
    ActionResult bctchBcReserveRecordByReserveTime(String appId) throws  Exception;

    /**
     * @deprecated 同上
     */
    @Deprecated
    int deleteAllByReserveTime(String appId,Date reserveTime)throws  Exception;

    /**
     * 获取指定用户某年某月的报餐记录(用于预订页日历标记), 每条含 id/reserveTime/reserveTimeWeek
     * @param appId
     * @param curYearMonth
     * @return
     * @throws Exception
     */
    List<Map<String,Object>> getBcReserveRecordByYearAndMonth(String appId,Long bcUserId,String curYearMonth)throws Exception;
}
