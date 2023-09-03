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


package org.anyline.data.mongodb.adapter;

import com.mongodb.client.FindIterable;
import com.mongodb.client.MongoCollection;
import com.mongodb.client.MongoDatabase;
import com.mongodb.client.model.Filters;
import com.mongodb.client.model.Projections;
import com.mongodb.client.model.Updates;
import com.mongodb.client.result.DeleteResult;
import com.mongodb.client.result.UpdateResult;
import org.anyline.adapter.EntityAdapter;
import org.anyline.data.adapter.DriverAdapter;
import org.anyline.data.adapter.init.DefaultDriverAdapter;
import org.anyline.data.mongodb.entity.MongoDataRow;
import org.anyline.data.mongodb.runtime.MongoRuntime;
import org.anyline.data.param.ConfigStore;
import org.anyline.data.param.init.DefaultConfigStore;
import org.anyline.data.prepare.Condition;
import org.anyline.data.prepare.ConditionChain;
import org.anyline.data.prepare.RunPrepare;
import org.anyline.data.prepare.auto.AutoCondition;
import org.anyline.data.prepare.auto.TablePrepare;
import org.anyline.data.prepare.auto.init.DefaultTablePrepare;
import org.anyline.data.run.Run;
import org.anyline.data.run.SimpleRun;
import org.anyline.data.run.TableRun;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.data.util.DataSourceUtil;
import org.anyline.data.util.ThreadConfig;
import org.anyline.entity.*;
import org.anyline.entity.generator.PrimaryGenerator;
import org.anyline.exception.SQLQueryException;
import org.anyline.exception.SQLUpdateException;
import org.anyline.metadata.*;
import org.anyline.metadata.type.DatabaseType;
import org.anyline.proxy.EntityAdapterProxy;
import org.anyline.proxy.InterceptorProxy;
import org.anyline.util.BeanUtil;
import org.anyline.util.ConfigTable;
import org.anyline.util.LogUtil;
import org.anyline.util.SQLUtil;
import org.bson.conversions.Bson;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.lang.reflect.Field;
import java.sql.Connection;
import java.sql.ResultSetMetaData;
import java.util.*;

@Repository("anyline.data.adapter.mongo")
public class MongoAdapter extends DefaultDriverAdapter implements DriverAdapter {

    @Override
    public DatabaseType type() {
        return DatabaseType.MongoDB;
    }

    @Override
    public boolean exists(DataRuntime runtime, String random, RunPrepare prepare, ConfigStore configs, String... conditions) {
        return false;
    }

    /**
     * insert [入口]<br/>
     * 执行完成后会补齐自增主键值
     * @param runtime 运行环境主要包含驱动适配器 数据源或客户端
     * @param random 用来标记同一组命令
     * @param dest 表
     * @param data 数据
     * @param checkPrimary 是否需要检查重复主键,默认不检查
     * @param columns 列
     * @return 影响行数
     */
    @Override
    public long insert(DataRuntime runtime, String random, int batch, String dest, Object data, boolean checkPrimary, List<String> columns) {
        return super.insert(runtime, random, batch, dest, data, checkPrimary, columns);
    }

