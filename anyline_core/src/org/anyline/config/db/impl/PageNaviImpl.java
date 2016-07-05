/* 
 * Copyright 2006-2015 www.anyline.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 *
 *          AnyLine以及一切衍生库 不得用于任何与网游相关的系统
 */


package org.anyline.config.db.impl;

import java.io.Serializable;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashMap;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

import org.anyline.config.db.Order;
import org.anyline.config.db.OrderStore;
import org.anyline.config.db.PageNavi;
import org.anyline.util.ConfigTable;
import org.anyline.util.NumberUtil;
import org.apache.log4j.Logger;


public class PageNaviImpl implements PageNavi, Serializable{
	private static Logger LOG = Logger.getLogger(PageNaviImpl.class);
//
//	public static final String PAGE_VOL				= "pageRows"							;
//	public static final String PAGE_NO				= "pageNo"								;
	public static final String PAGE_ROWS			= "_anyline_page_rows"					;
	public static final String PAGE_NO				= "_anyline_page"						;
	
	private int totalRow;					//记录总数
	private int totalPage; 					//最大页数
	private int curPage=1;					//当前页数
	
	private int pageRange=10;				//显示多少个分页下标
	private int pageRows=10;				//每页多少条
	private int displayPageFirst = 0;		//显示的第一页标签
	private int displayPageLast = 0;		//显示的最后页标签
	private String baseLink;				//基础URL
	private OrderStore orders;				//排序依据(根据 orderCol 排序分页)
	private int calType = 0;				//分页计算方式(0-按页数 1-按开始结束数)
	private int firstRow = 0;				//第一行
	private int lastRow = -1;				//最后一行
	private boolean lazy = false;
	private int lazyPeriod = 0;				//总条数懒加载时间间隔(秒)
	

//	private String scriptFile = "/common/web/common/script/navi.js";
//	private String styleFile = "/common/web/common/style/navi.css";
	
	private Map<String,List<Object>> params;	//查询参数
	
	private static final String BR = "\n";
	//private static final String TAB = "\t";
	private static final String BR_TAB = "\n\t";
	
	private StringBuilder builder = new StringBuilder();		//分页HTML
	public PageNaviImpl(int totalRow, int curPage, int pageRows, String baseLink) {
		this.totalRow = totalRow;
		this.curPage = curPage;
		setPageRows(pageRows);
		this.baseLink = baseLink;
	}
	public PageNaviImpl(int curPage,int pageRows, String baseLink){
		this.curPage = curPage;
		setPageRows(pageRows);
		this.baseLink = baseLink;
	}
	public PageNaviImpl(String baseLink){
		this.curPage = 1;
		this.baseLink = baseLink;
	}
	public PageNaviImpl(){}
	/**
	 * 分页计算方式
	 * @param type	0-按页数 1-按开始结束记录数
	 */
	public void setCalType(int type){
		this.calType = type;
	}
	public int getCalType(){
		return calType;
	}
	/**
	 * 计算分页变量
	 */
	public void calculate() {
		setTotalPage((totalRow - 1) / pageRows + 1);					//总页数
		setDisplayPageFirst(curPage - pageRange/2);				//显示的第一页
		if(displayPageFirst > totalPage - pageRange){
			setDisplayPageFirst(totalPage - pageRange + 1);
		}
		if(displayPageFirst < 1){ 
			setDisplayPageFirst(1);
		}
		
		setDisplayPageLast(displayPageFirst + pageRange - 1);		//显示的最后页
		if (displayPageLast > totalPage)
			setDisplayPageLast(totalPage);
	}
	//创建隐藏参数
	private String createHidParams(){
		String html = "";
		try{
			if(null != params){
				for(Iterator<String> itrKey=params.keySet().iterator(); itrKey.hasNext();){
					String key = itrKey.next();
					Object values = params.get(key);
					html += createHidParam(key,values);
				}
			}
		}catch(Exception e){
			LOG.error(e);
		}
		return html;
	}
	
