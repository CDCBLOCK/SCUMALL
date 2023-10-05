package ltd.scu.mall.task;

import ltd.scu.mall.dao.MallOrderMapper;
import ltd.scu.mall.entity.MallOrder;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.ApplicationArguments;
import org.springframework.boot.ApplicationRunner;
import org.springframework.stereotype.Component;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.temporal.ChronoUnit;
import java.util.Date;
import java.util.List;

/**
 * 定时任务脚本，系统启动时检查是否有超时订单
 */
@Component
public class TaskStartupRunner implements ApplicationRunner {

    public static final Long UN_PAID_ORDER_EXPIRE_TIME = 30L;
    @Autowired
    private MallOrderMapper mallOrderMapper;
    @Autowired
    private TaskService taskService;

    //用一个子线程区记录 订单延时时间
    @Override
    public void run(ApplicationArguments args) throws Exception {
        List<MallOrder> mallOrders = mallOrderMapper.selectPrePayOrders();
        for (MallOrder order : mallOrders) {
            Date date = order.getCreateTime();
            Instant instant = date.toInstant();
            ZoneId zoneId = ZoneId.systemDefault();

            LocalDateTime add = instant.atZone(zoneId).toLocalDateTime();
            LocalDateTime now = LocalDateTime.now();
            LocalDateTime expire = add.plusMinutes(UN_PAID_ORDER_EXPIRE_TIME);
            if (expire.isBefore(now)) {
                // 已经过期，则加入延迟队列立即执行
                taskService.addTask(new OrderUnPaidTask(order.getOrderId(), 0));
            } else {
                // 还没过期，则加入延迟队列
                long delay = ChronoUnit.MILLIS.between(now, expire);
                taskService.addTask(new OrderUnPaidTask(order.getOrderId(), delay));
            }
        }
    }
}
