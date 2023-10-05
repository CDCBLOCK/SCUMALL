package ltd.scu.mall.service;

import ltd.scu.mall.controller.vo.ExposerVO;
import ltd.scu.mall.controller.vo.SeckillSuccessVO;
import ltd.scu.mall.entity.MallSeckill;
import ltd.scu.mall.util.PageQueryUtil;
import ltd.scu.mall.util.PageResult;

import java.util.List;

public interface MallSeckillService {

    PageResult getSeckillPage(PageQueryUtil pageUtil);

    boolean saveSeckill(MallSeckill mallSeckill);

    boolean updateSeckill(MallSeckill mallSeckill);

    MallSeckill getSeckillById(Long id);

    boolean deleteSeckillById(Long id);

    List<MallSeckill> getHomeSeckillPage();

    ExposerVO exposerUrl(Long seckillId);

    SeckillSuccessVO executeSeckill(Long seckillId, Long userId);

    MallSeckill getSeckillByGoodsId(Long goodsId);
}
