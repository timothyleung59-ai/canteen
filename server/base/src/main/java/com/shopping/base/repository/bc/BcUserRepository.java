package com.shopping.base.repository.bc;

import com.shopping.base.domain.bc.BcUser;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

/**
 * @author hx
 * @date 2019/7/18 14:59
 */
public interface BcUserRepository extends JpaRepository<BcUser,Long> {

    @Modifying
    @Query("update  BcUser set status=:status  where appId =:appId and id=:id")
    int updateStatusById(@Param("status")int status,@Param("appId")String appId,@Param("id")Long id);

    @Modifying
    @Query("update BcUser set userDepartmentId =null where appId = :appId and userDepartmentId=:userDepartmentId")
    int updateBcUserDepartmentId(@Param("appId")String appId,@Param("userDepartmentId")Long userDepartmentId);

    @Modifying
    @Query("update BcUser  set userDepartmentId =:userDepartmentId where appId =:appId and id=:id")
    int editBcUserDepartmentById(@Param("userDepartmentId")Long userDepartmentId,@Param("appId")String appId,@Param("id")Long id);

    /**
     * 软删除员工(只打删除标记, 不物理删除该行), 保留历史报餐记录的关联查询(报表 join bc_user 不受影响)
     */
    @Modifying
    @Query("update BcUser set deleteStatus=true where appId =:appId and id=:id")
    int softDeleteById(@Param("appId")String appId,@Param("id")Long id);
}
