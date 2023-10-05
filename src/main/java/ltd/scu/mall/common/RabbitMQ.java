package ltd.scu.mall.common;


import ltd.scu.mall.controller.vo.MallOrderDetailVO;
import ltd.scu.mall.entity.MallOrder;
import ltd.scu.mall.service.MallOrderService;
import org.springframework.amqp.core.MessageListener;
import org.springframework.amqp.rabbit.annotation.RabbitListener;
import org.springframework.amqp.rabbit.listener.SimpleMessageListenerContainer;
import org.springframework.amqp.rabbit.listener.api.ChannelAwareMessageListener;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Component;


import org.springframework.amqp.core.Message;
import org.springframework.amqp.core.MessageListener;
import org.springframework.stereotype.Component;

//自动消费消息用rabbitmqlistener；
//手动则是使用 caml这个接口

@Component
@RabbitListener(queues = "delayed_queue")
public class RabbitMQ{

    @Autowired
    private MallOrderService mallOrderService;

    public MallOrder Onmessage( MallOrderDetailVO orderDetailVO){
        String orderNo = orderDetailVO.getOrderNo();
        MallOrder orderIdByOrderNO = mallOrderService.getOrderIdByOrderNO(orderNo);

        return orderIdByOrderNO;
    }
}



