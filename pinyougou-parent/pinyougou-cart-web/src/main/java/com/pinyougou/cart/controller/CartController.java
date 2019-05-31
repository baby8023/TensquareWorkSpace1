package com.pinyougou.cart.controller;

import java.util.List;

import javax.servlet.http.HttpServletRequest;
import javax.servlet.http.HttpServletResponse;

import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.web.bind.annotation.CrossOrigin;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.alibaba.dubbo.config.annotation.Reference;
import com.alibaba.fastjson.JSON;
import com.pinyougou.cart.service.CartService;
import com.pinyougou.pojogroup.Cart;

import entity.Result;

@RestController
@RequestMapping("/cart")
public class CartController {

	@Autowired
	private HttpServletRequest request;
	
	@Autowired
	private HttpServletResponse response;
	
	@Reference(timeout=6000)
	private CartService cartService;
	
	/**
	 * 获取购物车列表
	 * @return
	 */
	@RequestMapping("/findCartList")
	public List<Cart> findCartList(){
		//获取当前登陆人账号
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		System.out.println("当前登录人账号"+username);
		
		//1.从cookie中提取购物车列表	(JSON字符串)			
		String cartListString = util.CookieUtil.getCookieValue(request, "cartList","UTF-8");
		if(cartListString==null || cartListString.equals("")){
			cartListString="[]";
		}		
		//2.转换为List
		List<Cart> cartList_cookie = JSON.parseArray(cartListString, Cart.class);	
		
		if(username.equals("anonymousUser")){//如果未登录
			
			System.out.println("从cookie提取购物车");
			return cartList_cookie;		
			
		}else{//如果已登录
			//从redis中提取购物车
			List<Cart> cartList_redis = cartService.findCartListFromRedis(username);
			if(cartList_cookie.size()>0){
				System.out.println("合并购物车数据");
				//购物车合并
				cartList_redis=cartService.mergeCartList(cartList_redis, cartList_cookie);
				//将合并后的购物车存入redis
				cartService.saveCartListToRedis(username, cartList_redis);
				//清除cookie中的购物车数据
				util.CookieUtil.deleteCookie(request, response, "cartList");
			}
			return cartList_redis;
		}
		
		
		
	}
	
	/**
	 * 添加商品到购物车
	 * @param itemId
	 * @param num
	 * @return
	 */
	@CrossOrigin(origins="http://localhost:9109")//allowCredentials="true"  可以缺省
	@RequestMapping("/addGoodsToCartList")
	public Result addGoodsToCartList(Long itemId,Integer num){
		
		//设置可访问的来源地址(必须)
		//response.setHeader("Access-Control-Allow-Origin", "http://localhost:9109");
		
		//跨域cookie  如果当前方法涉及cookie  Access-Control-Allow-Origin 头信息不可以为*
		//response.setHeader("Access-Control-Allow-Credentials", "true");
		
		//获取当前登陆人账号
		String username = SecurityContextHolder.getContext().getAuthentication().getName();
		System.out.println("当前登录人账号"+username);
				
				
		
		try {
			//1.从cookie中获取购物车列表
			List<Cart> cartList = findCartList();		
			//2.将商品添加到购物车
			cartList = cartService.addGoodsToCartList(cartList, itemId, num);	
			
			if(username.equals("anonymousUser")){//如果未登录
				System.out.println("将购物车存入cookie");
				//3.将购物车存入cookie		
				util.CookieUtil.setCookie(request, response, "cartList", JSON.toJSONString(cartList),3600*24,"UTF-8");
				
			}else{
				
				cartService.saveCartListToRedis(username, cartList);
			}
			
			
			return new Result(true, "添加购物车成功");
			
		}catch (RuntimeException e) {
			e.printStackTrace();
			return new Result(false, e.getMessage());
		}catch (Exception e) {
			e.printStackTrace();
			return new Result(false, "添加购物车失败");
		}
		
	}
	
}
