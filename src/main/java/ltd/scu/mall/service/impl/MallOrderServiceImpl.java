/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本系统已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2020 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package ltd.scu.mall.service.impl;

import ltd.scu.mall.common.*;
import ltd.scu.mall.common.*;
import ltd.scu.mall.config.ProjectConfig;
import ltd.scu.mall.config.RabbitMQConfig;
import ltd.scu.mall.controller.vo.*;
import ltd.scu.mall.dao.*;
import ltd.scu.mall.entity.*;
import ltd.scu.mall.entity.*;
import ltd.scu.mall.exception.NewBeeMallException;
import ltd.scu.mall.service.MallOrderService;
import ltd.scu.mall.task.OrderUnPaidTask;
import ltd.scu.mall.task.TaskService;
import ltd.scu.mall.util.BeanUtil;
import ltd.scu.mall.util.NumberUtil;
import ltd.scu.mall.util.PageQueryUtil;
import ltd.scu.mall.util.PageResult;
import ltd.scu.mall.controller.vo.*;
import ltd.scu.mall.dao.*;
import org.apache.commons.lang3.StringUtils;
import org.springframework.amqp.AmqpException;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.util.CollectionUtils;
import ltd.scu.mall.config.RabbitMQConfig;

import java.util.*;
import java.util.function.Function;
import java.util.stream.Collectors;

import static java.util.stream.Collectors.groupingBy;

@Service
public class MallOrderServiceImpl implements MallOrderService {

    @Autowired
    private MallOrderMapper mallOrderMapper;
    @Autowired
    private MallOrderItemMapper mallOrderItemMapper;
    @Autowired
    private MallShoppingCartItemMapper mallShoppingCartItemMapper;
    @Autowired
    private GoodsMapper goodsMapper;
    @Autowired
    private MallUserCouponRecordMapper mallUserCouponRecordMapper;
    @Autowired
    private CouponMapper couponMapper;
    @Autowired
    private MallSeckillMapper mallSeckillMapper;
    @Autowired
    private MallSeckillSuccessMapper mallSeckillSuccessMapper;
    @Autowired
    private TaskService taskService;
    @Autowired
    private RabbitTemplate rabbitTemplate;

    @Override
    public PageResult<MallOrder> getNewBeeMallOrdersPage(PageQueryUtil pageUtil) {
        int total = mallOrderMapper.getTotalNewBeeMallOrders(pageUtil);
        List<MallOrder> mallOrders = mallOrderMapper.findNewBeeMallOrderList(pageUtil);
        return new PageResult<>(mallOrders, total, pageUtil.getLimit(), pageUtil.getPage());
    }

    @Override
    @Transactional
    public String updateOrderInfo(MallOrder mallOrder) {
        MallOrder temp = mallOrderMapper.selectByPrimaryKey(mallOrder.getOrderId());
        // 不为空且orderStatus>=0且状态为出库之前可以修改部分信息
        if (temp != null && temp.getOrderStatus() >= 0 && temp.getOrderStatus() < 3) {
            temp.setTotalPrice(mallOrder.getTotalPrice());
            temp.setUserAddress(mallOrder.getUserAddress());
            temp.setUpdateTime(new Date());
            if (mallOrderMapper.updateByPrimaryKeySelective(temp) > 0) {
                return ServiceResultEnum.SUCCESS.getResult();
            }
            return ServiceResultEnum.DB_ERROR.getResult();
        }
        return ServiceResultEnum.DATA_NOT_EXIST.getResult();
    }

    @Override
    public boolean updateByPrimaryKeySelective(MallOrder mallOrder) {
        return mallOrderMapper.updateByPrimaryKeySelective(mallOrder) > 0;
    }