	/**
	 * 第一行
	 * @return
	 */
	public int getFirstRow(){
		if(calType == 0){
			if(curPage <= 0) {
				return 0;
			}
			return (curPage-1) * pageRows;
		}else{
			return firstRow;
		}
	}
	/**
	 * 最后一行
	 * @return
	 */
	public int getLastRow(){
		if(calType == 0){
			if(curPage == 0) {
				return pageRows -1;
			}
			return curPage * pageRows - 1;
		}else{
			return lastRow;
		}
	}
	/**
	 * 页面显示的第一页
	 * @return
	 */
	public int getDisplayPageFirst() {
		return displayPageFirst;
	}
	/**
	 * 设置页面显示的第一页
	 * @param displayPageFirst
	 */
	public void setDisplayPageFirst(int displayPageFirst) {
		this.displayPageFirst = displayPageFirst;
	}
	/**
	 * 页面显示的最后一页
	 * @return
	 */
	public int getDisplayPageLast() {
		return displayPageLast;
	}
	/**
	 * 设置页面显示的最后一页
	 * @param displayPageLast
	 */
	public void setDisplayPageLast(int displayPageLast) {
		this.displayPageLast = displayPageLast;
	}

	@SuppressWarnings("unchecked")
	public void addParam(String key, Object value){
		if(null == key || null == value){
			return;
		}
		if(null == this.params){
			this.params = new HashMap<String,List<Object>>();
		}
		List<Object> values = params.get(key);
		if(null == values){
			values = new ArrayList<Object>();
		}
		if(value instanceof Collection){
			values.addAll((Collection)value);
		}else{
			values.add(value);
		}
		params.put(key, values);
	}
	public Object getParams(String key){
		Object result = null;
		if(null != params){
			result = params.get(key);
		}
		return result;
	}
	@SuppressWarnings("unchecked")
	public Object getParam(String key){
		Object result = null;
		if(null != params){
			Object values = getParams(key);
			if(null != values && values instanceof List){
				result = ((List)values).get(0);
			}else{
				result = values;
			}
		}
		return result;
	}
	public String getOrderText(boolean require){
		return getOrderText(require, null);
	}
	public String getOrderText(boolean require, OrderStore store, String disKey){
		String result = "";
		if(null == orders){
			orders = store;
		}else{
			if(null != store){
				for(Order order:store.getOrders()){
					orders.order(order);
				}
			}
		}
		if(null != orders){
			result = orders.getRunText(disKey);
		}
		if(require && result.length() == 0){
			result = "ORDER BY CD";
		}
		return result;
	}
	/**
	 * 设置排序方式
	 * @param order
	 * @return
	 */
	public PageNavi order(Order order){
		if(null == orders){
			orders = new OrderStoreImpl();
		}
		orders.order(order);
		return this;
	}
	/**
	 * 设置排序方式
	 * @param order
	 * @param type
	 * @return
	 */
	@Override
	public PageNavi order(String order, String type){
		return order(new OrderImpl(order, type));
	}
	@Override
	public PageNavi order(String order){
		return order(new OrderImpl(order));
	}
	
	/**
	 * 设置总行数
	 * @param totalRow
	 */
	@Override
	public PageNavi setTotalRow(int totalRow) {
		this.totalRow = totalRow;
		return this;
	}
	/**
	 * 设置最后一页
	 * @param totalPage
	 */
	@Override
	public PageNavi setTotalPage(int totalPage) {
		this.totalPage = totalPage;
		return this;
	}
	/**
	 * 设置当前页
	 * @param curPage
	 */
	@Override
	public PageNavi setCurPage(int curPage) {
		this.curPage = curPage;
		return this;
	}
	/**
	 * 设置每页显示的行数
	 * @param pageRows
	 */
	@Override
	public PageNavi setPageRows(int pageRows) {
		if(pageRows > 0){
			this.pageRows = pageRows;
		}
		return this;
	}
	@Override
	public int getTotalRow() {
		return totalRow;
	}

	@Override
	public int getTotalPage() {
		return totalPage;
	}

	@Override
	public int getCurPage() {
		return curPage;
	}

	@Override
	public int getPageRows() {
		return pageRows;
	}

	@Override
	public String getBaseLink() {
		return baseLink;
	}
	@Override
	public PageNavi setBaseLink(String baseLink) {
		this.baseLink = baseLink;
		return this;
	}
	@Override
	public PageNavi setFirstRow(int firstRow) {
		this.firstRow = firstRow;
		return this;
	}
	@Override
	public PageNavi setLastRow(int lastRow) {
		this.lastRow = lastRow;
		return this;
	}
	
