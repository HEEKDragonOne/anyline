package org.anyline.data.jdbc.kingbase;

import org.anyline.data.metadata.TypeMetadataAlias;
import org.anyline.metadata.type.init.StandardTypeMetadata;
import org.anyline.metadata.type.TypeMetadata;

public enum KingBaseTypeMetadataAlias implements TypeMetadataAlias {
	ACLITEM                            ("ACLITEM"                          ,StandardTypeMetadata.ACLITEM                            , 1, 1, 1),
	AGG_STATE                          ("AGG_STATE"                        ,StandardTypeMetadata.NONE                               ),
	ARRAY                              ("ARRAY"                            ,StandardTypeMetadata.NONE                               ),
	BFILE                              ("BFILE"                            ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	BIGINT                             ("BIGINT"                           ,StandardTypeMetadata.BIGINT                             , 1, 1, 1),
	BIGSERIAL                          ("BIGSERIAL"                        ,StandardTypeMetadata.BIGSERIAL                          , 1, 1, 1),
	BINARY                             ("BINARY"                           ,StandardTypeMetadata.BLOB                               , 1, 1, 1),
	BINARY_DOUBLE                      ("BINARY_DOUBLE"                    ,StandardTypeMetadata.BINARY_DOUBLE                      , 1, 0, 0),
	BINARY_FLOAT                       ("BINARY_FLOAT"                     ,StandardTypeMetadata.BINARY_FLOAT                       , 1, 0, 0),
	BINARY_INTEGER                     ("BINARY_INTEGER"                   ,StandardTypeMetadata.BINARY_INTEGER                     , 1, 1, 1),
	BIT                                ("BIT"                              ,StandardTypeMetadata.BIT                                , 1, 1, 1),
	BIT_VARYING                        ("BIT VARYING"                      ,StandardTypeMetadata.BIT_VARYING                        , 1, 1, 1),
	BITMAP                             ("BITMAP"                           ,StandardTypeMetadata.NONE                               ),
	BLOB                               ("BLOB"                             ,StandardTypeMetadata.BLOB                               , 1, 1, 1),
	BOOL                               ("BOOL"                             ,StandardTypeMetadata.BOOL                               , 1, 1, 1),
	BOOLEAN                            ("BOOLEAN"                          ,StandardTypeMetadata.NONE                               ),
	BOX                                ("BOX"                              ,StandardTypeMetadata.BOX                                , 1, 1, 1),
	BPCHAR                             ("BPCHAR"                           ,StandardTypeMetadata.BPCHAR                             , 1, 1, 1),
	BPCHARBYTE                         ("BPCHARBYTE"                       ,StandardTypeMetadata.BPCHARBYTE                         , 1, 1, 1),
	BYTE                               ("BYTE"                             ,StandardTypeMetadata.NONE                               ),
	BYTEA                              ("BYTEA"                            ,StandardTypeMetadata.BYTEA                              , 1, 1, 1),
	CHAR                               ("CHAR"                             ,StandardTypeMetadata.CHAR                               , 0, 1, 1),
	CHARACTER                          ("CHARACTER"                        ,StandardTypeMetadata.CHARACTER                          , 1, 1, 1),
	CID                                ("CID"                              ,StandardTypeMetadata.CID                                , 1, 1, 1),
	CIDR                               ("CIDR"                             ,StandardTypeMetadata.CIDR                               , 1, 1, 1),
	CIRCLE                             ("CIRCLE"                           ,StandardTypeMetadata.CIRCLE                             , 1, 1, 1),
	CLICKHOUSE_DATE32                  ("DATE32"                           ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_DATETIME64              ("DATETIME64"                       ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_DECIMAL128              ("DECIMAL128"                       ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_DECIMAL256              ("DECIMAL256"                       ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_DECIMAL32               ("DECIMAL32"                        ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_DECIMAL64               ("DECIMAL64"                        ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_FLOAT32                 ("FLOAT32"                          ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_FLOAT64                 ("FLOAT64"                          ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_INT128                  ("INT128"                           ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_INT16                   ("INT16"                            ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_INT256                  ("INT256"                           ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_INT32                   ("INT32"                            ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_INT64                   ("INT64"                            ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_INT8                    ("INT8"                             ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_UINT128                 ("UINT128"                          ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_UINT16                  ("UINT16"                           ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_UINT256                 ("UINT256"                          ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_UINT32                  ("UINT32"                           ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_UINT64                  ("UINT64"                           ,StandardTypeMetadata.NONE                               ),
	CLICKHOUSE_UINT8                   ("UINT8"                            ,StandardTypeMetadata.NONE                               ),
	CLOB                               ("CLOB"                             ,StandardTypeMetadata.CLOB                               , 1, 1, 1),
	CURSOR                             ("CURSOR"                           ,StandardTypeMetadata.NONE                               ),
	DATE                               ("DATE"                             ,StandardTypeMetadata.DATE                               , 1, 1, 1),
	DATERANGE                          ("DATERANGE"                        ,StandardTypeMetadata.DATERANGE                          , 1, 1, 1),
	DATETIME                           ("DATETIME"                         ,StandardTypeMetadata.DATETIME                           , 1, 1, 1),
	DATETIME2                          ("DATETIME2"                        ,StandardTypeMetadata.DATETIME                           , 1, 1, 1),
	DATETIMEOFFSET                     ("DATETIMEOFFSET"                   ,StandardTypeMetadata.DATETIME                           , 1, 1, 1),
	DECFLOAT                           ("DECFLOAT"                         ,StandardTypeMetadata.NONE                               ),
	DECIMAL                            ("DECIMAL"                          ,StandardTypeMetadata.DECIMAL                            , 1, 0, 0),
	DOUBLE                             ("DOUBLE"                           ,StandardTypeMetadata.DOUBLE                             , 1, 1, 1),
	DOUBLE_PRECISION                   ("DOUBLE PRECISION"                 ,StandardTypeMetadata.DOUBLE_PRECISION                   , 1, 1, 1),
	DSINTERVAL                         ("DSINTERVAL"                       ,StandardTypeMetadata.DSINTERVAL                         , 1, 1, 1),
	ENUM                               ("ENUM"                             ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	FIXEDSTRING                        ("FixedString"                      ,StandardTypeMetadata.NONE                               ),
	FLOAT                              ("FLOAT"                            ,StandardTypeMetadata.FLOAT                              , 1, 2, 1),
	FLOAT4                             ("FLOAT4"                           ,StandardTypeMetadata.FLOAT4                             , 1, 2, 1),
	FLOAT8                             ("FLOAT8"                           ,StandardTypeMetadata.FLOAT8                             , 1, 2, 1),
	GEOGRAPHY                          ("GEOGRAPHY"                        ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	GEOGRAPHY_POINT                    ("GEOGRAPHY_POINT"                  ,StandardTypeMetadata.NONE                               ),
	GEOMETRY                           ("GEOMETRY"                         ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	GEOMETRYCOLLECTION                 ("GEOMETRYCOLLECTION"               ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	GTSVECTOR                          ("GTSVECTOR"                        ,StandardTypeMetadata.GTSVECTOR                          , 1, 1, 1),
	GUID                               ("GUID"                             ,StandardTypeMetadata.NONE                               ),
	HIERARCHYID                        ("HIERARCHYID"                      ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	HLL                                ("HLL"                              ,StandardTypeMetadata.NONE                               ),
	IMAGE                              ("IMAGE"                            ,StandardTypeMetadata.BLOB                               , 1, 1, 1),
	INET                               ("INET"                             ,StandardTypeMetadata.INET                               , 1, 1, 1),
	INT                                ("INT"                              ,StandardTypeMetadata.INT                                , 1, 1, 1),
	INT128                             ("INT128"                           ,StandardTypeMetadata.NONE                               ),
	INT2                               ("INT2"                             ,StandardTypeMetadata.INT2                               , 1, 1, 1),
	INT256                             ("INT256"                           ,StandardTypeMetadata.NONE                               ),
	INT32                              ("INT32"                            ,StandardTypeMetadata.NONE                               ),
	INT4                               ("INT4"                             ,StandardTypeMetadata.INT4                               , 1, 1, 1),
	INT4RANGE                          ("INT4RANGE"                        ,StandardTypeMetadata.INT4RANGE                          , 1, 1, 1),
	INT64                              ("INT64"                            ,StandardTypeMetadata.NONE                               ),
	INT8                               ("INT8"                             ,StandardTypeMetadata.INT8                               , 1, 1, 1),
	INT8RANGE                          ("INT8RANGE"                        ,StandardTypeMetadata.INT8RANGE                          , 1, 1, 1),
	INTEGER                            ("INTEGER"                          ,StandardTypeMetadata.INTEGER                            , 1, 1, 1),
	INTERVAL                           ("INTERVAL"                         ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	INTERVAL_DAY                       ("INTERVAL DAY"                     ,StandardTypeMetadata.INTERVAL_DAY                       , 1, 2, 2),
	INTERVAL_DAY_HOUR                  ("INTERVAL DAY TO HOUR"             ,StandardTypeMetadata.INTERVAL_DAY_HOUR                  , 1, 2, 2),
	INTERVAL_DAY_MINUTE                ("INTERVAL DAY TO MINUTE"           ,StandardTypeMetadata.INTERVAL_DAY_MINUTE                , 1, 2, 2),
	INTERVAL_DAY_SECOND                ("INTERVAL DAY TO SECOND"           ,StandardTypeMetadata.INTERVAL_DAY_SECOND                , 1, 2, 2),
	INTERVAL_HOUR                      ("INTERVAL HOUR"                    ,StandardTypeMetadata.INTERVAL_HOUR                      , 1, 2, 1),
	INTERVAL_HOUR_MINUTE               ("INTERVAL HOUR TO MINUTE"          ,StandardTypeMetadata.INTERVAL_HOUR_MINUTE               , 1, 2, 2),
	INTERVAL_HOUR_SECOND               ("INTERVAL HOUR TO SECOND"          ,StandardTypeMetadata.INTERVAL_HOUR_SECOND               , 1, 2, 2),
	INTERVAL_MINUTE                    ("INTERVAL MINUTE"                  ,StandardTypeMetadata.INTERVAL_MINUTE                    , 1, 2, 1),
	INTERVAL_MINUTE_SECOND             ("INTERVAL MINUTE TO SECOND"        ,StandardTypeMetadata.INTERVAL_MINUTE_SECOND             , 1, 2, 2),
	INTERVAL_MONTH                     ("INTERVAL MONTH"                   ,StandardTypeMetadata.INTERVAL_MONTH                     , 1, 2, 1),
	INTERVAL_SECOND                    ("INTERVAL SECOND"                  ,StandardTypeMetadata.INTERVAL_SECOND                    , 1, 2, 1),
	INTERVAL_YEAR                      ("INTERVAL YEAR"                    ,StandardTypeMetadata.INTERVAL_YEAR                      , 1, 2, 1),
	INTERVAL_YEAR_MONTH                ("INTERVAL YEAR TO MONTH"           ,StandardTypeMetadata.INTERVAL_YEAR_MONTH                , 1, 2, 2),
	IPV4                               ("IPV4"                             ,StandardTypeMetadata.NONE                               ),
	IPV6                               ("IPV6"                             ,StandardTypeMetadata.NONE                               ),
	JAVA_OBJECT                        ("JAVA_OBJECT"                      ,StandardTypeMetadata.NONE                               ),
	JSON                               ("JSON"                             ,StandardTypeMetadata.JSON                               , 1, 1, 1),
	JSONB                              ("JSONB"                            ,StandardTypeMetadata.JSONB                              , 1, 1, 1),
	JSONPATH                           ("JSONPATH"                         ,StandardTypeMetadata.JSONPATH                           , 1, 1, 1),
	KEYWORD                            ("KEYWORD"                          ,StandardTypeMetadata.NONE                               ),
	LARGEINT                           ("LARGEINT"                         ,StandardTypeMetadata.NONE                               ),
	LINE                               ("LINE"                             ,StandardTypeMetadata.LINE                               , 1, 1, 1),
	LINESTRING                         ("LINESTRING"                       ,StandardTypeMetadata.NONE                               ),
	LIST                               ("LIST"                             ,StandardTypeMetadata.NONE                               ),
	LONG_TEXT                          ("LONG"                             ,StandardTypeMetadata.LONG_TEXT                          , 1, 1, 1),
	LONGBLOB                           ("LONGBLOB"                         ,StandardTypeMetadata.BLOB                               , 1, 1, 1),
	LONGTEXT                           ("LONGTEXT"                         ,StandardTypeMetadata.CLOB                               , 1, 1, 1),
	LOWCARDINALITY                     ("LowCardinality"                   ,StandardTypeMetadata.NONE                               ),
	LSEG                               ("LSEG"                             ,StandardTypeMetadata.LSEG                               , 1, 1, 1),
	LVARCHAR                           ("LVARCHAR"                         ,StandardTypeMetadata.NONE                               ),
	MACADDR                            ("MACADDR"                          ,StandardTypeMetadata.MACADDR8                           , 1, 1, 1),
	MACADDR8                           ("MACADDR8"                         ,StandardTypeMetadata.MACADDR8                           , 1, 1, 1),
	MAP                                ("MAP"                              ,StandardTypeMetadata.NONE                               ),
	MEDIUMBLOB                         ("MEDIUMBLOB"                       ,StandardTypeMetadata.BLOB                               , 1, 1, 1),
	MEDIUMINT                          ("MEDIUMINT"                        ,StandardTypeMetadata.INT2                               , 1, 1, 1),
	MEDIUMTEXT                         ("MEDIUMTEXT"                       ,StandardTypeMetadata.TEXT                               , 1, 1, 1),
	MONEY                              ("MONEY"                            ,StandardTypeMetadata.MONEY                              , 1, 1, 1),
	MULTILINESTRING                    ("MULTILINESTRING"                  ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	MULTIPOINT                         ("MULTIPOINT"                       ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	MULTIPOLYGON                       ("MULTIPOLYGON"                     ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	MULTISET                           ("MULTISET"                         ,StandardTypeMetadata.NONE                               ),
	NATURALN                           ("NATURALN"                         ,StandardTypeMetadata.NATURALN                           , 1, 1, 1),
	NCHAR                              ("NCHAR"                            ,StandardTypeMetadata.NCHAR                              , 0, 1, 1),
	NCLOB                              ("NCLOB"                            ,StandardTypeMetadata.NCLOB                              , 1, 1, 1),
	NTEXT                              ("NTEXT"                            ,StandardTypeMetadata.TEXT                               , 1, 1, 1),
	NUMBER                             ("NUMBER"                           ,StandardTypeMetadata.NUMBER                             , 1, 2, 2),
	NUMERIC                            ("NUMERIC"                          ,StandardTypeMetadata.NUMBER                             , 1, 2, 2),
	NUMRANGE                           ("NUMRANGE"                         ,StandardTypeMetadata.NUMRANGE                           , 1, 1, 1),
	NVARCHAR                           ("NVARCHAR"                         ,StandardTypeMetadata.NVARCHAR                           , 0, 1, 1),
	NVARCHAR2                          ("NVARCHAR2"                        ,StandardTypeMetadata.NVARCHAR2                          , 0, 1, 1),
	OBJECT                             ("OBJECT"                           ,StandardTypeMetadata.NONE                               ),
	OID                                ("OID"                              ,StandardTypeMetadata.OID                                , 1, 1, 1),
	ORA_DATE                           ("ORA_DATE"                         ,StandardTypeMetadata.ORA_DATE                           , 1, 1, 1),
	PATH                               ("PATH"                             ,StandardTypeMetadata.PATH                               , 1, 1, 1),
	PG_SNAPSHOT                        ("PG_SNAPSHOT"                      ,StandardTypeMetadata.NONE                               ),
	POINT                              ("POINT"                            ,StandardTypeMetadata.POINT                              , 1, 1, 1),
	POLYGON                            ("POLYGON"                          ,StandardTypeMetadata.POLYGON                            , 1, 1, 1),
	POSITIVE                           ("POSITIVE"                         ,StandardTypeMetadata.POSITIVE                           , 1, 1, 1),
	POSITIVEN                          ("POSITIVEN"                        ,StandardTypeMetadata.POSITIVEN                          , 1, 1, 1),
	RAW                                ("RAW"                              ,StandardTypeMetadata.RAW                                , 1, 1, 1),
	REAL                               ("REAL"                             ,StandardTypeMetadata.REAL                               , 1, 0, 0),
	REFCURSOR                          ("REFCURSOR"                        ,StandardTypeMetadata.REFCURSOR                          , 1, 1, 1),
	REGCLASS                           ("REGCLASS"                         ,StandardTypeMetadata.REGCLASS                           , 1, 1, 1),
	REGCONFIG                          ("REGCONFIG"                        ,StandardTypeMetadata.REGCONFIG                          , 1, 1, 1),
	REGDICTIONARY                      ("REGDICTIONARY"                    ,StandardTypeMetadata.REGDICTIONARY                      , 1, 1, 1),
	REGNAMESPACE                       ("REGNAMESPACE"                     ,StandardTypeMetadata.REGNAMESPACE                       , 1, 1, 1),
	REGOPER                            ("REGOPER"                          ,StandardTypeMetadata.REGOPER                            , 1, 1, 1),
	REGOPERATOR                        ("REGOPERATOR"                      ,StandardTypeMetadata.REGOPERATOR                        , 1, 1, 1),
	REGPROC                            ("REGPROC"                          ,StandardTypeMetadata.REGPROC                            , 1, 1, 1),
	REGPROCEDURE                       ("REGPROCEDURE"                     ,StandardTypeMetadata.REGPROCEDURE                       , 1, 1, 1),
	REGROLE                            ("REGROLE"                          ,StandardTypeMetadata.REGROLE                            , 1, 1, 1),
	REGTYPE                            ("REGTYPE"                          ,StandardTypeMetadata.REGTYPE                            , 1, 1, 1),
	RING                               ("RING"                             ,StandardTypeMetadata.NONE                               ),
	ROW                                ("ROW"                              ,StandardTypeMetadata.NONE                               ),
	ROWID                              ("ROWID"                            ,StandardTypeMetadata.ROWID                              , 1, 1, 1),
	SECONDDATE                         ("SECONDDATE"                       ,StandardTypeMetadata.NONE                               ),
	SERIAL                             ("SERIAL"                           ,StandardTypeMetadata.SERIAL                             , 1, 1, 1),
	SERIAL2                            ("SERIAL2"                          ,StandardTypeMetadata.SERIAL2                            , 1, 1, 1),
	SERIAL4                            ("SERIAL4"                          ,StandardTypeMetadata.SERIAL4                            , 1, 1, 1),
	SERIAL8                            ("SERIAL8"                          ,StandardTypeMetadata.SERIAL8                            , 1, 1, 1),
	SET                                ("SET"                              ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	SHORT                              ("SHORT"                            ,StandardTypeMetadata.NONE                               ),
	SIGNTYPE                           ("SIGNTYPE"                         ,StandardTypeMetadata.SIGNTYPE                           , 1, 1, 1),
	SIMPLE_DOUBLE                      ("SIMPLE_DOUBLE"                    ,StandardTypeMetadata.SIMPLE_DOUBLE                      , 1, 1, 1),
	SIMPLE_FLOAT                       ("SIMPLE_FLOAT"                     ,StandardTypeMetadata.SIMPLE_FLOAT                       , 1, 2, 1),
	SIMPLE_INTEGER                     ("SIMPLE_INTEGER"                   ,StandardTypeMetadata.SIMPLE_INTEGER                     , 1, 1, 1),
	SIMPLEAGGREGATEFUNCTION            ("SimpleAggregateFunction"          ,StandardTypeMetadata.NONE                               ),
	SMALLDATETIME                      ("SMALLDATETIME"                    ,StandardTypeMetadata.TIMESTAMP                          , 1, 1, 1),
	SMALLDECIMAL                       ("SMALLDECIMAL"                     ,StandardTypeMetadata.NONE                               ),
	SMALLFLOAT                         ("SMALLFLOAT"                       ,StandardTypeMetadata.NONE                               ),
	SMALLINT                           ("SMALLINT"                         ,StandardTypeMetadata.INT4                               , 1, 1, 1),
	SMALLMONEY                         ("SMALLMONEY"                       ,StandardTypeMetadata.MONEY                              , 1, 1, 1),
	SMALLSERIAL                        ("SMALLSERIAL"                      ,StandardTypeMetadata.SMALLSERIAL                        , 1, 1, 1),
	SQL_DATETIMEOFFSET                 ("SQL_DATETIMEOFFSET"               ,StandardTypeMetadata.NONE                               ),
	SQL_VARIANT                        ("SQL_VARIANT"                      ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	ST_GEOMETRY                        ("ST_GEOMETRY"                      ,StandardTypeMetadata.NONE                               ),
	ST_POINT                           ("ST_POINT"                         ,StandardTypeMetadata.NONE                               ),
	STRING                             ("STRING"                           ,StandardTypeMetadata.STRING                             , 1, 1, 1),
	STRUCT                             ("STRUCT"                           ,StandardTypeMetadata.NONE                               ),
	STRUCTS                            ("STRUCTS"                          ,StandardTypeMetadata.NONE                               ),
	SYS_REFCURSOR                      ("SYS_REFCURSOR"                    ,StandardTypeMetadata.NONE                               ),
	SYSNAME                            ("SYSNAME"                          ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	TEXT                               ("TEXT"                             ,StandardTypeMetadata.TEXT                               , 1, 1, 1),
	TID                                ("TID"                              ,StandardTypeMetadata.TID                                , 1, 1, 1),
	TIME                               ("TIME"                             ,StandardTypeMetadata.TIMESTAMP                          , 1, 1, 1),
	TIME_TZ_UNCONSTRAINED              ("TIME TZ UNCONSTRAINED"            ,StandardTypeMetadata.TIME_TZ_UNCONSTRAINED              , 1, 1, 1),
	TIME_UNCONSTRAINED                 ("TIME_UNCONSTRAINED"               ,StandardTypeMetadata.TIME_UNCONSTRAINED                 , 1, 1, 1),
	TIME_WITH_TIME_ZONE                ("TIME WITH TIME ZONE"              ,StandardTypeMetadata.TIME_WITH_TIME_ZONE                , 1, 1, 1),
	TIME_WITHOUT_TIME_ZONE             ("TIME WITHOUT TIME ZONE"           ,StandardTypeMetadata.TIME_WITHOUT_TIME_ZONE             , 1, 1, 1),
	TIMESTAMP                          ("TIMESTAMP"                        ,StandardTypeMetadata.TIMESTAMP                          , 1, 1, 1),
	TIMESTAMP_WITH_LOCAL_ZONE          ("TIMESTAMP WITH LOCAL TIME ZONE"   ,StandardTypeMetadata.TIMESTAMP                          , 1, 1, 1),
	TIMESTAMP_WITH_TIME_ZONE           ("TIMESTAMP WITH TIME ZONE"         ,StandardTypeMetadata.TIMESTAMP_WITH_TIME_ZONE           , 1, 1, 1),
	TIMESTAMP_WITHOUT_TIME_ZONE        ("TIMESTAMP WITHOUT TIME ZONE"      ,StandardTypeMetadata.TIMESTAMP_WITHOUT_TIME_ZONE        , 1, 1, 1),
	TIMESTAMPTZ                        ("TIMESTAMPTZ"                      ,StandardTypeMetadata.NONE                               ),
	TIMEZ                              ("TIMEZ"                            ,StandardTypeMetadata.TIMESTAMP                          , 1, 1, 1),
	TINYBLOB                           ("TINYBLOB"                         ,StandardTypeMetadata.BLOB                               , 1, 1, 1),
	TINYINT                            ("TINYINT"                          ,StandardTypeMetadata.INT2                               , 1, 1, 1),
	TINYTEXT                           ("TINYTEXT"                         ,StandardTypeMetadata.TEXT                               , 1, 1, 1),
	TSQUERY                            ("TSQUERY"                          ,StandardTypeMetadata.TSQUERY                            , 1, 1, 1),
	TSRANGE                            ("TSRANGE"                          ,StandardTypeMetadata.TSRANGE                            , 1, 1, 1),
	TSTZRANGE                          ("TSTZRANGE"                        ,StandardTypeMetadata.TSTZRANGE                          , 1, 1, 1),
	TSVECTOR                           ("TSVECTOR"                         ,StandardTypeMetadata.TSVECTOR                           , 1, 1, 1),
	TUPLE                              ("TUPLE"                            ,StandardTypeMetadata.NONE                               ),
	TXID_SNAPSHOT                      ("TXID_SNAPSHOT"                    ,StandardTypeMetadata.TXID_SNAPSHOT                      , 1, 1, 1),
	UNIQUEIDENTIFIER                   ("UNIQUEIDENTIFIER"                 ,StandardTypeMetadata.ILLEGAL                            , -1, -1, -1),
	UROWID                             ("UROWID"                           ,StandardTypeMetadata.UROWID                             , 1, 1, 1),
	UUID                               ("UUID"                             ,StandardTypeMetadata.UUID                               , 1, 1, 1),
	VARBINARY                          ("VARBINARY"                        ,StandardTypeMetadata.BLOB                               , 1, 1, 1),
	VARBIT                             ("VARBIT"                           ,StandardTypeMetadata.VARBIT                             , 1, 1, 1),
	VARCHAR                            ("VARCHAR"                          ,StandardTypeMetadata.VARCHAR                            , 0, 1, 1),
	VARCHAR2                           ("VARCHAR2"                         ,StandardTypeMetadata.VARCHAR2                           , 0, 1, 1),
	VARCHARBYTE                        ("VARCHARBYTE"                      ,StandardTypeMetadata.VARCHARBYTE                        , 1, 1, 1),
	XID                                ("XID"                              ,StandardTypeMetadata.XID                                , 1, 1, 1),
	XML                                ("XML"                              ,StandardTypeMetadata.XML                                , 1, 1, 1),
	YEAR                               ("YEAR"                             ,StandardTypeMetadata.DATE                               , 1, 1, 1),
	YMINTERVAL                         ("YMINTERVAL"                       ,StandardTypeMetadata.YMINTERVAL                         , 1, 1, 1);

	private String compatible                ; // 输入名称(根据输入名称转换成标准类型)(名称与枚举名不一致的需要,如带空格的)
	private final TypeMetadata standard      ; // 标准类型
	private int ignoreLength            = -1 ; // 是否忽略长度
	private int ignorePrecision         = -1 ; // 是否忽略有效位数
	private int ignoreScale             = -1 ; // 是否忽略小数位数
	private String lengthRefer               ; // 读取元数据依据-长度
	private String precisionRefer            ; // 读取元数据依据-有效位数
	private String scaleRefer                ; // 读取元数据依据-小数位数
	private TypeMetadata.Config config       ; // 集成元数据读写配置

	KingBaseTypeMetadataAlias(String compatible, TypeMetadata standard, String lengthRefer, String precisionRefer, String scaleRefer, int ignoreLength, int ignorePrecision, int ignoreScale){
		this.compatible = compatible;
		this.standard = standard;
		this.lengthRefer = lengthRefer;
		this.precisionRefer = precisionRefer;
		this.scaleRefer = scaleRefer;
		this.ignoreLength = ignoreLength;
		this.ignorePrecision = ignorePrecision;
		this.ignoreScale = ignoreScale;
	}

	KingBaseTypeMetadataAlias(String compatible, TypeMetadata standard, int ignoreLength, int ignorePrecision, int ignoreScale){
		this(compatible, standard, null, null, null, ignoreLength, ignorePrecision, ignoreScale);
	}

	KingBaseTypeMetadataAlias(TypeMetadata standard, String lengthRefer, String precisionRefer, String scaleRefer, int ignoreLength, int ignorePrecision, int ignoreScale){
		this(null, standard, lengthRefer, precisionRefer, scaleRefer, ignoreLength, ignorePrecision, ignoreScale);
	}

	KingBaseTypeMetadataAlias(String compatible, TypeMetadata standard){
		this.compatible = compatible;
		this.standard = standard;
	}

	KingBaseTypeMetadataAlias(TypeMetadata standard){
		this.standard = standard;
	}

	@Override
	public String compatible(){
		if(null == compatible){
			compatible = name();
		}
		return compatible;
	}

	@Override
	public TypeMetadata standard() {
		return standard;
	}

	@Override
	public TypeMetadata.Config config() {
		if(null == config){
			config = new TypeMetadata.Config();
			if(null != lengthRefer) {
				config.setLengthRefer(lengthRefer).setPrecisionRefer(precisionRefer).setScaleRefer(scaleRefer);
			}
			if(-1 != ignoreLength) {
				config.setIgnoreLength(ignoreLength).setIgnorePrecision(ignorePrecision).setIgnoreScale(ignoreScale);
			}
		}
		return config;
	}
}
