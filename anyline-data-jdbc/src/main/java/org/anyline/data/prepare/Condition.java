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


package org.anyline.data.prepare;
 
import org.anyline.data.adapter.JDBCAdapter;
import org.anyline.data.run.RunValue;

import java.util.List;
import java.util.Map;



public interface Condition extends Cloneable{
	enum EMPTY_VALUE_CROSS{
		 DEFAULT   //默认由参数格式决定  如 +ID:id  ++ID:id,默认情况下如果值为空则忽略当前条件
		, BREAK	   //中断执行 整个SQL不执行
		, CONTINUE //当前条件不参考最终SQL执行 其他条件继续执行
		, NULL	   //生成 WHERE ID IS NULL
		, SRC	   //原样处理 会生成 WHERE ID = NULL
	}
	String CONDITION_JOIN_TYPE_AND		= " AND "	;	// 拼接方式 AND
	String CONDITION_JOIN_TYPE_OR		= " OR "	;	// 拼接方式 OR
	// 参数变量类型
	int VARIABLE_FLAG_TYPE_INDEX	= 0			;	// 按下标区分
	int VARIABLE_FLAG_TYPE_KEY		= 1			;	// 按KEY区分
	int VARIABLE_FLAG_TYPE_NONE		= 2			;	// 没有变量
	 
	/** 
	 * 运行时文本 
	 * @param prefix 前缀
	 * @param adapter adapter
	 * @return String
	 */
	String getRunText(String prefix, JDBCAdapter adapter);

	/**
	 * 静态SQL
	 * @param text TEXT
	 * @return Condition
	 */
	Condition setRunText(String text);
	/** 
	 * 运行时参数值 
	 * @return List
	 */ 
	List<RunValue> getRunValues();
	/** 
	 * 拼接方式 
	 * @return String
	 */ 
	String getJoin(); 
	Condition setJoin(String join); 
	/** 
	 * 当前条件所处容器 
	 * @return ConditionChain
	 */ 
	ConditionChain getContainer(); 
	boolean hasContainer(); 
	boolean isContainer(); 
	/** 
	 * 设置当前条件所处容器 
	 * @param chain  chain
	 * @return Condition
	 */ 
	Condition setContainer(ConditionChain chain); 
	 
	/** 
	 * 初始化 
	 */ 
	void init(); 
	void initRunValue(); 
	boolean isActive();
	boolean isRequired();
	void setRequired(boolean required);
	boolean isStrictRequired();
	void setStrictRequired(boolean strictRequired);
	boolean isValid();
	void setActive(boolean active); 
	int getVariableType();
	void setVariableType(int variableType);

	EMPTY_VALUE_CROSS getCross() ;

	void setCross(EMPTY_VALUE_CROSS cross) ;
	/* ************************************************************************************************************ 
	 *  
	 * 													 自动生成 
	 * 
	 * ***********************************************************************************************************/ 
	 
	 
	/* ************************************************************************************************************ 
	 *  
	 * 													 XML定义 
	 * 
	 * ***********************************************************************************************************/ 
	String getId();
	Object clone()throws CloneNotSupportedException; 
	void setValue(String key, Object value);
	void setTest(String test);
	String getTest();
	Map<String,Object> getRunValuesMap();
	List<Variable> getVariables();
	Variable getVariable(String name);

	/**
	 * 是否只是用来给变量赋值的
	 * 用来给java/xml定义SQL中变量赋值,本身并不拼接到最终SQL
	 * @return boolean
	 */
	boolean isVariableSlave();
	void setVariableSlave(boolean bol);
	boolean isSetValue();
	boolean isSetValue(String variable);
} 
