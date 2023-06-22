/*
 * Copyright 2006-2023 www.anyline.org
 *
 * Licensed under the Apache License, Version 2.0 (JDBCRuntime runtime, String random, the "License"){}
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
package org.anyline.data.listener;

import org.anyline.data.jdbc.ds.JDBCRuntime;
import org.anyline.data.param.ConfigStore;
import org.anyline.data.prepare.RunPrepare;
import org.anyline.data.run.Run;
import org.anyline.entity.DataSet;
import org.anyline.entity.EntitySet;
import org.anyline.entity.data.ACTION;
import org.anyline.entity.data.ACTION.SWITCH;
import org.anyline.entity.data.Procedure;

import java.util.List;

public interface DMListener {

    /**
     * 创建查相关的SQL之前调用,包括slect exists count等<br/>
     * 要修改查询条件可以在这一步实现,注意不是在beforeQuery
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param prepare  prepare
     * @param configs 查询条件配置
     * @param conditions 查询条件
     * @return 如果返回false 则中断执行
     */
    default SWITCH prepareQuery(JDBCRuntime runtime, String random, RunPrepare prepare, ConfigStore configs, String ... conditions){return SWITCH.CONTINUE;}


    /**
     * 统计总记录数之前调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     */
    default void beforeTotal(JDBCRuntime runtime, String random, Run run){}
    /**
     * 统计总记录数之后调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @param success SQL是否成功执行
     * @param total 总行数
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterTotal(JDBCRuntime runtime, String random, Run run, boolean success, int total, long millis){}
    /**
     * 查询之前调用<br/>
     * 不满足查询条件的不会走到这一步(JDBCRuntime runtime, String random, 如必须参数未提供)
     * 只有确定执行查询时才会到这一步，到了这一步已经不能修改查询条件<br/>
     * 要修改查询条件可以在prepareQuery实现
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @param total 上一步合计的总行数
     */
    default void beforeQuery(JDBCRuntime runtime, String random, Run run, int total){}
    /**
     * 查询之后调用(JDBCRuntime runtime, String random, 调用service.map或service.maps)
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @param maps 查询结果
     * @param success SQL是否成功执行
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterQuery(JDBCRuntime runtime, String random, Run run, boolean success, List<?>  maps, long millis){}
    default void afterQuery(JDBCRuntime runtime, String random, Run run, boolean success, EntitySet<?> maps, long millis){}
    /**
     * 查询之后调用(JDBCRuntime runtime, String random, 调用service.query或service.querys)
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @param set 查询结果
     * @param success SQL是否成功执行
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterQuery(JDBCRuntime runtime, String random, Run run, boolean success, DataSet set, long millis){}
    /**
     * count之前调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     */
    default void beforeCount(JDBCRuntime runtime, String random, Run run){}
    /**
     * count之后调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @param result 行数
     * @param success SQL是否成功执行
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterCount(JDBCRuntime runtime, String random, Run run, boolean success, int result, long millis){}

    /**
     * 判断是否存在之前调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     */
    default void beforeExists(JDBCRuntime runtime, String random, Run run){}
    /**
     * 判断是否存在之后调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @param exists 是否存在
     * @param success SQL是否成功执行
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterExists(JDBCRuntime runtime, String random, Run run, boolean success, boolean exists, long millis){}


    /**
     * 创建更新相关的SQL之前调用<br/>
     * 要修改更新内容或条件可以在这一步实现,注意不是在beforeUpdate
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param dest 表
     * @param obj Entity或DtaRow
     * @param checkPrimary 是否需要检查重复主键,默认不检查
     * @param columns 需要更新的列
     * @param configs 更新条件
     * @return 如果返回false 则中断执行
     */
    default SWITCH  prepareUpdate(JDBCRuntime runtime, String random, String dest, Object obj, ConfigStore configs, boolean checkPrimary, List<String> columns){return SWITCH.CONTINUE;}


    /**
     * 更新之前调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run run
     * @param dest 需要更新的表
     * @param obj 更新内容
     * @param columns 需要更新的列
     * @return 是否执行  如果返回false 将不执行更新
     */
    default SWITCH  beforeUpdate(JDBCRuntime runtime, String random, Run run, String dest, Object obj, List<String> columns){return SWITCH.CONTINUE;}
    /**
     * 更新之前调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run run
     * @param count 影响行数
     * @param dest 需要更新的表
     * @param obj 更新内容
     * @param success SQL是否成功执行
     * @param qty 景程行数，如果执行不成功返回-1
     * @param columns 需要更新的列
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterUpdate(JDBCRuntime runtime, String random, Run run, int count, String dest, Object obj, List<String> columns, boolean success, int qty,  long millis){}


    /**
     * 创建插入相关的SQL之前调用<br/>
     * 要修改插入内容可以在这一步实现,注意不是在beforeInsert
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param dest 表
     * @param obj 实体
     * @param checkPrimary 是否需要检查重复主键,默认不检查
     * @param columns 需要抛入的列 如果不指定  则根据实体属性解析
     * @return 如果返回false 则中断执行
     */
    default SWITCH  prepareInsert(JDBCRuntime runtime, String random, String dest, Object obj, boolean checkPrimary, List<String> columns){return SWITCH.CONTINUE;}
    /**
     * 创建insert sql之前调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @param dest 需要插入的表
     * @param obj 接入内容
     * @param checkPrimary 是否需要检查重复主键,默认不检查
     * @param columns 需要插入的列
     * @return 是否执行  如果返回false 将不执行插入
     */
    default SWITCH  beforeInsert(JDBCRuntime runtime, String random, Run run, String dest, Object obj, boolean checkPrimary, List<String> columns){return SWITCH.CONTINUE;}

    /**
     * 插入之后调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @param count 影响行数
     * @param dest 需要插入的表
     * @param obj 接入内容
     * @param success SQL是否成功执行
     * @param checkPrimary 是否需要检查重复主键,默认不检查
     * @param columns 需要插入的列
     * @param qty 景程行数，如果执行不成功返回-1
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterInsert(JDBCRuntime runtime, String random, Run run, int count, String dest, Object obj, boolean checkPrimary, List<String> columns,  boolean success, int qty, long millis){}

    /**
     * 执行SQL之前调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @return 是否执行 如果返回false装不执行sql
     */
    default SWITCH  beforeExecute(JDBCRuntime runtime, String random, Run run){return SWITCH.CONTINUE;}

    /**
     * 执行SQL之后调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @param success SQL是否成功执行
     * @param qty 景程行数，如果执行不成功返回-1
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterExecute(JDBCRuntime runtime, String random, Run run, boolean success, int qty, long millis){}

    /**
     * 执行存储过程之前调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param procedure 存储过程
     * @return 是否执行 如果返回false装不执行存储过程
     */
    default SWITCH  prepareExecute(JDBCRuntime runtime, String random, Procedure procedure){return SWITCH.CONTINUE;}
    default SWITCH  beforeExecute(JDBCRuntime runtime, String random, Procedure procedure){return SWITCH.CONTINUE;}

    /**
     * 执行存储过程之后调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param procedure 存储过程
     * @param success SQL是否成功执行 如果需要返回值需要从procedure中获取
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterExecute(JDBCRuntime runtime, String random, Procedure procedure, boolean success, long millis){}

    /**
     * 查询存过程之前调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param procedure 存储过程
     */
    default void beforeQuery(JDBCRuntime runtime, String random, Procedure procedure){}

    /**
     * 查询存储过程之后调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param procedure 存储过程
     * @param set 返回结果集
     * @param success SQL是否成功执行
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterQuery(JDBCRuntime runtime, String random, Procedure procedure, boolean success, DataSet set, long millis){}

    /**
     * 创建删除SQL前调用(JDBCRuntime runtime, String random, 根据Entity/DataRow),修改删除条件可以在这一步实现<br/>
     * 注意不是beforeDelete<br/>
     * 注意prepareDelete有两个函数需要实现
     * service.delete(JDBCRuntime runtime, String random, DataRow/Entity){}
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param dest 表
     * @param obj entity或DataRow
     * @param columns 删除条件的我
     * @return 如果返回false 则中断执行
     */
    default SWITCH  prepareDelete(JDBCRuntime runtime, String random, String dest, Object obj, String ... columns){return SWITCH.CONTINUE;}
    /**
     * 创建删除SQL前调用(JDBCRuntime runtime, String random, 根据条件),修改删除条件可以在这一步实现<br/>
     * 注意不是beforeDelete<br/>
     * 注意prepareDelete有两个函数需要实现
     * service.delete(JDBCRuntime runtime, String random, "CRM_USER", "ID", "1", "2", "3"){}
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param table 表
     * @param key key
     * @param values values
     * @return 如果返回false 则中断执行
     */
    default SWITCH  prepareDelete(JDBCRuntime runtime, String random, String table, String key, Object values){return SWITCH.CONTINUE;}
    /**
     * 执行删除前调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @return 是否执行 如果返回false装不执行删除
     */
    default SWITCH  beforeDelete(JDBCRuntime runtime, String random, Run run){return SWITCH.CONTINUE;}

    /**
     * 执行删除后调用
     *
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等
     * @param run 包含最终执行的SQL以及占位参数值
     * @param success SQL是否成功执行
     * @param qty 景程行数，如果执行不成功返回-1
     * @param millis 耗时(JDBCRuntime runtime, String random, 毫秒)
     */
    default void afterDelete(JDBCRuntime runtime, String random, Run run, boolean success, int qty, long millis){}

    /**
     * 执行SQL时间超限时触发
     
     * @param runtime  包含数据源(key)、适配器、JDBCTemplate、dao
     * @param random 用来标记同一组SQL、执行结构、参数等 
     * @param action 执行命令
     * @param run 包含最终执行的SQL以及占位参数值
     * @param sql SQL或存储过程名称
     * @param inputs 输入参数
     * @param outputs 输出参数
     * @param success SQL 是否成功执行
     * @param result 执行结果
     * @param millis 执行耗时
     */
    default void slow(JDBCRuntime runtime, String random, ACTION.DML action, Run run, String sql, List inputs, List outputs, boolean success, Object result, long millis){}
}
