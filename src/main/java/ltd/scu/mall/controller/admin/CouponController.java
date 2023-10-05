package ltd.scu.mall.controller.admin;

import jakarta.servlet.http.HttpServletRequest;
import ltd.scu.mall.entity.MallCoupon;
import ltd.scu.mall.service.MallCouponService;
import ltd.scu.mall.util.PageQueryUtil;
import ltd.scu.mall.util.Result;
import ltd.scu.mall.util.ResultGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.Date;
import java.util.Map;

@Controller
@RequestMapping("admin")
public class CouponController {

    @Autowired
    private MallCouponService mallCouponService;

    @GetMapping("/coupon")
    public String index(HttpServletRequest request) {
        request.setAttribute("path", "newbee_mall_coupon");
        return "admin/coupon";
    }

    @ResponseBody
    @GetMapping("/coupon/list")
    public Result list(@RequestParam Map<String, Object> params) {
        if (StringUtils.isEmpty((CharSequence) params.get("page")) || StringUtils.isEmpty((CharSequence) params.get("limit"))) {
            return ResultGenerator.genFailResult("参数异常！");
        }
        PageQueryUtil pageUtil = new PageQueryUtil(params);
        return ResultGenerator.genSuccessResult(mallCouponService.getCouponPage(pageUtil));
    }

    /**
     * 保存
     */
    @ResponseBody
    @PostMapping("/coupon/save")
    public Result save(@RequestBody MallCoupon mallCoupon) {
        return ResultGenerator.genDmlResult(mallCouponService.saveCoupon(mallCoupon));
    }

    /**
     * 更新
     */
    @PostMapping("/coupon/update")
    @ResponseBody
    public Result update(@RequestBody MallCoupon mallCoupon) {
        mallCoupon.setUpdateTime(new Date());
        return ResultGenerator.genDmlResult(mallCouponService.updateCoupon(mallCoupon));
    }

    /**
     * 详情
     */
    @GetMapping("/coupon/{id}")
    @ResponseBody
    public Result Info(@PathVariable("id") Long id) {
        MallCoupon mallCoupon = mallCouponService.getCouponById(id);
        return ResultGenerator.genSuccessResult(mallCoupon);
    }

    /**
     * 删除
     */
    @DeleteMapping("/coupon/{id}")
    @ResponseBody
    public Result delete(@PathVariable Long id) {
        return ResultGenerator.genDmlResult(mallCouponService.deleteCouponById(id));
    }
}
