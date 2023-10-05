package ltd.scu.mall.dao;

import ltd.scu.mall.entity.MallUserCouponRecord;
import ltd.scu.mall.util.PageQueryUtil;

import java.util.List;

public interface MallUserCouponRecordMapper {
    int deleteByPrimaryKey(Long couponUserId);

    int insert(MallUserCouponRecord record);

    int insertSelective(MallUserCouponRecord record);

    MallUserCouponRecord selectByPrimaryKey(Long couponUserId);

    int updateByPrimaryKeySelective(MallUserCouponRecord record);

    int updateByPrimaryKey(MallUserCouponRecord record);

    int getUserCouponCount(Long userId, Long couponId);

    int getCouponCount(Long couponId);

    List<MallUserCouponRecord> selectMyCoupons(PageQueryUtil pageQueryUtil);

    Integer countMyCoupons(PageQueryUtil pageQueryUtil);

    List<MallUserCouponRecord> selectMyAvailableCoupons(Long userId);

    MallUserCouponRecord getUserCouponByOrderId(Long orderId);
}
