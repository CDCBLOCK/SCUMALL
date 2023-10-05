package ltd.scu.mall.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import ltd.scu.mall.common.Constants;
import ltd.scu.mall.entity.MallSeckill;
import ltd.scu.mall.redis.RedisCache;
import ltd.scu.mall.service.MallSeckillService;
import ltd.scu.mall.util.PageQueryUtil;
import ltd.scu.mall.util.Result;
import ltd.scu.mall.util.ResultGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Map;

@Controller
@RequestMapping("admin")
public class MallSeckillController {

    @Autowired
    private MallSeckillService mallSeckillService;
    @Autowired
    private RedisCache redisCache;

    @GetMapping("/seckill")
    public String index(HttpServletRequest request) {
        request.setAttribute("path", "newbee_mall_seckill");
        return "admin/seckill";
    }

    @ResponseBody
    @GetMapping("/seckill/list")
    public Result list(@RequestParam Map<String, Object> params) {
        if (StringUtils.isEmpty((CharSequence) params.get("page")) || StringUtils.isEmpty((CharSequence) params.get("limit"))) {
            return ResultGenerator.genFailResult("参数异常！");
        }
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        return ResultGenerator.genSuccessResult(mallSeckillService.getSeckillPage(pageUtil));
    }

    /**
     * 保存
     */
    @ResponseBody
    @PostMapping("/seckill/save")
    public Result save(@RequestBody MallSeckill mallSeckill) {
        if (mallSeckill == null || mallSeckill.getGoodsId() < 1 || mallSeckill.getSeckillNum() < 1 || mallSeckill.getSeckillPrice() < 1) {
            return ResultGenerator.genFailResult("参数异常");
        }
        boolean result = mallSeckillService.saveSeckill(mallSeckill);
        MallSeckill generatedSeckill = mallSeckillService.getSeckillByGoodsId(mallSeckill.getGoodsId());
        Long seckillId = generatedSeckill.getSeckillId();
        System.out.println("自动生成的seckillId：" + seckillId);

        // 虚拟库存预热
        redisCache.setCacheObject(Constants.SECKILL_GOODS_STOCK_KEY + seckillId, mallSeckill.getSeckillNum());
        return ResultGenerator.genDmlResult(result);
    }

    /**
     * 更新
     */
    @PostMapping("/seckill/update")
    @ResponseBody
    public Result update(@RequestBody MallSeckill mallSeckill) {
        if (mallSeckill == null || mallSeckill.getSeckillId() == null || mallSeckill.getGoodsId() < 1 || mallSeckill.getSeckillNum() < 1 || mallSeckill.getSeckillPrice() < 1) {
            return ResultGenerator.genFailResult("参数异常");
        }
        boolean result = mallSeckillService.updateSeckill(mallSeckill);
        if (result) {
            // 虚拟库存预热
            redisCache.setCacheObject(Constants.SECKILL_GOODS_STOCK_KEY + mallSeckill.getSeckillId(), mallSeckill.getSeckillNum());
            redisCache.deleteObject(Constants.SECKILL_GOODS_DETAIL + mallSeckill.getSeckillId());
            redisCache.deleteObject(Constants.SECKILL_GOODS_LIST);
        }
        return ResultGenerator.genDmlResult(result);
    }

    /**
     * 详情
     */
    @GetMapping("/seckill/{id}")
    @ResponseBody
    public Result Info(@PathVariable("id") Long id) {
        MallSeckill mallSeckill = mallSeckillService.getSeckillById(id);
        return ResultGenerator.genSuccessResult(mallSeckill);
    }

    /**
     * 删除
     */
    @DeleteMapping("/seckill/{id}")
    @ResponseBody
    public Result delete(@PathVariable Long id) {
        redisCache.deleteObject(Constants.SECKILL_GOODS_DETAIL + id);
        redisCache.deleteObject(Constants.SECKILL_GOODS_LIST);
        return ResultGenerator.genDmlResult(mallSeckillService.deleteSeckillById(id));
    }
}
