package ltd.scu.mall.service;

import ltd.scu.mall.controller.vo.MallCouponVO;
import ltd.scu.mall.controller.vo.MallMyCouponVO;
import ltd.scu.mall.controller.vo.MallShoppingCartItemVO;
import ltd.scu.mall.entity.MallCoupon;
import ltd.scu.mall.util.PageQueryUtil;
import ltd.scu.mall.util.PageResult;

import java.util.List;

public interface MallCouponService {

    PageResult getCouponPage(PageQueryUtil pageUtil);

    boolean saveCoupon(MallCoupon mallCoupon);

    boolean updateCoupon(MallCoupon mallCoupon);

    MallCoupon getCouponById(Long id);

    boolean deleteCouponById(Long id);

    /**
     * 查询可用优惠券
     *
     * @param userId
     * @return
     */
    List<MallCouponVO> selectAvailableCoupon(Long userId);

    /**
     * 用户领取优惠劵
     *
     * @param couponId 优惠劵ID
     * @param userId   用户ID
     * @return boolean
     */
    boolean saveCouponUser(Long couponId, Long userId);

    /**
     * 查询我的优惠券
     *
     * @param userId 用户ID
     * @return
     */
    PageResult<MallCouponVO> selectMyCoupons(PageQueryUtil pageQueryUtil);

    /**
     * 查询当前订单可用的优惠券
     *
     * @param myShoppingCartItems
     * @param priceTotal
     * @param userId
     * @return
     */
    List<MallMyCouponVO> selectOrderCanUseCoupons(List<MallShoppingCartItemVO> myShoppingCartItems, int priceTotal, Long userId);

    /**
     * 删除用户优惠券
     *
     * @param couponUserId
     * @return
     */
    boolean deleteCouponUser(Long couponUserId);

    /**
     * 回复未支付的优惠券
     * @param orderId
     */
    void releaseCoupon(Long orderId);

}