    @Override
    @Transactional
    public String checkDone(Long[] ids) {
        // 查询所有的订单 判断状态 修改状态和更新时间
        List<MallOrder> orders = mallOrderMapper.selectByPrimaryKeys(Arrays.asList(ids));
        StringBuilder errorOrderNos = new StringBuilder();
        if (!CollectionUtils.isEmpty(orders)) {
            for (MallOrder mallOrder : orders) {
                if (mallOrder.getIsDeleted() == 1) {
                    errorOrderNos.append(mallOrder.getOrderNo()).append(" ");
                    continue;
                }
                if (mallOrder.getOrderStatus() != 1) {
                    errorOrderNos.append(mallOrder.getOrderNo()).append(" ");
                }
            }
            if (StringUtils.isEmpty(errorOrderNos.toString())) {
                // 订单状态正常 可以执行配货完成操作 修改订单状态和更新时间
                if (mallOrderMapper.checkDone(Arrays.asList(ids)) > 0) {
                    return ServiceResultEnum.SUCCESS.getResult();
                } else {
                    return ServiceResultEnum.DB_ERROR.getResult();
                }
            } else {
                // 订单此时不可执行出库操作
                if (errorOrderNos.length() > 0 && errorOrderNos.length() < 100) {
                    return errorOrderNos + "订单的状态不是支付成功无法执行出库操作";
                } else {
                    return "你选择了太多状态不是支付成功的订单，无法执行配货完成操作";
                }
            }
        }
        // 未查询到数据 返回错误提示
        return ServiceResultEnum.DATA_NOT_EXIST.getResult();
    }

    @Override
    @Transactional
    public String checkOut(Long[] ids) {
        // 查询所有的订单 判断状态 修改状态和更新时间
        List<MallOrder> orders = mallOrderMapper.selectByPrimaryKeys(Arrays.asList(ids));
        StringBuilder errorOrderNos = new StringBuilder();
        if (!CollectionUtils.isEmpty(orders)) {
            for (MallOrder mallOrder : orders) {
                if (mallOrder.getIsDeleted() == 1) {
                    errorOrderNos.append(mallOrder.getOrderNo()).append(" ");
                    continue;
                }
                if (mallOrder.getOrderStatus() != 1 && mallOrder.getOrderStatus() != 2) {
                    errorOrderNos.append(mallOrder.getOrderNo()).append(" ");
                }
            }
            if (StringUtils.isEmpty(errorOrderNos.toString())) {
                // 订单状态正常 可以执行出库操作 修改订单状态和更新时间
                if (mallOrderMapper.checkOut(Arrays.asList(ids)) > 0) {
                    return ServiceResultEnum.SUCCESS.getResult();
                } else {
                    return ServiceResultEnum.DB_ERROR.getResult();
                }
            } else {
                // 订单此时不可执行出库操作
                if (errorOrderNos.length() > 0 && errorOrderNos.length() < 100) {
                    return errorOrderNos + "订单的状态不是支付成功或配货完成无法执行出库操作";
                } else {
                    return "你选择了太多状态不是支付成功或配货完成的订单，无法执行出库操作";
                }
            }
        }
        // 未查询到数据 返回错误提示
        return ServiceResultEnum.DATA_NOT_EXIST.getResult();
    }

