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
 */


package org.anyline.data.prepare.init;

import org.anyline.data.prepare.Condition;
import org.anyline.data.prepare.ConditionChain;
import org.anyline.data.run.RunValue;
import org.anyline.util.BasicUtil;

import java.util.ArrayList;
import java.util.Collection;
import java.util.List;
 
/** 
 * 自动生成的参数 
 * @author zh 
 * 
 */ 
public abstract class DefaultConditionChain extends DefaultCondition implements ConditionChain {
	protected List<Condition> conditions = new ArrayList<Condition>();
	protected int joinSize; 
	 
	public void init(){
		for(Condition condition:conditions){
			if(null == condition){
				continue;
			} 
			condition.init(); 
		} 
	} 
	/** 
	 * 附加条件 
	 * @param condition  condition
	 * @return ConditionChain
	 */ 
	public ConditionChain addCondition(Condition condition){
		conditions.add(condition); 
		return this; 
	}

	protected void addRunValue(RunValue value){
		if(null == value){
			return;
		}
		runValues.add(value);
	}
	protected void addRunValue(List<RunValue> values){
		for(RunValue value:values){
			addRunValue(value);
		}
	}
	@SuppressWarnings("unchecked") 
	protected void addRunValue(String key, Object value){
		if(null == value){
			return; 
		}
		if(null == key){
			key = "none";
		}
		if(value instanceof RunValue){
			throw new RuntimeException("run value");
		}
		if(value instanceof Collection){
			Collection<Object> list = (Collection<Object>)value;
			for(Object obj:list){
				RunValue v = new RunValue(key, obj);
				addRunValue(v);
			}
		}else{
			RunValue v = new RunValue(key, value);
			addRunValue(v);
		} 
	} 
	public List<RunValue> getRunValues(){
		return runValues; 
	} 
	public String getJoin(){
		if(BasicUtil.isNotEmpty(join)){
			return join;
		}
		return Condition.CONDITION_JOIN_TYPE_AND; 
	} 
	public int getJoinSize(){
		return joinSize; 
	} 
	public List<Condition> getConditions() {
		return conditions; 
	}
	public boolean isValid(){
		if(!super.isValid()){
			return false;
		}
		for(Condition con:conditions){
			if(null != con && !con.isValid()){
				return false;
			}
		}
		return true;
	} 
} 
