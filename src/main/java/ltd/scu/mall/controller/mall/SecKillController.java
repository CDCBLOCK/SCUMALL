package ltd.scu.mall.controller.mall;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import ltd.scu.mall.common.Constants;
import ltd.scu.mall.entity.MallGoods;
import ltd.scu.mall.exception.NewBeeMallException;
import ltd.scu.mall.controller.vo.ExposerVO;
import ltd.scu.mall.controller.vo.MallSeckillGoodsVO;
import ltd.scu.mall.controller.vo.MallUserVO;
import ltd.scu.mall.controller.vo.SeckillSuccessVO;
import ltd.scu.mall.dao.GoodsMapper;
import ltd.scu.mall.entity.MallSeckill;
import ltd.scu.mall.redis.RedisCache;
import ltd.scu.mall.service.MallSeckillService;
import ltd.scu.mall.util.BeanUtil;
import ltd.scu.mall.util.MD5Util;
import ltd.scu.mall.util.Result;
import ltd.scu.mall.util.ResultGenerator;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.ResponseBody;

import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.List;
import java.util.Objects;
import java.util.concurrent.TimeUnit;
import java.util.stream.Collectors;

@Controller
public class SecKillController {

    @Autowired
    private MallSeckillService mallSeckillService;

    @Autowired
    private GoodsMapper goodsMapper;

    @Autowired
    private RedisCache redisCache;

    @GetMapping("/seckill")
    public String seckillIndex() {
        return "mall/seckill-list";
    }

    /**
     * 获取服务器时间
     *
     * @return result
     */
    @ResponseBody
    @GetMapping("/seckill/time/now")
    public Result getTimeNow() {
        return ResultGenerator.genSuccessResult(System.currentTimeMillis());
    }

    /**
     * 判断秒杀商品虚拟库存是否足够
     *
     * @param seckillId 秒杀ID
     * @return result
     */
    @ResponseBody
    @PostMapping("/seckill/{seckillId}/checkStock")
    public Result seckillCheckStock(@PathVariable Long seckillId) {
        Integer stock = redisCache.getCacheObject(Constants.SECKILL_GOODS_STOCK_KEY + seckillId);
        if (stock == null || stock < 0) {
            return ResultGenerator.genFailResult("秒杀商品库存不足");
        }
        // redis虚拟库存大于等于0时，可以执行秒杀
        return ResultGenerator.genSuccessResult();
    }

    /**
     * 获取秒杀链接
     *
     * @param seckillId 秒杀商品ID
     * @return result
     */
    @ResponseBody
    @PostMapping("/seckill/{seckillId}/exposer")
    public Result exposerUrl(@PathVariable Long seckillId) {
        ExposerVO exposerVO = mallSeckillService.exposerUrl(seckillId);
        return ResultGenerator.genSuccessResult(exposerVO);
    }

    /**
     * 使用限流注解进行接口限流操作
     *
     * @param seckillId 秒杀ID
     * @param userId    用户ID
     * @param md5       秒杀链接的MD5加密信息
     * @return result
     */
    @ResponseBody
    @PostMapping(value = "/seckillExecution/{seckillId}/{userId}/{md5}")
    public Result execute(@PathVariable Long seckillId,
                          @PathVariable Long userId,
                          @PathVariable String md5) {
        // 判断md5信息是否合法
        if (md5 == null || userId == null || !md5.equals(MD5Util.MD5Encode(seckillId.toString(), Constants.UTF_ENCODING))) {
            throw new NewBeeMallException("秒杀商品不存在");
        }
        SeckillSuccessVO seckillSuccessVO = mallSeckillService.executeSeckill(seckillId, userId);
        return ResultGenerator.genSuccessResult(seckillSuccessVO);
    }

    @GetMapping("/seckill/info/{seckillId}")
    public String seckillInfo(@PathVariable Long seckillId,
                              HttpServletRequest request,
                              HttpSession httpSession) {
        MallUserVO user = (MallUserVO) httpSession.getAttribute(Constants.MALL_USER_SESSION_KEY);
        if (user != null) {
            request.setAttribute("userId", user.getUserId());
        }
        request.setAttribute("seckillId", seckillId);
        return "mall/seckill-detail";
    }

