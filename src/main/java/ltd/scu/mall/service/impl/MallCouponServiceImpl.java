package ltd.scu.mall.service.impl;

import ltd.scu.mall.controller.vo.MallCouponVO;
import ltd.scu.mall.controller.vo.MallMyCouponVO;
import ltd.scu.mall.controller.vo.MallShoppingCartItemVO;
import ltd.scu.mall.dao.CouponMapper;
import ltd.scu.mall.dao.GoodsMapper;
import ltd.scu.mall.dao.MallUserCouponRecordMapper;
import ltd.scu.mall.entity.MallCoupon;
import ltd.scu.mall.entity.MallGoods;
import ltd.scu.mall.entity.MallUserCouponRecord;
import ltd.scu.mall.service.MallCouponService;
import ltd.scu.mall.util.BeanUtil;
import ltd.scu.mall.util.PageQueryUtil;
import ltd.scu.mall.util.PageResult;
import ltd.scu.mall.exception.NewBeeMallException;
import org.apache.commons.collections4.CollectionUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

import static java.util.stream.Collectors.toList;
import static java.util.stream.Collectors.toMap;

@Service
public class MallCouponServiceImpl implements MallCouponService {

    @Autowired
    private CouponMapper couponMapper;

    @Autowired
    private MallUserCouponRecordMapper mallUserCouponRecordMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Override
    public PageResult getCouponPage(PageQueryUtil pageUtil) {
        List<MallCoupon> carousels = couponMapper.findCouponlList(pageUtil);
        int total = couponMapper.getTotalCoupons(pageUtil);
        return new PageResult(carousels, total, pageUtil.getLimit(), pageUtil.getPage());
    }

    @Override
    public boolean saveCoupon(MallCoupon mallCoupon) {
        return couponMapper.insertSelective(mallCoupon) > 0;
    }

    @Override
    public boolean updateCoupon(MallCoupon mallCoupon) {
        return couponMapper.updateByPrimaryKeySelective(mallCoupon) > 0;
    }

    @Override
    public MallCoupon getCouponById(Long id) {
        return couponMapper.selectByPrimaryKey(id);
    }

    @Override
    public boolean deleteCouponById(Long id) {
        return couponMapper.deleteByPrimaryKey(id) > 0;
    }

    @Override
    public List<MallCouponVO> selectAvailableCoupon(Long userId) {
        List<MallCoupon> mallCoupons = couponMapper.selectAvailableCoupon();
        List<MallCouponVO> couponVOS = BeanUtil.copyList(mallCoupons, MallCouponVO.class);
        for (MallCouponVO couponVO : couponVOS) {
            if (userId != null) {
                int num = mallUserCouponRecordMapper.getUserCouponCount(userId, couponVO.getCouponId());
                if (num > 0) {
                    couponVO.setHasReceived(true);
                }
            }
            if (couponVO.getCouponTotal() != 0) {
                int count = mallUserCouponRecordMapper.getCouponCount(couponVO.getCouponId());
                if (count >= couponVO.getCouponTotal()) {
                    couponVO.setSaleOut(true);
                }
            }
        }
        return couponVOS;
    }

    @Transactional(rollbackFor = Exception.class)
    @Override
    public boolean saveCouponUser(Long couponId, Long userId) {
        MallCoupon mallCoupon = couponMapper.selectByPrimaryKey(couponId);
        if (mallCoupon.getCouponLimit() != 0) {
            int num = mallUserCouponRecordMapper.getUserCouponCount(userId, couponId);
            if (num != 0) {
                throw new NewBeeMallException("优惠券已经领过了,无法再次领取！");
            }
        }
        if (mallCoupon.getCouponTotal() != 0) {
            int count = mallUserCouponRecordMapper.getCouponCount(couponId);
            if (count >= mallCoupon.getCouponTotal()) {
                throw new NewBeeMallException("优惠券已经领完了！");
            }
            if (couponMapper.reduceCouponTotal(couponId) <= 0) {
                throw new NewBeeMallException("优惠券领取失败！");
            }
        }
        MallUserCouponRecord couponUser = new MallUserCouponRecord();
        couponUser.setUserId(userId);
        couponUser.setCouponId(couponId);
        return mallUserCouponRecordMapper.insertSelective(couponUser) > 0;
    }

    @Override
    public PageResult<MallCouponVO> selectMyCoupons(PageQueryUtil pageUtil) {
        Integer total = mallUserCouponRecordMapper.countMyCoupons(pageUtil);
        List<MallCouponVO> couponVOS = new ArrayList<>();
        if (total > 0) {
            List<MallUserCouponRecord> userCouponRecords = mallUserCouponRecordMapper.selectMyCoupons(pageUtil);
            List<Long> couponIds = userCouponRecords.stream().map(MallUserCouponRecord::getCouponId).collect(toList());
            if (CollectionUtils.isNotEmpty(couponIds)) {
                List<MallCoupon> mallMallCoupons = couponMapper.selectByIds(couponIds);
                Map<Long, MallCoupon> listMap = mallMallCoupons.stream().collect(toMap(MallCoupon::getCouponId, newBeeMallCoupon -> newBeeMallCoupon));
                for (MallUserCouponRecord couponUser : userCouponRecords) {
                    MallCouponVO mallCouponVO = new MallCouponVO();
                    MallCoupon mallCoupon = listMap.getOrDefault(couponUser.getCouponId(), new MallCoupon());
                    BeanUtil.copyProperties(mallCoupon, mallCouponVO);
                    mallCouponVO.setCouponUserId(couponUser.getCouponUserId());
                    mallCouponVO.setUseStatus(couponUser.getUseStatus() == 1);
                    couponVOS.add(mallCouponVO);
                }
            }
        }
        return new PageResult<>(couponVOS, total, pageUtil.getLimit(), pageUtil.getPage());
    }

