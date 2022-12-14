package com.atguigu.gmall.order.service.impl;

import com.atguigu.gmall.common.cart.GetUserCartUtils;
import com.atguigu.gmall.common.constant.SysRedisConstant;
import com.atguigu.gmall.common.util.Jsons;
import com.atguigu.gmall.model.enums.OrderStatus;
import com.atguigu.gmall.model.enums.ProcessStatus;
import com.atguigu.gmall.model.order.OrderDetail;
import com.atguigu.gmall.model.order.OrderInfo;
import com.atguigu.gmall.model.to.mq.OrderMsg;
import com.atguigu.gmall.model.vo.order.OrderSubmitVo;
import com.atguigu.gmall.order.mapper.OrderInfoMapper;
import com.atguigu.gmall.order.service.OrderDetailService;
import com.atguigu.gmall.order.service.OrderInfoService;
import com.atguigu.gmall.rabbit.constant.RabbitConstant;
import com.baomidou.mybatisplus.extension.service.impl.ServiceImpl;
import org.springframework.amqp.rabbit.core.RabbitTemplate;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import javax.annotation.Resource;
import java.math.BigDecimal;
import java.util.Date;
import java.util.List;
import java.util.stream.Collectors;

/**
* @author lfy
* @description 针对表【order_info(订单表 订单表)】的数据库操作Service实现
* @createDate 2022-09-09 14:41:24
*/
@Service
public class OrderInfoServiceImpl extends ServiceImpl<OrderInfoMapper, OrderInfo>
    implements OrderInfoService{

    @Resource
    OrderInfoMapper orderInfoMapper;


    @Resource
    OrderDetailService orderDetailService;

    @Autowired
    RabbitTemplate rabbitTemplate;


    @Transactional
    @Override
    public Long saveOrder(OrderSubmitVo submitVo, String tradeNo) {
        //1、准备订单数据
        OrderInfo orderInfo = prepareOrderInfo(submitVo,tradeNo);
        //2、保存-OrderInfo
        orderInfoMapper.insert(orderInfo);

        //2、保存-OrderDetail
        //订单明细。指这个订单到底买了那些商品？
        //所有订单明细
        List<OrderDetail> details = prepareOrderDetail(submitVo,orderInfo);
        orderDetailService.saveBatch(details);

        //将订单数据存到mq中，超过时间还未支付订单，自动关闭订单
        //存到延时队列中
        OrderMsg orderMsg = new OrderMsg(orderInfo.getId(), orderInfo.getUserId());

        //存入到消息队列中
        rabbitTemplate.convertAndSend(
                RabbitConstant.EXCHANGE_ORDER,
                RabbitConstant.RK_ORDER_CREATED,
                Jsons.toStr(orderMsg)
        );
        //3、返回订单id
        return orderInfo.getId();
    }

    @Override
    public void changeOrderStatus(Long userId, Long orderId, ProcessStatus closed, List<ProcessStatus> want) {

        String orderStatus = closed.getOrderStatus().name();
        String processStatus = closed.name();

        List<String> expects = want.stream()
                .map(status -> status.name())
                .collect(Collectors.toList());

        //幂等修改订单
        orderInfoMapper.updateOrderStatus(orderId,userId,processStatus,orderStatus,expects);
    }

    private List<OrderDetail> prepareOrderDetail(OrderSubmitVo submitVo, OrderInfo orderInfo) {
        List<OrderDetail> detailList = submitVo.getOrderDetailList().stream()
                .map(vo -> {
                    OrderDetail detail = new OrderDetail();
                    //订单id
                    detail.setOrderId(orderInfo.getId());
                    //skuId
                    detail.setSkuId(vo.getSkuId());
                    //用户id
                    detail.setUserId(orderInfo.getUserId());

                    detail.setSkuName(vo.getSkuName());
                    detail.setImgUrl(vo.getImgUrl());
                    detail.setOrderPrice(vo.getOrderPrice());
                    detail.setSkuNum(vo.getSkuNum());
                    detail.setHasStock(vo.getHasStock());
                    detail.setCreateTime(new Date());
                    detail.setSplitTotalAmount(vo.getOrderPrice().multiply(new BigDecimal(vo.getSkuNum() + "")));
                    detail.setSplitActivityAmount(new BigDecimal("0"));
                    detail.setSplitCouponAmount(new BigDecimal("0"));
                    return detail;
                }).collect(Collectors.toList());
        return detailList;
    }

    //将submitvo转换成为orderInfo对象
    private OrderInfo prepareOrderInfo(OrderSubmitVo submitVo,String tradeNo) {
        OrderInfo info = new OrderInfo();
        //收货人
        info.setConsignee(submitVo.getConsignee());
        info.setConsigneeTel(submitVo.getConsigneeTel());



        //用户id
        Long userId = GetUserCartUtils.getCartUser().getUserId();
        info.setUserId(userId);

        //支付方式
        info.setPaymentWay(submitVo.getPaymentWay());
        //配送地址
        info.setDeliveryAddress(submitVo.getDeliveryAddress());
        //订单备注
        info.setOrderComment(submitVo.getOrderComment());

        //对外交易号
        info.setOutTradeNo(tradeNo);

        //交易体。 拿到这个订单中购买的第一个商品的名字，作为订单的体
        info.setTradeBody(submitVo.getOrderDetailList().get(0).getSkuName());
        //创建时间
        info.setCreateTime(new Date());

        //过期时间。订单多久没支付以后就过期。过期未支付：订单就会成为已关闭状态；
        //new Date() + 45min = x
        info.setExpireTime(new Date(System.currentTimeMillis() + 1000 * SysRedisConstant.ORDER_CLOSE_TTL));

        //订单的处理状态
        info.setProcessStatus(ProcessStatus.UNPAID.name());
        //订单状态。默认未支付
        info.setOrderStatus(OrderStatus.UNPAID.name());


        //物流单编号，发货后才有
        info.setTrackingNo("");

        //父订单id（拆单场景）
        info.setParentOrderId(0L);

        //订单的图片
        info.setImgUrl(submitVo.getOrderDetailList().get(0).getImgUrl());


        //当前单被优惠活动减掉的金额
        info.setActivityReduceAmount(new BigDecimal("0"));
        //当前单被优惠券减掉的金额
        info.setCouponAmount(new BigDecimal("0"));

        //计算总价
        BigDecimal totalAmount = submitVo.getOrderDetailList()
                .stream()
                .map(o -> o.getOrderPrice()
                        .multiply(new BigDecimal(o.getSkuNum() + "")))
                .reduce((o1, o2) -> o1.add(o2))
                .get();
        //订单原始总额
        info.setOriginalTotalAmount(totalAmount);

        //订单总额 = OriginalTotalAmount - ActivityReduceAmount - CouponAmount
        info.setTotalAmount(totalAmount);

        //可退款的最后时间
        info.setRefundableTime(new Date(System.currentTimeMillis() + SysRedisConstant.ORDER_REFUND_TTL * 1000));

        //运费。第三方物流平台，动态计算运费
        info.setFeightFee(new BigDecimal("0"));

        info.setOperateTime(new Date());


        return info;
    }
}