    @Override
    @Transactional
    public String closeOrder(Long[] ids) {
        // 查询所有的订单 判断状态 修改状态和更新时间
        List<MallOrder> orders = mallOrderMapper.selectByPrimaryKeys(Arrays.asList(ids));
        StringBuilder errorOrderNos = new StringBuilder();
        if (!CollectionUtils.isEmpty(orders)) {
            for (MallOrder mallOrder : orders) {
                // isDeleted=1 一定为已关闭订单
                if (mallOrder.getIsDeleted() == 1) {
                    errorOrderNos.append(mallOrder.getOrderNo()).append(" ");
                    continue;
                }
                // 已关闭或者已完成无法关闭订单
                if (mallOrder.getOrderStatus() == 4 || mallOrder.getOrderStatus() < 0) {
                    errorOrderNos.append(mallOrder.getOrderNo()).append(" ");
                }
            }
            if (StringUtils.isEmpty(errorOrderNos.toString())) {
                // 订单状态正常 可以执行关闭操作 修改订单状态和更新时间
                if (mallOrderMapper.closeOrder(Arrays.asList(ids), NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_JUDGE.getOrderStatus()) > 0) {
                    return ServiceResultEnum.SUCCESS.getResult();
                } else {
                    return ServiceResultEnum.DB_ERROR.getResult();
                }
            } else {
                // 订单此时不可执行关闭操作
                if (errorOrderNos.length() > 0 && errorOrderNos.length() < 100) {
                    return errorOrderNos + "订单不能执行关闭操作";
                } else {
                    return "你选择的订单不能执行关闭操作";
                }
            }
        }
        // 未查询到数据 返回错误提示
        return ServiceResultEnum.DATA_NOT_EXIST.getResult();
    }

