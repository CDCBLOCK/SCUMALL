package ltd.scu.mall.dao;

import ltd.scu.mall.entity.MallCoupon;
import ltd.scu.mall.util.PageQueryUtil;

import java.util.List;

public interface CouponMapper {
    int deleteByPrimaryKey(Long couponId);

    int deleteBatch(Integer[] couponIds);

    int insert(MallCoupon record);

    int insertSelective(MallCoupon record);

    MallCoupon selectByPrimaryKey(Long couponId);

    int updateByPrimaryKeySelective(MallCoupon record);

    int updateByPrimaryKey(MallCoupon record);

    List<MallCoupon> findCouponlList(PageQueryUtil pageUtil);

    int getTotalCoupons(PageQueryUtil pageUtil);

    List<MallCoupon> selectAvailableCoupon();

    int reduceCouponTotal(Long couponId);

    List<MallCoupon> selectByIds(List<Long> couponIds);

    List<MallCoupon> selectAvailableGiveCoupon();

}
