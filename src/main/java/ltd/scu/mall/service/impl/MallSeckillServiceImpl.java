package ltd.scu.mall.service.impl;

import com.google.common.util.concurrent.RateLimiter;
import ltd.scu.mall.common.Constants;
import ltd.scu.mall.exception.NewBeeMallException;
import ltd.scu.mall.common.SeckillStatusEnum;
import ltd.scu.mall.common.ServiceResultEnum;
import ltd.scu.mall.controller.vo.ExposerVO;
import ltd.scu.mall.controller.vo.MallSeckillGoodsVO;
import ltd.scu.mall.controller.vo.SeckillSuccessVO;
import ltd.scu.mall.dao.GoodsMapper;
import ltd.scu.mall.dao.MallSeckillMapper;
import ltd.scu.mall.dao.MallSeckillSuccessMapper;
import ltd.scu.mall.entity.MallSeckill;
import ltd.scu.mall.entity.MallSeckillSuccess;
import ltd.scu.mall.redis.RedisCache;
import ltd.scu.mall.service.MallSeckillService;
import ltd.scu.mall.util.MD5Util;
import ltd.scu.mall.util.PageQueryUtil;
import ltd.scu.mall.util.PageResult;
import org.apache.commons.collections4.MapUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.Date;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.concurrent.TimeUnit;

@Service
public class MallSeckillServiceImpl implements MallSeckillService {

    // 使用令牌桶RateLimiter 限流
    private static final RateLimiter RATE_LIMITER = RateLimiter.create(100);

    @Autowired
    private MallSeckillMapper mallSeckillMapper;

    @Autowired
    private MallSeckillSuccessMapper mallSeckillSuccessMapper;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private RedisCache redisCache;

    @Override
    public PageResult getSeckillPage(PageQueryUtil pageUtil) {
        List<MallSeckill> carousels = mallSeckillMapper.findSeckillList(pageUtil);
        int total = mallSeckillMapper.getTotalSeckills(pageUtil);
        return new PageResult(carousels, total, pageUtil.getLimit(), pageUtil.getPage());
    }

    @Override
    //看看该商品是否存在
    public boolean saveSeckill(MallSeckill mallSeckill) {
        if (goodsMapper.selectByPrimaryKey(mallSeckill.getGoodsId()) == null) {
            NewBeeMallException.fail(ServiceResultEnum.GOODS_NOT_EXIST.getResult());
        }
        return mallSeckillMapper.insertSelective(mallSeckill) > 0;
    }

    @Override
    public boolean updateSeckill(MallSeckill mallSeckill) {
        if (goodsMapper.selectByPrimaryKey(mallSeckill.getGoodsId()) == null) {
            NewBeeMallException.fail(ServiceResultEnum.GOODS_NOT_EXIST.getResult());
        }
        MallSeckill temp = mallSeckillMapper.selectByPrimaryKey(mallSeckill.getSeckillId());
        if (temp == null) {
            NewBeeMallException.fail(ServiceResultEnum.DATA_NOT_EXIST.getResult());
        }
        mallSeckill.setUpdateTime(new Date());
        return mallSeckillMapper.updateByPrimaryKeySelective(mallSeckill) > 0;
    }

    @Override
    public MallSeckill getSeckillById(Long id) {
        return mallSeckillMapper.selectByPrimaryKey(id);
    }

    @Override
    public boolean deleteSeckillById(Long id) {
        return mallSeckillMapper.deleteByPrimaryKey(id) > 0;
    }

    @Override
    public List<MallSeckill> getHomeSeckillPage() {
        return mallSeckillMapper.findHomeSeckillList();
    }

