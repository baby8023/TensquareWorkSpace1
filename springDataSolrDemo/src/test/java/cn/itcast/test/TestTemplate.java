package cn.itcast.test;

import java.math.BigDecimal;
import java.util.ArrayList;
import java.util.List;

import org.junit.Test;
import org.junit.runner.RunWith;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.data.solr.core.SolrTemplate;
import org.springframework.data.solr.core.query.Criteria;
import org.springframework.data.solr.core.query.Query;
import org.springframework.data.solr.core.query.SimpleQuery;
import org.springframework.data.solr.core.query.result.ScoredPage;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit4.SpringJUnit4ClassRunner;

import cn.itcast.pojo.TbItem;

@RunWith(SpringJUnit4ClassRunner.class)
@ContextConfiguration(locations="classpath:applicationContext-solr.xml")
public class TestTemplate {
	
	@Autowired
	private SolrTemplate solrTemplate;
	
	/**
	 * 增加数据
	 */
	@Test
	public void testAdd(){
				
		TbItem item=new TbItem();
		item.setId(1L);
		item.setTitle("华为METE10");
		item.setGoodsId(1L);
		item.setPrice(new BigDecimal(3030.01));
		item.setBrand("华为");
		item.setSeller("华为旗舰店");
		item.setCategory("手机");
		
		
		solrTemplate.saveBean(item);
		solrTemplate.commit();
	}
	
	@Test
	public void testFindOne(){
		
		TbItem item = solrTemplate.getById(1L, TbItem.class);		
		System.out.println(item.getTitle());
		
	}
	
	@Test
	public void testDeleteOne(){
		solrTemplate.deleteById("1");
		solrTemplate.commit();
	}
	
	@Test
	public void testAddList(){
		List<TbItem> list=new ArrayList();
		
		for(int i=0;i<100;i++){
			TbItem item=new TbItem();
			item.setId(i+1L);
			item.setTitle("华为METE"+i);
			item.setGoodsId(1L);
			item.setPrice(new BigDecimal(3030.01+i));
			item.setBrand("华为");
			item.setSeller("华为旗舰店");
			item.setCategory("手机");				
			list.add(item);			
		}
		solrTemplate.saveBeans(list);
		solrTemplate.commit();
	}
	
	
	@Test
	public void queryByPage(){
				
		Query query=new SimpleQuery("*:*");
		
		query.setOffset(20);//下标从0开始
		query.setRows(20);
		ScoredPage<TbItem> page = solrTemplate.queryForPage(query, TbItem.class);
		List<TbItem> itemList = page.getContent();
		showList(itemList);//显示数据
	}
	
	
	@Test
	public void queryByMuti(){
				
		Query query=new SimpleQuery();
		
		Criteria criteria=new Criteria("item_brand").is("华为");
		
		criteria=criteria.and("item_title").contains("1");
		
		query.addCriteria(criteria);	
		
		query.setOffset(10);//下标从0开始
		//query.setRows(20);
		ScoredPage<TbItem> page = solrTemplate.queryForPage(query, TbItem.class);
		List<TbItem> itemList = page.getContent();
		showList(itemList);//显示数据
	}
	
	@Test
	public void deleteAll(){
		Query query=new SimpleQuery("*:*");
		solrTemplate.delete(query);
		solrTemplate.commit();
	}
	
	
	private void showList(List<TbItem> itemList ){
		
		for(TbItem item:itemList){
			System.out.println(item.getTitle()+"  "+item.getBrand()+"  "+item.getPrice());			
		}		
	}
	
}
