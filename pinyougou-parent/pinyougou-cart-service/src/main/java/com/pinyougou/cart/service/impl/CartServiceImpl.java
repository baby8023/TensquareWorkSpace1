package com.pinyougou.cart.service.impl;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.redis.core.RedisTemplate;

import com.alibaba.dubbo.config.annotation.Service;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.mapper.TbItemMapper;
import com.pinyougou.pojo.TbItem;
import com.pinyougou.pojo.TbOrderItem;
import com.pinyougou.pojogroup.Cart;
@Service
public class CartServiceImpl implements CartService {
	
	@Autowired
	private TbItemMapper itemMapper;

	@Override
	public List<Cart> addGoodsToCartList(List<Cart> cartList, Long itemId, Integer num) {
		
		//1.根据SKU ID 查询商品信息
		TbItem item = itemMapper.selectByPrimaryKey(itemId);
		if(item==null){
			throw new RuntimeException("该商品不存在");
		}
		if(!item.getStatus().equals("1")){
			throw new RuntimeException("该商品状态无效");
		}
		
		//2.根据SKU商品对象得到商家ID
		
		String sellerId = item.getSellerId();
		
		//3.根据商家ID，查询购物车列表 得到购物车对象
		
		Cart cart = searchCartBySellerId(cartList,sellerId);
		
		if(cart==null){//4.如果购物车对象不存在 （购物车列表中没有该商家）
				
			
			//4.1 创建新的购物车对象			
			cart=new Cart();
			cart.setSellerId(sellerId);
			cart.setSellerName(item.getSeller());//商家名称
			
			System.out.println("商家名称："+cart.getSellerName());
			
			//4.2 创建新的购物车明细对象,并且添加到购物车对象中的购物车明细列表中
			TbOrderItem orderItem = createOrderItem(item,num);
			List orderItemList=new ArrayList();			
			orderItemList.add(orderItem);
			cart.setOrderItemList(orderItemList);
			//4.3 将新的购物车对象添加到购物车列表			
			cartList.add(cart);
			
		}else{//5.如果购物车对象存在 
			
			// 判断购物车明细列表中是否存在该商品
			TbOrderItem orderItem = searchOrderItemByItemId(cart.getOrderItemList(),itemId);
			if(orderItem==null){//5.1 如果该商品不存在 
			
				// 创建新的购物车明细对象
				orderItem = createOrderItem(item,num);
				// 添加到购物车明细列表中
				cart.getOrderItemList().add(orderItem);				
				
			}else{//5.2 如果该商品存在
				// 修改购物车明细对象的数量和金额
				orderItem.setNum( orderItem.getNum()+ num  );
				orderItem.setTotalFee(  new BigDecimal(orderItem.getPrice().doubleValue()* orderItem.getNum()) );
				
				//如果操作后的数量小于等于0，将此记录移除
				if(orderItem.getNum()<=0){
					cart.getOrderItemList().remove(orderItem);			
				}
				//如果购物车明细列表中无记录
				if(cart.getOrderItemList().size()==0){
					cartList.remove(cart);
				}				
			}			
		}
		
		return cartList;
	}
	
	/**
	 * 根据商家ID查询购物车对象
	 * @param cartList
	 * @param sellerId
	 * @return
	 */
	private Cart searchCartBySellerId(List<Cart> cartList,String sellerId ){
		for(Cart cart:cartList){
			if(cart.getSellerId().equals(sellerId)){
				return cart;
			}			
		}
		return null;		
	}
	
	/**
	 * 根据SKU ID查询购物车明细对象
	 * @param orderItemList
	 * @param itemId
	 * @return
	 */
	private TbOrderItem searchOrderItemByItemId(List<TbOrderItem> orderItemList,Long itemId){
		
		for(TbOrderItem orderItem :orderItemList){
			if(orderItem.getItemId().longValue()==itemId.longValue()){
				return orderItem;
			}
		}
		return null;
	}
	
	/**
	 * 创建购物车明细对象
	 * @param item
	 * @param num
	 * @return
	 */
	private TbOrderItem createOrderItem(TbItem item,Integer num){
		
		if(num<0){
			throw new RuntimeException("数量非法");
		}
		
		TbOrderItem orderItem=new TbOrderItem();
		orderItem.setGoodsId(item.getGoodsId());//SPU ID
		orderItem.setItemId(item.getId());// SKU ID
		orderItem.setNum(num);
		orderItem.setPicPath(item.getImage());//商品图片
		orderItem.setPrice(item.getPrice());//商品价格
		orderItem.setSellerId(item.getSellerId());//商家ID
		orderItem.setTitle(item.getTitle());//商品标题
		orderItem.setTotalFee( new BigDecimal(item.getPrice().doubleValue()*num) );//商品金额
		
		return orderItem;		
	}

	@Autowired
	private RedisTemplate redisTemplate;
	
	@Override
	public List<Cart> findCartListFromRedis(String username) {
		System.out.println("从redis获取购物车"+username);
		List<Cart> cartList = (List<Cart>) redisTemplate.boundHashOps("cartList").get(username);
		if(cartList==null){
			cartList=new ArrayList(); 
		}		
		return cartList;
	}

	@Override
	public void saveCartListToRedis(String username, List<Cart> cartList) {
		System.out.println("向redis存储购物车"+username);
		redisTemplate.boundHashOps("cartList").put(username, cartList);
		
	}

	@Override
	public List<Cart> mergeCartList(List<Cart> cartList1, List<Cart> cartList2) {
		//List<Cart> cartList=new ArrayList();
		
		//for(Cart cart:cartList1){			
		//	for(TbOrderItem orderItem :cart.getOrderItemList() ){
		//		cartList= addGoodsToCartList(cartList,orderItem.getItemId(),orderItem.getNum());				
		//	}			
		//}
		
		for(Cart cart:cartList2){			
			for(TbOrderItem orderItem :cart.getOrderItemList() ){
				cartList1= addGoodsToCartList(cartList1,orderItem.getItemId(),orderItem.getNum());				
			}			
		}
		
		return cartList1;
	}

}
