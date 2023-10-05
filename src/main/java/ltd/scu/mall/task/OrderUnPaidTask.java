package ltd.scu.mall.task;

import ltd.scu.mall.common.Constants;
import ltd.scu.mall.common.NewBeeMallOrderStatusEnum;
import ltd.scu.mall.dao.GoodsMapper;
import ltd.scu.mall.dao.MallOrderItemMapper;
import ltd.scu.mall.dao.MallOrderMapper;
import ltd.scu.mall.dao.MallSeckillMapper;
import ltd.scu.mall.entity.MallOrder;
import ltd.scu.mall.entity.MallOrderItem;
import ltd.scu.mall.redis.RedisCache;
import ltd.scu.mall.service.MallCouponService;
import ltd.scu.mall.util.SpringContextUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.Date;
import java.util.List;

/**
 * 未支付订单超时自动取消任务
 */
public class OrderUnPaidTask extends Task {
    /**
     * 默认延迟时间30分钟，单位毫秒
     */
    private static final long DELAY_TIME = 30 * 60 * 1000;

    private final Logger log = LoggerFactory.getLogger(OrderUnPaidTask.class);
    /**
     * 订单id
     */
    private final Long orderId;

    public OrderUnPaidTask(Long orderId, long delayInMilliseconds) {
        super("OrderUnPaidTask-" + orderId, delayInMilliseconds);
        this.orderId = orderId;
    }

    public OrderUnPaidTask(Long orderId) {
        super("OrderUnPaidTask-" + orderId, DELAY_TIME);
        this.orderId = orderId;
    }

//    @Override
//    public void run() {
//        log.info("系统开始处理延时任务---订单超时未付款--- {}", this.orderId);
//
//        MallOrderMapper mallOrderMapper = SpringContextUtil.getBean(MallOrderMapper.class);
//        MallOrderItemMapper mallOrderItemMapper = SpringContextUtil.getBean(MallOrderItemMapper.class);
//        GoodsMapper goodsMapper = SpringContextUtil.getBean(GoodsMapper.class);
//        MallCouponService mallCouponService = SpringContextUtil.getBean(MallCouponService.class);
//
//        MallOrder order = mallOrderMapper.selectByPrimaryKey(orderId);
//        if (order == null) {
//            log.info("系统结束处理延时任务---订单超时未付款--- {}", this.orderId);
//            return;
//        }
//        if (order.getOrderStatus() != NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus()) {
//            log.info("系统结束处理延时任务---订单超时未付款--- {}", this.orderId);
//            return;
//        }
//
//        // 设置订单为已取消状态
//        order.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_EXPIRED.getOrderStatus());
//        order.setUpdateTime(new Date());
//        if (mallOrderMapper.updateByPrimaryKey(order) <= 0) {
//            throw new RuntimeException("更新数据已失效");
//        }
//
//        // 商品货品数量增加
//        List<MallOrderItem> mallOrderItems = mallOrderItemMapper.selectByOrderId(orderId);
//        for (MallOrderItem orderItem : mallOrderItems) {
//            if (orderItem.getSeckillId() != null) {
//                Long seckillId = orderItem.getSeckillId();
//                MallSeckillMapper mallSeckillMapper = SpringContextUtil.getBean(MallSeckillMapper.class);
//                RedisCache redisCache = SpringContextUtil.getBean(RedisCache.class);
//                if (!mallSeckillMapper.addStock(seckillId)) {
//                    throw new RuntimeException("秒杀商品货品库存增加失败");
//                }
//                redisCache.increment(Constants.SECKILL_GOODS_STOCK_KEY + seckillId);
//            } else {
//                Long goodsId = orderItem.getGoodsId();
//                Integer goodsCount = orderItem.getGoodsCount();
//                if (!goodsMapper.addStock(goodsId, goodsCount)) {
//                    throw new RuntimeException("商品货品库存增加失败");
//                }
//            }
//        }
//
//        // 返还优惠券
//        mallCouponService.releaseCoupon(orderId);
//        log.info("系统结束处理延时任务---订单超时未付款--- {}", this.orderId);
//    }
//

    //更新 使用延迟消息队列
    @Override
    public void run() {
        log.info("系统开始处理延时任务---订单超时未付款--- {}", this.orderId);

        MallOrderMapper mallOrderMapper = SpringContextUtil.getBean(MallOrderMapper.class);
        MallOrderItemMapper mallOrderItemMapper = SpringContextUtil.getBean(MallOrderItemMapper.class);
        GoodsMapper goodsMapper = SpringContextUtil.getBean(GoodsMapper.class);
        MallCouponService mallCouponService = SpringContextUtil.getBean(MallCouponService.class);

        MallOrder order = mallOrderMapper.selectByPrimaryKey(orderId);
        if (order == null) {
            log.info("系统结束处理延时任务---订单超时未付款--- {}", this.orderId);
            return;
        }
        if (order.getOrderStatus() != NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus()) {
            log.info("系统结束处理延时任务---订单超时未付款--- {}", this.orderId);
            return;
        }

        // 设置订单为已取消状态
        order.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_EXPIRED.getOrderStatus());
        order.setUpdateTime(new Date());
        if (mallOrderMapper.updateByPrimaryKey(order) <= 0) {
            throw new RuntimeException("更新数据已失效");
        }

        // 商品货品数量增加
        List<MallOrderItem> mallOrderItems = mallOrderItemMapper.selectByOrderId(orderId);
        for (MallOrderItem orderItem : mallOrderItems) {
            if (orderItem.getSeckillId() != null) {
                Long seckillId = orderItem.getSeckillId();
                MallSeckillMapper mallSeckillMapper = SpringContextUtil.getBean(MallSeckillMapper.class);
                RedisCache redisCache = SpringContextUtil.getBean(RedisCache.class);
                if (!mallSeckillMapper.addStock(seckillId)) {
                    throw new RuntimeException("秒杀商品货品库存增加失败");
                }
                redisCache.increment(Constants.SECKILL_GOODS_STOCK_KEY + seckillId);
            } else {
                Long goodsId = orderItem.getGoodsId();
                Integer goodsCount = orderItem.getGoodsCount();
                if (!goodsMapper.addStock(goodsId, goodsCount)) {
                    throw new RuntimeException("商品货品库存增加失败");
                }
            }
        }

        // 返还优惠券
        mallCouponService.releaseCoupon(orderId);
        log.info("系统结束处理延时任务---订单超时未付款--- {}", this.orderId);
    }

}