    /**
     * 根据entity创建 INSERT RunPrepare
     * @param runtime 运行环境主要包含驱动适配器 数据源或客户端
     * @param dest 表
     * @param obj 数据
     * @param checkPrimary 是否需要检查重复主键,默认不检查
     * @param columns 需要插入的列
     * @return Run 最终执行命令 如果是JDBC类型库 会包含 SQL 与 参数值
     */
    @Override
    protected Run createInsertRun(DataRuntime runtime, String dest, Object obj, boolean checkPrimary, List<String> columns){
        Run run = new TableRun(runtime, dest);
        PrimaryGenerator generator = checkPrimaryGenerator(type(), dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""));
        if(null != generator) {
            Object pv = BeanUtil.getFieldValue(obj, "_id");
            if(null == pv){
                List<String> pk = new ArrayList<>();
                pk.add("_id");
                generator.create(obj, DatabaseType.MongoDB, dest, pk, null);
            }
        }
        run.setValue(obj);
        return run;
    }

    /**
     * 根据collection创建 INSERT RunPrepare
     * @param runtime 运行环境主要包含驱动适配器 数据源或客户端
     * @param dest 表
     * @param list 对象集合
     * @param checkPrimary 是否需要检查重复主键,默认不检查
     * @param columns 需要插入的列,如果不指定则全部插入
     * @return Run 最终执行命令 如果是JDBC类型库 会包含 SQL 与 参数值
     */
    @Override
    protected Run createInsertRunFromCollection(DataRuntime runtime, int batch, String dest, Collection list, boolean checkPrimary, List<String> columns){
        Run run = new TableRun(runtime, dest);
        PrimaryGenerator generator = checkPrimaryGenerator(type(), dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""));
        if(null != generator) {
            List<String> pk = new ArrayList<>();
            pk.add("_id");
            for (Object item : list) {
                Object pv = BeanUtil.getFieldValue(item, "_id");
                if(null != pv){
                    break;
                }
                generator.create(item, DatabaseType.MongoDB, dest, pk, null);
            }
        }
        run.setValue(list);
        return run;
    }
    /**
     * insert [执行]
     * @param runtime 运行环境主要包含驱动适配器 数据源或客户端
     * @param random 用来标记同一组命令
     * @param data entity|DataRow|DataSet
     * @param run 最终待执行的命令和参数(如果是JDBC环境就是SQL)
     * @param pks pks
     * @return int 影响行数
     */
    @Override
    public long insert(DataRuntime runtime, String random, Object data, Run run, String[] pks) {
        long cnt = 0;
        Object value = run.getValue();
        String collection = run.getTable();
        if(null == value){
            if(ConfigTable.IS_SHOW_SQL && log.isWarnEnabled()){
                log.warn("[valid:false][action:insert][不具备执行条件][dest:"+run.getTable()+"]");
            }
            return -1;
        }
        MongoRuntime rt = (MongoRuntime) runtime;
        MongoDatabase database = rt.getDatabase();
        long fr = System.currentTimeMillis();
        try {
            MongoCollection cons = null;
            if(value instanceof List){
                List list = (List) value;
                cons = database.getCollection(run.getTable(), list.get(0).getClass());
                cnt = list.size();
                cons.insertMany(list);
            }else if(value instanceof DataSet){
                DataSet set = (DataSet)value;
                cons = database.getCollection(run.getTable(), ConfigTable.DEFAULT_MONGO_ENTITY_CLASS);
                cons.insertMany(set.getRows());
                cnt = set.size();
            }else if(value instanceof EntitySet){
                List<Object> datas = ((EntitySet)value).getDatas();
                cons = database.getCollection(run.getTable(),datas.get(0).getClass());
                cons.insertMany(datas);
                cnt = datas.size();
            }else if(value instanceof Collection){
                Collection items = (Collection) value;
                List<Object> list = new ArrayList<>();
                for(Object item:items){
                    list.add(item);
                    cnt ++;
                }
                cons = database.getCollection(run.getTable(), list.get(0).getClass());
                cons.insertMany(list);
            }else{
                cons = database.getCollection(run.getTable(), value.getClass());
                cons.insertOne(value);
                cnt = 1;
            }

            long millis = System.currentTimeMillis() - fr;
            boolean slow = false;
            long SLOW_SQL_MILLIS = ThreadConfig.check(runtime.getKey()).SLOW_SQL_MILLIS();
            if(SLOW_SQL_MILLIS > 0){
                if(millis > SLOW_SQL_MILLIS){
                    slow = true;
                    log.warn("{}[slow cmd][action:insert][millis:{}ms][collection:{}]", random, millis, collection);
                    if(null != dmListener){
                        dmListener.slow(runtime, random, ACTION.DML.INSERT, run, null, null, null, true, cnt, millis);
                    }
                }
            }
            if (!slow && ConfigTable.IS_SHOW_SQL && log.isInfoEnabled()) {
                log.info("{}[action:insert][执行耗时:{}ms][影响行数:{}]", random, millis, LogUtil.format(cnt, 34));
            }
        }catch(Exception e){
            if(ConfigTable.IS_PRINT_EXCEPTION_STACK_TRACE) {
                e.printStackTrace();
            }
            if(ConfigTable.IS_THROW_SQL_UPDATE_EXCEPTION){
                SQLUpdateException ex = new SQLUpdateException("insert异常:"+e, e);
                throw ex;
            }else{
                if(ConfigTable.IS_SHOW_SQL_WHEN_ERROR){
                    log.error("{}[{}][collection:{}][param:{}]", random, LogUtil.format("插入异常:", 33)+e, run.getTable(), BeanUtil.object2json(data));
                }
            }
        }
        return cnt;
    }

    @Override
    public long save(DataRuntime runtime, String random, String dest, Object data, boolean checkPrimary, List<String> columns) {
        return 0;
    }

    /**
     * 创建查询SQL
     * @param prepare  构建最终执行命令的全部参数，包含表（或视图｜函数｜自定义SQL)查询条件 排序 分页等
     * @param configs 过滤条件及相关配置
     * @param conditions 简单过滤条件
     * @return Run 最终执行命令 如果是JDBC类型库 会包含 SQL 与 参数值
     */
    @Override
    public Run buildQueryRun(DataRuntime runtime, RunPrepare prepare, ConfigStore configs, String ... conditions){
        Run run = null;
        if(prepare instanceof TablePrepare){
            run = new TableRun(runtime, prepare.getTable());
        }else{
            throw new RuntimeException("不支持查询的类型");
        }
        if(null != run){
            run.setRuntime(runtime);
            //如果是text类型 将解析文本并抽取出变量
            run.setPrepare(prepare);
            run.setConfigStore(configs);
            run.addCondition(conditions);
            if(run.checkValid()) {
                //为变量赋值
                run.init();
                //构造最终的查询SQL
                fillQueryContent(runtime, run);
            }
        }
        return run;
    }

    @Override
    protected void fillQueryContent(DataRuntime runtime, TableRun run){
        Bson bson = null;
        ConditionChain chain = run.getConditionChain();
        bson = parseCondition(bson, chain);
        run.setFilter(bson);

        List<String> excludeColumns = run.getPrepare().getExcludeColumns();
        if(null == excludeColumns || excludeColumns.size() ==0){
            ConfigStore configs = run.getConfigStore();
            if(null != configs) {
                excludeColumns = configs.columns();
            }
        }
        if(null != excludeColumns && excludeColumns.size()>0){
            run.setExcludeColumns(excludeColumns);
        }

        List<String> queryColumns = run.getPrepare().getQueryColumns();
        if(null == queryColumns || queryColumns.size() ==0){
            ConfigStore configs = run.getConfigStore();
            if(null != configs) {
                queryColumns = configs.columns();
            }
        }
        if(null != queryColumns && queryColumns.size()>0){
            run.setQueryColumns(queryColumns);
        }
    }
    private Bson parseCondition(Bson bson, Condition condition){
        if(condition instanceof ConditionChain){
            ConditionChain chain = (ConditionChain)condition;
            bson = parseCondition(bson, chain);
        }else{
            if(condition instanceof AutoCondition){
                AutoCondition auto = (AutoCondition)condition;
                //List<RunValue> values = condition.getRunValues();
                List<Object> values = auto.getValues();
                String column = condition.getId();
                String join = condition.getJoin();
                Bson create = bson(auto.getCompare(), column, values);
                if(null != create) {
                    if ("or".equalsIgnoreCase(join)) {
                        bson = Filters.or(create);
                    } else {
                        bson = Filters.and(create);
                    }
                }
            }
        }
        return bson;
    }

    private Bson parseCondition(Bson bson, ConditionChain chain){
        String join = chain.getJoin();
        Bson child = null;
        List<Condition> conditions = chain.getConditions();
        for(Condition con:conditions){
            child = parseCondition(child, con);
        }
        if(null == bson){
            bson = child;
        }else {
            if ("or".equalsIgnoreCase(join)) {
                bson = Filters.or(bson, child);
            } else {
                bson = Filters.and(bson, child);
            }
        }
        return bson;
    }
    private Bson bson(Compare compare, String column, List<Object> values){
        Bson bson = null;
        if(null != values && !values.isEmpty()) {
            Object value = null;
            boolean multiple = compare.isMultipleValue();
            if (multiple) {
                List<Object> list = new ArrayList<>();
                /*for (RunValue rv : values) {
                    list.add(rv.getValue());
                }*/
                list.addAll(values);
                value = list;
            } else {
                value = values.get(0);
            }
            int cc = compare.getCode();
            if(cc == 10){                                           //  EQUAL
                bson = Filters.eq(column, value);
            }else if(cc == 50 || cc == 999){                        //  LIKE Compare.REGEX
                bson = Filters.regex(column, value.toString());
            }else if(cc == 51){                                     //  START_WITH
                bson = Filters.regex(column, "^"+value);
            }else if(cc == 52){                                     //  END_WITH
                bson = Filters.regex(column, value+"$");
            }else if(cc == 20){                                     //  GREAT
                bson = Filters.gt(column, value);
            }else if(cc == 21){                                     //  GREAT_EQUAL
                bson = Filters.gte(column, value);
            }else if(cc == 30){                                     //  LESS
                bson = Filters.lt(column, value);
            }else if(cc == 31){                                     //  LESS_EQUAL
                bson = Filters.lte(column, value);
            }else if(cc == 40){                                     //  IN
                bson = Filters.in(column, BeanUtil.list2array(values));
            }else if(cc == 80){                                     //  BETWEEN
                if(values.size() > 1){
                    bson = Filters.and(Filters.gte(column, values.get(0)), Filters.lte(column, values.get(1)));
                }
            }else if(cc == 110){                                    //  NOT EQUAL
                bson = Filters.ne(column, value);
            }else if(cc == 140){                                    //  NOT IN
                bson = Filters.nin(column, BeanUtil.list2array(values));
            }else if(cc == 150){                                    //  NOT LIKE
            }else if(cc == 151){                                     //  NOT START_WITH
            }else if(cc == 152){                                     //  NOT END_WITH
            }

        }
        return bson;
    }
    @Override
    public DataSet select(DataRuntime runtime, String random, boolean system, String table, ConfigStore configs, Run run) {
        long fr = System.currentTimeMillis();
        if(null == random){
            random = random(runtime);
        }
        DataSet set = new DataSet();
        //根据这一步中的JDBC结果集检测类型不准确,如:实际POINT 返回 GEOMETRY 如果要求准确 需要开启到自动检测
        //在DataRow中 如果检测到准确类型 JSON XML POINT 等 返回相应的类型,不返回byte[]（所以需要开启自动检测）
        //Entity中 JSON XML POINT 等根据属性类型返回相应的类型（所以不需要开启自动检测）
        // LinkedHashMap<String,Column> columns = new LinkedHashMap<>();

        if(!system && ThreadConfig.check(runtime.getKey()).IS_AUTO_CHECK_METADATA() && null != table){
            // columns = columns(runtime, false, new Table(null, null, table), null);
        }
        try{
            MongoRuntime rt = (MongoRuntime) runtime;
            MongoDatabase database = rt.getDatabase();
            Bson bson = (Bson)run.getFilter();
            if(null == bson){
                bson = Filters.empty();
            }
            if(ConfigTable.IS_SHOW_SQL && log.isInfoEnabled()){
                log.info("{}[cmd:select][collection:{}][filter:{}]", random, run.getTable(), bson);
            }
            FindIterable<MongoDataRow> rows = database.getCollection(run.getTable(), ConfigTable.DEFAULT_MONGO_ENTITY_CLASS).find(bson);
            List<Bson> fields = new ArrayList<>();
            List<String> queryColumns = run.getQueryColumns();
            //查询的列
            if(null != queryColumns && queryColumns.size()>0){
                fields.add(Projections.include(queryColumns));
            }
            //不查询的列
            List<String> excludeColumn = run.getExcludeColumns();
            if(null != excludeColumn && excludeColumn.size()>0){
                fields.add(Projections.exclude(excludeColumn));
            }
            if(fields.size() > 0){
                rows.projection(Projections.fields(fields));
            }
            PageNavi navi = run.getPageNavi();
            if(null != navi){
                rows.skip((int)navi.getFirstRow()).limit(navi.getPageRows());
            }
            for(MongoDataRow row:rows){
                set.add(row);
            }
            if(ConfigTable.IS_SHOW_SQL && log.isInfoEnabled()){
                log.info("{}[封装耗时:{}ms][封装行数:{}]", random, System.currentTimeMillis() - fr, set.size());
            }
        }catch(Exception e){
            if(ConfigTable.IS_PRINT_EXCEPTION_STACK_TRACE) {
                e.printStackTrace();
            }
            if(ConfigTable.IS_THROW_SQL_QUERY_EXCEPTION){
                SQLQueryException ex = new SQLQueryException("query异常:"+e,e);
                throw ex;
            }else{
                if(ConfigTable.IS_SHOW_SQL_WHEN_ERROR){
                    log.error("{}[{}][cmd:select][collection:{}]", random, LogUtil.format("查询异常:", 33)+e.toString(), run.getTable());
                }
            }
        }
        return set;
    }

    @Override
    public long count(DataRuntime runtime, String random, RunPrepare prepare, ConfigStore configs, String... conditions) {
        return 0;
    }

    @Override
    public long count(DataRuntime runtime, String random, Run run) {
        MongoRuntime rt = (MongoRuntime) runtime;
        MongoDatabase database = rt.getDatabase();
        Bson bson = (Bson)run.getFilter();
        if(null == bson){
            bson = Filters.empty();
        }
        if(ConfigTable.IS_SHOW_SQL && log.isInfoEnabled()){
            log.info("{}[cmd:select][collection:{}][filter:{}]", random, run.getTable(), bson);
        }
        return database.getCollection(run.getTable()).countDocuments(bson);
    }

    @Override
    public List<Map<String, Object>> maps(DataRuntime runtime, String random, RunPrepare prepare, ConfigStore configs, String... conditions) {
        return null;
    }

    @Override
    public List<Map<String, Object>> maps(DataRuntime runtime, String random, ConfigStore configs, Run run) {
        return null;
    }

    @Override
    public Map<String, Object> map(DataRuntime runtime, String random, Run run) {
        return null;
    }

    @Override
    public DataRow sequence(DataRuntime runtime, String random, boolean next, String... names) {
        return null;
    }

    @Override
    public long execute(DataRuntime runtime, String random, RunPrepare prepare, ConfigStore configs, String... conditions) {
        return 0;
    }

    @Override
    public long execute(DataRuntime runtime, String random, int batch, String sql, List<Object> values) {
        return 0;
    }


    @Override
    public long update(DataRuntime runtime, String random, String dest, Object data, Run run) {
        long result = -1;
        ACTION.SWITCH swt = ACTION.SWITCH.CONTINUE;
        long fr = System.currentTimeMillis();
        log.info("{}[action:update][collection:{}][update:{}][filter:{}]", random, run.getTable(), run.getUpdate(),run.getFilter());
        MongoRuntime rt = (MongoRuntime) runtime;
        MongoDatabase database = rt.getDatabase();
        MongoCollection cons = database.getCollection(run.getTable());
        UpdateResult dr = cons.updateMany((Bson)run.getFilter(), (Bson)run.getUpdate());
        result = dr.getMatchedCount();
        long millis = System.currentTimeMillis() - fr;
        if (ConfigTable.IS_SHOW_SQL && log.isInfoEnabled()) {
            log.info("{}[action:update][执行耗时:{}ms][影响行数:{}]", random, millis, LogUtil.format(result, 34));
        }
        return result;
    }

    @Override
    public long execute(DataRuntime runtime, String random, Run run) {
        return 0;
    }

    @Override
    public boolean execute(DataRuntime runtime, String random, Procedure procedure) {
        return false;
    }

    @Override
    public <T> long deletes(DataRuntime runtime, String random, int batch, String table, String key, Collection<T> values) {
        ConfigStore configs = new DefaultConfigStore();
        configs.and(key, values);
        return delete(runtime, random, table, configs);
    }

    @Override
    public long delete(DataRuntime runtime, String random, String dest, Object obj, String... columns) {
        dest = DataSourceUtil.parseDataSource(dest,obj);
        ACTION.SWITCH swt = ACTION.SWITCH.CONTINUE;
        long size = 0;
        if(null != obj){
            if(obj instanceof Collection){
                Collection list = (Collection) obj;
                for(Object item:list){
                    long qty = delete(runtime, random, dest, item, columns);
                    //如果不执行会返回-1
                    if(qty > 0){
                        size += qty;
                    }
                }
                if(log.isInfoEnabled()) {
                    log.info("[delete Collection][影响行数:{}]", LogUtil.format(size, 34));
                }
            }else{
                swt = InterceptorProxy.prepareDelete(runtime, random, 0, dest, obj, columns);
                if(swt == ACTION.SWITCH.BREAK){
                    return -1;
                }
                if(null != dmListener){
                    swt = dmListener.prepareDelete(runtime, random, 0, dest, obj, columns);
                }
                if(swt == ACTION.SWITCH.BREAK){
                    return -1;
                }
                Run run = buildDeleteRun(runtime, dest, obj, columns);
                if(!run.isValid()){
                    if(ConfigTable.IS_SHOW_SQL && log.isWarnEnabled()){
                        log.warn("[valid:false][不具备执行条件][dest:" + dest + "][thread:" + Thread.currentThread().getId() + "][ds:" + runtime.datasource() + "]");
                    }
                    return -1;
                }
                size = delete(runtime, random,  run);
            }
        }
        return size;
    }

    @Override
    public long delete(DataRuntime runtime, String random, String table, ConfigStore configs, String... conditions) {
        table = DataSourceUtil.parseDataSource(table, null);
        ACTION.SWITCH swt = ACTION.SWITCH.CONTINUE;
        swt = InterceptorProxy.prepareDelete(runtime, random, 0, table, configs, conditions);
        if(swt == ACTION.SWITCH.BREAK){
            return -1;
        }
        if(null != dmListener){
            swt = dmListener.prepareDelete(runtime,random, 0, table, configs, conditions);
        }
        if(swt == ACTION.SWITCH.BREAK){
            return -1;
        }
        Run run = buildDeleteRun(runtime, table, configs, conditions);
        if(!run.isValid()){
            if(ConfigTable.IS_SHOW_SQL && log.isWarnEnabled()){
                log.warn("[valid:false][不具备执行条件][table:" + table + "][thread:" + Thread.currentThread().getId() + "][ds:" + runtime.datasource() + "]");
            }
            return -1;
        }
        long result = delete(runtime, random, run);
        return result;
    }

    @Override
    public int truncate(DataRuntime runtime, String random, String table) {
        return 0;
    }

    @Override
    public DataSet querys(DataRuntime runtime, String random, Procedure procedure, PageNavi navi) {
        return null;
    }

    @Override
    public DataSet querys(DataRuntime runtime, String random, RunPrepare prepare, ConfigStore configs, String... conditions) {
        return super.querys(runtime, random, prepare, configs, conditions);
    }

    @Override
    public <T> EntitySet<T> selects(DataRuntime runtime, String random, RunPrepare prepare, Class<T> clazz, ConfigStore configs, String... conditions) {
        return null;
    }


    @Override
    public long insert(DataRuntime runtime, String random, Object data, Run run, String[] pks, boolean simple) {
        return 0;
    }



    /**
     * UPDATE [入口]
     * @param runtime 运行环境主要包含驱动适配器 数据源或客户端
     * @param random 用来标记同一组命令
     * @param dest 表
     * @param data 数据
     * @param configs 条件
     * @param columns 列
     * @return 影响行数
     */
    @Override
    public long update(DataRuntime runtime, String random, int batch, String dest, Object data, ConfigStore configs, List<String> columns){
        return super.update(runtime, random, batch, dest, data, configs, columns);
    }

    @Override
    public Run buildUpdateRunFromEntity(DataRuntime runtime, String dest, Object obj, ConfigStore configs, boolean checkPrimary, LinkedHashMap<String, Column> columns){
        TableRun run = new TableRun(runtime, dest);
        run.setFrom(2);
        LinkedHashMap<String,Column> cols = new LinkedHashMap<>();
        List<String> primaryKeys = new ArrayList<>();
        if(null != columns && columns.size() >0 ){
            cols = columns;
        }else{
            cols.putAll(EntityAdapterProxy.columns(obj.getClass(), EntityAdapter.MODE.UPDATE)); ;
        }
        if(EntityAdapterProxy.hasAdapter(obj.getClass())){
            primaryKeys.addAll(EntityAdapterProxy.primaryKeys(obj.getClass()).keySet());
        }else{
            primaryKeys = new ArrayList<>();
            primaryKeys.add("_id");
        }

        // 不更新主键 除非显示指定
        for(String pk:primaryKeys){
            if(!columns.containsKey(pk.toUpperCase())) {
                cols.remove(pk.toUpperCase());
            }
        }
        //不更新默认主键  除非显示指定
        if(!columns.containsKey("_ID")) {
            cols.remove("_ID");
        }
        boolean isReplaceEmptyNull = ConfigTable.IS_REPLACE_EMPTY_NULL;
        cols = checkMetadata(runtime, dest, cols);
        List<Bson> updates = new ArrayList<>();
        /*构造SQL*/
        if(!cols.isEmpty()){
            for(Column column:cols.values()){
                String key = column.getName();
                Object value = null;
                if(EntityAdapterProxy.hasAdapter(obj.getClass())){
                    Field field = EntityAdapterProxy.field(obj.getClass(), key);
                    value = BeanUtil.getFieldValue(obj, field);
                }else {
                    value = BeanUtil.getFieldValue(obj, key);
                }
                if(null != value && value.toString().startsWith("${") && value.toString().endsWith("}")){
                    String str = value.toString();
                    value = str.substring(2, str.length()-1);
                }else{
                    if("NULL".equals(value)){
                        value = null;
                    }else if("".equals(value) && isReplaceEmptyNull){
                        value = null;
                    }
                    boolean chk = true;
                    if(null == value){
                        if(!ConfigTable.IS_UPDATE_NULL_FIELD){
                            chk = false;
                        }
                    }else if("".equals(value)){
                        if(!ConfigTable.IS_UPDATE_EMPTY_FIELD){
                            chk = false;
                        }
                    }
                    if(chk){
                        updates.add(Updates.set(key, value));
                    }
                }
            }
            run.setUpdate(Updates.combine(updates));

            if(null == configs) {
                configs = new DefaultConfigStore();
                for (String pk : primaryKeys) {
                    if (EntityAdapterProxy.hasAdapter(obj.getClass())) {
                        Field field = EntityAdapterProxy.field(obj.getClass(), pk);
                        configs.and(pk, BeanUtil.getFieldValue(obj, field));
                    } else {
                        configs.and(pk, BeanUtil.getFieldValue(obj, pk));
                    }
                }
            }
            run.setConfigStore(configs);
            run.init();
            run.appendCondition();
        }
        run.setFilter(parseCondition(null, run.getConditionChain()));
        return run;
    }

    @Override
    public Run buildUpdateRunFromDataRow(DataRuntime runtime, String dest, DataRow row, ConfigStore configs, boolean checkPrimary, LinkedHashMap<String,Column> columns){
        TableRun run = new TableRun(runtime, dest);
        run.setFrom(1);
        /*确定需要更新的列*/
        LinkedHashMap<String, Column> cols = confirmUpdateColumns(runtime, dest, row, configs, BeanUtil.getMapKeys(columns));
        List<String> primaryKeys = row.getPrimaryKeys();
        if(primaryKeys.size() == 0){
            throw new SQLUpdateException("[更新更新异常][更新条件为空,update方法不支持更新整表操作]");
        }

        // 不更新主键 除非显示指定
        for(String pk:primaryKeys){
            if(!columns.containsKey(pk.toUpperCase())) {
                cols.remove(pk.toUpperCase());
            }
        }
        //不更新默认主键  除非显示指定
        if(!columns.containsKey("_ID")) {
            cols.remove("_ID");
        }

        boolean replaceEmptyNull = row.isReplaceEmptyNull();

        List<Bson> updates = new ArrayList<>();
        if(!cols.isEmpty()){
            for(Column col:cols.values()){
                String key = col.getName();
                Object value = row.get(key);
                if(null != value && value.toString().startsWith("${") && value.toString().endsWith("}") ){
                    String str = value.toString();
                    value = str.substring(2, str.length()-1);
                 }else{
                     if("NULL".equals(value)){
                        value = null;
                    }else if("".equals(value) && replaceEmptyNull){
                        value = null;
                    }
                }
                updates.add(Updates.set(key, value));
            }
            run.setUpdate(Updates.combine(updates));
            if(null == configs) {
                configs = new DefaultConfigStore();
                for (String pk : primaryKeys) {
                    configs.and(pk, row.get(pk));
                }
            }
            run.setConfigStore(configs);
            run.init();
            run.appendCondition();
            run.setFilter(parseCondition(null, run.getConditionChain()));
        }

        return run;
    }

    @Override
    public Run buildUpdateRunFromCollection(DataRuntime runtime, int batch, String dest, Collection list, ConfigStore configs, boolean checkPrimary, LinkedHashMap<String, Column> columns) {
        return null;
    }

    @Override
    public String mergeFinalQuery(DataRuntime runtime, Run run) {
        return null;
    }

    @Override
    public Object createConditionLike(DataRuntime runtime, StringBuilder builder, Compare compare, Object value) {
        return null;
    }

    @Override
    public Object createConditionFindInSet(DataRuntime runtime, StringBuilder builder, String column, Compare compare, Object value) {
        return null;
    }

    @Override
    public StringBuilder createConditionIn(DataRuntime runtime, StringBuilder builder, Compare compare, Object value) {
        return null;
    }


    @Override
    public Run buildDeleteRunFromTable(DataRuntime runtime, int batch, String table, String key, Object values) {
        if(null == key || null == values){
            return null;
        }
        ConfigStore configs = new DefaultConfigStore();
        if(values instanceof Collection){
            Collection collection = (Collection)values;
            if(collection.isEmpty()){
                return null;
            }
            configs.in(key, collection);
        }else{
            configs.and(key, values);
        }
        return buildDeleteRun(runtime, table, configs);
    }

    @Override
    public Run buildDeleteRunFromEntity(DataRuntime runtime, String dest, Object obj, String... columns) {
        if(null == obj){
            return null;
        }
        if(null == columns || columns.length == 0){
           columns = new String[]{"_id"};
        }
        ConfigStore configs = new DefaultConfigStore();
        for(String column:columns){
            configs.and(column, BeanUtil.getFieldValue(obj, column));
        }
        return buildDeleteRun(runtime, dest, configs);
    }

    /* *****************************************************************************************************************
     * 													DELETE
     * -----------------------------------------------------------------------------------------------------------------
     * Run buildDeleteRun(DataRuntime runtime, String table, String key, Object values)
     * Run buildDeleteRun(DataRuntime runtime, String dest, Object obj, String ... columns)
     * Run fillDeleteRunContent(DataRuntime runtime, Run run)
     *
     * protected Run buildDeleteRunFromTable(String table, String key, Object values)
     * protected Run buildDeleteRunFromEntity(String dest, Object obj, String ... columns)
     ******************************************************************************************************************/
    @Override
    public Run buildDeleteRun(DataRuntime runtime, int batch, String table, String key, Object values){
        return buildDeleteRunFromTable(runtime, batch, table, key, values);
    }
    @Override
    public Run buildDeleteRun(DataRuntime runtime, String dest, Object obj, String ... columns){
        if(null == obj){
            return null;
        }
        Run run = null;
        if(null == dest){
            dest = DataSourceUtil.parseDataSource(dest,obj);
        }
        if(null == dest){
            Object entity = obj;
            if(obj instanceof Collection){
                entity = ((Collection)obj).iterator().next();
            }
            Table table = EntityAdapterProxy.table(entity.getClass());
            if(null != table){
                dest = table.getName();
            }
        }
        if(obj instanceof ConfigStore){
            run = new TableRun(runtime,dest);
            RunPrepare prepare = new DefaultTablePrepare();
            prepare.setDataSource(dest);
            run.setPrepare(prepare);
            run.setConfigStore((ConfigStore)obj);
            run.addCondition(columns);
            run.init();
            fillDeleteRunContent(runtime, run);
        }else{
            run = buildDeleteRunFromEntity(runtime, dest, obj, columns);
        }
        return run;
    }


    @Override
    public List<Run> buildTruncateRun(DataRuntime runtime, String table){
        List<Run> runs = new ArrayList<>();
        Run run = new SimpleRun();
        runs.add(run);
        StringBuilder builder = run.getBuilder();
        builder.append("TRUNCATE TABLE ");
        SQLUtil.delimiter(builder, table, delimiterFr, delimiterTo);
        return runs;
    }
    /**
     * 构造删除主体
     * @param run 最终待执行的命令和参数(如果是JDBC环境就是SQL)
     * @return Run 最终执行命令 如果是JDBC类型库 会包含 SQL 与 参数值
     */
    @Override
    public void fillDeleteRunContent(DataRuntime runtime, Run run){
        if(null != run){
            if(run instanceof TableRun){
                TableRun r = (TableRun) run;
                fillDeleteRunContent(runtime, r);
            }
        }
    }

    protected void fillDeleteRunContent(DataRuntime runtime, TableRun run){
        Bson bson = null;
        ConditionChain chain = run.getConditionChain();
        bson = parseCondition(bson, chain);
        run.setFilter(bson);
    }
    /**
     * 执行删除
     * @param runtime DataRuntime
     * @param run 最终待执行的命令和参数(如果是JDBC环境就是SQL)
     * @return int
     */
    public long delete(DataRuntime runtime, String random, Run run){
        long result = -1;
        boolean cmd_success = false;
        ACTION.SWITCH swt = ACTION.SWITCH.CONTINUE;
        long fr = System.currentTimeMillis();
        swt = InterceptorProxy.beforeDelete(runtime, random, run);
        if(swt == ACTION.SWITCH.BREAK){
            return -1;
        }
        if(null != dmListener){
            swt = dmListener.beforeDelete(runtime, random, run);
        }
        if(swt == ACTION.SWITCH.BREAK){
            return -1;
        }
        log.info("{}[action:delete][collection:{}][filter:{}]", random, run.getTable(), run.getFilter());
        MongoRuntime rt = (MongoRuntime) runtime;
        MongoDatabase database = rt.getDatabase();
        MongoCollection cons = database.getCollection(run.getTable());
        DeleteResult dr = cons.deleteMany((Bson)run.getFilter());
        result = dr.getDeletedCount();
        cmd_success = true;
        long millis = System.currentTimeMillis() - fr;
        if (ConfigTable.IS_SHOW_SQL && log.isInfoEnabled()) {
            log.info("{}[action:delete][执行耗时:{}ms][影响行数:{}]", random, millis, LogUtil.format(result, 34));
        }
        if(null != dmListener){
            dmListener.afterDelete(runtime, random, run, cmd_success, result, millis);
        }
        InterceptorProxy.afterDelete(runtime, random, run,  cmd_success, result, millis);
        return result;
    }

    @Override
    public void checkSchema(DataRuntime runtime, DataSource dataSource, Table table) {

    }

    @Override
    public void checkSchema(DataRuntime runtime, Connection con, Table table) {

    }

    @Override
    public void checkSchema(DataRuntime runtime, Table table) {

    }

    @Override
    public LinkedHashMap<String, Database> databases(DataRuntime runtime, String random) {
        return null;
    }

    @Override
    public Database database(DataRuntime runtime, String random, String name) {
        return null;
    }

    @Override
    public <T extends Table> List<T> tables(DataRuntime runtime, String random, boolean greedy, String catalog, String schema, String pattern, String types) {
        return null;
    }

    @Override
    public <T extends Table> LinkedHashMap<String, T> tables(DataRuntime runtime, String random, String catalog, String schema, String pattern, String types) {
        return null;
    }

    @Override
    public List<String> ddl(DataRuntime runtime, String random, Table table, boolean init) {
        return null;
    }

    @Override
    public <T extends View> LinkedHashMap<String, T> views(DataRuntime runtime, String random, boolean greedy, String catalog, String schema, String pattern, String types) {
        return null;
    }

    @Override
    public List<String> ddl(DataRuntime runtime, String random, View view) {
        return null;
    }

    @Override
    public <T extends MasterTable> LinkedHashMap<String, T> mtables(DataRuntime runtime, String random, boolean greedy, String catalog, String schema, String pattern, String types) {
        return null;
    }

    @Override
    public List<String> ddl(DataRuntime runtime, String random, MasterTable table) {
        return null;
    }

    @Override
    public <T extends PartitionTable> LinkedHashMap<String, T> ptables(DataRuntime runtime, String random, boolean greedy, MasterTable master, Map<String, Object> tags, String name) {
        return null;
    }

    @Override
    public List<String> ddl(DataRuntime runtime, String random, PartitionTable table) {
        return null;
    }

    @Override
    public <T extends Column> LinkedHashMap<String, T> columns(DataRuntime runtime, String random, boolean greedy, Table table, boolean primary) {
        return null;
    }

    @Override
    public <T extends Column> LinkedHashMap<String, T> columns(DataRuntime runtime, int index, boolean create, Table table, LinkedHashMap<String, T> columns, DataSet set) throws Exception {
        return null;
    }


    @Override
    public <T extends Column> LinkedHashMap<String, T> columns(DataRuntime runtime, boolean create, LinkedHashMap<String, T> columns, Table table, String pattern) throws Exception {
        return null;
    }


    @Override
    public <T extends Column> LinkedHashMap<String, T> columns(DataRuntime runtime, String random, boolean create, Table table, LinkedHashMap<String, T> columns, List<Run> runs) {
        return null;
    }

    @Override
    public Column column(DataRuntime runtime, Column column, ResultSetMetaData rsm, int index) {
        return null;
    }

    @Override
    public <T extends Tag> LinkedHashMap<String, T> tags(DataRuntime runtime, String random, boolean greedy, Table table) {
        return null;
    }

    @Override
    public PrimaryKey primary(DataRuntime runtime, String random, boolean greedy, Table table) {
        return null;
    }

    @Override
    public <T extends ForeignKey> LinkedHashMap<String, T> foreigns(DataRuntime runtime, String random, boolean greedy, Table table) {
        return null;
    }

    @Override
    public <T extends Index> LinkedHashMap<String, T> indexs(DataRuntime runtime, String random, boolean greedy, Table table, String name) {
        return null;
    }

    @Override
    public <T extends Trigger> LinkedHashMap<String, T> triggers(DataRuntime runtime, String random, boolean greedy, Table table, List<Trigger.EVENT> events) {
        return null;
    }

    @Override
    public <T extends Procedure> LinkedHashMap<String, T> procedures(DataRuntime runtime, String random, boolean greedy, String catalog, String schema, String name) {
        return null;
    }

    @Override
    public <T extends Function> LinkedHashMap<String, T> functions(DataRuntime runtime, String random, boolean recover, String catalog, String schema, String name) {
        return null;
    }

    @Override
    public String concat(DataRuntime runtime, String... args) {
        return null;
    }
}