	@Override
	public boolean isLazy() {
		return this.lazy;
	}
	@Override
	public int getLazyPeriod() {
		return this.lazyPeriod;
	}
	@Override
	public PageNavi setLazy(int sec) {
		this.lazy = true;
		this.lazyPeriod = sec;
		return this;
	}
	@Override
	public PageNavi setLazyPeriod(int period){
		this.lazy = true;
		this.lazyPeriod = period;
		return this;
	}
	@Override
	public String createHidParam(String name, Object values) {
		String html = "";
		if(null == values){
			html = "<inpu type='hidden' name='"+name+"' value=''>\n";
		}else{
			if(values instanceof Collection<?>){
				Collection<?> list = (Collection<?>)values;
				for(Object obj:list){
					html += "<inpu type='hidden' name='"+name+"' value='"+obj+"'>\n";
				}
			}else{
				html += "<inpu type='hidden' name='"+name+"' value='"+values+"'>\n";
			}
		}
		return html;
	}
	@Override
	public String getOrderText(boolean require, OrderStore store) {
		// TODO Auto-generated method stub
		return null;
	}
	
	private String statFormat = "<div class='navi-summary'>共<span class='navi-total-row'>{totalRow}</span>条 第<span class='navi-cur-page'>{curPage}</span>/<span class='navi-total-page'>{totalPage}</span>页</div>";
	private String tagFirst = "第一页";
	private String tagPrev = "上一页";
	private String tagNext = "下一页";
	private String tagLast = "最后页";
	private String tagGo = "确定";
	public String ajaxPage(){
		return html(false);
	}
	public String jspPage(){
		return html(true);
	}
	/**
	 * 
	 * @param script 是否引用js css文件
	 * @return
	 */
	public String html(boolean include){
		calculate();
		StringBuilder builder = new StringBuilder();
		if(include){
			builder.append("<link rel=\"stylesheet\" href=\""+ConfigTable.getString("NAVI_STYLE_FILE_PATH")+"\" type=\"text/css\"/>\n");
			builder.append("<script type=\"text/javascript\" src=\""+ConfigTable.getString("NAVI_SCRIPT_FILE_PATH")+"\"></script>\n");
		}
		builder.append("<form id=\"_navi_frm\" action=\"" + baseLink + "\" method=\"post\">\n");
		builder.append("<input type=\"hidden\" name=\""+PageNavi.PAGE_NO+"\" id=\"__hidPageNo\" value='"+curPage+"'/>\n");
		builder.append(createHidParams());
		builder.append("</form>\n");
		builder.append("<div class=\"anyline_navi\">\n");
		String stat = statFormat.replace("{totalRow}", totalRow+"").replace("{curPage}", curPage+"").replace("{totalPage}", totalPage+"");
		builder.append(stat).append("\n");
		createPageTag(builder, "navi-first-button", ConfigTable.getString("NAVI_TAG_FIRST", tagFirst), 1);
		createPageTag(builder, "navi-prev-button", ConfigTable.getString("NAVI_TAG_PREV", tagPrev), NumberUtil.getMax(curPage-1,1));
		builder.append("<div class='navi-num-border'>\n");
		int range = ConfigTable.getInt("NAVI_PAGE_RANGE",10);
		int fr = NumberUtil.getMax(1,curPage - range/2);
		int to = fr + range - 1;
		boolean match = false;
		if(totalPage > range && curPage>range/2){
			match = ConfigTable.getBoolean("NAVI_PAGE_MATCH", true);
		}
		if(match){
			to = curPage + range/2;
		}
		if(totalPage - curPage < range/2){
			fr = totalPage - range;
		}
		fr = NumberUtil.getMax(fr, 1);
		to = NumberUtil.getMin(to, totalPage);
		
		for(int i=fr; i<=to; i++){
			createPageTag(builder, "navi-num-item", i + "", i);
		}
		builder.append("</div>\n");
		createPageTag(builder, "navi-next-button", ConfigTable.getString("NAVI_TAG_NEXT", tagNext), (int)NumberUtil.getMin(curPage+1, totalPage));
		createPageTag(builder, "navi-last-button", ConfigTable.getString("NAVI_TAG_LAST", tagLast), totalPage);
		builder.append("转到<input type='text' id='_anyline_go' value='");
		builder.append(curPage);
		builder.append("' class='navi-go-txt'/>页<span class='navi-go-button' onclick='_navi_go()'>")
		.append(ConfigTable.getString("NAVI_TAG_GO",tagGo)).append("</span>\n");
		builder.append("</div>");
		return builder.toString();
	}
	private void createPageTag(StringBuilder builder, String clazz, String tag, int page){
		builder.append("<span class ='").append(clazz);
		if(page == curPage){
			builder.append(" navi-disabled");
			if(clazz.contains("navi-num-item")){
				builder.append(" navi-num-item-cur");
			}
			builder.append("'");
		}else{
			builder.append("' onclick='_navi_go(").append(page).append(")'");
		}
		builder.append(">");
		builder.append(tag).append("</span>\n");
	}
	public String toString(){
		return html(true);
	}

}