    @Override
    @Transactional(rollbackFor = Exception.class)
    public String saveOrder(MallUserVO user, Long couponUserId, List<MallShoppingCartItemVO> myShoppingCartItems) {
//       rabbitTemplate.convertAndSend(RabbitMQConfig.EXCHANGE_NAME,"boot.haha","boot mq hello~");
        List<Long> itemIdList = myShoppingCartItems.stream().map(MallShoppingCartItemVO::getCartItemId).collect(Collectors.toList());
        List<Long> goodsIds = myShoppingCartItems.stream().map(MallShoppingCartItemVO::getGoodsId).collect(Collectors.toList());
        List<MallGoods> mallGoods = goodsMapper.selectByPrimaryKeys(goodsIds);
        // 检查是否包含已下架商品
        List<MallGoods> goodsListNotSelling = mallGoods.stream()
                .filter(newBeeMallGoodsTemp -> newBeeMallGoodsTemp.getGoodsSellStatus() != Constants.SELL_STATUS_UP)
                .collect(Collectors.toList());
        if (!CollectionUtils.isEmpty(goodsListNotSelling)) {
            // goodsListNotSelling 对象非空则表示有下架商品
            NewBeeMallException.fail(goodsListNotSelling.get(0).getGoodsName() + "已下架，无法生成订单");
        }
        Map<Long, MallGoods> MallGoodsMap = mallGoods.stream().collect(Collectors.toMap(MallGoods::getGoodsId, Function.identity(), (entity1, entity2) -> entity1));
        // 判断商品库存
        for (MallShoppingCartItemVO shoppingCartItemVO : myShoppingCartItems) {
            // 查出的商品中不存在购物车中的这条关联商品数据，直接返回错误提醒
            if (!MallGoodsMap.containsKey(shoppingCartItemVO.getGoodsId())) {
                NewBeeMallException.fail(ServiceResultEnum.SHOPPING_ITEM_ERROR.getResult());
            }
            // 存在数量大于库存的情况，直接返回错误提醒
            if (shoppingCartItemVO.getGoodsCount() > MallGoodsMap.get(shoppingCartItemVO.getGoodsId()).getStockNum()) {
                NewBeeMallException.fail(ServiceResultEnum.SHOPPING_ITEM_COUNT_ERROR.getResult());
            }
        }
        if (CollectionUtils.isEmpty(itemIdList) || CollectionUtils.isEmpty(goodsIds) || CollectionUtils.isEmpty(mallGoods)) {
            NewBeeMallException.fail(ServiceResultEnum.ORDER_GENERATE_ERROR.getResult());
        }
        if (mallShoppingCartItemMapper.deleteBatch(itemIdList) <= 0) {
            NewBeeMallException.fail(ServiceResultEnum.DB_ERROR.getResult());
        }
        List<StockNumDTO> stockNumDTOS = BeanUtil.copyList(myShoppingCartItems, StockNumDTO.class);
        int updateStockNumResult = goodsMapper.updateStockNum(stockNumDTOS);
        if (updateStockNumResult < 1) {
            NewBeeMallException.fail(ServiceResultEnum.SHOPPING_ITEM_COUNT_ERROR.getResult());
        }
        // 生成订单号
        String orderNo = NumberUtil.genOrderNo();
        int priceTotal = 0;
        // 保存订单
        MallOrder mallOrder = new MallOrder();
        mallOrder.setOrderNo(orderNo);
        mallOrder.setUserId(user.getUserId());
        mallOrder.setUserAddress(user.getAddress());
        // 总价
        for (MallShoppingCartItemVO mallShoppingCartItemVO : myShoppingCartItems) {
            priceTotal += mallShoppingCartItemVO.getGoodsCount() * mallShoppingCartItemVO.getSellingPrice();
        }
        // 如果使用了优惠券
        if (couponUserId != null) {
            MallUserCouponRecord mallUserCouponRecord = mallUserCouponRecordMapper.selectByPrimaryKey(couponUserId);
            Long userId = mallUserCouponRecord.getUserId();
            if (!Objects.equals(userId, user.getUserId())) {
                NewBeeMallException.fail("优惠卷所属用户与当前用户不一致！");
            }
            Long couponId = mallUserCouponRecord.getCouponId();
            MallCoupon mallCoupon = couponMapper.selectByPrimaryKey(couponId);
            priceTotal -= mallCoupon.getDiscount();
        }
        if (priceTotal < 1) {
            NewBeeMallException.fail(ServiceResultEnum.ORDER_PRICE_ERROR.getResult());
        }
        mallOrder.setTotalPrice(priceTotal);
        String extraInfo = "支付宝沙箱支付";
        mallOrder.setExtraInfo(extraInfo);
        // 生成订单项并保存订单项纪录
        if (mallOrderMapper.insertSelective(mallOrder) <= 0) {
            NewBeeMallException.fail(ServiceResultEnum.DB_ERROR.getResult());
        }
        // 如果使用了优惠券，则更新优惠券状态
        if (couponUserId != null) {
            MallUserCouponRecord couponUser = new MallUserCouponRecord();
            couponUser.setCouponUserId(couponUserId);
            couponUser.setOrderId(mallOrder.getOrderId());
            couponUser.setUseStatus((byte) 1);
            couponUser.setUsedTime(new Date());
            couponUser.setUpdateTime(new Date());
            mallUserCouponRecordMapper.updateByPrimaryKeySelective(couponUser);
        }
        // 生成所有的订单项快照，并保存至数据库
        List<MallOrderItem> mallOrderItems = new ArrayList<>();
        for (MallShoppingCartItemVO mallShoppingCartItemVO : myShoppingCartItems) {
            MallOrderItem mallOrderItem = new MallOrderItem();
            // 使用BeanUtil工具类将MallShoppingCartItemVO中的属性复制到MallOrderItem对象中
            BeanUtil.copyProperties(mallShoppingCartItemVO, mallOrderItem);
            // MallOrderMapper文件insert()方法中使用了useGeneratedKeys因此orderId可以获取到
            mallOrderItem.setOrderId(mallOrder.getOrderId());
            mallOrderItems.add(mallOrderItem);
        }
        // 保存至数据库
        if (mallOrderItemMapper.insertBatch(mallOrderItems) <= 0) {
            NewBeeMallException.fail(ServiceResultEnum.DB_ERROR.getResult());
        }
        // 订单支付超期任务，超过300秒自动取消订单
//        taskService.addTask(new OrderUnPaidTask(mallOrder.getOrderId(), ProjectConfig.getOrderUnpaidOverTime() * 1000));
//        try {
//            System.out.println(mallOrderItems);
//            System.out.println(orderNo);
//        } catch (AmqpException e){
//            e.printStackTrace();
//        }
        // 所有操作成功后，将订单号返回，以供Controller方法跳转到订单详情

        return orderNo;
    }

//    @RabbitListener(queues = "order_queue")
//    public void receiveMessage() {
//        System.out.println("Received message from order_queue: " );
//    }