    @Override
    public ExposerVO exposerUrl(Long seckillId) {
        MallSeckillGoodsVO mallSeckillGoodsVO = redisCache.getCacheObject(Constants.SECKILL_GOODS_DETAIL + seckillId);
        Date startTime = mallSeckillGoodsVO.getSeckillBegin();
        Date endTime = mallSeckillGoodsVO.getSeckillEnd();
        // 系统当前时间
        Date nowTime = new Date();
        if (nowTime.getTime() < startTime.getTime() || nowTime.getTime() > endTime.getTime()) {
            return new ExposerVO(SeckillStatusEnum.NOT_START, seckillId, nowTime.getTime(), startTime.getTime(), endTime.getTime());
        }
        // 检查虚拟库存
        Integer stock = redisCache.getCacheObject(Constants.SECKILL_GOODS_STOCK_KEY + seckillId);
        if (stock == null || stock < 0) {
            return new ExposerVO(SeckillStatusEnum.STARTED_SHORTAGE_STOCK, seckillId);
        }
        // 加密
        String md5 = MD5Util.MD5Encode(seckillId.toString(), Constants.UTF_ENCODING);
        return new ExposerVO(SeckillStatusEnum.START, md5, seckillId);
    }

    @Override
    public SeckillSuccessVO executeSeckill(Long seckillId, Long userId) {
        // 判断能否在500毫秒内得到令牌，如果不能则立即返回false，不会阻塞程序
        if (!RATE_LIMITER.tryAcquire(500, TimeUnit.MILLISECONDS)) {
            throw new NewBeeMallException("秒杀失败");
        }
        // 判断用户是否购买过秒杀商品
        if (redisCache.containsCacheSet(Constants.SECKILL_SUCCESS_USER_ID + seckillId, userId)) {
            throw new NewBeeMallException("您已经购买过秒杀商品，请勿重复购买");
        }
        // 更新秒杀商品虚拟库存
        Long stock = redisCache.luaDecrement(Constants.SECKILL_GOODS_STOCK_KEY + seckillId);
        if (stock < 0) {
            throw new NewBeeMallException("秒杀商品已售空");
        }
        MallSeckill mallSeckill = redisCache.getCacheObject(Constants.SECKILL_KEY + seckillId);
        if (mallSeckill == null) {
            mallSeckill = mallSeckillMapper.selectByPrimaryKey(seckillId);
            redisCache.setCacheObject(Constants.SECKILL_KEY + seckillId, mallSeckill, 24, TimeUnit.HOURS);
        }
        // 判断秒杀商品是否再有效期内
        long beginTime = mallSeckill.getSeckillBegin().getTime();
        long endTime = mallSeckill.getSeckillEnd().getTime();
        Date now = new Date();
        long nowTime = now.getTime();
        if (nowTime < beginTime) {
            throw new NewBeeMallException("秒杀未开启");
        } else if (nowTime > endTime) {
            throw new NewBeeMallException("秒杀已结束");
        }

        Date killTime = new Date();
        Map<String, Object> map = new HashMap<>(8);
        map.put("seckillId", seckillId);
        map.put("userId", userId);
        map.put("killTime", killTime);
        map.put("result", null);
        // 执行存储过程，result被赋值
        try {
            mallSeckillMapper.killByProcedure(map);
        } catch (Exception e) {
            throw new NewBeeMallException(e.getMessage());
        }
        // 获取result -2sql执行失败 -1未插入数据 0未更新数据 1sql执行成功
        map.get("result");
        int result = MapUtils.getInteger(map, "result", -2);
        if (result != 1) {
            throw new NewBeeMallException("很遗憾！未抢购到秒杀商品");
        }
        // 记录购买过的用户
        redisCache.setCacheSet(Constants.SECKILL_SUCCESS_USER_ID + seckillId, userId);
        long endExpireTime = endTime / 1000;
        long nowExpireTime = nowTime / 1000;
        redisCache.expire(Constants.SECKILL_SUCCESS_USER_ID + seckillId, endExpireTime - nowExpireTime, TimeUnit.SECONDS);
        MallSeckillSuccess seckillSuccess = mallSeckillSuccessMapper.getSeckillSuccessByUserIdAndSeckillId(userId, seckillId);
        SeckillSuccessVO seckillSuccessVO = new SeckillSuccessVO();
        Long seckillSuccessId = seckillSuccess.getSecId();
        seckillSuccessVO.setSeckillSuccessId(seckillSuccessId);
        seckillSuccessVO.setMd5(MD5Util.MD5Encode(seckillSuccessId + Constants.SECKILL_ORDER_SALT, Constants.UTF_ENCODING));
        return seckillSuccessVO;
    }

    @Override
    public MallSeckill getSeckillByGoodsId(Long goodsId) {
        return mallSeckillMapper.selectByGoodsKey(goodsId);
    }

}
