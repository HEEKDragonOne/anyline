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


package org.anyline.metadata;

import org.anyline.metadata.type.TypeMetadata;
import org.anyline.metadata.type.JavaType;
import org.anyline.metadata.type.init.StandardTypeMetadata;
import org.anyline.util.BasicUtil;

import java.io.Serializable;
import java.util.*;

public class Column extends BaseMetadata<Column> implements Serializable {

    public static LinkedHashMap<TypeMetadata.CATEGORY, TypeMetadata.Config> typeCategoryConfigs = new LinkedHashMap<>();
    static {
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.CHAR, new TypeMetadata.Config( 0, 1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.TEXT, new TypeMetadata.Config(1, 1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.BOOLEAN, new TypeMetadata.Config(1,1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.BYTES, new TypeMetadata.Config(0, 1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.BLOB, new TypeMetadata.Config(1,1,1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.INT, new TypeMetadata.Config(1, 1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.FLOAT, new TypeMetadata.Config(1, 0, 0));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.DATE, new TypeMetadata.Config(1, 1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.TIME, new TypeMetadata.Config(1, 1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.DATETIME, new TypeMetadata.Config(1, 1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.TIMESTAMP, new TypeMetadata.Config(1, 1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.COLLECTION, new TypeMetadata.Config(1, 1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.GEOMETRY, new TypeMetadata.Config(1, 1, 1));
        typeCategoryConfigs.put(TypeMetadata.CATEGORY.OTHER, new TypeMetadata.Config(1, 1, 1));
    }
    public enum Aggregation {
        MIN			    ("MIN"  			, "最小"),
        MAX			    ("MAX"  			, "最大"),
        SUM			    ("SUM"  			, "求和"),
        REPLACE			    ("REPLACE"  			, "替换"), //对于维度列相同的行，指标列会按照导入的先后顺序，后导入的替换先导入的。
        REPLACE_IF_NOT_NULL			    ("REPLACE_IF_NOT_NULL"  			, "非空值替换"), //和 REPLACE 的区别在于对于null值，不做替换。这里要注意的是字段默认值要给NULL，而不能是空字符串，如果是空字符串，会给你替换成空字符串。
        HLL_UNION			    ("HLL_UNION"  			, "HLL 类型的列的聚合方式"),//通过 HyperLogLog 算法聚合
        BITMAP_UNION			    ("BITMAP_UNION"  			, "BIMTAP 类型的列的聚合方式，");//进行位图的并集聚合
        final String code;
        final String name;
        Aggregation(String code, String name){
            this.code = code;
            this.name = name;
        }
        public String getName(){
            return name;
        }
        public String getCode(){
            return code;
        }
    }
    public static  <T extends Column>  void sort(Map<String, T> columns){
        sort(columns, false);
    }
    public static  <T extends Column>  void sort(Map<String, T> columns, boolean nullFirst){
        List<T> list = new ArrayList<>();
        list.addAll(columns.values());
        sort(list, nullFirst);
        columns.clear();
        for(T column:list){
            columns.put(column.getName().toUpperCase(), column);
        }
    }
    public static  <T extends Column>  void sort(List<T> columns){
        sort(columns, false);
    }

    /**
     * 列排序
     * @param columns 列集合
     * @param nullFirst 未设置过位置(setPosition)的列是否排在最前面
     * @param <T> Column
     */
    public static  <T extends Column>  void sort(List<T> columns, boolean nullFirst){
        Collections.sort(columns, new Comparator<T>() {
            @Override
            public int compare(T o1, T o2) {
                Integer p1 = o1.getPosition();
                Integer p2 = o2.getPosition();
                if(p1 == p2){
                    return 0;
                }
                if(nullFirst) {
                    if (null == p1) {
                        return -1;
                    }
                    if (null == p2) {
                        return 1;
                    }
                }else{
                    if (null == p1) {
                        return 1;
                    }
                    if (null == p2) {
                        return -1;
                    }
                }
                return p1 > p2 ? 1:-1;
            }
        });
    }
    protected String keyword = "COLUMN"           ;
    protected String originalName                 ; // 原名 SELECT ID AS USER_ID FROM USER; originalName=ID, name=USER_ID
    protected String typeName                     ; // 类型名称 varchar完整类型调用getFullType > varchar(10)
    protected TypeMetadata typeMetadata           ;
    protected String fullType                     ; //完整类型名称
    protected int ignoreLength               = -1 ;
    protected int ignorePrecision            = -1 ;
    protected int ignoreScale                = -1 ;
    //数字类型:precision,scale 日期:length 时间戳:scale 其他:length
    protected Integer precisionLength             ; // 精确长度 根据数据类型返回precision或length
    protected Integer length                      ; // 长度(注意varchar,date,timestamp,number的区别)
    protected Integer precision                   ; // 有效位数 整个字段的长度(包含小数部分)  123.45：precision = 5, scale = 2 对于SQL Server 中 varchar(max)设置成 -1 null:表示未设置
    protected Integer scale                       ; // 小数部分的长度

    protected String className                    ; // 对应的Java数据类型 java.lang.Long
    protected Integer displaySize                 ; // display size
    protected Integer type                        ; // 类型
    protected String childTypeName                ;
    protected TypeMetadata childTypeMetadata;
    protected JavaType javaType                   ;
    protected String jdbcType                     ; // 有可能与typeName不一致 可能多个typeName对应一个jdbcType 如point>
    protected String dateScale                    ; // 日期类型 精度
    protected int nullable                   = -1 ; // 是否可以为NULL -1:未配置 1:是  0:否
    protected int caseSensitive              = -1 ; // 是否区分大小写
    protected int currency = -1                   ; // 是否是货币
    protected int signed = -1                     ; // 是否可以带正负号
    protected int autoIncrement = -1              ; // 是否自增
    protected Integer incrementSeed          =  1 ; // 自增起始值
    protected Integer incrementStep          =  1 ; // 自增增量
    protected int primary = -1                    ; // 是否主键
    protected int generated = -1                  ; // 是否generated
    protected Object defaultValue                 ; // 默认值
    protected String defaultConstraint            ; // 默认约束名
    protected String charset                      ; // 编码
    protected String collate                      ; // 排序编码
    protected Aggregation aggregation; //聚合类型
    protected int withTimeZone                = -1;
    protected int withLocalTimeZone           = -1;
    protected Column reference                    ; // 外键依赖列
    protected Integer srid                        ; // SRID
    protected boolean array                       ; // 是否数组

    protected Boolean index                       ; // 是否需要创建索引
    protected Boolean store                       ; // 是否需要存储
    protected String analyzer                     ; // 分词器
    protected String searchAnalyzer               ; // 查询分词器
    protected Integer ignoreAbove                 ; // 可创建索引的最大词长度

    protected Integer position                    ; // 在表或索引中的位置, 如果需要在第一列 设置成0
    protected String order                        ; // 在索引中的排序方式ASC | DESC

    protected String after                        ; // 修改列时 在表中的位置
    protected String before                       ; // 修改列时 在表中的位置
    protected int onUpdate = -1                   ; // 是否在更新行时 更新这一列数据
    protected Object value                        ;
    protected boolean defaultCurrentDateTime = false;
    protected int parseLvl                      = 0;// 类型解析级别0:未解析 1:column解析 2:adapter解析

    public Column(){
    }
    public Column(Table table, String name, String type){
        setTable(table);
        setName(name);
        setType(type);
    }
    public Column(String name){
        setName(name);
    }
    public Column(Schema schema, String table, String name){
        this(null, schema, table, name);
    }
    public Column(Catalog catalog, Schema schema, String table, String name){
        setCatalog(catalog);
        setSchema(schema);
        setName(name);
        setTable(table);
    }
    public Column(String name, String type, int precision, int scale){
        this.name = name;
        setType(type);
        this.precision = precision;
        this.scale = scale;
    }

    public Column(String name, String type, int precision){
        this.name = name;
        setType(type);
        this.precision = precision;
    }
    public Column(Table table, String name, String type, int precision, int scale){
        setTable(table);
        this.name = name;
        setType(type);
        this.precision = precision;
        this.scale = scale;
    }

    public Column(Table table, String name, String type, int precision){
        setTable(table);
        this.name = name;
        setType(type);
        this.precision = precision;
    }

    public Column(String name, String type){
        this.name = name;
        setType(type);
    }


    public Column drop(){
        this.action = ACTION.DDL.COLUMN_DROP;
        return super.drop();
    }

    public void setKeyword(String keyword) {
        this.keyword = keyword;
    }
    public boolean isArray() {
        return array;
    }

    public Column setArray(boolean array) {
        this.array = array;
        return this;
    }

    public String getDateScale() {
        if(getmap && null != update){
            return update.getDateScale();
        }
        return dateScale;
    }

    public int getWithTimeZone() {
        return withTimeZone;
    }

    public void setWithTimeZone(int withTimeZone) {
        this.withTimeZone = withTimeZone;
    }

    public int getWithLocalTimeZone() {
        return withLocalTimeZone;
    }

    public void setWithLocalTimeZone(int withLocalTimeZone) {
        this.withLocalTimeZone = withLocalTimeZone;
    }

    public Column setDateScale(String dateScale) {
        if(setmap && null != update){
            update.setDateScale(dateScale);
            return this;
        }
        this.dateScale = dateScale;
        return this;
    }


    public String getClassName() {
        if(getmap && null != update){
            return update.getClassName();
        }
        return className;
    }

    public Column setClassName(String className) {
        if(setmap && null != update){
            update.setClassName(className);
            return this;
        }
        this.className = className;
        return this;
    }

    public String getChildTypeName() {
        if(getmap && null != childTypeName){
            return update.getChildTypeName();
        }
        return childTypeName;
    }

    public Column setChildTypeName(String childTypeName) {
        if(setmap && null != update){
            update.setChildTypeName(childTypeName);
            return this;
        }
        this.childTypeName = childTypeName;
        return this;
    }

    public TypeMetadata getChildTypeMetadata() {
        if(getmap && null != update){
            return update.childTypeMetadata;
        }
        if(array && null != childTypeMetadata){
            childTypeMetadata.setArray(array);
        }
        return childTypeMetadata;
    }

    public Column setChildTypeMetadata(TypeMetadata childTypeMetadata) {
        if(setmap && null != update){
            update.setChildTypeMetadata(childTypeMetadata);
            return this;
        }
        this.childTypeMetadata = childTypeMetadata;
        return this;
    }

    public Integer getDisplaySize() {
        if(getmap && null != update){
            return update.getDisplaySize();
        }
        return displaySize;
    }

    public Column setDisplaySize(Integer displaySize) {
        if(setmap && null != update){
            update.setDisplaySize(displaySize);
            return this;
        }
        this.displaySize = displaySize;
        return this;
    }

    public Integer getType() {
        if(getmap && null != update){
            return update.getType();
        }
        return type;
    }

    /**
     * 设置数据类型 根据 jdbc定义的类型ID
     * @param type type
     * @return Column
     */
    public Column setType(Integer type) {
        if(setmap && null != update){
            update.setType(type);
            return this;
        }
        if(this.type != type) {
            this.className = null;
        }
        this.type = type;
        return this;
    }
    /**
     * 设置数据类型 根据数据库定义的数据类型 实际调用了setTypeName(String)
     * @param type  数据类型 如 int  varchar(10) decimal(18, 6)
     * @return Column
     */
    public Column setType(String type) {
        if(setmap && null != update){
            update.setType(type);
            return this;
        }
        return setTypeName(type);
    }

    public String getTypeName() {
        if(getmap && null != update){
            return update.getTypeName();
        }
        if(null == typeName){
            if(null != typeMetadata && typeMetadata != TypeMetadata.ILLEGAL && typeMetadata != TypeMetadata.NONE){
                typeName = typeMetadata.getName();
            }
        }
        return typeName;
    }

    public String getJdbcType() {
        if(getmap && null != update){
            return update.jdbcType;
        }
        return jdbcType;
    }

    public Column setJdbcType(String jdbcType) {
        if(setmap && null != update){
            update.setJdbcType(jdbcType);
            return this;
        }
        this.jdbcType = jdbcType;
        return this;
    }

    /**
     * 设置数据类型 根据数据库定义的数据类型
     * @param typeName 数据类型 如 int  varchar(10) decimal(18, 6)
     * @return Column
     */
    public Column setTypeName(String typeName) {
        if(setmap && null != update){
            update.setTypeName(typeName);
            return this;
        }
        this.typeName = typeName;
        parseType(1);
        fullType = null;
        return this;
    }
    public Column parseType(int lvl){
        if(lvl <= parseLvl){
            return this;
        }
        String typeName = this.typeName;
        if(null != typeName){
            //数组类型
            if(typeName.contains("[]")){
                setArray(true);
            }
            //数组类型
            if(typeName.startsWith("_")){
                typeName = typeName.substring(1);
                setArray(true);
            }
            typeName = typeName.trim().replace("'","");

            if(typeName.toUpperCase().contains("IDENTITY")){
                autoIncrement(true);
                if(typeName.contains(" ")) {
                    // TYPE_NAME=int identity
                    typeName = typeName.split(" ")[0];
                }
            }

            if(typeName.contains("(")){
                //decimal(10, 2) varchar(10) geometry(Polygon, 4326) geometry(Polygon) geography(Polygon, 4326)
                this.precision = 0;
                this.scale = 0;
                String tmp = typeName.substring(typeName.indexOf("(")+1, typeName.indexOf(")"));
                if(tmp.contains(",")){
                    //有精度或srid
                    String[] lens = tmp.split("\\,");
                    if(BasicUtil.isNumber(lens[0])) {
                        setPrecision(BasicUtil.parseInt(lens[0], null));
                        setScale(BasicUtil.parseInt(lens[1], null));
                    }else{
                        setChildTypeName(lens[0]);
                        setSrid(BasicUtil.parseInt(lens[1], null));
                    }
                }else{
                    //没有精度和srid
                    if(BasicUtil.isNumber(tmp)){
                        setPrecision(BasicUtil.parseInt(tmp, null));
                    }else{
                        setChildTypeName(tmp);
                    }
                }
                typeName = typeName.substring(0, typeName.indexOf("(") );
            }
        }
        if(!BasicUtil.equalsIgnoreCase(typeName, this.typeName)) {
            this.className = null;
        }
        try{
            TypeMetadata tm = StandardTypeMetadata.valueOf(typeName.toUpperCase());
            if(null != tm){
                this.typeMetadata = tm;
                ignoreLength = tm.ignoreLength();
                ignorePrecision = tm.ignorePrecision();
                ignoreScale = tm.ignoreScale();
            }
        }catch (Exception e){

        }
        this.typeName = typeName;
        if(ignorePrecision == 1){
            if(null == length || length == -1){
                length = precision;
            }
        }
        parseLvl = lvl;
        return this;
    }

    public int getParseLvl() {
        return parseLvl;
    }

    public void setParseLvl(int parseLvl) {
        this.parseLvl = parseLvl;
    }

    public Column setFullType(String fullType) {
        this.fullType = fullType;
        return this;
    }
    public String getFullType(){
        if(getmap && null != update){
            return update.getFullType();
        }
        if(null != fullType){
            return fullType;
        }
        StringBuilder builder = new StringBuilder();
        String type = null;
        if(null != typeMetadata && typeMetadata != TypeMetadata.NONE && typeMetadata != TypeMetadata.ILLEGAL){
            type = typeMetadata.getName();
        }else{
            type = getTypeName();
        }
        builder.append(type);
        boolean appendLength = false;
        boolean appendPrecision = false;
        boolean appendScale = false;

        if(ignoreLength() != 1){
            if(null == length){
                //null表示没有设置过,有可能用的precision,复制precision值
                // -1也表示设置过了不要再用length覆盖
                if(null != precision && precision != -1){
                    length = precision;
                }
            }
            if(null != length){
                if(length > 0 || length == -2){ //-2:max
                    appendLength = true;
                }
            }
        }
        if(ignorePrecision() != 1){
            if(null == precision){
                //null表示没有设置过,有可能用的length,复制length
                // -1也表示设置过了不要再用length覆盖
                if(null != length && length != -1){
                    precision = length;
                }
            }
            if(null != precision){
                if(precision > 0){
                    appendPrecision = true;
                }
            }
        }
        if(ignoreScale() != 1){
            if(null != scale){
                if(scale > 0){
                    appendScale = true;
                }
            }
        }
        if(appendLength || appendPrecision || appendScale){
            builder.append("(");
        }
        if(appendLength){
            if(length == -2){
                builder.append("max");
            }else {
                builder.append(length);
            }
        }else {
            if (appendPrecision) {
                builder.append(precision);
            }
            if(appendScale){//可能单独出现
                if(appendPrecision){
                    builder.append(",");
                }
                builder.append(scale);
            }
        }
        if(appendLength || appendPrecision || appendScale){
            builder.append(")");
        }

        String child = getChildTypeName();
        Integer srid = getSrid();
        if(null != child){
            builder.append("(");
            builder.append(child);
            if(null != srid){
                builder.append(",").append(srid);
            }
            builder.append(")");
        }
        if(isArray()){
            builder.append("[]");
        }

        fullType = builder.toString();
        return fullType;
    }

    /**
     * 精确长度 根据数据类型返回precision或length
     * @return Integer
     */
    public Integer getPrecisionLength(){
        if(null != precisionLength && precisionLength != -1){
            return precisionLength;
        }
        if(null != precision && precision != -1){
            precisionLength = precision;
        }else{
            precisionLength = length;
        }
        return precisionLength;
    }
    public Integer getLength() {
        if(getmap && null != update){
            return update.getLength();
        }
        if(null != length && length != -1){
            return length;
        }
        return precision;
    }
    public Column setLength(Integer length) {
        if(setmap && null != update){
            update.setLength(length);
            return this;
        }
        this.length = length;
        fullType = null;
        return this;
    }

    public Integer getPrecision() {
        if(getmap && null != update){
            return update.getPrecision();
        }
        if(null != precision && precision != -1){
            return precision;
        }
        return length;
    }

    public Column setPrecision(Integer precision) {
        if(setmap && null != update){
            update.setPrecision(precision);
            return this;
        }
        this.precision = precision;
        fullType = null;
        return this;
    }
    public Column setPrecision(Integer precision, Integer scale) {
        if(setmap && null != update){
            update.setPrecision(precision, scale);
            return this;
        }
        this.precision = precision;
        this.scale = scale;
        fullType = null;
        return this;
    }


    public Object getValue() {
        if(getmap && null != update){
            return update.value;
        }
        return value;
    }

    public Column setValue(Object value) {
        if(setmap && null != update){
            update.setValue(value);
            return this;
        }
        this.value = value;
        return this;
    }

    public int isCaseSensitive() {
        if(getmap && null != update){
            return update.caseSensitive;
        }
        return caseSensitive;
    }

    public Column setCaseSensitive(int caseSensitive) {
        if(setmap && null != update){
            update.setCaseSensitive(caseSensitive);
            return this;
        }
        this.caseSensitive = caseSensitive;
        return this;
    }
    public Column caseSensitive(int caseSensitive) {
        return setCaseSensitive(caseSensitive);
    }
    public Column caseSensitive(Boolean caseSensitive) {
        if(setmap && null != update){
            update.caseSensitive(caseSensitive);
            return this;
        }
        if(null != caseSensitive) {
            if(caseSensitive) {
                this.caseSensitive = 1;
            }else {
                this.caseSensitive = 0;
            }
        }
        return this;
    }

    public int isCurrency() {
        if(getmap && null != update){
            return update.currency;
        }
        return currency;
    }

    public Column setCurrency(int currency) {
        if(setmap && null != update){
            update.setCurrency(currency);
            return this;
        }
        this.currency = currency;
        return this;
    }
    public Column currency(int currency) {
        return setCurrency(currency);
    }
    public Column setCurrency(Boolean currency) {
        return currency(currency);
    }
    public Column currency(Boolean currency) {
        if(setmap && null != update){
            update.currency(currency);
            return this;
        }
        if(null != currency){
            if(currency){
                this.currency = 1;
            }else{
                this.currency = 0;
            }
        }
        return this;
    }

    public int isSigned() {
        if(getmap && null != update){
            return update.signed;
        }
        return signed;
    }

    public Column setSigned(int signed) {
        if(setmap && null != update){
            update.setSigned(signed);
            return this;
        }
        this.signed = signed;
        return this;
    }
    public Column signed(int signed) {
        return setSigned(signed);
    }
    public Column setSigned(Boolean signed) {
        if(setmap && null != update){
            update.setSigned(signed);
            return this;
        }
        if(null != signed){
            if(signed){
                this.signed = 1;
            }else{
                this.signed = 0;
            }
        }
        return this;
    }

    public Aggregation getAggregation() {
        if(getmap && null != update){
            return update.aggregation;
        }
        return aggregation;
    }

    public Column setAggregation(Aggregation aggregation) {
        if(setmap && null != update){
            update.setAggregation(aggregation);
            return this;
        }
        this.aggregation = aggregation;
        return this;
    }

    public Integer getScale() {
        if(getmap && null != update){
            return update.getScale();
        }
        return scale;
    }

    public Column setScale(Integer scale) {
        if(setmap && null != update){
            update.setScale(scale);
            return this;
        }
        this.scale = scale;
        fullType = null;
        return this;
    }

    public int isNullable() {
        if(getmap && null != update){
            return update.nullable;
        }
        return nullable;
    }

    public Column setNullable(int nullable) {
        if(setmap && null != update){
            update.setNullable(nullable);
            return this;
        }
        this.nullable = nullable;
        return this;
    }
    public Column nullable(int nullable) {
        return setNullable(nullable);
    }
    public Column setNullable(Boolean nullable) {
        return nullable(nullable);
    }
    public Column nullable(Boolean nullable) {
        if(setmap && null != update){
            update.nullable(nullable);
            return this;
        }
        if(null != nullable){
            if(nullable){
                this.nullable = 1;
            }else{
                this.nullable = 0;
            }
        }
        return this;
    }

    public int isAutoIncrement() {
        if(getmap && null != update){
            return update.autoIncrement;
        }
        return autoIncrement;
    }

    public Column setAutoIncrement(int autoIncrement) {
        if(setmap && null != update){
            update.setAutoIncrement(autoIncrement);
            return this;
        }
        this.autoIncrement = autoIncrement;
        if(autoIncrement == 1){
            nullable(false);
        }
        fullType = null;
        return this;
    }

    public Column autoIncrement(int autoIncrement) {
        return setAutoIncrement(autoIncrement);
    }
    //不要实现 setAutoIncrement有些工具会因为参数与属性类型不一致抛出异常
    public Column setAutoIncrement(Boolean autoIncrement) {
        return autoIncrement(autoIncrement);
    }
    public Column autoIncrement(Boolean autoIncrement) {
        if(setmap && null != update){
            update.autoIncrement(autoIncrement);
            return this;
        }
        if(null != autoIncrement) {
            if(autoIncrement){
                this.autoIncrement = 1;
                nullable(false);
            }else{
                this.autoIncrement = 0;
            }
        }
        return this;
    }

    /**
     * 递增列
     * @param seed 起始值
     * @param step 增量
     * @return  Column
     */
    public Column setAutoIncrement(int seed, int step) {
        if(setmap && null != update){
            update.setAutoIncrement(seed, step);
            return this;
        }
        setAutoIncrement(1);
        this.incrementSeed= seed;
        this.incrementStep = step;
        return this;
    }

    public int isPrimaryKey() {
        if(getmap && null != update){
            return update.primary;
        }
        return primary;
    }

    public Column setPrimary(int primary) {
        if(setmap && null != update){
            update.setPrimary(primary);
            return this;
        }
        this.primary = primary;
        return this;
    }
    public Column primary(int primary) {
        return setPrimary(primary);
    }
    public Column setPrimary(Boolean primary) {
        return primary(primary);
    }
    public Column setPrimaryKey(Boolean primary) {
        return primary(primary);
    }
    public Column primary(Boolean primary) {
        if(setmap && null != update){
            update.primary(primary);
            return this;
        }
        if(null != primary){
            if(primary){
                this.primary = 1 ;
                nullable(false);
            }else{
                this.primary = 0 ;
            }
        }
        return this;
    }

    public int isGenerated() {
        if(getmap && null != update){
            return update.generated;
        }
        return generated;
    }

    public Column setGenerated(int generated) {
        if(setmap && null != update){
            update.setGenerated(generated);
            return this;
        }
        this.generated = generated;
        return this;
    }
    public Column generated(int generated) {
       return setGenerated(generated);
    }
    public Column setGenerated(Boolean generated) {
        return generated(generated);
    }
    public Column generated(Boolean generated) {
        if(setmap && null != update){
            update.generated(generated);
            return this;
        }
        if(null != generated){
            if(generated){
                this.generated = 1;
            }else{
                this.generated = 0;
            }
        }
        return this;
    }

    public Object getDefaultValue() {
        if(getmap && null != update){
            return update.defaultValue;
        }
        return defaultValue;
    }

    public Column setDefaultValue(Object defaultValue) {
        if(setmap && null != update){
            update.setDefaultValue(defaultValue);
            return this;
        }
        this.defaultValue = defaultValue;
        return this;
    }
    public Column setDefaultCurrentDateTime(boolean currentDateTime){
        if(setmap && null != update){
            update.setDefaultCurrentDateTime(currentDateTime);
            return this;
        }
        this.defaultCurrentDateTime = currentDateTime;
        return this;
    }
    public Column setDefaultCurrentDateTime(){
        return setDefaultCurrentDateTime(true);
    }
    public boolean isDefaultCurrentDateTime(){
        return this.defaultCurrentDateTime;
    }

    public String getDefaultConstraint() {
        if(getmap && null != update){
            return update.defaultConstraint;
        }
        return defaultConstraint;
    }

    public Column setDefaultConstraint(String defaultConstraint) {
        if(setmap && null != update){
            update.setDefaultConstraint(defaultConstraint);
            return this;
        }
        this.defaultConstraint = defaultConstraint;
        return this;
    }

    public Integer getPosition() {
        if(getmap && null != update){
            return update.position;
        }
        return position;
    }

    public String getOrder() {
        if(getmap && null != update){
            return update.order;
        }
        return order;
    }

    public Column setOrder(String order) {
        if(setmap && null != update){
            update.setOrder(order);
            return this;
        }
        this.order = order;
        return this;
    }

    public Column setPosition(Integer position) {
        if(setmap && null != update){
            update.setPosition(position);
            return this;
        }
        this.position = position;
        return this;
    }

    public String getAfter() {
        if(getmap && null != update){
            return update.after;
        }
        return after;
    }

    public Integer getIncrementSeed() {
        if(getmap && null != update){
            return update.incrementSeed;
        }
        return incrementSeed;
    }

    public Column setIncrementSeed(Integer incrementSeed) {
        if(setmap && null != update){
            update.setIncrementSeed(incrementSeed);
            return this;
        }
        this.incrementSeed = incrementSeed;
        return this;
    }

    public Integer getIncrementStep() {
        if(getmap && null != update){
            return update.incrementStep;
        }
        return incrementStep;
    }

    public Column setIncrementStep(Integer incrementStep) {
        if(setmap && null != update){
            update.setIncrementStep(incrementStep);
            return this;
        }
        this.incrementStep = incrementStep;
        return this;
    }

    public int isOnUpdate() {
        if(getmap && null != update){
            return update.onUpdate;
        }
        return onUpdate;
    }

    public Column setOnUpdate(int onUpdate) {
        this.onUpdate = onUpdate;
        return this;
    }
    public Column onUpdate(int onUpdate) {
        return setOnUpdate(onUpdate);
    }
    public Column setOnUpdate(boolean onUpdate) {
        return onUpdate(onUpdate);
    }
    public Column onUpdate(boolean onUpdate) {
        if(onUpdate){
            this.onUpdate = 1;
        }else{
            this.onUpdate = -1;
        }
        return this;
    }

    public Column setAfter(String after) {
        if(setmap && null != update){
            update.setAfter(after);
            return this;
        }
        this.after = after;
        return this;
    }

    public String getOriginalName() {
        if(getmap && null != update){
            return update.originalName;
        }
        return originalName;
    }

    public Column setOriginalName(String originalName) {
        if(setmap && null != update){
            update.setOriginalName(originalName);
            return this;
        }
        this.originalName = originalName;
        return this;
    }

    public String getBefore() {
        if(getmap && null != update){
            return update.before;
        }
        return before;
    }

    public String getCharset() {
        if(getmap && null != update){
            return update.charset;
        }
        return charset;
    }

    public Column setCharset(String charset) {
        if(setmap && null != update){
            update.setCharset(charset);
            return this;
        }
        this.charset = charset;
        return this;
    }

    public String getCollate() {
        if(getmap && null != update){
            return update.collate;
        }
        return collate;
    }

    public Column setCollate(String collate) {
        if(setmap && null != update){
            update.setCollate(collate);
            return this;
        }
        this.collate = collate;
        return this;
    }
    public Column setBefore(String before) {
        if(setmap && null != update){
            update.setBefore(before);
            return this;
        }
        this.before = before;
        return this;
    }
    public boolean equals(Column column) {
        return equals(column, true);
    }
    
    public boolean equals(Column column, boolean ignoreCase) {
        if(null == column){
            return false;
        }
        if (!BasicUtil.equals(name, column.getName(), ignoreCase)) {
            return false;
        }
        TypeMetadata columnTypeMetadata = column.getTypeMetadata();
        TypeMetadata origin = null;
        TypeMetadata columnOrigin = null;
        if(null != typeMetadata){
            origin = typeMetadata.getOrigin();
        }
        if(null != columnTypeMetadata){
            columnOrigin = columnTypeMetadata.getOrigin();
        }

        if(!BasicUtil.equals(typeMetadata, columnTypeMetadata, ignoreCase)
                && !BasicUtil.equals(typeMetadata, columnOrigin, ignoreCase)
                && !BasicUtil.equals(origin, columnTypeMetadata, ignoreCase)
                && !BasicUtil.equals(origin, columnOrigin, ignoreCase)
        ){
            return false;
        }
        if(null == typeMetadata || 0 == typeMetadata.ignoreLength()) {
            if (!BasicUtil.equals(getLength(), column.getLength())) {
                return false;
            }
        }
        if(null == typeMetadata || 0 == typeMetadata.ignorePrecision()) {
            if (!BasicUtil.equals(getPrecision(), column.getPrecision())) {
                return false;
            }
        }
        if(null == typeMetadata ||  0 == typeMetadata.ignoreScale()) {
            if (!BasicUtil.equals(getScale(), column.getScale())) {
                return false;
            }
        }
        if(!BasicUtil.equals(getDefaultValue(), column.getDefaultValue())){
            return false;
        }
        if(!BasicUtil.equals(getComment(), column.getComment())){
            return false;
        }
        if(!BasicUtil.equals(isNullable(), column.isNullable())){
            return false;
        }
        if(!BasicUtil.equals(isAutoIncrement(), column.isAutoIncrement())){
            return false;
        }
        if(!BasicUtil.equals(getCharset(), column.getCharset(), ignoreCase)){
            return false;
        }
        if(!BasicUtil.equals(isPrimaryKey(), column.isPrimaryKey())){
            return false;
        }
        /*if(!BasicUtil.equals(getPosition(), column.getPosition())){
            return false;
        }*/
        return true;
    }
    
    public TypeMetadata getTypeMetadata() {
        if(getmap && null != update){
            return update.typeMetadata;
        }
        if(array && null != typeMetadata){
            typeMetadata.setArray(array);
        }
        return typeMetadata;
    }

    public TypeMetadata.CATEGORY getTypeCategory(){
        if(null != typeMetadata){
            return typeMetadata.getCategory();
        }
        return TypeMetadata.CATEGORY.NONE;
    }
    public String getAnalyzer() {
        return analyzer;
    }

    public Column setAnalyzer(String analyzer) {
        this.analyzer = analyzer;
        return this;
    }

    public String getSearchAnalyzer() {
        return searchAnalyzer;
    }

    public Column setSearchAnalyzer(String searchAnalyzer) {
        this.searchAnalyzer = searchAnalyzer;
        return this;
    }

    public Integer getIgnoreAbove() {
        return ignoreAbove;
    }

    public Column setIgnoreAbove(Integer ignoreAbove) {
        this.ignoreAbove = ignoreAbove;
        return this;
    }

    public Column setTypeMetadata(TypeMetadata typeMetadata) {
        if(setmap && null != update){
            update.setTypeMetadata(typeMetadata);
            return this;
        }
        this.typeMetadata = typeMetadata;
        return this;
    }

    public Column setNewName(String newName, boolean setmap, boolean getmap) {
        if(null == update){
            update(setmap, getmap);
        }
        update.setName(newName);
        Table table = getTable();
        if(null != table){
            //修改主键列名
            LinkedHashMap<String, Column> pks = table.getPrimaryKeyColumns();
            if(null != pks && pks.containsKey(this.getName().toUpperCase())){
                pks.remove(this.getName().toUpperCase());
                pks.put(newName.toUpperCase(), update);
            }
        }
        return update;
    }
    
    public JavaType getJavaType() {
        if(getmap && null != update){
            return update.javaType;
        }
        return javaType;
    }

    
    public Column setJavaType(JavaType javaType) {
        if(setmap && null != update){
            update.setJavaType(javaType);
            return this;
        }
        this.javaType = javaType;
        return this;
    }


    public Integer getSrid() {
        if(getmap && null != update){
            return update.srid;
        }
        return srid;
    }

    public Column setSrid(Integer srid) {
        if(setmap && null != update){
            update.setSrid(srid);
            return this;
        }
        this.srid = srid;
        return this;
    }

    public Column getReference() {
        if(getmap && null != update){
            return update.reference;
        }
        return reference;
    }

    public Column setReference(Column reference) {
        if(setmap && null != update){
            update.setReference(reference);
            return this;
        }
        this.reference = reference;
        return this;
    }

    public Boolean getIndex() {
        return index;
    }

    public Column setIndex(Boolean index) {
        this.index = index;
        return this;
    }

    public Boolean getStore() {
        return store;
    }

    public Column setStore(Boolean store) {
        this.store = store;
        return this;
    }

    public void ignoreLength(int ignoreLength) {
        this.ignoreLength = ignoreLength;
    }

    public void ignorePrecision(int ignorePrecision) {
        this.ignorePrecision = ignorePrecision;
    }

    public void ignoreScale(int ignoreScale) {
        this.ignoreScale = ignoreScale;
    }

    /**
     * 是否需要指定精度 主要用来识别能取出精度，但DDL不需要精度的类型
     * 精确判断通过adapter
     * @return boolean
     */
    public int ignoreLength(){
        if(-1 != ignoreLength){
            return ignoreLength;
        }
        if(null != typeMetadata){
            return typeMetadata.ignoreLength();
        }
        return ignoreLength;
    }

    /**
     * 是否需要指定精度 主要用来识别能取出精度，但DDL不需要精度的类型
     * 精确判断通过adapter
     * @return boolean
     */
    public int ignorePrecision(){
        if(-1 != ignorePrecision){
            return ignorePrecision;
        }
        if(null != typeMetadata){
            return typeMetadata.ignorePrecision();
        }
        return ignorePrecision;
    }

    /**
     * 是否需要指定精度 主要用来识别能取出精度，但DDL不需要精度的类型
     * 精确判断通过adapter
     * @return boolean
     */
    public int ignoreScale(){
        if(-1 != ignoreScale){
            return ignoreScale;
        }
        if(null != typeMetadata){
            return typeMetadata.ignoreScale();
        }
        return ignoreScale;
    }
    public String toString(){
        StringBuilder builder = new StringBuilder();
        builder.append(name).append(" ");
        builder.append(getFullType());
        if(BasicUtil.isNotEmpty(defaultValue)){
            builder.append(" default ").append(defaultValue);
        }
        return builder.toString();
    }
    public String getKeyword() {
        return this.keyword;
    }
}