    @Override
    public String seckillSaveOrder(Long seckillSuccessId, Long userId) {
        MallSeckillSuccess mallSeckillSuccess = mallSeckillSuccessMapper.selectByPrimaryKey(seckillSuccessId);
        if (!mallSeckillSuccess.getUserId().equals(userId)) {
            throw new NewBeeMallException("当前登陆用户与抢购秒杀商品的用户不匹配");
        }
        Long seckillId = mallSeckillSuccess.getSeckillId();
        MallSeckill mallSeckill = mallSeckillMapper.selectByPrimaryKey(seckillId);
        Long goodsId = mallSeckill.getGoodsId();
        MallGoods mallGoods = goodsMapper.selectByPrimaryKey(goodsId);
        // 生成订单号
        String orderNo = NumberUtil.genOrderNo();
        // 保存订单
        MallOrder mallOrder = new MallOrder();
        mallOrder.setOrderNo(orderNo);
        mallOrder.setTotalPrice(mallSeckill.getSeckillPrice());
        mallOrder.setUserId(userId);
        mallOrder.setUserAddress("秒杀测试地址");
        mallOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus());
        mallOrder.setPayStatus((byte) PayStatusEnum.PAY_SUCCESS.getPayStatus());
        mallOrder.setPayType((byte) PayTypeEnum.WEIXIN_PAY.getPayType());
        mallOrder.setPayTime(new Date());
        String extraInfo = "";
        mallOrder.setExtraInfo(extraInfo);
        if (mallOrderMapper.insertSelective(mallOrder) <= 0) {
            throw new NewBeeMallException("生成订单内部异常");
        }
        // 保存订单商品项
        MallOrderItem mallOrderItem = new MallOrderItem();
        Long orderId = mallOrder.getOrderId();
        mallOrderItem.setOrderId(orderId);
        mallOrderItem.setSeckillId(seckillId);
        mallOrderItem.setGoodsId(mallGoods.getGoodsId());
        mallOrderItem.setGoodsCoverImg(mallGoods.getGoodsCoverImg());
        mallOrderItem.setGoodsName(mallGoods.getGoodsName());
        mallOrderItem.setGoodsCount(1);
        mallOrderItem.setSellingPrice(mallSeckill.getSeckillPrice());
        if (mallOrderItemMapper.insert(mallOrderItem) <= 0) {
            throw new NewBeeMallException("生成订单内部异常");
        }
        // 订单支付超期任务
        taskService.addTask(new OrderUnPaidTask(mallOrder.getOrderId(), 30 * 1000));
        return orderNo;
    }

    @Override
    public MallOrderDetailVO getOrderDetailByOrderNo(String orderNo, Long userId) {
        MallOrder mallOrder = mallOrderMapper.selectByOrderNo(orderNo);
        if (mallOrder == null) {
            NewBeeMallException.fail(ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult());
        }
        //验证是否是当前userId下的订单，否则报错
        if (!userId.equals(mallOrder.getUserId())) {
            NewBeeMallException.fail(ServiceResultEnum.NO_PERMISSION_ERROR.getResult());
        }
        List<MallOrderItem> orderItems = mallOrderItemMapper.selectByOrderId(mallOrder.getOrderId());
        //获取订单项数据
        if (CollectionUtils.isEmpty(orderItems)) {
            NewBeeMallException.fail(ServiceResultEnum.ORDER_ITEM_NOT_EXIST_ERROR.getResult());
        }
        List<MallOrderItemVO> mallOrderItemVOS = BeanUtil.copyList(orderItems, MallOrderItemVO.class);
        MallOrderDetailVO mallOrderDetailVO = new MallOrderDetailVO();
        BeanUtil.copyProperties(mallOrder, mallOrderDetailVO);
        mallOrderDetailVO.setOrderStatusString(NewBeeMallOrderStatusEnum.getNewBeeMallOrderStatusEnumByStatus(mallOrderDetailVO.getOrderStatus()).getName());
        mallOrderDetailVO.setPayTypeString(PayTypeEnum.getPayTypeEnumByType(mallOrderDetailVO.getPayType()).getName());
        mallOrderDetailVO.setNewBeeMallOrderItemVOS(mallOrderItemVOS);
        return mallOrderDetailVO;
    }

    @Override
    public MallOrder getNewBeeMallOrderByOrderNo(String orderNo) {
        return mallOrderMapper.selectByOrderNo(orderNo);
    }

    @Override
    public PageResult getMyOrders(PageQueryUtil pageUtil) {
        int total = mallOrderMapper.getTotalNewBeeMallOrders(pageUtil);
        List<MallOrderListVO> orderListVOS = new ArrayList<>();
        if (total > 0) {
            List<MallOrder> mallOrders = mallOrderMapper.findNewBeeMallOrderList(pageUtil);
            // 数据转换 将实体类转成vo
            orderListVOS = BeanUtil.copyList(mallOrders, MallOrderListVO.class);
            // 设置订单状态中文显示值
            for (MallOrderListVO mallOrderListVO : orderListVOS) {
                mallOrderListVO.setOrderStatusString(NewBeeMallOrderStatusEnum.getNewBeeMallOrderStatusEnumByStatus(mallOrderListVO.getOrderStatus()).getName());
            }
            List<Long> orderIds = mallOrders.stream().map(MallOrder::getOrderId).collect(Collectors.toList());
            if (!CollectionUtils.isEmpty(orderIds)) {
                List<MallOrderItem> orderItems = mallOrderItemMapper.selectByOrderIds(orderIds);
                Map<Long, List<MallOrderItem>> itemByOrderIdMap = orderItems.stream().collect(groupingBy(MallOrderItem::getOrderId));
                for (MallOrderListVO mallOrderListVO : orderListVOS) {
                    // 封装每个订单列表对象的订单项数据
                    if (itemByOrderIdMap.containsKey(mallOrderListVO.getOrderId())) {
                        List<MallOrderItem> orderItemListTemp = itemByOrderIdMap.get(mallOrderListVO.getOrderId());
                        // 将NewBeeMallOrderItem对象列表转换成NewBeeMallOrderItemVO对象列表
                        List<MallOrderItemVO> mallOrderItemVOS = BeanUtil.copyList(orderItemListTemp, MallOrderItemVO.class);
                        mallOrderListVO.setNewBeeMallOrderItemVOS(mallOrderItemVOS);
                    }
                }
            }
        }
        return new PageResult(orderListVOS, total, pageUtil.getLimit(), pageUtil.getPage());
    }

    @Override
    public String cancelOrder(String orderNo, Long userId) {
        MallOrder mallOrder = mallOrderMapper.selectByOrderNo(orderNo);
        if (mallOrder != null) {
            // 验证是否是当前userId下的订单，否则报错
            if (!userId.equals(mallOrder.getUserId())) {
                NewBeeMallException.fail(ServiceResultEnum.NO_PERMISSION_ERROR.getResult());
            }
            // 订单状态判断
            if (mallOrder.getOrderStatus().intValue() == NewBeeMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus()
                    || mallOrder.getOrderStatus().intValue() == NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_MALLUSER.getOrderStatus()
                    || mallOrder.getOrderStatus().intValue() == NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_EXPIRED.getOrderStatus()
                    || mallOrder.getOrderStatus().intValue() == NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_JUDGE.getOrderStatus()) {
                return ServiceResultEnum.ORDER_STATUS_ERROR.getResult();
            }
            if (mallOrderMapper.closeOrder(Collections.singletonList(mallOrder.getOrderId()), NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_MALLUSER.getOrderStatus()) > 0) {
                return ServiceResultEnum.SUCCESS.getResult();
            } else {
                return ServiceResultEnum.DB_ERROR.getResult();
            }
        }
        return ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult();
    }

    @Override
    public String finishOrder(String orderNo, Long userId) {

        MallOrder mallOrder = mallOrderMapper.selectByOrderNo(orderNo);
        if (mallOrder != null) {
            // 验证是否是当前userId下的订单，否则报错
            if (!userId.equals(mallOrder.getUserId())) {
                return ServiceResultEnum.NO_PERMISSION_ERROR.getResult();
            }
            // 订单状态判断 非出库状态下不进行修改操作
            if (mallOrder.getOrderStatus().intValue() != NewBeeMallOrderStatusEnum.ORDER_EXPRESS.getOrderStatus()) {
                return ServiceResultEnum.ORDER_STATUS_ERROR.getResult();
            }
            mallOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_SUCCESS.getOrderStatus());
            mallOrder.setUpdateTime(new Date());
            if (mallOrderMapper.updateByPrimaryKeySelective(mallOrder) > 0) {
                return ServiceResultEnum.SUCCESS.getResult();
            } else {
                return ServiceResultEnum.DB_ERROR.getResult();
            }
        }

        return ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult();
    }



    @Override
    public String paySuccess(String orderNo, int payType) {
        System.out.println("finish");
        MallOrder mallOrder = mallOrderMapper.selectByOrderNo(orderNo);
        if (mallOrder == null) {
            return ServiceResultEnum.ORDER_NOT_EXIST_ERROR.getResult();
        }
        // 订单状态判断 非待支付状态下不进行修改操作
        if (mallOrder.getOrderStatus().intValue() != NewBeeMallOrderStatusEnum.ORDER_PRE_PAY.getOrderStatus()) {
            return ServiceResultEnum.ORDER_STATUS_ERROR.getResult();
        }
        mallOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_PAID.getOrderStatus());
        mallOrder.setPayType((byte) payType);
        mallOrder.setPayStatus((byte) PayStatusEnum.PAY_SUCCESS.getPayStatus());
        mallOrder.setPayTime(new Date());
        mallOrder.setUpdateTime(new Date());
        long currentTime = System.currentTimeMillis();
        long paymentTime = mallOrder.getCreateTime().getTime();
        // 判断是否超时，执行相应的操作
        if ((currentTime - paymentTime) > 60000){
            mallOrder.setOrderStatus((byte) NewBeeMallOrderStatusEnum.ORDER_CLOSED_BY_EXPIRED.getOrderStatus());
            mallOrderMapper.updateByPrimaryKeySelective(mallOrder);
            return ServiceResultEnum.CROSSTIMEOUT.getResult();//如果mallorder的paytime超过了120seconds那么报超时错
        }
        if (mallOrderMapper.updateByPrimaryKeySelective(mallOrder) <= 0) {
            return ServiceResultEnum.DB_ERROR.getResult();
        }
//        taskService.removeTask(new OrderUnPaidTask(mallOrder.getOrderId()));


        return ServiceResultEnum.SUCCESS.getResult();
    }

    @Override
    public List<MallOrderItemVO> getOrderItems(Long id) {
        MallOrder mallOrder = mallOrderMapper.selectByPrimaryKey(id);
        if (mallOrder != null) {
            List<MallOrderItem> orderItems = mallOrderItemMapper.selectByOrderId(mallOrder.getOrderId());
            // 获取订单项数据
            if (!CollectionUtils.isEmpty(orderItems)) {
                return BeanUtil.copyList(orderItems, MallOrderItemVO.class);
            }
        }
        return null;
    }

    public MallOrder getOrderIdByOrderNO(String orderNo){
       return mallOrderMapper.selectByOrderNo(orderNo);
    }

}
