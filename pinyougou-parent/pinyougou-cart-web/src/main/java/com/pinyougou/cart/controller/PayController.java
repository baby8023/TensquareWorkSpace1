package com.pinyougou.cart.controller;

import java.util.Map;

import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.pinyougou.order.service.OrderService;
import com.pinyougou.pay.service.WeixinPayService;
import com.pinyougou.pojo.TbPayLog;

import entity.Result;
import util.IdWorker;

@RestController
@RequestMapping("/pay")
public class PayController {
	
	@Reference
	private WeixinPayService weixinPayService;
	
	@Reference
	private OrderService orderService;
	
	/**
	 * 请求统一下单接口
	 * @return
	 */
	@RequestMapping("/createNative")
	public Map createNative(){
		
		String userId=SecurityContextHolder.getContext().getAuthentication().getName();
		TbPayLog payLog = orderService.searchPayLogFromRedis(userId);
				
		return weixinPayService.createNative(payLog.getOutTradeNo(), payLog.getTotalFee()+"");		
	}
	
	
	@RequestMapping("/queryOrderStatus")
	public Result queryOrderStatus(String out_trade_no){
		Result result=null;		
		int x=0;
		while(true){
			
			Map map = weixinPayService.queryOrderStatus(out_trade_no);
			if(map==null){
				result=new Result(false, "查询发生错误！");
				break;
			}
			if("SUCCESS".equals(map.get("trade_state"))  ){//判断交易状态
				result=new Result(true, "支付成功");	
				//修改订单状态
				orderService.updateOrderStatus(out_trade_no,(String)map.get("transaction_id") );
				break;
			}
			System.out.println("调用查询："+map);
			
			
			try {
				Thread.sleep(3000);//间隔三秒
			} catch (InterruptedException e) {
				// TODO Auto-generated catch block
				e.printStackTrace();
			}
			x++;
			
			if(x>=100){
				result=new Result(false, "二维码超时");		
				break;
			}		
			
		}
		
		return result;
	}

}
