/*
 * Copyright 2006-2022 www.anyline.org
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
package org.anyline.entity;

import com.fasterxml.jackson.databind.JsonNode;
import ognl.Ognl;
import ognl.OgnlContext;
import org.anyline.entity.adapter.KeyAdapter;
import org.anyline.entity.adapter.KeyAdapter.KEY_CASE;
import org.anyline.entity.adapter.LowerKeyAdapter;
import org.anyline.entity.adapter.SrcKeyAdapter;
import org.anyline.entity.adapter.UpperKeyAdapter;
import org.anyline.util.*;
import org.dom4j.Attribute;
import org.dom4j.Document;
import org.dom4j.DocumentHelper;
import org.dom4j.Element;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;

import java.io.Serializable;
import java.lang.reflect.Field;
import java.lang.reflect.Modifier;
import java.math.BigDecimal;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.time.ZonedDateTime;
import java.util.*;

public class DataRow extends LinkedHashMap<String, Object> implements Serializable {
    private static final long serialVersionUID = -2098827041540802313L;
    protected static final Logger log = LoggerFactory.getLogger(DataRow.class);

    private static EntityAdapter adapter;
    private static boolean is_adapter_load = false;

    @Autowired(required = false)
    @Qualifier("anyline.entity.adapter")
    public void setAdapter(EntityAdapter adapter) {
        DataRow.adapter = adapter;
    }

    private static EntityAdapter getAdapter() {
        if (null != adapter) {
            return adapter;
        }
        if (!is_adapter_load) {
            try {
                adapter = (EntityAdapter) SpringContextUtil.getBean("anyline.entity.adapter");
            } catch (Exception e) {
            }
            is_adapter_load = true;
        }
        return adapter;
    }

    public static String PARENT = "PARENT"; //上级数据
    public static String ALL_PARENT = "ALL_PARENT"; //所有上级数据
    public static String CHILDREN = "CHILDREN"; //子数据
    public static String DEFAULT_PRIMARY_KEY = ConfigTable.getString("DEFAULT_PRIMARY_KEY", "ID");
    public static String ITEMS = "ITEMS";
    public static KEY_CASE DEFAULT_KEY_KASE = KEY_CASE.CONFIG;
    private DataSet container = null; //包含当前对象的容器

    private List<String> primaryKeys = new ArrayList<>(); //主键
    private List<String> updateColumns = new ArrayList<>();
    private List<String> ignoreUpdateColumns = new ArrayList<>();
    private String datalink = null;
    private String dataSource = null; //数据源(表|视图|XML定义SQL)
    private String schema = null;
    private String table = null;
    private Map<String, Object> queryParams = new HashMap<>(); //查询条件
    private Map<String, Object> attributes = new HashMap<>(); //属性
    private Object clientTrace = null; //客户端数据
    private long createTime = 0; //创建时间
    private long expires = -1; //过期时间(毫秒) 从创建时刻计时expires毫秒后过期
    protected Boolean isNew = false; //强制新建(适应hibernate主键策略)
    protected boolean isFromCache = false; //是否来自缓存
    private boolean updateNullColumn = ConfigTable.getBoolean("IS_UPDATE_NULL_COLUMN", true);
    private boolean updateEmptyColumn = ConfigTable.getBoolean("IS_UPDATE_EMPTY_COLUMN", true);
    private Map<String, String> keymap = new HashMap<>(); //keymap
    private boolean isUpperKey = false; //是否已执行大写key转换(影响到驼峰执行)
    private Map<String, String> converts = new HashMap<>(); //key是否已转换<key,src><当前key,原key>
    public boolean skip = false; //遍历计算时标记

    private KeyAdapter keyAdapter = null;

    private KEY_CASE keyCase 				= DEFAULT_KEY_KASE				; //列名格式

    public DataRow() {
        parseKeycase(null);
        String pk = keyAdapter.key(DEFAULT_PRIMARY_KEY);
        if (null != pk) {
            primaryKeys.add(DEFAULT_PRIMARY_KEY);
        }
        createTime = System.currentTimeMillis();
    }

    private void parseKeycase(KEY_CASE keyCase) {
        if(null == keyCase){
            keyCase = this.keyCase;
        }
        if (null != keyCase) {
            keyAdapter = KeyAdapter.parse(keyCase);
            if(keyCase == KEY_CASE.CONFIG){
                if (ConfigTable.IS_UPPER_KEY) {
                    keyAdapter = UpperKeyAdapter.getInstance();
                } else if (ConfigTable.IS_LOWER_KEY) {
                    keyAdapter = LowerKeyAdapter.getInstance();
                }
            }
        } else {
            keyAdapter = SrcKeyAdapter.getInstance();
        }

    }

    public DataRow(KEY_CASE keyCase) {
        this();
        parseKeycase(keyCase);
    }

    public DataRow(String table) {
        this();
        this.setTable(table);
    }

    public DataRow(Map<String, Object> map) {
        this();
        Set<Map.Entry<String, Object>> set = map.entrySet();
        for (Map.Entry<String, Object> entity : set) {
            put(keyAdapter.key(entity.getKey()), entity.getValue());
        }
    }

    /**
     * 数组解析成DataSet
     * @param list 数组
     * @param fields 下标对应的属性(字段/key)名称,如果不输入则以下标作为DataRow的key,如果属性数量超出list长度,取null值存入DataRow
     * @return DataRow
     */
    public static DataRow parseList(Collection<?> list, String... fields) {
        DataRow row = new DataRow();
        if (null != list) {

            if (null == fields || fields.length == 0) {
                int i = 0;
                for (Object obj : list) {
                    row.put("" + i++, obj);
                }
            } else {
                Object[] items = list.toArray();
                int len = fields.length;
                for (int i = 0; i < len; i++) {
                    String field = fields[i];
                    Object value = null;
                    if (i < items.length - 1) {
                        value = items[i];
                    }
                    row.put(field, value);
                }
            }
        }
        return row;
    }

    public DataRow setKeyCase(KEY_CASE keyCase) {
        parseKeycase(keyCase);
        return this;
    }

    /**
     * 解析实体类对象
     * @param obj obj
     * @param keys 列名:obj属性名 "ID:memberId"
     * @return DataRow
     */
    @SuppressWarnings("rawtypes")
    public static DataRow parse(Object obj, String... keys) {
        DataRow result = null;
        if (null != getAdapter()) {
            result = adapter.parse(obj, keys);
            if (null != result) {
                return result;
            }
        }
        return parse(KEY_CASE.CONFIG, obj, keys);
    }

    public static DataRow build(Object obj, String... keys) {
        return parse(obj, keys);
    }

    public static DataRow parse(KEY_CASE keyCase, Object obj, String... keys) {
        Map<String, String> map = new HashMap<String, String>();
        if (null != keys) {
            for (String key : keys) {
                String tmp[] = key.split(":");
                if (null != tmp && tmp.length > 1) {
                    map.put(putKeyCase(tmp[1].trim()), putKeyCase(tmp[0].trim()));
                }
            }
        }
        DataRow row = new DataRow(keyCase);
        if (null != obj) {
            if (obj instanceof JsonNode) {
                row = parseJson(keyCase, (JsonNode) obj);
            } else if (obj instanceof DataRow) {
                row = (DataRow) obj;
            } else if (obj instanceof Map) {
                Map mp = (Map) obj;
                List<String> ks = BeanUtil.getMapKeys(mp);
                for (String k : ks) {
                    Object value = mp.get(k);
                    if (null != value && value instanceof Map) {
                        value = parse(value);
                    }
                    row.put(k, value);
                }
            } else {
                List<Field> fields = ClassUtil.getFields(obj.getClass());
                for (Field field : fields) {
                    if (Modifier.isStatic(field.getModifiers())) {
                        continue;
                    }
                    String fieldName = field.getName();
                    String col = map.get(fieldName);
                    if (null == col) {
                        col = fieldName;
                    }
                    row.put(keyCase, col, BeanUtil.getFieldValue(obj, field));
                }
            }
        }
        return row;
    }

    public static DataRow build(KEY_CASE keyCase, Object obj, String... keys) {
        return parse(keyCase, obj, keys);
    }

    /*
     * 解析json结构字符
     * @param keyCase key大小写
     * @param json json
     * @return DataRow
     */
    public static DataRow parseJson(KEY_CASE keyCase, String json) {
        if (null != json) {
            try {
                return parseJson(keyCase, BeanUtil.JSON_MAPPER.readTree(json));
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static DataRow parseJson(String json) {
        return parseJson(KEY_CASE.CONFIG, json);
    }

    /**
     * 解析JSONObject
     * @param keyCase keyCase
     * @param json json
     * @return DataRow
     */
    public static DataRow parseJson(KEY_CASE keyCase, JsonNode json) {
        return (DataRow) parse(keyCase, json);
    }

    public static DataRow parseJson(JsonNode json) {
        return parseJson(KEY_CASE.CONFIG, json);
    }

    private static Object parse(KEY_CASE keyCase, JsonNode json) {
        if (null == json) {
            return null;
        }
        if (json.isValueNode()) {
            return BeanUtil.value(json);
        }
        if (json.isArray()) {
            List<Object> list = new ArrayList<Object>();
            Iterator<JsonNode> items = json.iterator();
            while (items.hasNext()) {
                JsonNode item = items.next();
                list.add(parse(keyCase, item));
            }
            return list;
        } else if (json.isObject()) {
            DataRow row = new DataRow(keyCase);
            Iterator<Map.Entry<String, JsonNode>> fields = json.fields();
            while (fields.hasNext()) {
                Map.Entry<String, JsonNode> field = fields.next();
                JsonNode value = field.getValue();
                String key = field.getKey();
                if (null != value) {
                    if (value.isValueNode()) {
                        row.put(key, BeanUtil.value(value));
                    } else if (value.isArray()) {
                        row.put(key, parse(keyCase, value));
                    } else if (value.isObject()) {
                        row.put(key, parseJson(keyCase, value));
                    }
                } else {
                    row.put(key, null);
                }

            }
            return row;
        }

        return null;
    }

    /**
     * 解析xml结构字符
     * @param keyCase KEY_CASE
     * @param xml xml
     * @return DataRow
     */
    public static DataRow parseXml(KEY_CASE keyCase, String xml) {
        if (null != xml) {
            try {
                Document doc = DocumentHelper.parseText(xml);
                return parseXml(keyCase, doc.getRootElement());
            } catch (Exception e) {
                e.printStackTrace();
            }
        }
        return null;
    }

    public static DataRow parseXml(String xml) {
        return parseXml(KEY_CASE.CONFIG, xml);
    }

    /**
     * 解析xml
     * @param keyCase KEY_CASE
     * @param element element
     * @return DataRow
     */
    public static DataRow parseXml(KEY_CASE keyCase, Element element) {
        DataRow row = new DataRow(keyCase);
        if (null == element) {
            return row;
        }
        Iterator<Element> childs = element.elementIterator();
        String key = element.getName();
        String namespace = element.getNamespacePrefix();
        if (BasicUtil.isNotEmpty(namespace)) {
            key = namespace + ":" + key;
        }
        if (element.isTextOnly() || !childs.hasNext()) {
            row.put(key, element.getTextTrim());
        } else {
            while (childs.hasNext()) {
                Element child = childs.next();
                String childKey = child.getName();
                String childNamespace = child.getNamespacePrefix();
                if (BasicUtil.isNotEmpty(childNamespace)) {
                    childKey = childNamespace + ":" + childKey;
                }
                if (child.isTextOnly() || !child.elementIterator().hasNext()) {
                    row.put(childKey, child.getTextTrim());
                    continue;
                }
                DataRow childRow = parseXml(keyCase, child);
                Object childStore = row.get(childKey);
                if (null == childStore) {
                    row.put(childKey, childRow);
                } else {
                    if (childStore instanceof DataRow) {
                        DataSet childSet = new DataSet();
                        childSet.add((DataRow) childStore);
                        childSet.add(childRow);
                        row.put(childKey, childSet);
                    } else if (childStore instanceof DataSet) {
                        ((DataSet) childStore).add(childRow);
                    }
                }
            }
        }
        Iterator<Attribute> attrs = element.attributeIterator();
        while (attrs.hasNext()) {
            Attribute attr = attrs.next();
            row.attr(attr.getName(), attr.getValue());
        }
        return row;
    }

    /**
     * 解析 key1,value1,key2,value2,key3:value3组合
     * @param kvs kvs
     * @return DataRow
     */
    public static DataRow parseArray(String... kvs) {
        DataRow result = new DataRow();
        int len = kvs.length;
        int i = 0;
        while (i < len) {
            String p1 = kvs[i];
            if (BasicUtil.isEmpty(p1)) {
                i++;
                continue;
            } else if (p1.contains(":")) {
                String ks[] = BeanUtil.parseKeyValue(p1);
                result.put(ks[0], ks[1]);
                i++;
                continue;
            } else {
                if (i + 1 < len) {
                    String p2 = kvs[i + 1];
                    if (BasicUtil.isEmpty(p2) || !p2.contains(":")) {
                        result.put(p1, p2);
                        i += 2;
                        continue;
                    } else {
                        String ks[] = BeanUtil.parseKeyValue(p2);
                        result.put(ks[0], ks[1]);
                        i += 2;
                        continue;
                    }
                }

            }
            i++;
        }
        return result;
    }

    /**
     * 创建时间
     * @return long
     */
    public long getCreateTime() {
        return createTime;
    }

    /**
     * 过期时间
     * @return long
     */
    public long getExpires() {
        return expires;
    }

    /**
     * 设置过期时间
     * @param millisecond millisecond
     * @return DataRow
     */
    public DataRow setExpires(long millisecond) {
        this.expires = millisecond;
        return this;
    }

    public DataRow setExpires(int millisecond) {
        this.expires = millisecond;
        return this;
    }

    /**
     * 合并数据
     * @param row  row
     * @param over key相同时是否覆盖原数据
     * @return DataRow
     */
    public DataRow merge(DataRow row, boolean over) {
        List<String> keys = row.keys();
        for (String key : keys) {
            if (over || null == this.get(key)) {
                this.put(key, row.get(KEY_CASE.SRC, key));
            }
        }
        return this;
    }

    public Object ognl(String formula, Object values) throws Exception {
        if (null == values) {
            values = this;
        }
        formula = BeanUtil.parseRuntimeValue(values, formula);
        OgnlContext context = new OgnlContext(null, null, new DefaultMemberAccess(true));
        Object value = Ognl.getValue(formula, context, values);
        return value;
    }

    public Object ognl(String formula) throws Exception {
        return ognl(formula, this);
    }

    public DataRow ognl(String key, String formula, Object values) throws Exception {
        put(key, ognl(key, formula, values));
        return this;
    }

    public DataRow ognl(String key, String formula) throws Exception {
        return ognl(key, formula, this);
    }

    public DataRow merge(DataRow row) {
        return merge(row, false);
    }

    /**
     * 是否是新数据
     * @return Boolean
     */
    public Boolean isNew() {
        String pk = getPrimaryKey();
        String pv = getString(pk);
        return (null == pv || (null == isNew) || isNew || BasicUtil.isEmpty(pv));
    }

    /**
     * 是否来自缓存
     * @return boolean
     */
    public boolean isFromCache() {
        return isFromCache;
    }

    /**
     * 设置是否来自缓存
     * @param bol bol
     * @return DataRow
     */
    public DataRow setIsFromCache(boolean bol) {
        this.isFromCache = bol;
        return this;
    }

    public String getCd() {
        return getString("cd");
    }

    public String getId() {
        return getString("id");
    }

    public String getCode() {
        return getString("code");
    }

    public String getNm() {
        return getString("nm");
    }

    public String getName() {
        return getString("name");
    }

    public String getTitle() {
        return getString("title");
    }

    /**
     * 默认子集
     * @return DataSet
     */
    public DataSet getItems() {
        Object items = get(ITEMS);
        if (items instanceof DataSet) {
            return (DataSet) items;
        }
        return null;
    }

    public DataRow putItems(Object obj) {
        put(ITEMS, obj);
        return this;
    }

    /**
     * key转换成小写
     * @param keys keys
     * @return DataRow
     */
    public DataRow toLowerKey(String... keys) {
        if (null != keys && keys.length > 0) {
            for (String key : keys) {
                Object value = get(key);
                remove(keyAdapter.key(key));
                key = key.toLowerCase();
                put(KEY_CASE.SRC, key, value);
            }
        } else {
            for (String key : keys()) {
                Object value = get(key);
                remove(keyAdapter.key(key));
                key = key.toLowerCase();
                put(KEY_CASE.SRC, key, value);
            }
        }
        parseKeycase(KEY_CASE.LOWER);
        return this;
    }

    /**
     * key转换成大写
     * @param keys keys
     * @return DataRow
     */
    public DataRow toUpperKey(String... keys) {
        if (null != keys && keys.length > 0) {
            for (String key : keys) {
                Object value = get(key);
                remove(keyAdapter.key(key));
                key = key.toUpperCase();
                put(KEY_CASE.SRC, key, value);
            }
        } else {
            for (String key : keys()) {
                Object value = get(key);
                remove(keyAdapter.key(key));
                key = key.toUpperCase();
                put(KEY_CASE.SRC, key, value);
            }
        }
        parseKeycase(KEY_CASE.UPPER);
        return this;
    }

    /**
     * 数字格式化
     * @param format format
     * @param cols cols
     * @return DataRow
     */
    public DataRow formatNumber(String format, String... cols) {
        if (null == cols || BasicUtil.isEmpty(format)) {
            return this;
        }
        for (String col : cols) {
            String value = getString(col);
            if (null != value) {
                value = NumberUtil.format(value, format);
                put(col, value);
            }
        }
        return this;
    }

    public DataRow numberFormat(String target, String key, String format) {
        if (null == target || null == key || isEmpty(key) || null == format) {
            return this;
        }
        put(target, NumberUtil.format(getString(key), format));
        return this;
    }

    /**
     * 日期格式化
     * @param format format
     * @param cols cols
     * @return DataRow
     */
    public DataRow formatDate(String format, String... cols) {
        if (null == cols || BasicUtil.isEmpty(format)) {
            return this;
        }
        for (String col : cols) {
            String value = getString(col);
            if (null != value) {
                value = DateUtil.format(value, format);
                put(col, value);
            }
        }
        return this;
    }

    public DataRow dateFormat(String target, String key, String format) {
        if (null == target || null == key || isEmpty(key) || null == format) {
            return this;
        }
        put(target, DateUtil.format(getString(key), format));
        return this;
    }

    public DataRow dateParse(String target, String key, String format) {
        if (null == target || null == key || isEmpty(key) || null == format) {
            return this;
        }
        put(target, DateUtil.parse(getString(key), format));
        return this;
    }

    public DataRow dateParse(String target, String key) {
        if (null == target || null == key || isEmpty(key)) {
            return this;
        }
        put(target, DateUtil.parse(getString(key)));
        return this;
    }

    public DataRow dateFormat(String key, String format) {
        return dateFormat(key, key, format);
    }

    /**
     * 指定列是否为空
     * @param key key
     * @return boolean
     */
    public boolean isNull(String key) {
        Object obj = get(key);
        return obj == null;
    }

    public boolean isNotNull(String key) {
        return !isNull(key);
    }

    public boolean isEmpty(String key) {
        Object obj = get(key);
        return BasicUtil.isEmpty(obj);
    }

    public boolean isNotEmpty(String key) {
        return !isEmpty(key);
    }

    /**
     * 添加主键
     * @param applyContainer 是否应用到上级容器 默认false
     * @param pks pks
     * @return DataRow
     */
    public DataRow addPrimaryKey(boolean applyContainer, String... pks) {
        if (null != pks) {
            List<String> list = new ArrayList<>();
            for (String pk : pks) {
                list.add(pk);
            }
            return addPrimaryKey(applyContainer, list);
        }
        return this;
    }

    public DataRow addPrimaryKey(String... pks) {
        return addPrimaryKey(false, pks);
    }

    public DataRow addPrimaryKey(boolean applyContainer, Collection<String> pks) {
        if (BasicUtil.isEmpty(pks)) {
            return this;
        }

        /*没有处于容器中时,设置自身主键*/
        if (null == primaryKeys) {
            primaryKeys = new ArrayList<>();
        }
        for (String item : pks) {
            if (BasicUtil.isEmpty(item)) {
                continue;
            }
            item = keyAdapter.key(item);
            if (!primaryKeys.contains(item)) {
                primaryKeys.add(item);
            }
        }
        /*设置容器主键*/
        if (hasContainer() && applyContainer) {
            getContainer().setPrimaryKey(false, primaryKeys);
        }
        return this;
    }

    public DataRow setPrimaryKey(boolean applyContainer, String... pks) {
        if (null != pks) {
            List<String> list = new ArrayList<>();
            for (String pk : pks) {
                list.add(pk);
            }
            return setPrimaryKey(applyContainer, list);
        }
        return this;
    }

    public DataRow setPrimaryKey(String... pks) {
        return setPrimaryKey(false, pks);
    }

    /**
     * 设置主键
     * @param applyContainer 是否应用到上级容器
     * @param pks keys
     * @return DataRow
     */
    public DataRow setPrimaryKey(boolean applyContainer, Collection<String> pks) {
        if (BasicUtil.isEmpty(pks)) {
            return this;
        }
        /*设置容器主键*/
        if (hasContainer() && applyContainer) {
            getContainer().setPrimaryKey(pks);
        }

        if (null == this.primaryKeys) {
            this.primaryKeys = new ArrayList<>();
        } else {
            this.primaryKeys.clear();
        }
        return addPrimaryKey(applyContainer, pks);
    }

    public DataRow setPrimaryKey(Collection<String> pks) {
        return setPrimaryKey(false, pks);
    }

    /**
     * 读取主键
     * 主键为空时且容器有主键时,读取容器主键,否则返回默认主键
     * @return List
     */
    public List<String> getPrimaryKeys() {
        /*有主键直接返回*/
        if (hasSelfPrimaryKeys()) {
            return primaryKeys;
        }

        /*处于容器中并且容器有主键,返回容器主键*/
        if (hasContainer() && getContainer().hasPrimaryKeys()) {
            return getContainer().getPrimaryKeys();
        }

        /*本身与容器都没有主键 返回默认主键*/
        List<String> defaultPrimary = new ArrayList<>();
        String configKey = ConfigTable.getString("DEFAULT_PRIMARY_KEY", "ID");
        if (null != configKey && !configKey.trim().equals("")) {
            defaultPrimary.add(configKey);
        }

        return defaultPrimary;
    }

    public String getPrimaryKey() {
        List<String> keys = getPrimaryKeys();
        if (null != keys && keys.size() > 0) {
            return keys.get(0);
        }
        return null;
    }

    /**
     * 主键值
     * @return List
     */
    public List<Object> getPrimaryValues() {
        List<Object> values = new ArrayList<Object>();
        List<String> keys = getPrimaryKeys();
        if (null != keys) {
            for (String key : keys) {
                values.add(get(key));
            }
        }
        return values;
    }

    public Object getPrimaryValue() {
        String key = getPrimaryKey();
        if (null != key) {
            return get(key);
        }
        return null;
    }

    /**
     * 是否有主键
     * @return boolean
     */
    public boolean hasPrimaryKeys() {
        if (hasSelfPrimaryKeys()) {
            return true;
        }
        if (null != getContainer()) {
            return getContainer().hasPrimaryKeys();
        }
        if (keys().contains(ConfigTable.getString("DEFAULT_PRIMARY_KEY", "ID"))) {
            return true;
        }
        return false;
    }

    /**
     * 自身是否有主键
     * @return boolean
     */
    public boolean hasSelfPrimaryKeys() {
        if (null != primaryKeys && primaryKeys.size() > 0) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 读取数据源
     * 数据源为空时,读取容器数据源
     * @return String
     */
    public String getDataSource() {
        String ds = table;
        if (BasicUtil.isNotEmpty(ds) && BasicUtil.isNotEmpty(schema)) {
            ds = schema + "." + ds;
        }
        if (BasicUtil.isEmpty(ds)) {
            ds = dataSource;
        }
        if (null == ds && null != getContainer()) {
            ds = getContainer().getDataSource();
        }

        return ds;
    }

    public String getDataLink() {
        if (BasicUtil.isEmpty(datalink) && null != getContainer()) {
            return getContainer().getDatalink();
        }
        return datalink;
    }

    /**
     * 设置数据源
     * 当前对象处于容器中时,设置容器数据源
     * @param dataSource  dataSource
     * @return DataRow
     */
    public DataRow setDataSource(String dataSource) {
        if (null == dataSource) {
            return this;
        }
        if (null != getContainer()) {
            getContainer().setDataSource(dataSource);
        } else {
            this.dataSource = dataSource;
            if (dataSource.contains(".") && !dataSource.contains(":")) {
                schema = dataSource.substring(0, dataSource.indexOf("."));
                table = dataSource.substring(dataSource.indexOf(".") + 1);
            }
        }
        return this;
    }

    /**
     * 子类
     * @return Object
     */
    public Object getChildren() {
        return get(CHILDREN);
    }

    public DataRow setChildren(Object children) {
        put(CHILDREN, children);
        return this;
    }

    /**
     * 父类
     * @return Object
     */
    public Object getParent() {
        return get(PARENT);
    }

    public DataRow setParent(Object parent) {
        put(PARENT, parent);
        return this;
    }

    /**
     * 所有上级数据(递归)
     * @return List
     */
    @SuppressWarnings("unchecked")
    public List<Object> getAllParent() {
        if (null != get(ALL_PARENT)) {
            return (List<Object>) get(ALL_PARENT);
        }
        List<Object> parents = new ArrayList<Object>();
        Object parent = getParent();
        if (null != parent) {
            parents.add(parent);
            if (parent instanceof DataRow) {
                DataRow tmp = (DataRow) parent;
                parents.addAll(tmp.getAllParent());
            }
        }
        return parents;
    }

    /**
     * 转换成对象
     * @param <T>  T
     * @param clazz  clazz
     * @param configs 属性对应关系  name:USER_NAME
     * @return T
     */
    public <T> T entity(Class<T> clazz, String... configs) {
        T entity = null;
        if (null == clazz) {
            return entity;
        }
        if (null != getAdapter()) {
            entity = adapter.entity(clazz, this);
            if (null != entity) {
                return entity;
            }
        }
        try {
            entity = (T) clazz.newInstance();
            /*读取类属性*/
            List<Field> fields = ClassUtil.getFields(clazz);
            for (Field field : fields) {
                if (Modifier.isStatic(field.getModifiers())) {
                    continue;
                }
                Object value = get(field.getName());
                /*属性赋值*/
                BeanUtil.setFieldValue(entity, field, value);
            }//end 自身属性

            if (null != configs) {
                for (String config : configs) {
                    String field = config;
                    String column = config;
                    if (config.contains(":")) {
                        String[] tmps = config.split(":");
                        if (tmps.length >= 2) {
                            field = tmps[0];
                            column = tmps[1];
                        }
                    }
                    Object value = get(column);
                    BeanUtil.setFieldValue(entity, field, value);
                }
            }
        } catch (InstantiationException e) {
            e.printStackTrace();
        } catch (IllegalAccessException e) {
            e.printStackTrace();
        } catch (SecurityException e) {
            e.printStackTrace();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return entity;
    }

    /**
     * 是否有指定的key
     * @param key key
     * @return boolean
     */
    public boolean has(String key) {
        return get(key) != null;
    }

    public boolean hasValue(String key) {
        return get(key) != null;
    }

    public boolean hasKey(String key) {
        return keys().contains(keyAdapter.key(key));
    }

    public boolean containsKey(String key) {
        return keys().contains(keyAdapter.key(key));
    }

    public List<String> keys() {
        List<String> keys = new ArrayList<>();
        for (Iterator<String> itr = this.keySet().iterator(); itr.hasNext(); ) {
            keys.add(itr.next());
        }
        return keys;
    }

    public DataRow put(KEY_CASE keyCase, String key, Object value){
        return put(true, keyCase, key, value);
    }
    public DataRow put(boolean checkUpdate,String key, Object value){
        return put(checkUpdate, null, key, value);
    }

    public DataRow put(boolean checkUpdate, KEY_CASE keyCase, String key, Object value) {
        KeyAdapter keyAdapter = this.keyAdapter;
        if(null != keyCase){
            keyAdapter = KeyAdapter.parse(keyCase);
        }
        if (null != key) {
            key = keyAdapter.key(key);
            if(checkUpdate) {
                if (key.startsWith("+")) {
                    key = key.substring(1);
                    addUpdateColumns(key);
                }
                Object oldValue = get(keyCase, key);
                if (null == oldValue || !oldValue.equals(value)) {
                    super.put(key, value);
                }
                if (!BasicUtil.equal(oldValue, value)) {
                    addUpdateColumns(key);
                }
            }else{
                super.put(key, value);
            }
            if (ConfigTable.IS_KEY_IGNORE_CASE) {
                String ignoreKey = key.replace("_", "").replace("-", "").toUpperCase();
                keymap.put(ignoreKey, key);
            }
        }
        return this;
    }

    /**
     *
     * @param keyCase keyCase
     * @param key key
     * @param value value
     * @param checkUpdate 检测是否需要更新
     * @param pk        是否是主键 pk		是否是主键
     * @param override    是否覆盖之前的主键(追加到primaryKeys) 默认覆盖(单一主键)
     * @return DataRow
     */
    public DataRow put(boolean checkUpdate, KEY_CASE keyCase, String key, Object value, boolean pk, boolean override) {
        if (pk) {
            if (override) {
                primaryKeys.clear();
            }
            addPrimaryKey(key);
        }
        put(checkUpdate, keyCase, key, value);
        return this;
    }
    public DataRow put(KEY_CASE keyCase, String key, Object value, boolean pk, boolean override) {
       return put(true, keyCase, key, value, pk, override);
    }

    public DataRow put(String key, Object value, boolean pk, boolean override) {
        return put(null, key, value, pk, override);
    }

    public DataRow put(KEY_CASE keyCase, String key, Object value, boolean pk) {
        put(keyCase, key, value, pk, true);
        return this;
    }

    public DataRow put(String key, Object value, boolean pk) {
        put(null, key, value, pk, true);
        return this;
    }

    /**
     * 这是重写的父类put不要改返回值类型
     * @param key key
     * @param value value
     * @return Object
     */
    @Override
    public Object put(String key, Object value) {
        put(null, key, value, false, true);
        return this;
    }

    public DataRow set(String key, Object value) {
        put(null, key, value, false, true);
        return this;
    }

    public DataRow attr(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public DataRow setAttribute(String key, Object value) {
        attributes.put(key, value);
        return this;
    }

    public Object attr(String key) {
        return attributes.get(key);
    }

    public Object getAttribute(String key) {
        return attributes.get(key);
    }

    public DataRow getRow(String key) {
        if (null == key) {
            return null;
        }
        Object obj = get(key);
        if (null != obj && obj instanceof DataRow) {
            return (DataRow) obj;
        }
        return null;
    }

    public DataRow getRow(String ... keys) {
        if (null == keys || keys.length == 0) {
            return null;
        }
        DataRow result = this;
        for(String key:keys){
            if(null != result){
                result = result.getRow(key);
            }else{
                return null;
            }
        }
        return result;
    }
    public DataSet getSet(String key) {
        if (null == key) {
            return null;
        }
        Object obj = get(key);
        if (null != obj) {
            if (obj instanceof DataSet) {
                return (DataSet) obj;
            } else if (obj instanceof List) {
                List<?> list = (List<?>) obj;
                DataSet set = new DataSet();
                for (Object item : list) {
                    set.add(DataRow.parse(item));
                }
                return set;
            }
        }
        return null;
    }

    public List<?> getList(String key) {
        if (null == key) {
            return null;
        }
        Object obj = get(key);
        if (null != obj && obj instanceof List) {
            return (List<?>) obj;
        }
        return null;
    }

    public String getStringNvl(String key, String... defs) {
        String result = getString(key);
        if (BasicUtil.isEmpty(result)) {
            if (null == defs || defs.length == 0) {
                result = "";
            } else {
                result = BasicUtil.nvl(defs).toString();
            }
        }
        return result;
    }

    public String getString(String key) {
        String result = null;
        if (null == key) {
            return result;
        }
        if (key.contains("${") && key.contains("}")) {
            result = BeanUtil.parseFinalValue(this, key);
        } else {
            Object value = get(key);
            if (null != value) {
                result = value.toString();
            }
        }
        return result;
    }


    /**
     * boolean类型true 解析成 1
     * @param key key
     * @return int
     * @throws Exception Exception
     */
    public int getInt(String key) throws Exception {
        Object val = get(key);
        if (val instanceof Boolean) {
            boolean bol = (Boolean) val;
            if (bol) {
                return 1;
            } else {
                return 0;
            }
        } else {
            return (int) Double.parseDouble(val.toString());
        }
    }

    public int getInt(String key, int def) {
        try {
            return getInt(key);
        } catch (Exception e) {
            return def;
        }
    }

    public double getDouble(String key) throws Exception {
        Object value = get(key);
        return Double.parseDouble(value.toString());
    }

    public double getDouble(String key, double def) {
        try {
            return getDouble(key);
        } catch (Exception e) {
            return def;
        }
    }

    public long getLong(String key) throws Exception {
        Object value = get(key);
        return Long.parseLong(value.toString());
    }

    public long getLong(String key, long def) {
        try {
            return getLong(key);
        } catch (Exception e) {
            return def;
        }
    }

    public float getFloat(String key) throws Exception {
        Object value = get(key);
        return Float.parseFloat(value.toString());
    }

    public float getFloat(String key, float def) {
        try {
            return getFloat(key);
        } catch (Exception e) {
            return def;
        }
    }

    public boolean getBoolean(String key, boolean def) {
        return BasicUtil.parseBoolean(getString(key), def);
    }

    public boolean getBoolean(String key) throws Exception {
        return BasicUtil.parseBoolean(getString(key));
    }

    public BigDecimal getDecimal(String key) throws Exception {
        return new BigDecimal(getString(key));
    }

    public BigDecimal getDecimal(String key, double def) {
        return getDecimal(key, new BigDecimal(def));
    }

    public BigDecimal getDecimal(String key, BigDecimal def) {
        try {
            BigDecimal result = getDecimal(key);
            if (null == result) {
                return def;
            }
            return result;
        } catch (Exception e) {
            return def;
        }
    }

    public String getDecimal(String key, String format) throws Exception {
        BigDecimal result = getDecimal(key);
        return NumberUtil.format(result, format);
    }

    public String getDecimal(String key, double def, String format) {
        return getDecimal(key, new BigDecimal(def), format);
    }

    public String getDecimal(String key, BigDecimal def, String format) {
        BigDecimal result = null;
        try {
            result = getDecimal(key);
            if (null == result) {
                result = def;
            }
        } catch (Exception e) {
            result = def;
        }
        return NumberUtil.format(result, format);
    }

    public Date getDate(String key, Date def) {
        Object date = get(key);
        if (null == date) {
            return def;
        }
        if (date instanceof Date) {
            return (Date) date;
        } else if (date instanceof Long) {
            Date d = new Date();
            d.setTime((Long) date);
            return d;
        } else if (date instanceof LocalDateTime) {
            ZoneId zoneId = ZoneId.systemDefault();
            ZonedDateTime zdt = ((LocalDateTime) date).atZone(zoneId);
            return Date.from(zdt.toInstant());
        } else {
            return DateUtil.parse(date.toString());
        }
    }

    public Date getDate(String key, String def) {
        try {
            return getDate(key);
        } catch (Exception e) {
            return DateUtil.parse(def);
        }
    }

    public Date getDate(String key) throws Exception {
        return DateUtil.parse(getString(key));
    }

    /**
     * 转换成json格式
     * @return String
     */
    public String toJSON() {
        return BeanUtil.map2json(this);
    }

    public String toJson() {
        return toJSON();
    }

    public String getJson() {
        return BeanUtil.map2json(this);
    }

    public DataRow removeEmpty(String... keys) {
        if (null != keys && keys.length > 0) {
            for (String key : keys) {
                if (this.isEmpty(key)) {
                    this.remove(key);
                }
            }
        } else {
            List<String> cols = keys();
            for (String key : cols) {
                if (this.isEmpty(key)) {
                    this.remove(key);
                }
            }
        }
        return this;
    }

    public DataRow removeNull(String... keys) {
        if (null != keys && keys.length > 0) {
            for (String key : keys) {
                if (null == this.get(key)) {
                    this.remove(key);
                }
            }
        }
        List<String> cols = keys();
        for (String key : cols) {
            if (null == this.get(key)) {
                this.remove(key);
            }
        }
        return this;
    }

    /**
     * 轮换成xml格式
     * @return String
     */
    public String toXML() {
        return BeanUtil.map2xml(this);
    }

    public String toXML(boolean border, boolean order) {
        return BeanUtil.map2xml(this, border, order);
    }

    /**
     * 是否处于容器内
     * @return boolean
     */
    public boolean hasContainer() {
        if (null != getContainer()) {
            return true;
        } else {
            return false;
        }
    }

    /**
     * 包含当前对象的容器
     * @return DataSet
     */
    public DataSet getContainer() {
        return container;
    }

    public DataRow setContainer(DataSet container) {
        this.container = container;
        return this;
    }

    public Object getClientTrace() {
        return clientTrace;
    }

    public DataRow setClientTrace(Object clientTrace) {
        this.clientTrace = clientTrace;
        return this;
    }

    public String getSchema() {
        if (null != schema) {
            return schema;
        } else {
            DataSet container = getContainer();
            if (null != container) {
                return container.getSchema();
            } else {
                return null;
            }
        }
    }

    public DataRow setSchema(String schema) {
        this.schema = schema;
        return this;
    }

    public String getTable() {
        if (null != table) {
            return table;
        } else {
            DataSet container = getContainer();
            if (null != container) {
                return container.getTable();
            } else {
                return null;
            }
        }
    }

    public DataRow setTable(String table) {
        if (null != table && table.contains(".")) {
            String[] tbs = table.split("\\.");
            this.table = tbs[1];
            this.schema = tbs[0];
        } else {
            this.table = table;
        }
        return this;
    }

    /**
     * 验证是否过期
     * 根据当前时间与创建时间对比
     * 过期返回 true
     * @param millisecond    过期时间(毫秒) millisecond
     * @return boolean
     */
    public boolean isExpire(int millisecond) {
        if (System.currentTimeMillis() - createTime > millisecond) {
            return true;
        }
        return false;
    }

    /**
     * 是否过期
     * @param millisecond millisecond
     * @return boolean
     */
    public boolean isExpire(long millisecond) {
        if (System.currentTimeMillis() - createTime > millisecond) {
            return true;
        }
        return false;
    }

    public boolean isExpire() {
        if (getExpires() == -1) {
            return false;
        }
        if (System.currentTimeMillis() - createTime > getExpires()) {
            return true;
        }
        return false;
    }

    /**
     * 复制数据
     * @return Object
     */
    public Object clone() {
        DataRow row = (DataRow) super.clone();
        row.container = this.container;
        row.primaryKeys = this.primaryKeys;
        row.dataSource = this.dataSource;
        row.schema = this.schema;
        row.table = this.table;
        row.clientTrace = this.clientTrace;
        row.createTime = this.createTime;
        row.isNew = this.isNew;
        return row;
    }

    public Boolean getIsNew() {
        return isNew;
    }

    public DataRow setIsNew(Boolean isNew) {
        this.isNew = isNew;
        return this;
    }

    public List<String> getUpdateColumns() {
        return updateColumns;
    }

    public List<String> getIgnoreUpdateColumns() {
        return ignoreUpdateColumns;
    }

    /**
     * 删除指定的key
     * 不和remove命名 避免调用remoate("ID","CODE")时与HashMap.remove(Object key, Object value) 冲突
     * @param keys keys
     * @return DataRow
     */
    public DataRow removes(String... keys) {
        if (null != keys) {
            for (String key : keys) {
                if (null != key) {
                    key = keyAdapter.key(key);
                    super.remove(key);
                    updateColumns.remove(key);
                }
            }
        }
        return this;
    }

    public DataRow removes(List<String> keys) {
        if (null != keys) {
            for (String key : keys) {
                if (null != key) {
                    key = keyAdapter.key(key);
                    super.remove(key);
                    updateColumns.remove(key);
                }
            }
        }
        return this;
    }

    public Object remove(Object key) {
        if (null == key) {
            return null;
        }
        updateColumns.remove( keyAdapter.key(key.toString()));
        return super.remove( keyAdapter.key(key.toString()));
    }

    /**
     * 清空需要更新的列
     * @return DataRow
     */
    public DataRow clearUpdateColumns() {
        updateColumns.clear();
        return this;
    }

    public DataRow clearIgnoreUpdateColumns() {
        ignoreUpdateColumns.clear();
        return this;
    }

    public DataRow removeUpdateColumns(String... cols) {
        if (null != cols) {
            for (String col : cols) {
                String key =  keyAdapter.key(col);
                updateColumns.remove(key);
                ignoreUpdateColumns.add(key);
            }
        }
        return this;
    }

    /**
     * 添加需要更新的列
     * @param cols cols
     * @return DataRow
     */
    public DataRow addUpdateColumns(String... cols) {
        if (null != cols) {
            for (String col : cols) {
                String key =  keyAdapter.key(col);
                if (!updateColumns.contains(key)) {
                    updateColumns.add(key);
                    ignoreUpdateColumns.remove(key);
                }
            }
        }
        return this;
    }

    public DataRow addAllUpdateColumns() {
        updateColumns.clear();
        updateColumns.addAll(keys());
        ignoreUpdateColumns.clear();
        return this;
    }

    /**
     * 将数据从data中复制到this
     * @param regex 是否开启正则匹配
     * @param data data
     * @param fixs fixs
     * @param data data
     * @param keys this与data中的key不同时 "this.key:data.key"(CD:ORDER_CD)
     * @return DataRow
     */
    public DataRow copy(boolean regex, DataRow data, String[] fixs, String... keys) {
        return copy(data, BeanUtil.array2list(fixs, keys));
    }

    public DataRow copy(boolean regex, DataRow data, String... keys) {
        if (null == data) {
            return this;
        }
        if (null == keys || keys.length == 0) {
            return copy(data, data.keys());
        } else {
            return copy(data, BeanUtil.array2list(keys));
        }

    }
    public DataRow copy(boolean regex, DataRow data, List<String> fixs, String... keys) {
        if (null == data || data.isEmpty()) {
            return this;
        }
        List<String> list = BeanUtil.merge(fixs, keys);
        for (String key : list) {
            String ks[] = BeanUtil.parseKeyValue(key);
            if (null != ks && ks.length > 1) {
                put(ks[0], data.get(ks[1]));
            } else {
                put(key, data.get(key));
            }
        }
        return this;
    }

    public DataRow copy(DataRow data, String[] fixs, String... keys) {
        return copy(false, data, fixs);
    }

    public DataRow copy(DataRow data, String... keys) {
        return copy(false, data, keys);
    }
    public DataRow copy(DataRow data, List<String> fixs, String... keys) {
        return copy(false, data, fixs, keys);
    }
    /**
     * 抽取指定列,生成新的DataRow,新的DataRow只包括指定列的值,不包含其他附加信息(如来源表)
     * @param keys keys
     * @param regex 是否开启正则匹配
     * @return DataRow
     */
    public DataRow extract(boolean regex, String... keys) {
        DataRow result = new DataRow();
        result.copy(regex,this, keys);
        return result;
    }

    public DataRow extract(boolean regex, List<String> keys) {
        DataRow result = new DataRow();
        result.copy(regex,this, keys);
        return result;
    }

    public DataRow extract(String... keys) {
        return extract(false, keys);
    }

    public DataRow extract(List<String> keys) {
       return extract(false, keys);
    }

    /**
     * 复制String类型数据
     * @param data data
     * @param keys keys
     * @return DataRow
     */
    public DataRow copyString(DataRow data, String... keys) {
        if (null == data || null == keys) {
            return this;
        }
        for (String key : keys) {
            String ks[] = BeanUtil.parseKeyValue(key);
            Object obj = data.get(ks[1]);
            if (BasicUtil.isNotEmpty(obj)) {
                this.put(ks[0], obj.toString());
            } else {
                this.put(ks[0], null);
            }
        }
        return this;
    }

    /**
     * 所有数字列
     * @return List
     */
    public List<String> numberKeys() {
        List<String> result = new ArrayList<>();
        List<String> keys = keys();
        for (String key : keys) {
            if (get(key) instanceof Number) {
                result.add(key);
            }
        }
        return result;
    }

    /**
     * 检测必选项
     * @param keys keys
     * @return boolean
     */
    public boolean checkRequired(String... keys) {
        List<String> ks = new ArrayList<>();
        if (null != keys && keys.length > 0) {
            for (String key : keys) {
                ks.add(key);
            }
        }
        return checkRequired(ks);
    }

    public boolean checkRequired(List<String> keys) {
        if (null != keys) {
            for (String key : keys) {
                if (isEmpty(key)) {
                    return false;
                }
            }
        }
        return true;
    }


    public DataRow camel() {
        List<String> keys = keys();
        for (String key : keys) {
            Object value = get(key);
            String camel = BeanUtil.camel(key);
            updateColumns.remove(key);
            super.remove(key);
            put(KEY_CASE.SRC, camel, value);
        }
        this.setKeyCase(KEY_CASE.camel);
        return this;
    }

    public DataRow Camel() {
        List<String> keys = keys();
        for (String key : keys) {
            Object value = get(key);
            String Camel = BeanUtil.Camel(key);
            updateColumns.remove(key);
            super.remove(key);
            put(KEY_CASE.SRC, Camel, value);
        }
        this.setKeyCase(KEY_CASE.Camel);
        return this;
    }

    /**
     * key大小写转换
     * @param keyCase keyCase
     * @param key key
     * @return String
     */
    private static String getKeyCase(KEY_CASE keyCase, String key) {
        if (null == key || keyCase == KEY_CASE.SRC) {
            return key;
        }
        if (keyCase == KEY_CASE.LOWER) {
            key = key.toLowerCase();
        } else if (keyCase == KEY_CASE.UPPER) {
            key = key.toUpperCase();
        } else if (keyCase == KEY_CASE.Camel) {
            key = BeanUtil.Camel(key);
        } else if (keyCase == KEY_CASE.camel) {
            key = BeanUtil.camel(key);
        }
        //else if(keyCase.getCode().contains("_")){
//			//驼峰转下划线
//			key = BeanUtil.camel_(key);
//			if(keyCase == KEY_CASE.CAMEL_CONFIG){
//					if(ConfigTable.IS_UPPER_KEY){
//						key = key.toUpperCase();
//					}
//					if(ConfigTable.IS_LOWER_KEY){
//						key = key.toLowerCase();
//					}
//			}else if(keyCase == KEY_CASE.CAMEL_LOWER){
//				key = key.toLowerCase();
//			}else if(keyCase == KEY_CASE.CAMEL_UPPER){
//				key = key.toUpperCase();
//			}
//		}
        return key;
    }

    public static String getKeyCase(String key) {
        return getKeyCase(KEY_CASE.CONFIG, key);
    }
/*	private String getKey(String key){
		return getKeyCase(this.keyCase, key);
	}
	private String getKey(KEY_CASE keyCase, String key){
		return getKeyCase(keyCase, key);
	}
	 */

    /**
     * key大小写转换
     * @param keyCase keyCase
     * @param key key
     * @return String
     */
    private static String putKeyCase(KEY_CASE keyCase, String key) {
        if (null == key || keyCase == KEY_CASE.SRC) {
            return key;
        }
        if (keyCase == KEY_CASE.UPPER) {
            key = key.toUpperCase();
        } else if (keyCase == KEY_CASE.LOWER) {
            key = key.toLowerCase();
        } else if (keyCase == KEY_CASE.Camel) {
            key = BeanUtil.Camel(key);
        } else if (keyCase == KEY_CASE.camel) {
            key = BeanUtil.camel(key);
        } else if (keyCase.getCode().contains("_")) {
            //驼峰转下划线
            key = BeanUtil.camel_(key);
            if (keyCase == KEY_CASE.CAMEL_CONFIG) {
                if (ConfigTable.IS_UPPER_KEY) {
                    key = key.toUpperCase();
                }
                if (ConfigTable.IS_LOWER_KEY) {
                    key = key.toLowerCase();
                }
            } else if (keyCase == KEY_CASE.CAMEL_LOWER) {
                key = key.toLowerCase();
            } else if (keyCase == KEY_CASE.CAMEL_UPPER) {
                key = key.toUpperCase();
            }
        }
        return key;
    }

    public static String putKeyCase(String key) {
        return putKeyCase(KEY_CASE.CONFIG, key);
    }
	/*private String putKey(String key){
		return putKeyCase(this.keyCase, key);
	}
	private String putKey(KEY_CASE keyCase, String key){
		return putKeyCase(keyCase, key);
	}
*/

    /**
     * 查询条件
     * @return Map
     */
    public Map<String, Object> getQueryParams() {
        if (queryParams.isEmpty()) {
            return container.getQueryParams();
        }
        return queryParams;
    }

    /**
     * 设置查询条件
     * @param queryParams queryParams
     * @return DataRow
     */
    public DataRow setQueryParams(Map<String, Object> queryParams) {
        this.queryParams = queryParams;
        return this;
    }

    public Object getQueryParam(String key) {
        if (queryParams.isEmpty()) {
            return container.getQueryParams().get(key);
        }
        return queryParams.get(key);
    }

    public DataRow addQueryParam(String key, Object param) {
        queryParams.put(key, param);
        return this;
    }

    /**
     * 是否更新null列
     * @return boolean
     */
    public boolean isUpdateNullColumn() {
        return updateNullColumn;
    }

    /**
     * 设置是否更新null列
     * @param updateNullColumn updateNullColumn
     * @return DataRow
     */
    public DataRow setUpdateNullColumn(boolean updateNullColumn) {
        this.updateNullColumn = updateNullColumn;
        return this;
    }

    /**
     * 是否更新空列
     * @return boolean
     */
    public boolean isUpdateEmptyColumn() {
        return updateEmptyColumn;
    }

    /**
     * 设置是否更新空列
     * @param updateEmptyColumn updateEmptyColumn
     * @return DataRow
     */
    public DataRow setUpdateEmptyColumn(boolean updateEmptyColumn) {
        this.updateEmptyColumn = updateEmptyColumn;
        return this;
    }

    /**
     * 替换key
     * @param key key
     * @param target target
     * @param remove 是否删除原来的key
     * @return DataRow
     */
    public DataRow changeKey(String key, String target, boolean remove) {
        put(target, get(key));
        if (remove && !target.equalsIgnoreCase(key)) {
            remove(keyAdapter.key(key));
        }
        return this;
    }

    public DataRow changeKey(String key, String target) {
        return changeKey(key, target, true);
    }

    /**
     * 替换所有空值
     * @param value value
     * @return DataRow
     */
    public DataRow replaceEmpty(String value) {
        List<String> keys = keys();
        for (String key : keys) {
            if (isEmpty(key)) {
                put(KEY_CASE.SRC, key, value);
            }
        }
        return this;
    }

    public DataRow trim() {
        List<String> keys = keys();
        for (String key : keys) {
            Object value = get(key);
            if (null != value && value instanceof String) {
                put(KEY_CASE.SRC, key, value.toString().trim());
            }
        }
        return this;
    }

    /**
     * 替换所有空值
     * @param key key
     * @param value value
     * @return DataRow
     */
    public DataRow replaceEmpty(String key, String value) {
        if (isEmpty(key)) {
            put(key, value);
        }
        return this;
    }

    /**
     * 替换所有NULL值
     * @param key key
     * @param value value
     * @return DataRow
     */
    public DataRow replaceNull(String key, String value) {
        if (null == get(key)) {
            put(key, value);
        }
        return this;
    }

    /**
     * 替换所有NULL值
     * @param value value
     * @return DataRow
     */
    public DataRow replaceNull(String value) {
        List<String> keys = keys();
        for (String key : keys) {
            if (null == get(key)) {
                put(key, value);
            }
        }
        return this;
    }

    public DataRow replace(String key, String oldChar, String newChar) {
        if (null == key || null == oldChar || null == newChar) {
            return this;
        }
        put(key, getStringNvl(key).replace(oldChar, newChar));
        return this;
    }

    public DataRow replace(String oldChar, String newChar) {
        List<String> keys = keys();
        if (null == newChar) {
            newChar = "";
        }
        for (String key : keys) {
            Object value = get(key);
            if (value != null && value instanceof String) {
                put(key, ((String) value).replace(oldChar, newChar));
            }
        }
        return this;
    }

    /**
     * 拼接value
     * @param keys keys
     * @return String
     */
    public String join(String... keys) {
        String result = "";
        if (null != keys) {
            for (String key : keys) {
                String val = getString(key);
                if (BasicUtil.isNotEmpty(val)) {
                    if ("".equals(result)) {
                        result = val;
                    } else {
                        result += "," + val;
                    }
                }
            }
        }
        return result;
    }

    /**
     * 指定key转换成number
     * @param keys keys
     * @return DataRow
     */
    public DataRow convertNumber(String... keys) {
        if (null != keys) {
            for (String key : keys) {
                String v = getString(key);
                if (null != v) {
                    remove(keyAdapter.key(key));
                    put(key, new BigDecimal(v));
                }
            }
        }
        return this;
    }

    public DataRow convertString(String... keys) {
        List<String> list = null;
        if (null == keys || keys.length == 0) {
            list = keys();
        } else {
            list = BeanUtil.array2list(keys);
        }
        for (String key : list) {
            String v = getString(key);
            if (null != v) {
                put(key, v.toString());
            }
        }
        return this;
    }

    public Object get(String key) {
        Object result = null;
        if (null != key) {
            key = keyAdapter.key(key);
            if (ConfigTable.IS_KEY_IGNORE_CASE) {
                String ignoreKey = key.replace("_", "").replace("-", "").toUpperCase();
                key = keymap.get(ignoreKey);
                result = super.get(key);
            } else {
                result = super.get(key);
            }
        }
        return result;
    }

    public Object get(KEY_CASE keyCase, String key) {
        KeyAdapter keyAdapter = this.keyAdapter;
        if(null != keyCase){
            keyAdapter = KeyAdapter.parse(keyCase);
        }
        Object result = null;
        if (null != key) {
            result = super.get(keyAdapter.key(key));
        }
        return result;
    }

    public Object get(boolean voluntary, String... keys) {
        if (null == keys || keys.length == 0) {
            return null;
        }
        Object result = this;
        for (String key : keys) {
            if (null != result) {
                if (result instanceof DataRow) {
                    result = ((DataRow) result).get(voluntary, key);
                } else if (ClassUtil.isWrapClass(result) && !(result instanceof String)) {
                    result = BeanUtil.getFieldValue(result, key);
                } else {
                    if (voluntary) {
                        return result;
                    } else {
                        result = null;
                    }
                }
            }
        }
        return result;
    }

    public Object get(String... keys) {
        return get(false, keys);
    }

    public Object nvl(String... keys) {
        return BeanUtil.nvl(this, keys);
    }

    public Object evl(String... keys) {
        return BeanUtil.evl(this, keys);
    }

    /**
     * 在key列基础上 +value,如果原来没有key列则默认0并put到target
     * 如果target与key一致则覆盖原值
     * @param target 计算结果key
     * @param key key
     * @param value value
     * @return DataRow
     */
    public DataRow add(String target, String key, int value) {
        put(target, getInt(key, 0) + value);
        return this;
    }

    public DataRow add(String target, String key, double value) {
        put(target, getDouble(key, 0) + value);
        return this;
    }

    public DataRow add(String target, String key, short value) {
        put(target, getInt(key, 0) + value);
        return this;
    }

    public DataRow add(String target, String key, float value) {
        put(target, getFloat(key, 0) + value);
        return this;
    }

    public DataRow add(String target, String key, BigDecimal value) {
        put(target, getDecimal(key, 0).add(value));
        return this;
    }

    public DataRow add(String key, int value) {
        return add(key, key, value);
    }

    public DataRow add(String key, double value) {
        return add(key, key, value);
    }

    public DataRow add(String key, short value) {
        return add(key, key, value);
    }

    public DataRow add(String key, float value) {
        return add(key, key, value);
    }

    public DataRow add(String key, BigDecimal value) {
        return add(key, key, value);
    }


    public DataRow subtract(String target, String key, int value) {
        put(target, getInt(key, 0) - value);
        return this;
    }

    public DataRow subtract(String target, String key, double value) {
        put(target, getDouble(key, 0) - value);
        return this;
    }

    public DataRow subtract(String target, String key, short value) {
        put(target, getInt(key, 0) - value);
        return this;
    }

    public DataRow subtract(String target, String key, float value) {
        put(target, getFloat(key, 0) - value);
        return this;
    }

    public DataRow subtract(String target, String key, BigDecimal value) {
        put(target, getDecimal(key, 0).subtract(value));
        return this;
    }


    public DataRow subtract(String key, int value) {
        return subtract(key, key, value);
    }

    public DataRow subtract(String key, double value) {
        return subtract(key, key, value);
    }

    public DataRow subtract(String key, short value) {
        return subtract(key, key, value);
    }

    public DataRow subtract(String key, float value) {
        return subtract(key, key, value);
    }

    public DataRow subtract(String key, BigDecimal value) {
        return subtract(key, key, value);
    }


    public DataRow multiply(String target, String key, int value) {
        put(target, getInt(key, 0) * value);
        return this;
    }

    public DataRow multiply(String target, String key, double value) {
        put(target, getDouble(key, 0) * value);
        return this;
    }

    public DataRow multiply(String target, String key, short value) {
        put(target, getInt(key, 0) * value);
        return this;
    }

    public DataRow multiply(String target, String key, float value) {
        put(target, getFloat(key, 0) * value);
        return this;
    }

    public DataRow multiply(String target, String key, BigDecimal value) {
        put(target, getDecimal(key, 0).multiply(value));
        return this;
    }


    public DataRow multiply(String key, int value) {
        return multiply(key, key, value);
    }

    public DataRow multiply(String key, double value) {
        return multiply(key, key, value);
    }

    public DataRow multiply(String key, short value) {
        return multiply(key, key, value);
    }

    public DataRow multiply(String key, float value) {
        return multiply(key, key, value);
    }

    public DataRow multiply(String key, BigDecimal value) {
        return multiply(key, key, value);
    }


    public DataRow divide(String target, String key, int value) {
        put(target, getInt(key, 0) / value);
        return this;
    }

    public DataRow divide(String target, String key, double value) {
        put(target, getDouble(key, 0) / value);
        return this;
    }

    public DataRow divide(String target, String key, short value) {
        put(target, getInt(key, 0) / value);
        return this;
    }

    public DataRow divide(String target, String key, float value) {
        put(target, getFloat(key, 0) / value);
        return this;
    }

    public DataRow divide(String target, String key, BigDecimal value, int mode) {
        put(target, getDecimal(key, 0).divide(value, mode));
        return this;
    }


    public DataRow divide(String key, int value) {
        return divide(key, key, value);
    }

    public DataRow divide(String key, double value) {
        return divide(key, key, value);
    }

    public DataRow divide(String key, short value) {
        return divide(key, key, value);
    }

    public DataRow divide(String key, float value) {
        return divide(key, key, value);
    }

    public DataRow divide(String key, BigDecimal value, int mode) {
        return divide(key, key, value, mode);
    }

    /**
     * 舍入
     * @param target 结果保存位置,如果与key一致则覆盖原值
     * @param key 属性
     * @param scale 小数位
     * @param mode 舍入 参考BigDecimal
     * ROUND_UP 舍入远离零的舍入模式 在丢弃非零部分之前始终增加数字（始终对非零舍弃部分前面的数字加 1） 如:2.36 转成 2.4
     * ROUND_DOWN 接近零的舍入模式 在丢弃某部分之前始终不增加数字(从不对舍弃部分前面的数字加1,即截短). 如:2.36 转成 2.3
     * ROUND_CEILING 接近正无穷大的舍入模式 如果 BigDecimal 为正,则舍入行为与 ROUND_UP 相同 如果为负,则舍入行为与 ROUND_DOWN 相同 相当于是 ROUND_UP 和 ROUND_DOWN 的合集
     * ROUND_FLOOR 接近负无穷大的舍入模式 如果 BigDecimal 为正,则舍入行为与 ROUND_DOWN 相同 如果为负,则舍入行为与 ROUND_UP 相同 与ROUND_CEILING 正好相反
     * ROUND_HALF_UP 四舍五入
     * ROUND_HALF_DOWN 五舍六入
     * ROUND_HALF_EVEN 四舍六入 五留双 如果舍弃部分左边的数字为奇数,则舍入行为与 ROUND_HALF_UP 相同（四舍五入） 如果为偶数,则舍入行为与 ROUND_HALF_DOWN 相同（五舍六入） 如:1.15 转成 1.1,1.25 转成 1.2
     * ROUND_UNNECESSARY 断言请求的操作具有精确的结果,因此不需要舍入 如果对获得精确结果的操作指定此舍入模式,则抛出 ArithmeticException
     * @return DataRow
     */
    public DataRow round(String target, String key, int scale, int mode) {
        BigDecimal value = getDecimal(key, 0);
        value.setScale(scale, mode);
        put(target, value);
        return this;
    }

    public DataRow round(String key, int scale, int mode) {
        return round(key, key, scale, mode);
    }


}