package ltd.scu.mall.dao;

import ltd.scu.mall.entity.MallSeckillSuccess;

public interface MallSeckillSuccessMapper {
    int deleteByPrimaryKey(Integer secId);

    int insert(MallSeckillSuccess record);

    int insertSelective(MallSeckillSuccess record);

    MallSeckillSuccess selectByPrimaryKey(Long secId);

    int updateByPrimaryKeySelective(MallSeckillSuccess record);

    int updateByPrimaryKey(MallSeckillSuccess record);

    MallSeckillSuccess getSeckillSuccessByUserIdAndSeckillId(Long userId, Long seckillId);
}
