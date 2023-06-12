/* 
 * Copyright 2006-2023 www.anyline.org
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
 *          
 */


package org.anyline.data.param.init;

import org.anyline.data.param.Config;
import org.anyline.data.param.ConfigChain;
import org.anyline.data.param.ConfigParser;
import org.anyline.data.param.ParseResult;
import org.anyline.data.prepare.Condition;
import org.anyline.data.prepare.ConditionChain;
import org.anyline.data.prepare.auto.init.DefaultAutoCondition;
import org.anyline.data.prepare.auto.init.DefaultAutoConditionChain;
import org.anyline.entity.Compare;
import org.anyline.util.BasicUtil;
import org.anyline.util.BeanUtil;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.*;

public class DefaultConfig implements Config {
	protected static final Logger log = LoggerFactory.getLogger(DefaultConfig.class);
	protected String text				; // 静态条件(如原生SQL) 没有参数
	protected List<Object> values		; // VALUE
	protected List<Object> orValues		; // OR VALUE
	protected boolean empty				; // 是否值为空
	protected ParseResult parser;
	protected boolean overCondition  = true;
	protected boolean overValue		 = true;

	@Override
	public Object clone(){
		DefaultConfig config = new DefaultConfig();
		config.parser = this.parser;
		config.empty = this.empty;
		List<Object> values = new ArrayList<Object>();
		for(Object value:this.values){
			values.add(value);
		}
		config.values = values;
		return config;
	} 
	public DefaultConfig(){
		this.parser = new ParseResult();
	} 
	public String toString(){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("join", this.getJoin());
		map.put("prefix", this.getPrefix());
		map.put("var", this.getVariable());
		map.put("key", this.getKey());
		map.put("compare", this.getCompare().getCode());
		map.put("values", values);
		return BeanUtil.map2json(map);
	}
	public String cacheKey(){
		Map<String,Object> map = new HashMap<String,Object>();
		map.put("prefix", this.getPrefix());
		map.put("compare", this.getCompare().getCode());
		map.put("values", values);
		return BeanUtil.map2json(map);
	} 
 
	/** 
	 * 解析配置 
	 * 		[+]	SQL参数名	[.SQL变量名]	:	[&gt;=]request参数名		:默认值 
	 * 										[request参数名] 
	 * 										%request参数名% 
	 * 						 
	 * @param config  config
	 */ 
	public DefaultConfig(String config){
		parser = ConfigParser.parse(config, true); 
	}
	public void setValue(Map<String,Object> values){
		try{
			this.values = ConfigParser.getValues(values, parser);
			empty = BasicUtil.isEmpty(true, this.values); 
			setOrValue(values);
		}catch(Exception e){
			e.printStackTrace(); 
		} 
	} 
	public void setOrValue(Map<String,Object> values){
		try{
			this.orValues = ConfigParser.getValues(values, parser.getOr());
		}catch(Exception e){
			e.printStackTrace(); 
		} 
	}

	public List<Object> getValues() {
		return values; 
	} 
	public List<Object> getOrValues() {
		return orValues; 
	} 
	@SuppressWarnings({"rawtypes", "unchecked" })
	public void addValue(Object value){
		if(null == values){
			values = new ArrayList<Object>();
		}
		if(null != value){
			if(value instanceof Collection) {
				Collection list = (Collection) value;
				for(Object item:list){
					addValue(item);
				}
			}else if(value instanceof Object[]){
				Object[] tmps = (Object[]) value;
				for(Object tmp:tmps){
					addValue(tmp);
				}
			}else{
				values.add(value);
			}
		}else{
			values.add(value);
		}
	}
	public void setValue(Object value){
		values = new ArrayList<Object>();
		addValue(value);
	} 
	public void setOrValue(Object value){
		orValues = new ArrayList<Object>();
		addValue(value);
	} 
	/** 
	 *  createAutoCondition
	 * @param chain 容器 
	 * @return Condition
	 */ 
	public Condition createAutoCondition(ConditionChain chain){
		Condition condition = null; 
		if(isRequire() || !isEmpty()){
			if(this instanceof ConfigChain){
				condition = new DefaultAutoConditionChain((ConfigChain)this);
				condition.setJoin(this.getJoin());
				condition.setContainer(chain);
			}else{
				if(null != text){
					condition = new DefaultAutoCondition(this);
					condition.setRunText(text);
					condition.setContainer(chain);
					condition.setActive(true);
					condition.setVariableType(Condition.VARIABLE_FLAG_TYPE_NONE);
				}else {
					condition = new DefaultAutoCondition(this).setOrCompare(getOrCompare()).setJoin(parser.getJoin());
					condition.setContainer(chain);
				}
			} 
		} 
		return condition; 
	} 
	public String getPrefix() {
		return parser.getPrefix();
	} 

	public void setPrefix(String prefix) {
		parser.setPrefix(prefix);
	} 
 
	public String getVariable() {
		return parser.getVar();
	} 
 
	public void setVariable(String variable) {
		parser.setVar(variable);
	} 
 
 
 
	public String getKey() {
		return parser.getKey(); 
	} 
 
	public void setKey(String key) {
		parser.setKey(key); 
	} 
 
	public Compare getCompare() {
		return parser.getCompare(); 
	} 
 
	public void setCompare(Compare compare) {
		parser.setCompare(compare); 
	} 
 
	public boolean isEmpty() {
		return empty; 
	} 
 
	public void setEmpty(boolean empty) {
		this.empty = empty; 
	} 
 
	public boolean isRequire() {
		return parser.isRequired(); 
	} 
	 
	public void setRequire(boolean require) {
		parser.setRequired(require); 
	} 
	public boolean isStrictRequired() {
		return parser.isStrictRequired();
	}
	public void setStrictRequired(boolean strictRequired) {
		parser.setStrictRequired(strictRequired);
	} 
	public String getJoin() {
		return parser.getJoin(); 
	} 
 
	public void setJoin(String join) {
		parser.setJoin(join);
	} 
 
	public boolean isKeyEncrypt() {
		return parser.isKeyEncrypt(); 
	} 
 
	public boolean isValueEncrypt() {
		return parser.isValueEncrypt(); 
	}
	@Override
	public Compare getOrCompare() {
		ParseResult or = parser.getOr();
		if(null != or){
			return or.getCompare();
		}
		return parser.getCompare();
	}
	@Override
	public void setOrCompare(Compare compare) {
		ParseResult or = parser.getOr();
		if(null != or){
			or.setCompare(compare);
		}
	}
	public void setTable(String table){

	}
	public String getTable(){
		return parser.getPrefix();
	}

	@Override
	public void setText(String text) {
		this.text = text;
	}

	@Override
	public String getText() {
		return text;
	}

	@Override
	public boolean isOverCondition() {
		return overCondition;
	}

	@Override
	public void setOverCondition(boolean overCondition) {
		this.overCondition = overCondition;
	}

	@Override
	public boolean isOverValue() {
		return overValue;
	}

	@Override
	public void setOverValue(boolean overValue) {
		this.overValue = overValue;
	}
}