    @GetMapping("/seckill/list")
    @ResponseBody
    public Result secondKillGoodsList() {
        // 直接返回配置的秒杀商品列表
        // 不返回商品id，每配置一条秒杀数据，就生成一个唯一的秒杀id和发起秒杀的事件id，根据秒杀id去访问详情页
        List<MallSeckillGoodsVO> mallSeckillGoodsVOS = redisCache.getCacheObject(Constants.SECKILL_GOODS_LIST);
        if (mallSeckillGoodsVOS == null) {
            List<MallSeckill> list = mallSeckillService.getHomeSeckillPage();
            SimpleDateFormat sdf = new SimpleDateFormat("MM-dd HH:mm");
            mallSeckillGoodsVOS = list.stream().map(MallSeckill -> {
                MallSeckillGoodsVO mallSeckillGoodsVO = new MallSeckillGoodsVO();
                BeanUtil.copyProperties(MallSeckill, mallSeckillGoodsVO);
                MallGoods mallGoods = goodsMapper.selectByPrimaryKey(MallSeckill.getGoodsId());
                if (mallGoods == null) {
                    return null;
                }
                mallSeckillGoodsVO.setGoodsName(mallGoods.getGoodsName());
                mallSeckillGoodsVO.setGoodsCoverImg(mallGoods.getGoodsCoverImg());
                mallSeckillGoodsVO.setSellingPrice(mallGoods.getSellingPrice());
                Date seckillBegin = mallSeckillGoodsVO.getSeckillBegin();
                Date seckillEnd = mallSeckillGoodsVO.getSeckillEnd();
                String formatBegin = sdf.format(seckillBegin);
                String formatEnd = sdf.format(seckillEnd);
                mallSeckillGoodsVO.setSeckillBeginTime(formatBegin);
                mallSeckillGoodsVO.setSeckillEndTime(formatEnd);
                return mallSeckillGoodsVO;
            }).filter(Objects::nonNull).collect(Collectors.toList());
            redisCache.setCacheObject(Constants.SECKILL_GOODS_LIST, mallSeckillGoodsVOS, 60 * 60 * 100, TimeUnit.SECONDS);
        }
        return ResultGenerator.genSuccessResult(mallSeckillGoodsVOS);
    }

    @GetMapping("/seckill/{seckillId}")
    @ResponseBody
    public Result seckillGoodsDetail(@PathVariable Long seckillId) {
        // 返回秒杀商品详情VO，如果秒杀时间未到，不允许访问详情页，也不允许返回数据，参数为秒杀id
        // 根据返回的数据解析出秒杀的事件id，发起秒杀
        // 不访问详情页不会获取到秒杀的事件id，不然容易被猜到url路径从而直接发起秒杀请求
        MallSeckillGoodsVO mallSeckillGoodsVO = redisCache.getCacheObject(Constants.SECKILL_GOODS_DETAIL + seckillId);
        if (mallSeckillGoodsVO == null) {
            MallSeckill mallSeckill = mallSeckillService.getSeckillById(seckillId);
            if (!mallSeckill.getSeckillStatus()) {
                return ResultGenerator.genFailResult("秒杀商品已下架");
            }
            mallSeckillGoodsVO = new MallSeckillGoodsVO();
            BeanUtil.copyProperties(mallSeckill, mallSeckillGoodsVO);
            MallGoods mallGoods = goodsMapper.selectByPrimaryKey(mallSeckill.getGoodsId());
            mallSeckillGoodsVO.setGoodsName(mallGoods.getGoodsName());
            mallSeckillGoodsVO.setGoodsIntro(mallGoods.getGoodsIntro());
            mallSeckillGoodsVO.setGoodsDetailContent(mallGoods.getGoodsDetailContent());
            mallSeckillGoodsVO.setGoodsCoverImg(mallGoods.getGoodsCoverImg());
            mallSeckillGoodsVO.setSellingPrice(mallGoods.getSellingPrice());
            Date seckillBegin = mallSeckillGoodsVO.getSeckillBegin();
            Date seckillEnd = mallSeckillGoodsVO.getSeckillEnd();
            mallSeckillGoodsVO.setStartDate(seckillBegin.getTime());
            mallSeckillGoodsVO.setEndDate(seckillEnd.getTime());
            redisCache.setCacheObject(Constants.SECKILL_GOODS_DETAIL + seckillId, mallSeckillGoodsVO);
        }
        return ResultGenerator.genSuccessResult(mallSeckillGoodsVO);
    }

}
