package org.anyline.data.jdbc.mysql;

import java.util.Hashtable;
import java.util.Map;

public class DataTypeAdapter extends org.anyline.data.adapter.DataTypeAdapter {
    private static Map<String, DataType> types = new Hashtable<>();
    
    public org.anyline.entity.metadata.DataType type(String type){
        if(null != type){
            org.anyline.entity.metadata.DataType dt = types.get(type.toUpperCase());
            if(null == dt){
                dt = super.type(type);
            }
            return dt;
        }
        return null;
    }
    public DataTypeAdapter(){
        // 类型定位时通过key,先通过子类定位,失败后通过父类
        // key:开发中有可能书写的类型(特别是在多数据库环境下,创建表时不为mysql写一个脚本,pg写一个脚本,而一个脚本两处执行)
        // value:实际执行时的类型
        // 执行时通过value
        // 子类中把每种可能的key put进types并对应准确的类型(子类自己创建一个enum 保证value只能自己支持的类型)
        // 父类enum中有的value,子类enum不需要重复实现,需要重新实现的一般是与内置函数有关的类型(如to_date)

        // 以下按字母顺序 方便查找
        // 后面备注表示key在哪个数据库中使用过
        //以下所有的key每个子类中保持完全一致,有不支持的put DataType.

        types.put("BFILE"                   , DataType.ILLEGAL               ); //     ,  ,oracle,
        types.put("BINARY_DOUBLE"           , DataType.DOUBLE                ); //     ,  ,oracle,
        types.put("BINARY_FLOAT"            , DataType.FLOAT                 ); //     ,  ,oracle,
        types.put("BIGINT"                  , DataType.BIGINT                ); //mysql,  ,      ,mssql,
        types.put("BIGSERIAL"               , DataType.BIGINT                ); //     ,pg,
        types.put("BINARY"                  , DataType.BINARY                ); //mysql,  ,      ,mssql,
        types.put("BIT"                     , DataType.BIT                   ); //mysql,pg,      ,mssql,
        types.put("BLOB"                    , DataType.BLOB                  ); //mysql,  ,oracle,     ,sqlite
        types.put("BOOL"                    , DataType.BIT                   ); //     ,pg
        types.put("BOX"                     , DataType.ILLEGAL               ); //     ,pg
        types.put("BYTEA"                   , DataType.VARBINARY             ); //     ,pg
        types.put("CHAR"                    , DataType.CHAR                  ); //mysql,pg,oracle,mssql,
        types.put("CIDR"                    , DataType.ILLEGAL               ); //      pg
        types.put("CIRCLE"                  , DataType.ILLEGAL               ); //      pg
        types.put("CLOB"                    , DataType.TEXT                  ); //     ,  ,oracle
        types.put("DATE"                    , DataType.DATE                  ); //mysql,pg,oracle,mssql
        types.put("DATETIME"                , DataType.DATETIME              ); //mysql,  ,      ,mssql
        types.put("DATETIME2"               , DataType.DATETIME2             ); //mysql,  ,      ,mssql
        types.put("DATETIMEOFFSET"          , DataType.DATETIMEOFFSET        ); //mysql,  ,      ,mssql
        types.put("DECIMAL"                 , DataType.DECIMAL               ); //mysql,pg,oracle,mssql
        types.put("DOUBLE"                  , DataType.DECIMAL               ); //mysql,
        types.put("ENUM"                    , DataType.ENUM                  ); //mysql,
        types.put("FLOAT"                   , DataType.FLOAT                 ); //mysql,  ,oracle,mssql
        types.put("FLOAT4"                  , DataType.FLOAT                 ); //     ,pg
        types.put("FLOAT8"                  , DataType.FLOAT                 ); //     ,pg
        types.put("GEOGRAPHY"               , DataType.ILLEGAL               ); //     ,  ,      ,mssql
        types.put("GEOMETRY"                , DataType.GEOMETRY              ); //mysql
        types.put("GEOMETRYCOLLECTION"      , DataType.GEOMETRYCOLLECTION    ); //mysql
        types.put("HIERARCHYID"             , DataType.ILLEGAL               ); //     ,  ,      ,mssql
        types.put("IMAGE"                   , DataType.BLOB                  ); //     ,  ,      ,mssql
        types.put("INET"                    , DataType.ILLEGAL               ); //     ,pg
        types.put("INTERVAL"                , DataType.ILLEGAL               ); //     ,pg
        types.put("INT"                     , DataType.INT                   ); //mysql,  ,      ,mssql,
        types.put("INT2"                    , DataType.INT                   ); //     ,pg
        types.put("INT4"                    , DataType.INT                   ); //     ,pg
        types.put("INT8"                    , DataType.BIGINT                ); //     ,pg
        types.put("INTEGER"                 , DataType.INT                   ); //mysql                 ,sqlite
        types.put("JSON"                    , DataType.JSON                  ); //mysql,pg
        types.put("JSONB"                   , DataType.BLOB                  ); //     ,pg
        types.put("LINE"                    , DataType.LINE                  ); //mysql,pg
        types.put("LONG"                    , DataType.BIGINT                ); //     ,  ,oracle
        types.put("LONGBLOB"                , DataType.VARBINARY             ); //mysql
        types.put("LONGTEXT"                , DataType.LONGTEXT              ); //mysql
        types.put("LSEG"                    , DataType.ILLEGAL               ); //     ,pg
        types.put("MACADDR"                 , DataType.ILLEGAL               ); //     ,pg
        types.put("MONEY"                   , DataType.DECIMAL               ); //     ,pg,      ,mssql
        types.put("NUMBER"                  , DataType.NUMERIC               ); //     ,  ,oracle
        types.put("NCHAR"                   , DataType.VARCHAR               ); //     ,  ,oracle,mssql
        types.put("NCLOB"                   , DataType.TEXT                  ); //     ,  ,oracle
        types.put("NTEXT"                   , DataType.TEXT                  ); //     ,  ,      ,mssql
        types.put("NVARCHAR"                , DataType.VARCHAR               ); //     ,  ,      ,mssql
        types.put("NVARCHAR2"               , DataType.VARCHAR               ); //     ,  ,oracle
        types.put("PATH"                    , DataType.ILLEGAL               ); //     ,pg
        types.put("MEDIUMBLOB"              , DataType.MEDIUMBLOB            ); //mysql,
        types.put("MEDIUMINT"               , DataType.MEDIUMINT             ); //mysql,
        types.put("MEDIUMTEXT"              , DataType.MEDIUMTEXT            ); //mysql,
        types.put("MULTILINESTRING"         , DataType.MULTILINESTRING       ); //mysql,
        types.put("MULTIPOINT"              , DataType.MULTIPOINT            ); //mysql,
        types.put("MULTIPOLYGON"            , DataType.MULTIPOLYGON          ); //mysql,
        types.put("NUMERIC"                 , DataType.NUMERIC               ); //mysql,  ,       ,mssql,sqlite
        types.put("POINT"                   , DataType.POINT                 ); //mysql,pg
        types.put("POLYGON"                 , DataType.POLYGON               ); //mysql,pg
        types.put("REAL"                    , DataType.REAL                  ); //mysql,  ,      ,mssql,sqlite
        types.put("RAW"                     , DataType.ILLEGAL               ); //     ,  ,oracle
        types.put("ROWID"                   , DataType.ILLEGAL               ); //     ,  ,oracle
        types.put("SERIAL"                  , DataType.TINYINT               ); //     ,pg,
        types.put("SERIAL2"                 , DataType.TINYINT               ); //     ,pg,
        types.put("SERIAL4"                 , DataType.INT                   ); //     ,pg,
        types.put("SERIAL8"                 , DataType.BIGINT                ); //     ,pg,
        types.put("SET"                     , DataType.SET                   ); //mysql,
        types.put("SMALLDATETIME"           , DataType.DATETIME              ); //     ,  ,      ,mssql
        types.put("SMALLMONEY"              , DataType.DECIMAL               ); //     ,  ,      ,mssql
        types.put("SMALLINT"                , DataType.TINYINT               ); //mysql,
        types.put("SMALLSERIAL"             , DataType.TINYINT               ); //     ,pg,
        types.put("SQL_VARIANT"             , DataType.ILLEGAL               ); //     ,  ,      ,mssql
        types.put("SYSNAME"                 , DataType.ILLEGAL               ); //     ,  ,      ,mssql
        types.put("TEXT"                    , DataType.TEXT                  ); //mysql,pg,      ,mssql,sqlite
        types.put("TIME"                    , DataType.TIME                  ); //mysql,pg,      ,mssql
        types.put("TIMEZ"                   , DataType.TIME                  ); //     ,pg
        types.put("TIMESTAMP"               , DataType.TIMESTAMP             ); //mysql,pg,oracle,mssql
        types.put("TIMESTAMP_LOCAL_ZONE"    , DataType.TIMESTAMP             ); //     ,pg
        types.put("TIMESTAMP_ZONE"          , DataType.TIMESTAMP             ); //     ,pg
        types.put("TSQUERY"                 , DataType.ILLEGAL               ); //     ,pg
        types.put("TSVECTOR"                , DataType.ILLEGAL               ); //     ,pg
        types.put("TXID_SNAPSHOT"           , DataType.ILLEGAL               ); //     ,pg
        types.put("UNIQUEIDENTIFIER"        , DataType.ILLEGAL               ); //     ,  ，     ,mssql
        types.put("UUID"                    , DataType.ILLEGAL               ); //     ,pg
        types.put("UROWID"                  , DataType.ILLEGAL               ); //     ,  ,oracle
        types.put("VARBIT"                  , DataType.VARBINARY             ); //     ,pg
        types.put("TINYBLOB"                , DataType.TINYBLOB              ); //mysql,
        types.put("TINYINT"                 , DataType.TINYINT               ); //mysql,  ,      ,mssql
        types.put("TINYTEXT"                , DataType.TINYTEXT              ); //mysql,
        types.put("VARBINARY"               , DataType.VARBINARY             ); //mysql,  ,      ,mssql
        types.put("VARCHAR"                 , DataType.VARCHAR               ); //mysql,pg,oracle,mssql
        types.put("VARCHAR2"                , DataType.VARCHAR               ); //     ,  ,oracle,
        types.put("XML"                     , DataType.TEXT                  ); //     ,pg，      ,mssql
        types.put("YEAR"                    , DataType.DATE                  ); //mysql,


    }
    
}
