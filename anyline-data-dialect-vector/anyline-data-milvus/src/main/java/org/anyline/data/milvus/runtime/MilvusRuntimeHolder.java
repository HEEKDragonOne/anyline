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

package org.anyline.data.milvus.runtime;

import io.milvus.v2.client.MilvusClientV2;
import org.anyline.bean.BeanDefine;
import org.anyline.bean.init.DefaultBeanDefine;
import org.anyline.dao.init.DefaultDao;
import org.anyline.data.adapter.DriverAdapter;
import org.anyline.data.adapter.DriverAdapterHolder;
import org.anyline.data.datasource.DataSourceMonitor;
import org.anyline.data.milvus.adapter.MilvusAdapter;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.data.runtime.RuntimeHolder;
import org.anyline.data.runtime.init.AbstractRuntimeHolder;
import org.anyline.service.init.DefaultService;
import org.anyline.util.ConfigTable;

import java.util.HashMap;
import java.util.Map;

public class MilvusRuntimeHolder extends AbstractRuntimeHolder {
    /**
     * 临时数据源
     */

    private static Map<String, MilvusClientV2> temporary = new HashMap<>();

    private static final MilvusRuntimeHolder instance = new MilvusRuntimeHolder();
    public MilvusRuntimeHolder() {
    }
    public static MilvusRuntimeHolder instance() {
        return instance;
    }

	/**
     * 注册数据源 子类覆盖 生成简单的DataRuntime不注册到spring
     * @param client 数据源, 如DruidDataSource, MongoClient, es.MilvusClientV2
     * @param database 数据库, jdbc类型数据源不需要
     * @param adapter 如果确认数据库类型可以提供如 new MySQLAdapter(), 如果不提供则根据ds检测
     * @return DataRuntime
     * @throws Exception 异常 Exception
     */

    public DataRuntime temporary(Object client, String database, DriverAdapter adapter) throws Exception {
        MilvusRuntime runtime = new MilvusRuntime();
        if(null == adapter) {
            adapter = ConfigTable.environment().getBean(MilvusAdapter.class);
        }
        if(client instanceof MilvusClientV2) {
            String key = "temporary_es";
            temporary.remove(key);
            //DriverAdapterHolder.remove(key);
            //创建新数据源
            runtime.setKey(key);
            runtime.setAdapter(adapter);
            runtime.setProcessor(client);
            temporary.put(key, (MilvusClientV2)client);
            log.warn("[创建临时数据源][key:{}][type:{}]", key, client.getClass().getSimpleName());
        }else{
            throw new Exception("请提供MilvusClientV2兼容类型");
        }
        //runtime.setHolder(this);
        return runtime;
    }
    public DataRuntime reg(String key, MilvusClientV2 client) {
        String datasource_key = DataRuntime.ANYLINE_DATASOURCE_BEAN_PREFIX + key;
        log.info("[注入数据源][type:milvus][key:{}][bean:{}]", key, datasource_key);
        ConfigTable.environment().regBean(datasource_key, client);
        return reg(key, client, null);
    }

	/**
     * 注册运行环境
     * @param datasource 数据源前缀
     * @param client MilvusClientV2
     * @param adapter adapter 可以为空 第一次执行时补齐
     */

    public MilvusRuntime reg(String datasource, MilvusClientV2 client, DriverAdapter adapter) {
        log.debug("[create milvus runtime][key:{}]", datasource);
        if(null == adapter) {
            adapter = ConfigTable.environment().getBean(MilvusAdapter.class);
        }
        MilvusRuntime runtime = new MilvusRuntime(datasource, client, adapter);
        if(runtimes.containsKey(datasource)) {
            destroy(datasource);
        }
        runtimes.put(datasource, runtime);

        String dao_key = DataRuntime.ANYLINE_DAO_BEAN_PREFIX +  datasource;
        String service_key = DataRuntime.ANYLINE_SERVICE_BEAN_PREFIX +  datasource;
        log.info("[instance service][data source:{}][instance id:{}]", datasource, service_key);

        BeanDefine daoDefine = new DefaultBeanDefine(DefaultDao.class);
        daoDefine.addValue("runtime", runtime);
        ConfigTable.environment().regBean(dao_key, daoDefine);

        BeanDefine serviceDefine = new DefaultBeanDefine(DefaultService.class);
        serviceDefine.addReferenceValue("dao", dao_key);
        ConfigTable.environment().regBean(service_key, serviceDefine);
        return runtime;

    }

    public boolean destroy(String key) {

        int close = 0;
        DataSourceMonitor monitor = DriverAdapterHolder.getMonitor();
        if(null != monitor){
            MilvusRuntime runtime = (MilvusRuntime) runtimes.get(key);
            if(null != runtime){
                //这一步有可能抛出 异常
                close = monitor.destroy(runtime, key, runtime.getProcessor());
            }
        }
        try {
            runtimes.remove(key);
            ConfigTable.environment().destroyBean(DataRuntime.ANYLINE_SERVICE_BEAN_PREFIX +  key);
            ConfigTable.environment().destroyBean(DataRuntime.ANYLINE_DAO_BEAN_PREFIX +  key);
            ConfigTable.environment().destroyBean(DataRuntime.ANYLINE_TRANSACTION_BEAN_PREFIX +  key);

            ConfigTable.environment().destroyBean(DataRuntime.ANYLINE_DATASOURCE_BEAN_PREFIX + key);
            log.warn("[注销数据源及相关资源][key:{}]", key);
            //从当前数据源复制的 子源一块注销
            Map<String, DataRuntime> runtimes = RuntimeHolder.runtimes(key);
            for(String item:runtimes.keySet()) {
                destroy(item);
            }
        }catch (Exception e) {
            return false;
        }
        return true;
    }

}
