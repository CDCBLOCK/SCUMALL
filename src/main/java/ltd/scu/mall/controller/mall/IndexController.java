/**
 * 严肃声明：
 * 开源版本请务必保留此注释头信息，若删除我方将保留所有法律责任追究！
 * 本系统已申请软件著作权，受国家版权局知识产权以及国家计算机软件著作权保护！
 * 可正常分享和学习源码，不得用于违法犯罪活动，违者必究！
 * Copyright (c) 2019-2020 十三 all rights reserved.
 * 版权所有，侵权必究！
 */
package ltd.scu.mall.controller.mall;

import jakarta.annotation.Resource;
import jakarta.servlet.http.HttpServletRequest;
import ltd.scu.mall.common.Constants;
import ltd.scu.mall.common.IndexConfigTypeEnum;
import ltd.scu.mall.exception.NewBeeMallException;
import ltd.scu.mall.controller.vo.MallIndexCarouselVO;
import ltd.scu.mall.controller.vo.MallIndexCategoryVO;
import ltd.scu.mall.controller.vo.MallIndexConfigGoodsVO;
import ltd.scu.mall.service.MallCarouselService;
import ltd.scu.mall.service.MallCategoryService;
import ltd.scu.mall.service.MallIndexConfigService;
import org.springframework.stereotype.Controller;
import org.springframework.util.CollectionUtils;
import org.springframework.web.bind.annotation.GetMapping;

import java.util.List;

@Controller
public class IndexController {

    @Resource
    private MallCarouselService mallCarouselService;

    @Resource
    private MallIndexConfigService mallIndexConfigService;

    @Resource
    private MallCategoryService mallCategoryService;

    @GetMapping({"/index", "/", "/index.html"})
    public String indexPage(HttpServletRequest request) {
        List<MallIndexCategoryVO> categories = mallCategoryService.getCategoriesForIndex();
        if (CollectionUtils.isEmpty(categories)) {
            NewBeeMallException.fail("分类数据不完善");
        }
        List<MallIndexCarouselVO> carousels = mallCarouselService.getCarouselsForIndex(Constants.INDEX_CAROUSEL_NUMBER);
        List<MallIndexConfigGoodsVO> hotGoodses = mallIndexConfigService.getConfigGoodsesForIndex(IndexConfigTypeEnum.INDEX_GOODS_HOT.getType(), Constants.INDEX_GOODS_HOT_NUMBER);
        List<MallIndexConfigGoodsVO> newGoodses = mallIndexConfigService.getConfigGoodsesForIndex(IndexConfigTypeEnum.INDEX_GOODS_NEW.getType(), Constants.INDEX_GOODS_NEW_NUMBER);
        List<MallIndexConfigGoodsVO> recommendGoodses = mallIndexConfigService.getConfigGoodsesForIndex(IndexConfigTypeEnum.INDEX_GOODS_RECOMMOND.getType(), Constants.INDEX_GOODS_RECOMMOND_NUMBER);
        // 分类数据
        request.setAttribute("categories", categories);
        // 轮播图
        request.setAttribute("carousels", carousels);
        // 热销商品
        request.setAttribute("hotGoodses", hotGoodses);
        // 新品
        request.setAttribute("newGoodses", newGoodses);
        // 推荐商品
        request.setAttribute("recommendGoodses", recommendGoodses);
        return "mall/index";
    }
}
