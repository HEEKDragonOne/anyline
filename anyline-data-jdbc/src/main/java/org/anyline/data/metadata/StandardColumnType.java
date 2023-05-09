package org.anyline.data.metadata;

import com.fasterxml.jackson.databind.JsonNode;
import org.anyline.adapter.init.ConvertAdapter;
import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.entity.Point;
import org.anyline.entity.data.DatabaseType;
import org.anyline.entity.metadata.ColumnType;
import org.anyline.util.Base64Util;
import org.anyline.util.BasicUtil;
import org.anyline.util.BeanUtil;
import org.anyline.util.DateUtil;

import java.lang.reflect.Field;
import java.math.BigDecimal;
import java.sql.Time;
import java.time.LocalDateTime;
import java.util.Date;

import static org.anyline.entity.data.DatabaseType.*;


public enum StandardColumnType implements ColumnType {


    /* *****************************************************************************************************************
     *
     * 													SQL DATA TYPE
     *
     * =================================================================================================================
     * String
     * String-format
     * number-int/long
     * number-double/float
     * date
     * byte[]
     * byte[]-file
     * byte[]-geometry
     *
     ******************************************************************************************************************/

    /* *****************************************************************************************************************
     *
     *                                              String
     *
     * ****************************************************************************************************************/
    /**
     * mysql,pg
     */
    CHAR("CHAR", new DatabaseType[]{MYSQL, PostgreSQL}, String.class, false, true){
        
        public Object write(Object value, Object def, boolean placeholder){
            if(null == value){
                value = def;
            }
            if(value instanceof String){
            }else if(value instanceof Date){
                value = DateUtil.format((Date)value);
            }else{
                value = value.toString();
            }
            if(!placeholder){
                value = "'" + value + "'";
            }
            return value;
        }
    }
    /**
     * oracle,mssql
     */
    ,NCHAR("NCHAR", new DatabaseType[]{ORACLE, MSSQL},String.class, false, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * oracle
     */
    ,CLOB("CLOB", new DatabaseType[]{ORACLE}, String.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * oracle
     */
    ,NCLOB("NCLOB", new DatabaseType[]{ORACLE}, String.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * mssql
     */
    ,NVARCHAR("NVARCHAR", new DatabaseType[]{MSSQL}, String.class, false, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * oracle
     */
    ,NVARCHAR2("NVARCHAR2", new DatabaseType[]{ORACLE}, String.class, false, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * mysql
     */
    ,LONGTEXT("LONGTEXT", new DatabaseType[]{MYSQL}, String.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * mysql
     */
    ,MEDIUMTEXT("MEDIUMTEXT", new DatabaseType[]{MYSQL}, String.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * mysql,pg,sqlite
     */
    ,TEXT("TEXT", new DatabaseType[]{MYSQL, PostgreSQL, SQLite}, String.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * mssql
     */
    ,NTEXT("NTEXT", new DatabaseType[]{MSSQL}, String.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * mysql
     */
    ,TINYTEXT("TINYTEXT", new DatabaseType[]{MYSQL}, String.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * mysql,pg,oracle
     */
    ,VARCHAR("VARCHAR", new DatabaseType[]{MYSQL, PostgreSQL, ORACLE}, String.class, false, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * oracle
     */
    ,VARCHAR2("VARCHAR2", new DatabaseType[]{ORACLE}, String.class, false, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * mssql
     */
    ,SYSNAME("SYSNAME", new DatabaseType[]{MSSQL}, String.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * pg
     */
    ,UUID("UUID", new DatabaseType[]{PostgreSQL}, String.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){
            if(null == value){
                value = def;
            }
            if(null != value){
                value = java.util.UUID.fromString(value.toString());
            }
            if(null == value){
                value = def;
            }
            return value;
        }
    }
    /**
     * mssql
     */
    ,UNIQUEIDENTIFIER("UNIQUEIDENTIFIER", new DatabaseType[]{MSSQL}, String.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return CHAR.write(value, def, placeholder);}
    }
    /**
     * mysql(byte[]),mssql
     */
    ,BINARY("BINARY", new DatabaseType[]{MYSQL, MSSQL}, byte[].class, false, true)
    /**
     * mysql(byte[]),mssql
     */
    ,VARBINARY("VARBINARY", new DatabaseType[]{MYSQL, MSSQL}, byte[].class, false, true)


    /* *****************************************************************************************************************
     *
     *                                              String-format
     *
     * ****************************************************************************************************************/

    /**
     * mysql,pg
     */
    ,JSON("JSON", new DatabaseType[]{MYSQL, PostgreSQL}, String.class, true, true){

        @Override
        public Object convert(Object value, Class target, Object def) {
            if(null == value){
                return def;
            }
            Class transfer = transfer();
            Class compatible = compatible();
            try{
                if(null == target) {
                    JsonNode node = BeanUtil.JSON_MAPPER.readTree(value.toString());
                    if (node.isArray()) {
                        value = DataSet.parseJson(node);
                    } else {
                        value = DataRow.parseJson(node);
                    }
                }else{
                    value = super.convert(value, target, def);
                }
            }catch (Exception e){
                //不能转成DataSet的List
                value = super.convert(value, target, def);
            }
            return value;
        }
        public Object read(Object value, Object def, Class clazz){
            if(null == value){
                return value;
            }
            if(value.getClass() == clazz){
                return value;
            }
            String str = value.toString().trim();
            try{
                JsonNode node = BeanUtil.JSON_MAPPER.readTree(str);
                if(null == clazz) {
                    if (node.isArray()) {
                        value = DataSet.parseJson(node);
                    } else {
                        value = DataRow.parseJson(node);
                    }
                }
            }catch (Exception e){
                e.printStackTrace();
            }
            return value;}

    }

    /**
     * mssql
     */
    ,XML("XML", new DatabaseType[]{MSSQL}, String.class, true, true){}
    /* *****************************************************************************************************************
     *
     *                                              number-int/long
     *
     * ****************************************************************************************************************/
    /**
     * mysql(Boolean),pg(Boolean),mssql
     */
    ,BIT("BIT", new DatabaseType[]{MYSQL, MSSQL}, Boolean.class, true, true)
    /**
     * pg
     */
    ,VARBIT("VARBIT", new DatabaseType[]{PostgreSQL}, Byte[].class, true, true)
    ,SHORT("SHORT", new DatabaseType[]{}, Short.class, true, true)
    /**
     * mysql,mssql
     */
    ,INT("INT", new DatabaseType[]{MYSQL, MSSQL}, Integer.class, true, true)
    /**
     * oracle
     */
    ,LONG("LONG", new DatabaseType[]{ORACLE}, String.class, true, true){}
    /**
     * pg
     */
    ,SERIAL("SERIAL", new DatabaseType[]{PostgreSQL}, Integer.class, true, true)
    /**
     * pg
     */
    ,SERIAL2("SERIAL2", new DatabaseType[]{PostgreSQL}, Integer.class, true, true)
    /**
     * pg
     */
    ,SERIAL4("SERIAL4", new DatabaseType[]{PostgreSQL}, Integer.class, true, true)
    /**
     * pg
     */
    ,SERIAL8("SERIAL8", new DatabaseType[]{PostgreSQL}, Long.class, true, true)
    /**
     * pg
     */
    ,SMALLSERIAL("SERIAL2", new DatabaseType[]{PostgreSQL}, Integer.class, true, true)
    /**
     * pg
     */
    ,BIGSERIAL("SERIAL8", new DatabaseType[]{PostgreSQL}, Long.class, true, true)
    /**
     * pg
     */
    ,INT2("INT2", new DatabaseType[]{PostgreSQL}, Integer.class, true, true)
    /**
     * pg
     */
    ,INT4("INT4", new DatabaseType[]{PostgreSQL}, Integer.class, true, true)
    /**
     * pg
     */
    ,INT8("INT8", new DatabaseType[]{PostgreSQL}, Long.class, true, true)
    /**
     * mysql
     */
    ,BIGINT("BIGINT", new DatabaseType[]{MYSQL}, Long.class, true, true)
    /**
     * mysql
     */
    ,MEDIUMINT("MEDIUMINT", new DatabaseType[]{MYSQL}, Integer.class, true, true)
    /**
     * mysql,sqlite
     */
    ,INTEGER("MEDIUMINT", new DatabaseType[]{MYSQL, SQLite}, Integer.class, true, true)
    /**
     * mysql
     */
    ,SMALLINT("SMALLINT", new DatabaseType[]{MYSQL}, Integer.class, true, true)
    /**
     * mysql
     */
    ,TINYINT("TINYINT", new DatabaseType[]{MYSQL}, Integer.class, true, true){
    }
    /**
     * pg
     */
    ,BOOLEAN("BOOLEAN", new DatabaseType[]{PostgreSQL}, Boolean.class, true, true)
    /**
     * pg
     */
    ,BOOL("BOOLEAN", new DatabaseType[]{PostgreSQL}, Boolean.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return BOOLEAN.write(value, def, placeholder);}
    }
    /* *****************************************************************************************************************
     *
     *                                              number-double/float
     *
     * ****************************************************************************************************************/

    /**
     * mysql,pg,oracle
     */
    ,DECIMAL("DECIMAL", new DatabaseType[]{MYSQL, PostgreSQL, ORACLE}, BigDecimal.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){
            if(null == value){
                value = def;
            }
            BigDecimal result = BasicUtil.parseDecimal(value, null);
            if(null != def && null == result){
                result = BasicUtil.parseDecimal(def, null);
            }
            return result;
        }
    }
    /**
     * mysql
     */
    ,DOUBLE("DOUBLE", new DatabaseType[]{MYSQL}, Double.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){
            if(null == value){
                value = def;
            }
            Double result = BasicUtil.parseDouble(value, null);
            if(null != def && null == result){
                result = BasicUtil.parseDouble(def, null);
            }
            return result;
        }
    }
    /**
     * mysql,  ,oracle(BigDecimal)
     */
    ,FLOAT("FLOAT", new DatabaseType[]{MYSQL, ORACLE}, Float.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){
            if(null == value){
                value = def;
            }
            Float result = BasicUtil.parseFloat(value, null);
            if(null != def && null == result){
                result = BasicUtil.parseFloat(def, null);
            }
            return result;
        }
    }
    /**
     * pg
     */
    ,FLOAT4("FLOAT4", new DatabaseType[]{PostgreSQL}, Float.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){return FLOAT.write(value, def, placeholder);}
    }
    /**
     * ORACLE
     */
    ,ORACLE_FLOAT("FLOAT", new DatabaseType[]{ORACLE}, BigDecimal.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){return DECIMAL.write(value, def, placeholder);}
    }
    /**
     * pg
     */
    ,FLOAT8("FLOAT8", new DatabaseType[]{PostgreSQL}, Double.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){return DOUBLE.write(value, def, placeholder);}
    }
    /**
     * oracle
     */
    ,BINARY_DOUBLE("BINARY_DOUBLE", new DatabaseType[]{ORACLE}, Double.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){return DOUBLE.write(value, def, placeholder);}
    }
    /**
     * oracle
     */
    ,BINARY_FLOAT("BINARY_FLOAT", new DatabaseType[]{ORACLE}, Float.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){return DOUBLE.write(value, def, placeholder);}
    }
    /**
     * pg
     */
    ,MONEY("MONEY", new DatabaseType[]{PostgreSQL}, BigDecimal.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return DECIMAL.write(value, def, placeholder);}
    }
    /**
     * mssql
     */
    ,SMALLMONEY("SMALLMONEY", new DatabaseType[]{MSSQL}, BigDecimal.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return DECIMAL.write(value, def, placeholder);}
    }
    /**
     * mysql,sqlite
     */
    ,NUMERIC("NUMERIC", new DatabaseType[]{MYSQL, SQLite}, BigDecimal.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){return DECIMAL.write(value, def, placeholder);}
    }
    /**
     * oracle
     */
    ,NUMBER("NUMBER", new DatabaseType[]{ORACLE}, BigDecimal.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){return DECIMAL.write(value, def, placeholder);}
    }
    /**
     * mysql(Double),sqlite
     */
    ,REAL("REAL", new DatabaseType[]{MYSQL, SQLite}, Double.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){return FLOAT.write(value, def, placeholder);}
    }
    /* *****************************************************************************************************************
     *
     *                                              date
     *                               write 需要根据数据库类型 由内置函数转换
     *
     * ****************************************************************************************************************/
    /**
     * mysql,pg
     */
    ,DATE("DATE", new DatabaseType[]{MYSQL, PostgreSQL}, java.sql.Date.class, false, false){
        public Object write(Object value, Object def, boolean placeholder){
            if(null == value){
                value = def;
            }
            Date date = DateUtil.parse(value);
            if(null == date && null != def){
                date = DateUtil.parse(def);
            }
            if (null != date) {
                if(placeholder){
                    value = new java.sql.Date(date.getTime());
                }else{
                    value = "'" + DateUtil.format(date, "yyyy-MM-dd");
                }
            }
            return value;
        }
    }
    /**
     * mysql(LocalDateTime)
     */
    ,DATETIME("DATETIME", new DatabaseType[]{MYSQL}, LocalDateTime.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){
            if(null == value){
                value = def;
            }
            Date date = DateUtil.parse(value);
            if(null == date && null != def){
                date = DateUtil.parse(def);
            }
            if(null != date) {
                if(placeholder){
                    value = new java.sql.Timestamp(date.getTime());
                }else{
                    value = "'" + DateUtil.format(date) + "'";
                }
            }else{
                value = null;
            }
            return value;
        }
    }
    /**
     * mssql
     */
    ,DATETIME2("DATETIME2", new DatabaseType[]{MSSQL}, java.sql.Timestamp.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return DATETIME.write(value, def, placeholder);}
    }
    /**
     * mssql<br/>
     * 2020-01-01 15:10:10.0000011
     */
    ,DATETIMEOFFSET("DATETIMEOFFSET", new DatabaseType[]{MSSQL}, java.sql.Timestamp.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return DATETIME.write(value, def, placeholder);}
    }
    /**
     * mssql
     */
    ,SMALLDATETIME("SMALLDATETIME", new DatabaseType[]{MSSQL}, java.sql.Timestamp.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return DATETIME.write(value, def, placeholder);}
    }
    /**
     * mssql
     */
    ,SQL_DATETIMEOFFSET("SQL_DATETIMEOFFSET", new DatabaseType[]{MSSQL}, java.sql.Timestamp.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return DATETIME.write(value, def, placeholder);}
    }
    /**
     * mysql,pg
     */
    ,TIME("TIME", new DatabaseType[]{MYSQL, PostgreSQL}, java.sql.Time.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){
            if(null == value){
                value = def;
            }
            Date date = DateUtil.parse(value);
            if(null == date && null != def){
                date = DateUtil.parse(def);
            }
            if(null != date) {
                if(placeholder){
                    value = new Time(date.getTime());
                }else{
                    value = "'" + DateUtil.format(date, "HH:mm:ss") + "'";
                }
            }else{
                value = null;
            }
            return value;
        }
    }
    /**
     * pg
     */
    ,TIMEZ("TIMEZ", new DatabaseType[]{PostgreSQL}, java.sql.Time.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){
            return TIME.write(value, def, placeholder);
        }
    }
    /**
     * mysql,pg,oracle
     */
    ,TIMESTAMP("TIMESTAMP", new DatabaseType[]{MYSQL, PostgreSQL, ORACLE}, java.sql.Timestamp.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){
            return DATETIME.write(value, def, placeholder);
        }
    }
    /**
     * pg
     */
    ,TIMESTAMP_ZONE("TIMESTAMP", new DatabaseType[]{PostgreSQL}, java.sql.Timestamp.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){
            return DATETIME.write(value, def, placeholder);
        }
    }
    /**
     * pg
     */
    ,TIMESTAMP_LOCAL_ZONE("TIMESTAMP", new DatabaseType[]{PostgreSQL}, java.sql.Timestamp.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){
            return DATETIME.write(value, def, placeholder);
        }
    }
    /**
     * mysql
     */
    ,YEAR("YEAR", new DatabaseType[]{MYSQL}, java.sql.Date.class, true, true){
        public Object write(Object value, Object def, boolean placeholder){
            return DATE.write(value, def, placeholder);
        }
    }
    /* *****************************************************************************************************************
     *
     *                                              byte[]
     *
     * ****************************************************************************************************************/
    /**
     * mysql(byte[]),  ,oracle,sqlite
     */
    ,BLOB("BLOB", new DatabaseType[]{MYSQL, ORACLE, SQLite}, byte[].class, true, true){
        public Object read(Object value, Object def, Class clazz){
            if(clazz == byte[].class){

            }else if(clazz == String.class){
                value = new String((byte[])value);
            }
            return value;
        }
        public Object write(Object value, Object def, boolean placeholder){
            if(null == value){
                value = def;
            }
            if(value instanceof byte[]){

            }else {
                if(value instanceof String){
                    String str = (String)value;
                    if(Base64Util.verify(str)){
                        try {
                            value = Base64Util.decode(str);
                        }catch (Exception e){
                            value = str.getBytes();
                        }
                    }else{
                        value = str.getBytes();
                    }
                }
            }
            return value;
        }
    }
    /**
     * mysql
     */
    ,LONGBLOB("LONGBLOB", new DatabaseType[]{MYSQL}, byte[].class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return BLOB.write(value, def, placeholder);}
    }
    /**
     * mysql
     */
    ,MEDIUMBLOB("MEDIUMBLOB", new DatabaseType[]{MYSQL}, byte[].class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return BLOB.write(value, def, placeholder);}
    }
    /**
     * mysql
     */
    ,TINYBLOB("TINYBLOB", new DatabaseType[]{MYSQL}, byte[].class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return BLOB.write(value, def, placeholder);}
    }
    /**
     * mysql
     */
    ,MULTILINESTRING("MULTILINESTRING", new DatabaseType[]{MYSQL}, byte[].class, true, true)
    /**
     * pg
     */
    ,BYTEA("BYTEA", new DatabaseType[]{PostgreSQL}, byte[].class, true, true)
    /**
     * pg
     */
    ,JSONB("JSONB", new DatabaseType[]{PostgreSQL}, byte[].class, true, true){
        public Object write(Object value, Object def, boolean placeholder){return BLOB.write(value, def, placeholder);}
    }
    /* *****************************************************************************************************************
     *
     *                                              byte[]-file
     *
     * ****************************************************************************************************************/
    /**
     * mssql
     */
    ,IMAGE("IMAGE", new DatabaseType[]{MSSQL}, byte[].class, true, true)
    /**
     * oracle
     */
    ,BFILE("BFILE", new DatabaseType[]{ORACLE}, byte[].class, true, true)
    /* *****************************************************************************************************************
     *
     *                                              byte[]-geometry
     *
     * ****************************************************************************************************************/
    /**
     * mysql,pg
     */
    ,POINT("POINT", new DatabaseType[]{MYSQL, PostgreSQL}, Point.class, byte[].class, true, true){
        public Object read(Object value, Object def, Class clazz){
            if(null == value){
                return value;
            }
            Point point = BasicUtil.parsePoint(value);
            if(null == clazz){
                value = point;
            }else if(null != point){
                if (clazz == Point.class) {
                    value = point;
                } else if (clazz == double[].class) {
                    value = BeanUtil.Double2double(point.getArray(), 0);
                } else if (clazz == Double[].class) {
                    value = point.getArray();
                } else if (clazz == byte[].class) {
                    value = point.bytes();
                }
            }
            return value;
        }
        public Object write(Object value, Object def, boolean placeholder){
            if(null == value){
                value = def;
            }
            if(value instanceof byte[]){
                return value;
            }
            if(value instanceof Point){
                value = ((Point)value).bytes();
            }else if(value instanceof double[]){
                double[] ds = (double[]) value;
                if(ds.length == 2){
                    if (ds.length >= 2) {
                        value = new Point(ds[0], ds[1]).bytes();
                    }
                }
            }else if(value instanceof Double[]){
                Double[] ds = (Double[]) value;
                if(ds.length == 2 && null != ds[0] && null != ds[1]){
                    value = new Point(ds[0], ds[1]).bytes();
                }
            }
            return value;
        }
    }
    /**
     * mysql
     */
    ,MULTIPOLYGON("MULTIPOLYGON", new DatabaseType[]{MYSQL}, byte[].class, true, true)
    /**
     * mysql
     */
    ,MULTIPOINT("MULTIPOINT", new DatabaseType[]{MYSQL}, byte[].class, true, true)
    /**
     * mysql,pg
     */
    ,POLYGON("POLYGON", new DatabaseType[]{MYSQL, PostgreSQL}, byte[].class, true, true)
    /**
     * mysql
     */
    ,GEOMETRY("GEOMETRY", new DatabaseType[]{MYSQL}, byte[].class, true, true)
    /**
     * mysql
     */
    ,GEOMETRYCOLLECTION("GEOMETRYCOLLECTION", new DatabaseType[]{MYSQL}, byte[].class, true, true)
    /**
     * mssql
     */
    ,HIERARCHYID("HIERARCHYID", new DatabaseType[]{MSSQL}, byte[].class, true, true)
    /**
     * mysql,pg
     */
    ,LINE("LINE", new DatabaseType[]{MYSQL, PostgreSQL}, byte[].class, true, true)
    /**
     * pg
     */
    ,LSEG("LSEG", new DatabaseType[]{PostgreSQL}, byte[].class, true, true)
    /**
     * mssql
     */
    ,GEOGRAPHY("GEOGRAPHY", new DatabaseType[]{MSSQL}, byte[].class, true, true)
    /**
     * pg
     */
    ,BOX("BOX", new DatabaseType[]{PostgreSQL}, byte[].class, true, true)
    /**
     * pg
     */
    ,CIDR("CIDR", new DatabaseType[]{PostgreSQL}, byte[].class, true, true)
    /**
     * pg
     */
    ,CIRCLE("CIRCLE", new DatabaseType[]{PostgreSQL}, byte[].class, true, true)
    /**
     * pg
     */
    ,INET("INET", new DatabaseType[]{PostgreSQL}, byte[].class, true, true)


    /* *****************************************************************************************************************
     *
     *                                              待实现
     *
     * ****************************************************************************************************************/

    /**
     * mysql
     */
    ,ENUM("ENUM", new DatabaseType[]{MYSQL}, String.class, true, true)
    /**
     * pg
     */
    ,INTERVAL("INTERVAL", new DatabaseType[]{PostgreSQL}, null, true, true)
    /**
     * pg
     */
    ,PATH("PATH",  new DatabaseType[]{PostgreSQL},null, true, true)
    /**
     * oracle
     */
    ,RAW("RAW",  new DatabaseType[]{ORACLE},byte[].class, true, true)
    /**
     * oracle
     */
    ,ROWID("ROWID",  new DatabaseType[]{ORACLE},null, true, true)
    /**
     * mysql
     */
    ,SET("SET",  new DatabaseType[]{MYSQL},String.class, true, true)
    /**
     * pg
     */
    ,TSQUERY("TSQUERY",  new DatabaseType[]{PostgreSQL},null, true, true)
    /**
     * pg
     */
    ,TSVECTOR("TSVECTOR",  new DatabaseType[]{PostgreSQL},null, true, true)
    /**
     * pg
     */
    ,MACADDR("MACADDR", new DatabaseType[]{PostgreSQL}, null, true, true)
    /**
     * pg
     */
    ,PG_SNAPSHOT("PG_SNAPSHOT", new DatabaseType[]{PostgreSQL}, null, true, true)
    /**
     * pg
     * 弃用 换成pg_snapshot
     */
    ,TXID_SNAPSHOT("TXID_SNAPSHOT", new DatabaseType[]{PostgreSQL}, null, true, true)
    /**
     * oracle
     */
    ,UROWID("UROWID",  new DatabaseType[]{ORACLE},null, true, true)
    /**
     * mssql
     */
    ,SQL_VARIANT("SQL_VARIANT",  new DatabaseType[]{MSSQL},null, true, true)

    ;
    private final DatabaseType[] dbs;
    private final String name;
    private Class transfer                  ; //中间转换类型 转换成其他格式前先转换成transfer类型
    private final Class compatible          ; //从数据库中读写数据的类型
    private final Boolean ignorePrecision;
    private final Boolean ignoreScale;
    private StandardColumnType(String name, DatabaseType[] dbs, Class transfer, Class compatible, Boolean ignorePrecision, Boolean ignoreScale){
        this.name = name;
        this.dbs = dbs;
        this.transfer = transfer;
        this.compatible = compatible;
        this.ignorePrecision = ignorePrecision;
        this.ignoreScale = ignoreScale;
    }
    private StandardColumnType(String name, DatabaseType[] dbs,  Class compatible, Boolean ignorePrecision, Boolean ignoreScale){
        this.name = name;
        this.dbs = dbs;
        this.compatible = compatible;
        this.ignorePrecision = ignorePrecision;
        this.ignoreScale = ignoreScale;
    }
    @Override
    public Object convert(Object value, Object def){
        return convert(value, null, def);
    }

    @Override
    public Object convert(Object value, Class target){
        Object def = null;
        return convert(value, target, def);
    }

    @Override
    public Object convert(Object value, Class target, Object def) {
        if(null == target){
            target = compatible;
        }
        if(null != value){
            if(value.getClass() == target){
                return value;
            }
            if(null != transfer) {
                value = ConvertAdapter.convert(value, transfer, def);
            }
            value = ConvertAdapter.convert(value, target, def);
        }
        return value;
    }

    @Override
    public Object convert(Object value, Object obj, Field field) {
        return convert(value, field.getType());
    }

    @Override
    public Object read(Object value, Object def, Class clazz) {
        if(null == clazz){
            clazz = transfer;
        }
        if(null == clazz){
            clazz = compatible;
        }
        value = ConvertAdapter.convert(value, clazz, def);
        return value;
    }

    @Override
    public Object write(Object value, Object def, boolean placeholder) {
        if(null != value){
            if(value.getClass() == compatible){
                return value;
            }
            if(null != transfer) {
                value = ConvertAdapter.convert(value, transfer, def);
            }
            value = ConvertAdapter.convert(value, compatible, def);
        }
        return value;
    }





    @Override
    public String getName() {
        return name;
    }

    @Override
    public boolean ignorePrecision() {
        return ignorePrecision;
    }

    @Override
    public boolean ignoreScale() {
        return ignoreScale;
    }

    @Override
    public boolean support() {
        return true;
    }

    @Override
    public Class compatible() {
        return compatible;
    }

    @Override
    public Class transfer() {
        return transfer;
    }

    @Override
    public DatabaseType[] dbs() {
        return dbs;
    }

}
