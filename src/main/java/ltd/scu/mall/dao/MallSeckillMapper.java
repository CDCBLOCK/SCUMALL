package ltd.scu.mall.dao;

import ltd.scu.mall.entity.MallSeckill;
import ltd.scu.mall.util.PageQueryUtil;

import java.util.List;
import java.util.Map;

public interface MallSeckillMapper {
    int deleteByPrimaryKey(Long seckillId);

    int insert(MallSeckill record);

    int insertSelective(MallSeckill record);

    MallSeckill selectByPrimaryKey(Long seckillId);

    int updateByPrimaryKeySelective(MallSeckill record);

    int updateByPrimaryKey(MallSeckill record);

    List<MallSeckill> findSeckillList(PageQueryUtil pageUtil);

    int getTotalSeckills(PageQueryUtil pageUtil);

    List<MallSeckill> findHomeSeckillList();

    int getHomeTotalSeckills(PageQueryUtil pageUtil);

    void killByProcedure(Map<String, Object> map);

    boolean addStock(Long seckillId);

    MallSeckill selectByGoodsKey(Long goodsId);
}
