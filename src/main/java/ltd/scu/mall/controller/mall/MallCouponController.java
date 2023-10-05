package ltd.scu.mall.controller.mall;

import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpSession;
import ltd.scu.mall.common.Constants;
import ltd.scu.mall.controller.vo.MallCouponVO;
import ltd.scu.mall.controller.vo.MallUserVO;
import ltd.scu.mall.service.MallCouponService;
import ltd.scu.mall.util.PageQueryUtil;
import ltd.scu.mall.util.PageResult;
import ltd.scu.mall.util.Result;
import ltd.scu.mall.util.ResultGenerator;
import org.apache.commons.lang3.StringUtils;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;

@Controller
public class MallCouponController {

    @Autowired
    private MallCouponService mallCouponService;

    @GetMapping("/couponList")
    public String couponList(HttpServletRequest request, HttpSession session) {
        Long userId = null;
        if (session.getAttribute(Constants.MALL_USER_SESSION_KEY) != null) {
            userId = ((MallUserVO) request.getSession().getAttribute(Constants.MALL_USER_SESSION_KEY)).getUserId();
        }
        List<MallCouponVO> coupons = mallCouponService.selectAvailableCoupon(userId);
        request.setAttribute("coupons", coupons);
        return "mall/coupon-list";
    }

    @GetMapping("/myCoupons")
    public String myCoupons(@RequestParam Map<String, Object> params, HttpServletRequest request, HttpSession session) {
        MallUserVO user = (MallUserVO) session.getAttribute(Constants.MALL_USER_SESSION_KEY);
        params.put("userId", user.getUserId());
        int status = Integer.parseInt((String) params.getOrDefault("status", "0"));
        params.put("status", status);
        if (StringUtils.isEmpty((CharSequence) params.get("page"))) {
            params.put("page", 1);
        }
        params.put("limit", Constants.MY_COUPONS_LIMIT);
        //封装我的订单数据
        PageQueryUtil pageUtil = new PageQueryUtil(params);

        PageResult<MallCouponVO> pageResult = mallCouponService.selectMyCoupons(pageUtil);
        request.setAttribute("pageResult", pageResult);
        request.setAttribute("path", "myCoupons");
        request.setAttribute("status", status);
        return "mall/my-coupons";
    }

    @ResponseBody
    @PostMapping("coupon/{couponId}")
    public Result save(@PathVariable Long couponId, HttpSession session) {
        MallUserVO userVO = (MallUserVO) session.getAttribute(Constants.MALL_USER_SESSION_KEY);
        return ResultGenerator.genDmlResult(mallCouponService.saveCouponUser(couponId, userVO.getUserId()));
    }

    @ResponseBody
    @DeleteMapping("coupon/{couponUserId}")
    public Result delete(@PathVariable Long couponUserId) {
        return ResultGenerator.genDmlResult(mallCouponService.deleteCouponUser(couponUserId));
    }
}