    @Override
    public List<MallMyCouponVO> selectOrderCanUseCoupons(List<MallShoppingCartItemVO> myShoppingCartItems, int priceTotal, Long userId) {
        List<MallUserCouponRecord> couponUsers = mallUserCouponRecordMapper.selectMyAvailableCoupons(userId);
        List<MallMyCouponVO> myCouponVOS = BeanUtil.copyList(couponUsers, MallMyCouponVO.class);
        List<Long> couponIds = couponUsers.stream().map(MallUserCouponRecord::getCouponId).collect(toList());
        if (!couponIds.isEmpty()) {
            ZoneId zone = ZoneId.systemDefault();
            List<MallCoupon> mallCoupons = couponMapper.selectByIds(couponIds);
            for (MallCoupon mallCoupon : mallCoupons) {
                for (MallMyCouponVO myCouponVO : myCouponVOS) {
                    if (mallCoupon.getCouponId().equals(myCouponVO.getCouponId())) {
                        myCouponVO.setName(mallCoupon.getCouponName());
                        myCouponVO.setCouponDesc(mallCoupon.getCouponDesc());
                        myCouponVO.setDiscount(mallCoupon.getDiscount());
                        myCouponVO.setMin(mallCoupon.getMin());
                        myCouponVO.setGoodsType(mallCoupon.getGoodsType());
                        myCouponVO.setGoodsValue(mallCoupon.getGoodsValue());
                        ZonedDateTime startZonedDateTime = mallCoupon.getCouponStartTime().atStartOfDay(zone);
                        ZonedDateTime endZonedDateTime = mallCoupon.getCouponEndTime().atStartOfDay(zone);
                        myCouponVO.setStartTime(Date.from(startZonedDateTime.toInstant()));
                        myCouponVO.setEndTime(Date.from(endZonedDateTime.toInstant()));
                    }
                }
            }
        }
        long nowTime = System.currentTimeMillis();
        return myCouponVOS.stream().filter(item -> {
            // 判断有效期
            Date startTime = item.getStartTime();
            Date endTime = item.getEndTime();
            if (startTime == null || endTime == null || nowTime < startTime.getTime() || nowTime > endTime.getTime()) {
                return false;
            }
            // 判断使用条件
            boolean b = false;
            if (item.getMin() <= priceTotal) {
                if (item.getGoodsType() == 1) { // 指定分类可用
                    String[] split = item.getGoodsValue().split(",");
                    List<Long> goodsValue = Arrays.stream(split).map(Long::valueOf).toList();
                    List<Long> goodsIds = myShoppingCartItems.stream().map(MallShoppingCartItemVO::getGoodsId).collect(toList());
                    List<MallGoods> goods = goodsMapper.selectByPrimaryKeys(goodsIds);
                    List<Long> categoryIds = goods.stream().map(MallGoods::getGoodsCategoryId).toList();
                    for (Long categoryId : categoryIds) {
                        if (goodsValue.contains(categoryId)) {
                            b = true;
                            break;
                        }
                    }
                } else if (item.getGoodsType() == 2) { // 指定商品可用
                    String[] split = item.getGoodsValue().split(",");
                    List<Long> goodsValue = Arrays.stream(split).map(Long::valueOf).toList();
                    List<Long> goodsIds = myShoppingCartItems.stream().map(MallShoppingCartItemVO::getGoodsId).toList();
                    for (Long goodsId : goodsIds) {
                        if (goodsValue.contains(goodsId)) {
                            b = true;
                            break;
                        }
                    }
                } else { // 全场通用
                    b = true;
                }
            }
            return b;
        }).sorted(Comparator.comparingInt(MallMyCouponVO::getDiscount)).collect(toList());
    }

    @Override
    public boolean deleteCouponUser(Long couponUserId) {
        return mallUserCouponRecordMapper.deleteByPrimaryKey(couponUserId) > 0;
    }

    @Override
    public void releaseCoupon(Long orderId) {
        MallUserCouponRecord mallUserCouponRecord = mallUserCouponRecordMapper.getUserCouponByOrderId(orderId);
        if (mallUserCouponRecord == null) {
            return;
        }
        mallUserCouponRecord.setUseStatus((byte) 0);
        mallUserCouponRecord.setUpdateTime(new Date());
        mallUserCouponRecordMapper.updateByPrimaryKey(mallUserCouponRecord);
    }
}
