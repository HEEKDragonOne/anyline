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


package org.anyline.config.db.sql.auto;

import java.util.List;

import org.anyline.config.db.Condition;


/**
 * 自动生成的参数
 * @author Administrator
 *
 */
public interface AutoCondition extends  Condition{
	public Object getValue();
	public List<Object> getValues();
	public String getId();
	public String getColumn() ;
	public void setColumn(String column) ;
	public void setValues(Object values) ;
	public int getCompare() ;
	public void setCompare(int compare) ;
}
