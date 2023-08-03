 
package org.anyline.data.jdbc.mariadb;

import org.anyline.data.adapter.JDBCAdapter;
import org.anyline.data.adapter.init.SQLAdapter;
import org.anyline.data.run.Run;
import org.anyline.data.run.SimpleRun;
import org.anyline.data.runtime.DataRuntime;
import org.anyline.entity.*;
import org.anyline.metadata.*;
import org.anyline.metadata.type.DatabaseType;
import org.anyline.util.BasicUtil;
import org.anyline.util.SQLUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

/**
 * 参考 MySQLAdapter
 */
@Repository("anyline.data.jdbc.adapter.mariadb")
public class MariaAdapter extends SQLAdapter implements JDBCAdapter, InitializingBean {

	public DatabaseType type(){
		return DatabaseType.MariaDB;
	}

	@Override
	public String generatedKey() {
		return "GENERATED_KEY";
	}

	public MariaAdapter(){
		super();
		delimiterFr = "`";
		delimiterTo = "`";
		for (MariaColumnTypeAlias alias: MariaColumnTypeAlias.values()){
			types.put(alias.name(), alias.standard());
		}
		for(MariaWriter writer: MariaWriter.values()){
			Object[] supports = writer.supports();
			if(null != supports) {
				for(Object support:supports)
					writers.put(support, writer.writer());
			}
		}
		for(MariaReader reader: MariaReader.values()){
			Object[] supports = reader.supports();
			if(null != supports) {
				for(Object support:supports)
					readers.put(support, reader.reader());
			}
		}
	}
	@Value("${anyline.data.jdbc.delimiter.mariadb:}")
	private String delimiter;

	@Override
	public void afterPropertiesSet()  {
		setDelimiter(delimiter);
	}
	/* *****************************************************************************************************
	 *
	 * 											DML
	 *
	 * ****************************************************************************************************/
	@Override
	public String parseFinalQuery(Run run){
		String sql = run.getBaseQuery();
		String cols = run.getQueryColumns();
		if(!"*".equals(cols)){
			String reg = "(?i)^select[\\s\\S]+from";
			sql = sql.replaceAll(reg,"SELECT "+cols+" FROM ");
		}
		OrderStore orders = run.getOrderStore();
		if(null != orders){
			sql += orders.getRunText(getDelimiterFr()+getDelimiterTo());
		}
		PageNavi navi = run.getPageNavi();
		if(null != navi){
			long limit = navi.getLastRow() - navi.getFirstRow() + 1;
			if(limit < 0){
				limit = 0;
			}
			sql += " LIMIT " + navi.getFirstRow() + "," + limit;
		}
		sql = sql.replaceAll("WHERE\\s*1=1\\s*AND", "WHERE");
		return sql;
	}

	public String concat(String ... args){
		return concatFun(args);
	}



	/**
	 * 构造 FIND_IN_SET 查询条件
	 * 如果不需要占位符 返回null  否则原样返回value
	 * @param builder builder
	 * @param compare compare
	 * @param column column
	 * @param value value
	 * @return value
	 */
	@Override
	public Object buildConditionFindInSet(StringBuilder builder, String column, Compare compare, Object value){
		List<Object> values = new ArrayList<>();
		if(null != value){
			if(value instanceof Collection){
				Collection cols = (Collection) value;
				for(Object col:cols){
					values.add(col);
				}
			}else if(value instanceof Object[]){
				Object[] array = (Object[]) value;
				for(Object obj:array){
					values.add(obj);
				}
			}else{
				values.add(value);
			}
		}
		if(values.size() > 1){
			builder.append("(");
		}
		boolean first = true;
		for(Object v:values){
			if(!first){
				builder.append(" OR ");
			}
			builder.append("FIND_IN_SET(?,").append(column).append(")");
			first = false;
		}
		if(values.size() > 1){
			builder.append(")");
		}
		return value;
	}




	/* *****************************************************************************************************************
	 *
	 * 													metadata
	 *
	 * =================================================================================================================
	 * database			: 数据库
	 * table			: 表
	 * master table		: 主表
	 * partition table	: 分区表
	 * column			: 列
	 * tag				: 标签
	 * primary key      : 主键
	 * foreign key		: 外键
	 * index			: 索引
	 * constraint		: 约束
	 * trigger		    : 触发器
	 * procedure        : 存储过程
	 * function         : 函数
	 ******************************************************************************************************************/

	@Override
	public void checkSchema(Connection con, Table table){
		try {
			if (null == table.getSchema()) {
				table.setSchema(con.getCatalog());
			}
		}catch (Exception e){
		}
		table.setCheckSchemaTime(new Date());
	}
	@Override
	public void checkSchema(DataSource dataSource, Table table){
		if(null == table || null != table.getCheckSchemaTime()){
			return;
		}
		/*
		 * Maria不支持catalog
		 *
		 * con.getCatalog:数据库名 赋值给table.schema
		 * con.getSchema:null
		 * 查表时 SELECT * FROM information_schema.TABLES WHERE TABLE_SCHEMA = '数据库名'
		 */
		Connection con = null;
		try {
			//注意这里与数据库不一致
			if (null == table.getSchema()) {
				con = DataSourceUtils.getConnection(dataSource);
				table.setSchema(con.getCatalog());
			}
			table.setCheckSchemaTime(new Date());
		}catch (Exception e){
			log.warn("[check schema][fail:{}]", e.toString());
		}finally {
			if(null != con && !DataSourceUtils.isConnectionTransactional(con, dataSource)){
				DataSourceUtils.releaseConnection(con, dataSource);
			}
		}
	}
	/* *****************************************************************************************************************
	 * 													database
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryDatabaseRunSQL() throws Exception
	 * LinkedHashMap<String, Database> databases(int index, boolean create, LinkedHashMap<String, Database> databases, DataSet set) throws Exception
	 ******************************************************************************************************************/
	public List<Run> buildQueryDatabaseRunSQL() throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("SHOW DATABASES");
		return runs;
	}

	/**
	 * 根据查询结果集构造 Database
	 * @param index 第几条SQL 对照 buildQueryDatabaseRunSQL 返回顺序
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param databases 上一步查询结果
	 * @param set DataSet
	 * @return databases
	 * @throws Exception
	 */
	public LinkedHashMap<String, Database> databases(int index, boolean create, LinkedHashMap<String, Database> databases, DataSet set) throws Exception{
		if(null == databases){
			databases = new LinkedHashMap<>();
		}
		for(DataRow row:set){
			Database database = new Database();
			database.setName(row.getString("DATABASE"));
			databases.put(database.getName().toUpperCase(), database);
		}
		return databases;
	}

	/* *****************************************************************************************************************
	 * 													table
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryTableRunSQL(String catalog, String schema, String pattern, String types)
	 * List<Run> buildQueryTableCommentRunSQL(String catalog, String schema, String pattern, String types)
	 * <T extends Table> LinkedHashMap<String, T> tables(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> tables, DataSet set) throws Exception
	 * <T extends Table> LinkedHashMap<String, T> tables(boolean create, LinkedHashMap<String, T> tables, DataRuntime runtime, String catalog, String schema, String pattern, String ... types) throws Exception
	 * <T extends Table> LinkedHashMap<String, T> comments(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> tables, DataSet set) throws Exception
	 * List<Run> buildQueryDDLRunSQL(Table table) throws Exception
	 * public List<String> ddl(int index, Table table, List<String> ddls, DataSet set)
	 ******************************************************************************************************************/

	/**
	 * 查询表
	 * @param catalog catalog
	 * @param schema schema
	 * @param pattern pattern
	 * @param types types
	 * @return String
	 */
	@Override
	public List<Run> buildQueryTableRunSQL(String catalog, String schema, String pattern, String types) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();

		builder.append("SELECT * FROM information_schema.TABLES WHERE 1=1 ");
		// 8.0版本中 这个表中 TABLE_CATALOG = def  TABLE_SCHEMA = 数据库名
		/*if(BasicUtil.isNotEmpty(catalog)){
			builder.append(" AND TABLE_SCHEMA = '").append(catalog).append("'");
		}*/
		if(BasicUtil.isNotEmpty(schema)){
			builder.append(" AND TABLE_SCHEMA = '").append(schema).append("'");
		}
		if(BasicUtil.isNotEmpty(pattern)){
			builder.append(" AND TABLE_NAME LIKE '").append(objectName(pattern)).append("'");
		}
		if(BasicUtil.isNotEmpty(types)){
			String[] tmps = types.split(",");
			builder.append(" AND TABLE_TYPE IN(");
			int idx = 0;
			for(String tmp:tmps){
				if(idx > 0){
					builder.append(",");
				}
				builder.append("'").append(tmp).append("'");
				idx ++;
			}
			builder.append(")");
		}else {
			builder.append(" AND TABLE_TYPE IN ('BASE TABLE','TABLE')");
		}
		return runs;
	}
	/**
	 * 查询表
	 * @param catalog catalog
	 * @param schema schema
	 * @param pattern pattern
	 * @param types types
	 * @return String
	 */
	@Override
	public List<Run> buildQueryTableCommentRunSQL(String catalog, String schema, String pattern, String types) throws Exception{
		return super.buildQueryTableCommentRunSQL(catalog, schema, pattern, types);
	}
	/**
	 *
	 * @param index 第几条SQL 对照buildQueryTableRunSQL返回顺序
	 * @param catalog catalog
	 * @param schema schema
	 * @param tables 上一步查询结果
	 * @param set DataSet
	 * @return tables
	 * @throws Exception 异常
	 */
	@Override
	public <T extends Table> LinkedHashMap<String, T> tables(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> tables, DataSet set) throws Exception{
		if(null == tables){
			tables = new LinkedHashMap<>();
		}
		for(DataRow row:set){
			String name = row.getString("TABLE_NAME");
			T table = tables.get(name.toUpperCase());
			if(null == table){
				table = (T)new Table();
			}
			//Maria不支付TABLE_CATALOG
			//table.setCatalog(row.getString("TABLE_CATALOG"));
			table.setSchema(row.getString("TABLE_SCHEMA"));
			table.setName(name);
			table.setEngine(row.getString("ENGINE"));
			table.setComment(row.getString("TABLE_COMMENT"));
			tables.put(name.toUpperCase(), table);
		}
		return tables;
	}
	@Override
	public <T extends Table> LinkedHashMap<String, T> tables(boolean create, LinkedHashMap<String, T> tables, DataRuntime runtime, String catalog, String schema, String pattern, String ... types) throws Exception{
		//参考 checkSchema()
		DataSource ds = null;
		Connection con = null;
		try {
			JdbcTemplate jdbc = jdbc(runtime);
			ds = jdbc.getDataSource();
			con = DataSourceUtils.getConnection(ds);
			DatabaseMetaData dbmd = con.getMetaData();

			ResultSet set = dbmd.getTables(catalog, schema, pattern, types);

			if(null == tables){
				tables = new LinkedHashMap<>();
			}
			Map<String,Integer> keys = keys(set);
			while(set.next()) {
				String tableName = string(keys, "TABLE_NAME", set);

				if (BasicUtil.isEmpty(tableName)) {
					tableName = string(keys, "NAME", set);
				}
				if (BasicUtil.isEmpty(tableName)) {
					continue;
				}
				T table = tables.get(tableName.toUpperCase());
				if (null == table) {
					if (create) {
						table = (T) new Table();
						tables.put(tableName.toUpperCase(), table);
					} else {
						continue;
					}
				}
				//参考 checkSchema()
				table.setSchema(BasicUtil.evl(string(keys, "TABLE_CAT", set), catalog));
				table.setCatalog(null);

				table.setName(tableName);
				table.setType(BasicUtil.evl(string(keys, "TABLE_TYPE", set), table.getType()));
				table.setComment(BasicUtil.evl(string(keys, "REMARKS", set), table.getComment()));
				table.setTypeCat(BasicUtil.evl(string(keys, "TYPE_CAT", set), table.getTypeCat()));
				table.setTypeName(BasicUtil.evl(string(keys, "TYPE_NAME", set), table.getTypeName()));
				table.setSelfReferencingColumn(BasicUtil.evl(string(keys, "SELF_REFERENCING_COL_NAME", set), table.getSelfReferencingColumn()));
				table.setRefGeneration(BasicUtil.evl(string(keys, "REF_GENERATION", set), table.getRefGeneration()));
				tables.put(tableName.toUpperCase(), table);

				// table_map.put(table.getType().toUpperCase()+"_"+tableName.toUpperCase(), tableName);
			}
		}finally {
			if(!DataSourceUtils.isConnectionTransactional(con, ds)){
				DataSourceUtils.releaseConnection(con, ds);
			}
		}
		return tables;
	}
	/* *****************************************************************************************************************
	 * 													view
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryViewRunSQL(String catalog, String schema, String pattern, String types)
	 * <T extends View> LinkedHashMap<String, T> views(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> views, DataSet set) throws Exception
	 * <T extends View> LinkedHashMap<String, T> views(boolean create, LinkedHashMap<String, T> views, DataRuntime runtime, String catalog, String schema, String pattern, String ... types) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 查询视图
	 * @param catalog catalog
	 * @param schema schema
	 * @param pattern pattern
	 * @param types types
	 * @return String
	 */
	@Override
	public List<Run> buildQueryViewRunSQL(String catalog, String schema, String pattern, String types) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();

		builder.append("SELECT * FROM information_schema.VIEWS WHERE 1=1 ");
		// 8.0版本中 这个视图中 TABLE_CATALOG = def  TABLE_SCHEMA = 数据库名
		/*if(BasicUtil.isNotEmpty(catalog)){
			builder.append(" AND TABLE_SCHEMA = '").append(catalog).append("'");
		}*/
		if(BasicUtil.isNotEmpty(schema)){
			builder.append(" AND TABLE_SCHEMA = '").append(schema).append("'");
		}
		if(BasicUtil.isNotEmpty(pattern)){
			builder.append(" AND TABLE_NAME LIKE '").append(objectName(pattern)).append("'");
		}
		return runs;
	}

	/**
	 *
	 * @param index 第几条SQL 对照buildQueryViewRunSQL返回顺序
	 * @param catalog catalog
	 * @param schema schema
	 * @param views 上一步查询结果
	 * @param set DataSet
	 * @return views
	 * @throws Exception 异常
	 */
	@Override
	public <T extends View> LinkedHashMap<String, T> views(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> views, DataSet set) throws Exception{
		if(null == views){
			views = new LinkedHashMap<>();
		}
		for(DataRow row:set){
			String name = row.getString("TABLE_NAME");
			T view = views.get(name.toUpperCase());
			if(null == view){
				view = (T)new View();
			}
			//Maria不支付TABLE_CATALOG
			//view.setCatalog(row.getString("TABLE_CATALOG"));
			view.setSchema(row.getString("TABLE_SCHEMA"));
			view.setName(name);
			view.setDefinition(row.getString("VIEW_DEFINITION"));
			views.put(name.toUpperCase(), view);
		}
		return views;
	}
	@Override
	public <T extends View> LinkedHashMap<String, T> views(boolean create, LinkedHashMap<String, T> views, DataRuntime runtime, String catalog, String schema, String pattern, String ... types) throws Exception{
		//参考 checkSchema()
		DataSource ds = null;
		Connection con = null;
		try{
			JdbcTemplate jdbc = jdbc(runtime);
			ds = jdbc.getDataSource();
			con = DataSourceUtils.getConnection(ds);
			DatabaseMetaData dbmd = con.getMetaData();
			ResultSet set = dbmd.getTables(catalog, schema, pattern, types);

			if(null == views){
				views = new LinkedHashMap<>();
			}
			Map<String,Integer> keys = keys(set);
			while(set.next()) {
				String viewName = string(keys, "TABLE_NAME", set);

				if(BasicUtil.isEmpty(viewName)){
					viewName = string(keys, "NAME", set);
				}
				if(BasicUtil.isEmpty(viewName)){
					continue;
				}
				T view = views.get(viewName.toUpperCase());
				if(null == view){
					if(create){
						view = (T)new View();
						views.put(viewName.toUpperCase(), view);
					}else{
						continue;
					}
				}
				//参考 checkSchema()
				view.setSchema(BasicUtil.evl(string(keys, "TABLE_CAT", set), catalog));
				view.setCatalog(null);

				view.setName(viewName);
				view.setType(BasicUtil.evl(string(keys, "TABLE_TYPE", set), view.getType()));
				view.setComment(BasicUtil.evl(string(keys, "REMARKS", set), view.getComment()));
				view.setTypeCat(BasicUtil.evl(string(keys, "TYPE_CAT", set), view.getTypeCat()));
				view.setTypeName(BasicUtil.evl(string(keys, "TYPE_NAME", set), view.getTypeName()));
				view.setSelfReferencingColumn(BasicUtil.evl(string(keys, "SELF_REFERENCING_COL_NAME", set), view.getSelfReferencingColumn()));
				view.setRefGeneration(BasicUtil.evl(string(keys, "REF_GENERATION", set), view.getRefGeneration()));
				views.put(viewName.toUpperCase(), view);

				// view_map.put(view.getType().toUpperCase()+"_"+viewName.toUpperCase(), viewName);
			}
		}finally {
			if(!DataSourceUtils.isConnectionTransactional(con, ds)){
				DataSourceUtils.releaseConnection(con, ds);
			}
		}
		return views;
	}


	/* *****************************************************************************************************************
	 * 													master table
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryMasterTableRunSQL(String catalog, String schema, String pattern, String types)
	 * <T extends MasterTable> LinkedHashMap<String, T> mtables(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> tables, DataSet set) throws Exception
	 * <T extends MasterTable> LinkedHashMap<String, T> mtables(boolean create, LinkedHashMap<String, T> tables, DataRuntime runtime, String catalog, String schema, String pattern, String ... types) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 查询主表
	 * @param catalog catalog
	 * @param schema schema
	 * @param pattern pattern
	 * @param types types
	 * @return String
	 */
	@Override
	public List<Run> buildQueryMasterTableRunSQL(String catalog, String schema, String pattern, String types) throws Exception{
		return super.buildQueryMasterTableRunSQL(catalog, schema, pattern, types);
	}

	/**
	 * 从jdbc结果中提取表结构
	 * ResultSet set = con.getMetaData().getTables()
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param catalog catalog
	 * @param schema schema
	 * @param runtime runtime
	 * @return List
	 */
	@Override
	public <T extends MasterTable> LinkedHashMap<String, T> mtables(boolean create, LinkedHashMap<String, T> tables, DataRuntime runtime, String catalog, String schema, String pattern, String ... types) throws Exception{
		return super.mtables(create, tables, runtime, catalog, schema, pattern, types);
	}


	/**
	 * 从上一步生成的SQL查询结果中 提取表结构
	 * @param index 第几条SQL
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param catalog catalog
	 * @param schema schema
	 * @param tables 上一步查询结果
	 * @param set DataSet
	 * @return tables
	 * @throws Exception 异常
	 */
	@Override
	public <T extends MasterTable> LinkedHashMap<String, T> mtables(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> tables, DataSet set) throws Exception{
		return super.mtables(index, create, catalog, schema, tables, set);
	}


	/* *****************************************************************************************************************
	 * 													partition table
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryPartitionTableRunSQL(String catalog, String schema, String pattern, String types)
	 * List<Run> buildQueryPartitionTableRunSQL(MasterTable master, Map<String,Object> tags, String name)
	 * List<Run> buildQueryPartitionTableRunSQL(MasterTable master, Map<String,Object> tags)
	 * <T extends PartitionTable> LinkedHashMap<String, T> ptables(int total, int index, boolean create, MasterTable master, String catalog, String schema, LinkedHashMap<String, T> tables, DataSet set) throws Exception
	 * <T extends PartitionTable> LinkedHashMap<String,T> ptables(boolean create, LinkedHashMap<String, T> tables, DataRuntime runtime, String catalog, String schema, MasterTable master) throws Exception
	 ******************************************************************************************************************/

	/**
	 * 查询分区表
	 * @param catalog catalog
	 * @param schema schema
	 * @param pattern pattern
	 * @param types types
	 * @return String
	 */
	@Override
	public List<Run> buildQueryPartitionTableRunSQL(String catalog, String schema, String pattern, String types) throws Exception{
		return super.buildQueryPartitionTableRunSQL(catalog, schema, pattern, types);
	}
	@Override
	public List<Run> buildQueryPartitionTableRunSQL(MasterTable master, Map<String,Object> tags, String name) throws Exception{
		return super.buildQueryPartitionTableRunSQL(master, tags, name);
	}
	@Override
	public List<Run> buildQueryPartitionTableRunSQL(MasterTable master, Map<String,Object> tags) throws Exception{
		return super.buildQueryPartitionTableRunSQL(master, tags);
	}

	/**
	 *  根据查询结果集构造Table
	 * @param total 合计SQL数量
	 * @param index 第几条SQL 对照 buildQueryMasterTableRunSQL返回顺序
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param master 主表
	 * @param catalog catalog
	 * @param schema schema
	 * @param tables 上一步查询结果
	 * @param set DataSet
	 * @return tables
	 * @throws Exception 异常
	 */
	@Override
	public <T extends PartitionTable> LinkedHashMap<String, T> ptables(int total, int index, boolean create, MasterTable master, String catalog, String schema, LinkedHashMap<String, T> tables, DataSet set) throws Exception{
		return super.ptables(total, index, create, master, catalog, schema, tables, set);
	}

	/**
	 * 根据JDBC
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param master 主表
	 * @param catalog catalog
	 * @param schema schema
	 * @param tables 上一步查询结果
	 * @param runtime runtime
	 * @return tables
	 * @throws Exception 异常
	 */
	@Override
	public <T extends PartitionTable> LinkedHashMap<String,T> ptables(boolean create, LinkedHashMap<String, T> tables, DataRuntime runtime, String catalog, String schema, MasterTable master) throws Exception{
		return super.ptables(create, tables, runtime, catalog, schema, master);
	}


	/* *****************************************************************************************************************
	 * 													column
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryColumnRunSQL(Table table, boolean metadata)
	 * <T extends Column> LinkedHashMap<String, T> columns(int index, boolean create, Table table, LinkedHashMap<String, T> columns, DataSet set) throws Exception
	 * <T extends Column> LinkedHashMap<String, T> columns(boolean create, LinkedHashMap<String, T> columns, Table table, SqlRowSet set) throws Exception
	 * <T extends Column> LinkedHashMap<String, T> columns(boolean create, LinkedHashMap<String, T> columns, DataRuntime runtime, Table table, String pattern) throws Exception
	 ******************************************************************************************************************/

	/**
	 * 查询表上的列
	 * @param table 表
	 * @return sql
	 */

	/**
	 * 查询表上的列
	 * @param table 表
	 * @param metadata 是否根据metadata(true:1=0,false:查询系统表)
	 * @return sql
	 */
	@Override
	public List<Run> buildQueryColumnRunSQL(Table table, boolean metadata) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		if(metadata){
			builder.append("SELECT * FROM ");
			name(builder, table);
			builder.append(" WHERE 1=0");
		}else{
			String catalog = table.getCatalog();
			String schema = table.getSchema();
			builder.append("SELECT * FROM INFORMATION_SCHEMA.COLUMNS WHERE 1=1 ");
			/*if(BasicUtil.isNotEmpty(catalog)){
				builder.append(" AND TABLE_CATALOG = '").append(catalog).append("'");
			}*/
			if(BasicUtil.isNotEmpty(schema)){
				builder.append(" AND TABLE_SCHEMA = '").append(schema).append("'");
			}
			builder.append(" AND TABLE_NAME = '").append(objectName(table.getName())).append("'");
		}
		return runs;
	}

	/**
	 * 根据查询结果集构造Tag
	 * @param index 第几条SQL 对照 buildQueryColumnRunSQL返回顺序
	 * @param table 表
	 * @param columns 上一步查询结果
	 * @param set set
	 * @return columns columns
	 * @throws Exception 异常
	 */
	@Override
	public <T extends Column> LinkedHashMap<String, T> columns(int index, boolean create, Table table, LinkedHashMap<String, T> columns, DataSet set) throws Exception{
		return super.columns(index, create, table, columns, set);
	}
	@Override
	public <T extends Column> LinkedHashMap<String, T> columns(boolean create, LinkedHashMap<String, T> columns, Table table, SqlRowSet set) throws Exception{
		return super.columns(create, columns, table, set);
	}
	@Override
	public <T extends Column> LinkedHashMap<String, T> columns(boolean create, LinkedHashMap<String, T> columns, DataRuntime runtime, Table table, String pattern) throws Exception{
		return super.columns(create, columns, runtime, table, pattern);
	}


	/* *****************************************************************************************************************
	 * 													tag
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryTagRunSQL(Table table, boolean metadata)
	 * <T extends Tag> LinkedHashMap<String, T> tags(int index, boolean create, Table table, LinkedHashMap<String, T> tags, DataSet set) throws Exception
	 * <T extends Tag> LinkedHashMap<String, T> tags(boolean create, Table table, LinkedHashMap<String, T> tags, SqlRowSet set) throws Exception
	 * <T extends Tag> LinkedHashMap<String, T> tags(boolean create, LinkedHashMap<String, T> tags, DataRuntime runtime, Table table, String pattern) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 不支持
	 * @param table 表
	 * @param metadata 是否根据metadata | 查询系统表
	 * @return sqls
	 */
	@Override
	public List<Run> buildQueryTagRunSQL(Table table, boolean metadata) throws Exception{
		return new ArrayList<>();
	}

	/**
	 * 不支持
	 * 根据查询结果集构造Tag
	 * @param index 第几条查询SQL 对照 buildQueryTagRunSQL返回顺序
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param table 表
	 * @param tags 上一步查询结果
	 * @param set set
	 * @return tags tags
	 * @throws Exception 异常
	 */
	@Override
	public <T extends Tag> LinkedHashMap<String, T> tags(int index, boolean create, Table table, LinkedHashMap<String, T> tags, DataSet set) throws Exception{
		return new LinkedHashMap();
	}
	@Override
	public <T extends Tag> LinkedHashMap<String, T> tags(boolean create, Table table, LinkedHashMap<String, T> tags, SqlRowSet set) throws Exception{
		return new LinkedHashMap();
	}
	@Override
	public <T extends Tag> LinkedHashMap<String, T> tags(boolean create, LinkedHashMap<String, T> tags, DataRuntime runtime, Table table, String pattern) throws Exception{
		return new LinkedHashMap();
	}


	/* *****************************************************************************************************************
	 * 													primary
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryPrimaryRunSQL(Table table) throws Exception
	 * PrimaryKey primary(int index, Table table, DataSet set) throws Exception
	 ******************************************************************************************************************/

	/**
	 * 查询表上的主键
	 * @param table 表
	 * @return sqls
	 */
	public List<Run> buildQueryPrimaryRunSQL(Table table) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("SHOW INDEX FROM ");
		name(builder, table);
		return runs;
	}

	/**
	 *  根据查询结果集构造PrimaryKey
	 * @param index 第几条查询SQL 对照 buildQueryIndexRunSQL 返回顺序
	 * @param table 表
	 * @param set sql查询结果
	 * @throws Exception 异常
	 */
	public PrimaryKey primary(int index, Table table, DataSet set) throws Exception{
		PrimaryKey primary = null;
		set = set.getRows("Key_name", "PRIMARY");
		if(set.size() > 0){
			primary = new PrimaryKey();
			for(DataRow row:set){
				primary.setName(row.getString("Key_name"));
				Column column = new Column(row.getString("Column_name"));
				primary.addColumn(column);
			}
		}
		return primary;
	}


	/* *****************************************************************************************************************
	 * 													foreign
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryForeignsRunSQL(Table table) throws Exception
	 * <T extends ForeignKey> LinkedHashMap<String, T> foreigns(int index, Table table, LinkedHashMap<String, T> foreigns, DataSet set) throws Exception
	 ******************************************************************************************************************/

	/**
	 * 查询表上的外键
	 * @param table 表
	 * @return sqls
	 */
	public List<Run> buildQueryForeignsRunSQL(Table table) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("SELECT * FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE where REFERENCED_TABLE_NAME IS NOT NULL\n");
		if(null != table){
			String name = table.getName();
			if(BasicUtil.isNotEmpty(name)){
				builder.append(" AND TABLE_NAME = '").append(name).append("'\n");
			}
		}
		builder.append("ORDER BY ORDINAL_POSITION");
		return runs;
	}

	/**
	 *  根据查询结果集构造PrimaryKey
	 * @param index 第几条查询SQL 对照 buildQueryForeignsRunSQL 返回顺序
	 * @param table 表
	 * @param foreigns 上一步查询结果
	 * @param set sql查询结果
	 * @throws Exception 异常
	 */
	public <T extends ForeignKey> LinkedHashMap<String, T> foreigns(int index, Table table, LinkedHashMap<String, T> foreigns, DataSet set) throws Exception{
		if(null == foreigns){
			foreigns = new LinkedHashMap<>();
		}
		for(DataRow row:set){
			String name = row.getString("CONSTRAINT_NAME");
			T foreign = foreigns.get(name.toUpperCase());
			if(null == foreign){
				foreign = (T)new ForeignKey();
				foreign.setName(name);
				foreign.setTable(row.getString("TABLE_NAME"));
				foreign.setReference(row.getString("REFERENCED_TABLE_NAME"));
				foreigns.put(name.toUpperCase(), foreign);
			}
			foreign.addColumn(new Column(row.getString("COLUMN_NAME")).setReference(row.getString("REFERENCED_COLUMN_NAME")).setPosition(row.getInt("ORDINAL_POSITION", 0)));

		}
		return foreigns;
	}

	/* *****************************************************************************************************************
	 * 													index
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryIndexRunSQL(Table table, boolean metadata)
	 * <T extends Index> LinkedHashMap<String, T> indexs(int index, boolean create, Table table, LinkedHashMap<String, T> indexs, DataSet set) throws Exception
	 * <T extends Index> LinkedHashMap<String, T> indexs(boolean create, Table table, LinkedHashMap<String, T> indexs, SqlRowSet set) throws Exception
	 * <T extends Index> LinkedHashMap<String, T> indexs(boolean create, LinkedHashMap<String, T> indexs, DataRuntime runtime, Table table, boolean unique, boolean approximate) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 查询表上的列
	 * @param table 表
	 * @param name 名称
	 * @return sql
	 */
	@Override
	public List<Run> buildQueryIndexRunSQL(Table table, String name){
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("SELECT * FROM INFORMATION_SCHEMA.STATISTICS\n");
		builder.append("WHERE 1=1\n");
		if(null != table) {
			if (null != table.getSchema()) {
				builder.append("AND TABLE_SCHEMA='").append(table.getSchema()).append("'\n");
			}
			if (null != table.getName()) {
				builder.append("AND TABLE_NAME='").append(objectName(table.getName())).append("'\n");
			}
		}
		if(BasicUtil.isNotEmpty(name)){
			builder.append("AND INDEX_NAME='").append(name).append("'\n");
		}
		builder.append("ORDER BY SEQ_IN_INDEX");
		return runs;
	}

	/**
	 *
	 * @param index 第几条查询SQL 对照 buildQueryIndexRunSQL 返回顺序
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param table 表
	 * @param indexs 上一步查询结果
	 * @param set set
	 * @return indexs indexs
	 * @throws Exception 异常
	 */
	@Override
	public <T extends Index> LinkedHashMap<String, T> indexs(int index, boolean create, Table table, LinkedHashMap<String, T> indexs, DataSet set) throws Exception{
		if(null == indexs){
			indexs = new LinkedHashMap<>();
		}
		for(DataRow row:set){
			String name = row.getString("INDEX_NAME");
			if(null == name){
				continue;
			}
			String schema = row.getString("TABLE_SCHEMA");
			String tableName = row.getString("TABLE_NAME");
			T idx = indexs.get(name.toUpperCase());
			if(null == idx && create){
				idx = (T)new Index();
				indexs.put(name.toUpperCase(), idx);
			}
			idx.setTable(tableName);
			idx.setName(name);
			if(null == table){
				table = new Table(tableName);
				table.setSchema(schema);
			}
			idx.setTable(table);
			if(name.equals("PRIMARY")){
				idx.setPrimary(true);
			}
			if("0".equals(row.getString("NON_UNIQUE"))){
				idx.setUnique(true);
			}
			idx.setComment(row.getString("INDEX_COMMENT"));
			idx.setType(row.getString("INDEX_TYPE"));

			String col = row.getString("COLUMN_NAME");
			Column column = idx.getColumn(col);
			if(null == column){
				idx.addColumn(col, null, row.getInt("SEQ_IN_INDEX", 0));
			}
			indexs.put(name.toUpperCase(), idx);
		}
		return indexs;
	}
	@Override
	public <T extends Index> LinkedHashMap<String, T> indexs(boolean create, Table table, LinkedHashMap<String, T> indexs, SqlRowSet set) throws Exception{
		return super.indexs(create, table, indexs, set);
	}
	@Override
	public <T extends Index> LinkedHashMap<String, T> indexs(boolean create, LinkedHashMap<String, T> indexs, DataRuntime runtime, Table table, boolean unique, boolean approximate) throws Exception{
		return super.indexs(create, indexs, runtime, table, unique, approximate);
	}


	/* *****************************************************************************************************************
	 * 													constraint
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryConstraintRunSQL(Table table, boolean metadata)
	 * LinkedHashMap<String, Constraint> constraints(int constraint, boolean create,  Table table, LinkedHashMap<String, Constraint> constraints, DataSet set) throws Exception
	 * <T extends Constraint> LinkedHashMap<String, T> constraints(boolean create, Table table, LinkedHashMap<String, T> constraints, SqlRowSet set) throws Exception
	 * <T extends Constraint> LinkedHashMap<String, T> constraints(boolean create, Table table, LinkedHashMap<String, T> constraints, ResultSet set) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 查询表上的约束
	 * @param table 表
	 * @param metadata 是否根据metadata | 查询系统表
	 * @return sqls
	 */
	@Override
	public List<Run> buildQueryConstraintRunSQL(Table table, boolean metadata) throws Exception{
		return super.buildQueryConstraintRunSQL(table, metadata);
	}

	/**
	 *  根据查询结果集构造Constraint
	 * @param index 第几条查询SQL 对照 buildQueryConstraintRunSQL 返回顺序
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param table 表
	 * @param constraints 上一步查询结果
	 * @param set set
	 * @return constraints constraints
	 * @throws Exception 异常
	 */
	@Override
	public <T extends Constraint> LinkedHashMap<String, T> constraints(int index , boolean create, Table table, LinkedHashMap<String, T> constraints, DataSet set) throws Exception{

		return super.constraints(index, create, table, constraints, set);
	}
	@Override
	public <T extends Constraint> LinkedHashMap<String, T> constraints(boolean create, Table table, LinkedHashMap<String, T> constraints, SqlRowSet set) throws Exception{
		return super.constraints(create, table, constraints, set);
	}

	@Override
	public <T extends Constraint> LinkedHashMap<String, T> constraints(boolean create, Table table, LinkedHashMap<String, T> constraints, ResultSet set) throws Exception{
		return super.constraints(create, table, constraints, set);
	}



	/* *****************************************************************************************************************
	 * 													trigger
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryTriggerRunSQL(Table table, List<Trigger.EVENT> events)
	 * <T extends Trigger> LinkedHashMap<String, T> triggers(int index, boolean create, Table table, LinkedHashMap<String, T> triggers, DataSet set)
	 ******************************************************************************************************************/
	/**
	 * 查询表上的trigger
	 * @param table 表
	 * @param events INSERT|UPATE|DELETE
	 * @return sqls
	 */

	@Override
	public List<Run> buildQueryTriggerRunSQL(Table table, List<Trigger.EVENT> events) {
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("SELECT * FROM INFORMATION_SCHEMA.TRIGGERS WHERE 1=1");
		if(null != table){
			String schemae = table.getSchema();
			String name = table.getName();
			if(BasicUtil.isNotEmpty(schemae)){
				builder.append(" AND TRIGGER_SCHEMA = '").append(schemae).append("'");
			}
			if(BasicUtil.isNotEmpty(name)){
				builder.append(" AND EVENT_OBJECT_TABLE = '").append(name).append("'");
			}
		}
		if(null != events && events.size()>0){
			builder.append(" AND(");
			boolean first = true;
			for(Trigger.EVENT event:events){
				if(!first){
					builder.append(" OR ");
				}
				builder.append("EVENT_MANIPULATION ='").append(event);
			}
			builder.append(")");
		}
		return runs;
	}

	/**
	 *  根据查询结果集构造Constraint
	 * @param index 第几条查询SQL 对照 buildQueryConstraintRunSQL 返回顺序
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param table 表
	 * @param triggers 上一步查询结果
	 * @param set DataSet
	 * @return constraints constraints
	 * @throws Exception 异常
	 */

	@Override
	public <T extends Trigger> LinkedHashMap<String, T> triggers(int index, boolean create, Table table, LinkedHashMap<String, T> triggers, DataSet set) throws Exception{
		if(null == triggers){
			triggers = new LinkedHashMap<>();
		}
		for(DataRow row:set){
			String name = row.getString("TRIGGER_NAME");
			T trigger = triggers.get(name.toUpperCase());
			if(null == trigger){
				trigger = (T)new Trigger();
			}
			trigger.setName(name);
			Table tab = new Table(row.getString("EVENT_OBJECT_TABLE"));
			tab.setSchema(row.getString("TRIGGER_SCHEMA"));
			trigger.setTable(tab);
			boolean each = false;
			if("ROW".equalsIgnoreCase(row.getString("ACTION_ORIENTATION"))){
				each = true;
			}
			trigger.setEach(each);
			try{
				String[] events = row.getStringNvl("EVENT_MANIPULATION").split(",");
				String time = row.getString("ACTION_TIMING");
				trigger.setTime(Trigger.TIME.valueOf(time));
				for(String event:events) {
					trigger.addEvent(Trigger.EVENT.valueOf(event));
				}
			}catch (Exception e){
				e.printStackTrace();
			}
			trigger.setDefinition(row.getString("ACTION_STATEMENT"));

			triggers.put(name.toUpperCase(), trigger);

		}
		return triggers;
	}

	/* *****************************************************************************************************************
	 *
	 * 													DDL
	 *
	 * =================================================================================================================
	 * database			: 数据库
	 * table			: 表
	 * master table		: 主表
	 * partition table	: 分区表
	 * column			: 列
	 * tag				: 标签
	 * primary key      : 主键
	 * foreign key		: 外键
	 * index			: 索引
	 * constraint		: 约束
	 * trigger		    : 触发器
	 * procedure        : 存储过程
	 * function         : 函数
	 ******************************************************************************************************************/


	/* *****************************************************************************************************************
	 * 													table
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildCreateRunSQL(Table table)
	 * List<Run> buildAddCommentRunSQL(Table table);
	 * List<Run> buildAlterRunSQL(Table table)
	 * List<Run> buildAlterRunSQL(Table table, Collection<Column> columns)
	 * List<Run> buildRenameRunSQL(Table table)
	 * List<Run> buildChangeCommentRunSQL(Table table)
	 * List<Run> buildDropRunSQL(Table table)
	 * StringBuilder checkTableExists(StringBuilder builder, boolean exists)
	 * StringBuilder primary(StringBuilder builder, Table table)
	 * StringBuilder comment(StringBuilder builder, Table table)
	 * StringBuilder name(StringBuilder builder, Table table)
	 ******************************************************************************************************************/


	@Override
	public List<Run> buildCreateRunSQL(Table table) throws Exception{
		return super.buildCreateRunSQL(table);
	}

	/**
	 * 添加表备注(表创建完成后调用,创建过程能添加备注的不需要实现)
	 * @param table 表
	 * @return sql
	 * @throws Exception 异常
	 */
	public List<Run> buildAddCommentRunSQL(Table table) throws Exception {
		return super.buildAddCommentRunSQL(table);
	}
	@Override
	public List<Run> buildAlterRunSQL(Table table) throws Exception{
		return super.buildAlterRunSQL(table);
	}
	/**
	 * 修改列
	 * 有可能生成多条SQL,根据数据库类型优先合并成一条执行
	 * @param table 表
	 * @param columns 列
	 * @return List
	 */
	public List<Run> buildAlterRunSQL(Table table, Collection<Column> columns) throws Exception {
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		if (columns.size() > 0) {
			builder.append("ALTER ").append(table.getKeyword()).append(" ");
			name(builder, table);
			List<Run> slices = new ArrayList<>();
			for(Column column:columns){
				String action = column.getAction();
				if("add".equals(action)){
					slices.addAll(buildAddRunSQL(column, true));
				}else if("alter".equals(action)){
					slices.addAll(buildAlterRunSQL(column, true));
				}else if("drop".equals(action)){
					slices.addAll(buildDropRunSQL(column, true));
				}
			}
			boolean first = true;
			for(Run slice:slices){
				if(BasicUtil.isNotEmpty(slice)){
					builder.append("\n");
					if(!first){
						builder.append(",");
					}
					builder.append(slice.getFinalUpdate());
					first = false;
				}
			}
		}
		return runs;
	}
	/**
	 * 修改表名
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param table 表
	 * @return String
	 */
	@Override
	public List<Run> buildRenameRunSQL(Table table) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("RENAME TABLE ");
		name(builder, table);
		builder.append(" TO ");
		name(builder, table.getUpdate());
		return runs;
	}
	/**
	 * 修改表备注
	 *  ALTER TABLE T COMMENT 'ABC';
	 * @param table 表
	 * @return sql
	 */
	@Override
	public List<Run> buildChangeCommentRunSQL(Table table) {
		List<Run> runs = new ArrayList<>();
		String comment = table.getComment();
		if(BasicUtil.isEmpty(comment)){
			return runs;
		}
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("ALTER TABLE ");
		name(builder, table);
		builder.append(" COMMENT '").append(comment).append("'");
		return runs;
	}
	/**
	 * 删除表
	 * @param table 表
	 * @return String
	 */
	@Override
	public List<Run> buildDropRunSQL(Table table) throws Exception{
		return super.buildDropRunSQL(table);
	}


	@Override
	public StringBuilder checkTableExists(StringBuilder builder, boolean exists){
		return super.checkTableExists(builder, exists);
	}


	/**
	 * 主键
	 * @param builder builder
	 * @param table 表
	 * @return builder
	 */
	@Override
	public StringBuilder primary(StringBuilder builder, Table table){
		List<Column> pks = table.primarys();
		if(pks.size()>0){
			builder.append(",PRIMARY KEY (");
			boolean first = true;
			for(Column pk:pks){
				if(!first){
					builder.append(",");
				}
				SQLUtil.delimiter(builder, pk.getName(), getDelimiterFr(), getDelimiterTo());
				String order = pk.getOrder();
				if(BasicUtil.isNotEmpty(order)){
					builder.append(" ").append(order);
				}
				first = false;
			}
			builder.append(")");
		}
		return builder;
	}

	/**
	 * 备注
	 *
	 * @param builder builder
	 * @param table 表
	 * @return builder
	 */
	@Override
	public StringBuilder comment(StringBuilder builder, Table table){
		return super.comment(builder, table);
	}

	/**
	 * 构造完整表名
	 * @param builder builder
	 * @param table 表
	 * @return StringBuilder
	 */
	@Override
	public StringBuilder name(StringBuilder builder, Table table){
		return super.name(builder, table);
	}

	/* *****************************************************************************************************************
	 * 													view
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildCreateRunSQL(View view);
	 * List<Run> buildAddCommentRunSQL(View view);
	 * List<Run> buildAlterRunSQL(View view);
	 * List<Run> buildRenameRunSQL(View view);
	 * List<Run> buildChangeCommentRunSQL(View view);
	 * List<Run> buildDropRunSQL(View view);
	 * StringBuilder checkViewExists(StringBuilder builder, boolean exists)
	 * StringBuilder primary(StringBuilder builder, View view)
	 * StringBuilder comment(StringBuilder builder, View view)
	 * StringBuilder name(StringBuilder builder, View view)
	 ******************************************************************************************************************/


	@Override
	public List<Run> buildCreateRunSQL(View view) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("CREATE OR REPLACE VIEW ");
		name(builder, view);
		builder.append(" AS \n").append(view.getDefinition());

		runs.addAll(buildAddCommentRunSQL(view));
		return runs;
	}

	@Override
	public List<Run> buildAddCommentRunSQL(View view) throws Exception{
		return super.buildAddCommentRunSQL(view);
	}


	@Override
	public List<Run> buildAlterRunSQL(View view) throws Exception{
		return super.buildAlterRunSQL(view);
	}
	/**
	 * 修改视图名
	 * 子类实现
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param view 视图
	 * @return String
	 */
	@Override
	public List<Run> buildRenameRunSQL(View view) throws Exception{
		return super.buildRenameRunSQL(view);
	}

	@Override
	public List<Run> buildChangeCommentRunSQL(View view) throws Exception{
		return super.buildChangeCommentRunSQL(view);
	}
	/**
	 * 删除视图
	 * @param view 视图
	 * @return String
	 */
	@Override
	public List<Run> buildDropRunSQL(View view) throws Exception{
		return super.buildDropRunSQL(view);
	}

	/**
	 * 创建或删除视图时检测视图是否存在
	 * @param builder builder
	 * @param exists exists
	 * @return StringBuilder
	 */
	@Override
	public StringBuilder checkViewExists(StringBuilder builder, boolean exists){
		return super.checkViewExists(builder, exists);
	}

	/**
	 * 备注 不支持创建视图时带备注的 在子视图中忽略
	 * @param builder builder
	 * @param view 视图
	 * @return builder
	 */
	@Override
	public StringBuilder comment(StringBuilder builder, View view){
		return super.comment(builder, view);
	}

	/* *****************************************************************************************************************
	 * 													master table
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildCreateRunSQL(MasterTable table)
	 * List<Run> buildAddCommentRunSQL(MasterTable table)
	 * List<Run> buildAlterRunSQL(MasterTable table)
	 * List<Run> buildDropRunSQL(MasterTable table)
	 * List<Run> buildRenameRunSQL(MasterTable table)
	 * List<Run> buildChangeCommentRunSQL(MasterTable table)
	 ******************************************************************************************************************/
	/**
	 * 创建主表
	 * @param table 表
	 * @return String
	 */
	@Override
	public List<Run> buildCreateRunSQL(MasterTable table) throws Exception{
		return super.buildCreateRunSQL(table);
	}
	@Override
	public List<Run> buildAlterRunSQL(MasterTable table) throws Exception{
		return super.buildAlterRunSQL(table);
	}
	@Override
	public List<Run> buildDropRunSQL(MasterTable table) throws Exception{
		return super.buildDropRunSQL(table);
	}
	@Override
	public List<Run> buildRenameRunSQL(MasterTable table) throws Exception{
		return super.buildRenameRunSQL(table);
	}
	@Override
	public List<Run> buildChangeCommentRunSQL(MasterTable table) throws Exception{
		return super.buildChangeCommentRunSQL(table);
	}


	/* *****************************************************************************************************************
	 * 													partition table
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildCreateRunSQL(PartitionTable table)
	 * List<Run> buildAlterRunSQL(PartitionTable table)
	 * List<Run> buildDropRunSQL(PartitionTable table)
	 * List<Run> buildRenameRunSQL(PartitionTable table)
	 * List<Run> buildChangeCommentRunSQL(PartitionTable table)
	 ******************************************************************************************************************/
	/**
	 * 创建分区表
	 * @param table 表
	 * @return String
	 */
	@Override
	public List<Run> buildCreateRunSQL(PartitionTable table) throws Exception{
		return super.buildCreateRunSQL(table);
	}
	@Override
	public List<Run> buildAlterRunSQL(PartitionTable table) throws Exception{
		return super.buildAlterRunSQL(table);
	}
	@Override
	public List<Run> buildDropRunSQL(PartitionTable table) throws Exception{
		return super.buildDropRunSQL(table);
	}
	@Override
	public List<Run> buildRenameRunSQL(PartitionTable table) throws Exception{
		return super.buildRenameRunSQL(table);
	}
	@Override
	public List<Run> buildChangeCommentRunSQL(PartitionTable table) throws Exception{
		return super.buildChangeCommentRunSQL(table);
	}

	/* *****************************************************************************************************************
	 * 													column
	 * -----------------------------------------------------------------------------------------------------------------
	 * String alterColumnKeyword()
	 * List<Run> buildAddRunSQL(Column column, boolean slice)
	 * List<Run> buildAddRunSQL(Column column)
	 * List<Run> buildAlterRunSQL(Column column, boolean slice)
	 * List<Run> buildAlterRunSQL(Column column)
	 * List<Run> buildDropRunSQL(Column column, boolean slice)
	 * List<Run> buildDropRunSQL(Column column)
	 * List<Run> buildRenameRunSQL(Column column)
	 * List<Run> buildChangeTypeRunSQL(Column column)
	 * List<Run> buildChangeDefaultRunSQL(Column column)
	 * List<Run> buildChangeNullableRunSQL(Column column)
	 * List<Run> buildChangeCommentRunSQL(Column column)
	 * List<Run> buildAddCommentRunSQL(Column column)
	 * StringBuilder define(StringBuilder builder, Column column)
	 * StringBuilder type(StringBuilder builder, Column column)
	 * boolean isIgnorePrecision(Column column);
	 * boolean isIgnoreScale(Column column);
	 * Boolean checkIgnorePrecision(String datatype);
	 * Boolean checkIgnoreScale(String datatype);
	 * boolean isIgnorePrecision(Column column);
	 * boolean isIgnoreScale(Column column);
	 * Boolean checkIgnorePrecision(String datatype);
	 * Boolean checkIgnoreScale(String datatype);
	 * StringBuilder nullable(StringBuilder builder, Column column)
	 * StringBuilder charset(StringBuilder builder, Column column)
	 * StringBuilder defaultValue(StringBuilder builder, Column column)
	 * StringBuilder increment(StringBuilder builder, Column column)
	 * StringBuilder onupdate(StringBuilder builder, Column column)
	 * StringBuilder position(StringBuilder builder, Column column)
	 * StringBuilder comment(StringBuilder builder, Column column)
	 * StringBuilder checkColumnExists(StringBuilder builder, boolean exists)
	 ******************************************************************************************************************/
	@Override
	public String alterColumnKeyword(){
		return "ALTER COLUMN ";
	}


	/**
	 * 添加列
	 * ALTER TABLE  HR_USER ADD COLUMN UPT_TIME datetime CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP comment '修改时间' AFTER ID;
	 * @param column 列
	 * @param slice 是否只生成片段(不含alter table部分，用于DDL合并)
	 * @return String
	 */
	@Override
	public List<Run> buildAddRunSQL(Column column, boolean slice) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		if(!slice) {
			Table table = column.getTable(true);
			builder.append("ALTER TABLE ");
			name(builder, table);
		}
		Column update = column.getUpdate();
		if(null == update){
			// 添加列
			addColumnGuide(builder, column);
			SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo()).append(" ");
			// 数据类型
			type(builder, column);
			// 编码
			charset(builder, column);
			// 默认值
			defaultValue(builder, column);
			// 非空
			nullable(builder, column);
			// 更新事件
			onupdate(builder, column);
			// 备注
			comment(builder, column);
			// 位置
			position(builder, column);
		}

		runs.addAll(buildAddCommentRunSQL(column));
		return runs;
	}

	/**
	 * 修改列 ALTER TABLE   HR_USER CHANGE UPT_TIME UPT_TIME datetime   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP  comment '修改时间' AFTER ID;
	 * @param column 列
	 * @param slice 是否只生成片段(不含alter table部分，用于DDL合并)
	 * @return sqls
	 */
	@Override
	public List<Run> buildAlterRunSQL(Column column, boolean slice) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		if(!slice) {
			Table table = column.getTable(true);
			builder.append("ALTER TABLE ");
			name(builder, table);
		}
		Column update = column.getUpdate();
		if(null != update){
			builder.append(" CHANGE ");
			SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo()).append(" ");
			if(!BasicUtil.equalsIgnoreCase(column.getName(), update.getTableName(true))) {
				SQLUtil.delimiter(builder, update.getName(), getDelimiterFr(), getDelimiterTo()).append(" ");
			}
			define(builder, update);
		}
		return runs;
	}


	/**
	 * 删除列
	 * ALTER TABLE HR_USER DROP COLUMN NAME;
	 * @param column 列
	 * @param slice 是否只生成片段(不含alter table部分，用于DDL合并)
	 * @return String
	 */
	@Override
	public List<Run> buildDropRunSQL(Column column, boolean slice) throws Exception{
		return super.buildDropRunSQL(column, slice);
	}

	/**
	 * 修改列名
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param column 列
	 * @return String
	 */
	@Override
	public List<Run> buildRenameRunSQL(Column column) throws Exception {
		return super.buildRenameRunSQL(column);
	}


	/**
	 * 修改数据类型
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param column 列
	 * @return sql
	 */
	@Override
	public List<Run> buildChangeTypeRunSQL(Column column) throws Exception{
		return super.buildChangeTypeRunSQL(column);
	}
	/**
	 * 修改默认值
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param column 列
	 * @return String
	 */
	@Override
	public List<Run> buildChangeDefaultRunSQL(Column column) throws Exception{
		return super.buildChangeDefaultRunSQL(column);
	}

	/**
	 * 修改非空限制
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param column 列
	 * @return String
	 */
	@Override
	public List<Run> buildChangeNullableRunSQL(Column column) throws Exception{
		return super.buildChangeNullableRunSQL(column);
	}
	/**
	 * 修改备注
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param column 列
	 * @return String
	 */
	@Override
	public List<Run> buildChangeCommentRunSQL(Column column) throws Exception{
		return super.buildChangeCommentRunSQL(column);
	}




	/**
	 * 取消自增
	 * @param column 列
	 * @return sql
	 * @throws Exception 异常
	 */
	public List<Run> buildDropAutoIncrement(Column column) throws Exception{
		return super.buildDropAutoIncrement(column);
	}
	/**
	 * 定义列
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder define(StringBuilder builder, Column column){
		return super.define(builder, column);
	}


	/**
	 * 数据类型
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder type(StringBuilder builder, Column column){
		return super.type(builder, column);
	}

	@Override
	public boolean isIgnorePrecision(Column column) {
		return super.isIgnorePrecision(column);
	}

	@Override
	public boolean isIgnoreScale(Column column) {
		return super.isIgnoreScale(column);
	}
	@Override
	public Boolean checkIgnorePrecision(String datatype) {
		return super.checkIgnorePrecision(datatype);
	}
	@Override
	public Boolean checkIgnoreScale(String datatype) {
		return super.checkIgnoreScale(datatype);
	}

	/**
	 * 编码
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder nullable(StringBuilder builder, Column column){
		return super.nullable(builder, column);
	}
	/**
	 * 编码
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder charset(StringBuilder builder, Column column){
		return super.charset(builder, column);
	}
	/**
	 * 默认值
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder defaultValue(StringBuilder builder, Column column){
		return super.defaultValue(builder, column);
	}
	/**
	 * 递增列
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder increment(StringBuilder builder, Column column){
		if(column.isAutoIncrement() == 1){
			builder.append(" AUTO_INCREMENT");
		}
		return builder;
	}




	/**
	 * 更新行事件
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	public StringBuilder onupdate(StringBuilder builder, Column column){
		if(column.isOnUpdate() == 1){
			builder.append(" ON UPDATE CURRENT_TIMESTAMP");
		}
		return builder;
	}
	/**
	 * 位置 
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder position(StringBuilder builder, Column column){
		Integer position = column.getPosition();
		if(null != position && 0 == position){
			builder.append(" FIRST");
		}else{
			String after = column.getAfter();
			if(BasicUtil.isNotEmpty(after)){
				builder.append(" AFTER ").append(after);
			}
		}
		return builder;
	}

	/**
	 * 备注
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder comment(StringBuilder builder, Column column){
		String comment = column.getComment();
		if(BasicUtil.isNotEmpty(comment)){
			builder.append(" COMMENT '").append(comment).append("'");
		}
		return builder;
	}

	/**
	 * 创建或删除列时检测是否存在
	 * @param builder builder
	 * @param exists exists
	 * @return sql
	 */
	@Override
	public StringBuilder checkColumnExists(StringBuilder builder, boolean exists){
		return super.checkColumnExists(builder, exists);
	}
	/* *****************************************************************************************************************
	 * 													tag
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildAddRunSQL(Tag tag);
	 * List<Run> buildAlterRunSQL(Tag tag);
	 * List<Run> buildDropRunSQL(Tag tag);
	 * List<Run> buildRenameRunSQL(Tag tag);
	 * List<Run> buildChangeDefaultRunSQL(Tag tag);
	 * List<Run> buildChangeNullableRunSQL(Tag tag);
	 * List<Run> buildChangeCommentRunSQL(Tag tag);
	 * List<Run> buildChangeTypeRunSQL(Tag tag);
	 * StringBuilder checkTagExists(StringBuilder builder, boolean exists)
	 ******************************************************************************************************************/

	/**
	 * 添加标签
	 * 不支持
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public List<Run> buildAddRunSQL(Tag tag) throws Exception{
		return new ArrayList<>();
	}


	/**
	 * 不支持
	 * @param tag 标签
	 * @return sqls
	 */
	@Override
	public List<Run> buildAlterRunSQL(Tag tag) throws Exception{
		return new ArrayList<>();
	}


	/**
	 * 删除标签
	 * 不支持
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public List<Run> buildDropRunSQL(Tag tag) throws Exception{
		return new ArrayList<>();
	}


	/**
	 * 修改标签名
	 * 不支持
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public List<Run> buildRenameRunSQL(Tag tag) throws Exception{
		return new ArrayList<>();
	}

	/**
	 * 修改默认值
	 * 不支持
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public List<Run> buildChangeDefaultRunSQL(Tag tag) throws Exception{
		return new ArrayList<>();
	}

	/**
	 * 修改非空限制
	 * 不支持
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public List<Run> buildChangeNullableRunSQL(Tag tag) throws Exception{
		return new ArrayList<>();
	}
	/**
	 * 修改备注
	 *
	 * 不支持
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public List<Run> buildChangeCommentRunSQL(Tag tag) throws Exception{
		return new ArrayList<>();
	}

	/**
	 * 修改数据类型
	 * 不支持
	 * @param tag 标签
	 * @return sql
	 */
	@Override
	public List<Run> buildChangeTypeRunSQL(Tag tag) throws Exception{
		return new ArrayList<>();
	}

	/**
	 * 创建或删除标签时检测是否存在
	 * @param builder builder
	 * @param exists exists
	 * @return sql
	 */
	@Override
	public StringBuilder checkTagExists(StringBuilder builder, boolean exists){
		return super.checkTagExists(builder, exists);
	}

	/* *****************************************************************************************************************
	 * 													primary
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildAddRunSQL(PrimaryKey primary) throws Exception
	 * List<Run> buildAlterRunSQL(PrimaryKey primary) throws Exception
	 * List<Run> buildDropRunSQL(PrimaryKey primary) throws Exception
	 * List<Run> buildRenameRunSQL(PrimaryKey primary) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 添加主键
	 * @param primary 主键
	 * @return String
	 */
	@Override
	public List<Run> buildAddRunSQL(PrimaryKey primary) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		Map<String,Column> columns = primary.getColumns();
		if(columns.size()>0) {
			builder.append("ALTER TABLE ");
			name(builder, primary.getTable(true));
			builder.append(" ADD PRIMARY KEY (");
			boolean first = true;
			for(Column column:columns.values()){
				if(!first){
					builder.append(",");
				}
				SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo());
				first = false;
			}
			builder.append(")");

		}
		return runs;
	}
	/**
	 * 修改主键
	 * 有可能生成多条SQL
	 * @param primary 主键
	 * @return List
	 */
	@Override
	public List<Run> buildAlterRunSQL(PrimaryKey primary) throws Exception{
		return super.buildAlterRunSQL(primary);
	}

	/**
	 * 删除主键
	 * @param primary 主键
	 * @return String
	 */
	@Override
	public List<Run> buildDropRunSQL(PrimaryKey primary) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("ALTER TABLE ");
		name(builder, primary.getTable(true));
		builder.append(" DROP PRIMARY KEY");
		return runs;
	}
	/**
	 * 修改主键名
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param primary 主键
	 * @return String
	 */
	@Override
	public List<Run> buildRenameRunSQL(PrimaryKey primary) throws Exception{
		return super.buildRenameRunSQL(primary);
	}

	/* *****************************************************************************************************************
	 * 													foreign
	 ******************************************************************************************************************/

	/** ALTER TABLE bb
	 ADD CONSTRAINT fk_constraint_name
	 FOREIGN KEY (aid, acode)
	 REFERENCES aa(id,code);

	 * 添加外键
	 * @param foreign 外键
	 * @return String
	 */
	public List<Run> buildAddRunSQL(ForeignKey foreign) throws Exception{
		return super.buildAddRunSQL(foreign);
	}
	/**
	 * 添加外键
	 * @param foreign 外键
	 * @return List
	 */
	public List<Run> buildAlterRunSQL(ForeignKey foreign) throws Exception{
		return super.buildAlterRunSQL(foreign);
	}

	/**
	 * 删除外键
	 * @param foreign 外键
	 * @return String
	 */
	public List<Run> buildDropRunSQL(ForeignKey foreign) throws Exception{
		return super.buildDropRunSQL(foreign);
	}

	/**
	 * 修改外键名
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param foreign 外键
	 * @return String
	 */
	public List<Run> buildRenameRunSQL(ForeignKey foreign) throws Exception{
		return super.buildRenameRunSQL(foreign);
	}

	/* *****************************************************************************************************************
	 * 													index
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildAddRunSQL(Index index) throws Exception
	 * List<Run> buildAlterRunSQL(Index index) throws Exception
	 * List<Run> buildDropRunSQL(Index index) throws Exception
	 * List<Run> buildRenameRunSQL(Index index) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 添加索引
	 * @param index 索引
	 * @return String
	 */
	@Override
	public List<Run> buildAddRunSQL(Index index) throws Exception{
		return super.buildAddRunSQL(index);
	}
	/**
	 * 修改索引
	 * 有可能生成多条SQL
	 * @param index 索引
	 * @return List
	 */
	@Override
	public List<Run> buildAlterRunSQL(Index index) throws Exception{
		return super.buildAlterRunSQL(index);
	}

	/**
	 * 删除索引
	 * @param index 索引
	 * @return String
	 */
	@Override
	public List<Run> buildDropRunSQL(Index index) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("ALTER TABLE ").append(index.getTableName(true));
		if(index.isPrimary()){
			builder.append(" DROP PRIMARY KEY");
		}else {
			builder.append(" DROP INDEX ").append(index.getName());
		}
		return runs;
	}
	/**
	 * 修改索引名
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param index 索引
	 * @return String
	 */
	@Override
	public List<Run> buildRenameRunSQL(Index index) throws Exception{
		return super.buildRenameRunSQL(index);
	}
	/**
	 * 索引备注
	 * @param builder
	 * @param index
	 */
	public void comment(StringBuilder builder, Index index){
		String comment = index.getComment();
		if(BasicUtil.isNotEmpty(comment)){
			builder.append(" COMMENT '").append(comment).append("'");
		};
	}
	/* *****************************************************************************************************************
	 * 													constraint
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildAddRunSQL(Constraint constraint) throws Exception
	 * List<Run> buildAlterRunSQL(Constraint constraint) throws Exception
	 * List<Run> buildDropRunSQL(Constraint constraint) throws Exception
	 * List<Run> buildRenameRunSQL(Constraint constraint) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 添加约束
	 * @param constraint 约束
	 * @return String
	 */
	@Override
	public List<Run> buildAddRunSQL(Constraint constraint) throws Exception{
		return super.buildAddRunSQL(constraint);
	}
	/**
	 * 修改约束
	 * 有可能生成多条SQL
	 * @param constraint 约束
	 * @return List
	 */
	@Override
	public List<Run> buildAlterRunSQL(Constraint constraint) throws Exception{
		return super.buildAlterRunSQL(constraint);
	}

	/**
	 * 删除约束
	 * @param constraint 约束
	 * @return String
	 */
	@Override
	public List<Run> buildDropRunSQL(Constraint constraint) throws Exception{
		return super.buildDropRunSQL(constraint);
	}
	/**
	 * 修改约束名
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param constraint 约束
	 * @return String
	 */
	@Override
	public List<Run> buildRenameRunSQL(Constraint constraint) throws Exception{
		return super.buildRenameRunSQL(constraint);
	}


	/* *****************************************************************************************************************
	 *
	 * 													common
	 *------------------------------------------------------------------------------------------------------------------
	 * boolean isBooleanColumn(Column column)
	 *  boolean isNumberColumn(Column column)
	 * boolean isCharColumn(Column column)
	 * String value(Column column, SQL_BUILD_IN_VALUE value)
	 * String type(String type)
	 * String type2class(String type)
	 ******************************************************************************************************************/

	@Override
	public boolean isBooleanColumn(Column column) {
		return super.isBooleanColumn(column);
	}
	/**
	 * 是否同数字
	 * @param column 列
	 * @return boolean
	 */
	@Override
	public  boolean isNumberColumn(Column column){
		return super.isNumberColumn(column);
	}

	@Override
	public boolean isCharColumn(Column column) {
		return super.isCharColumn(column);
	}
	/**
	 * 内置函数
	 * @param value SQL_BUILD_IN_VALUE
	 * @return String
	 */

	@Override
	public String value(Column column, SQL_BUILD_IN_VALUE value){
		if(value == SQL_BUILD_IN_VALUE.CURRENT_TIME){
			return "now()";
		}
		return null;
	}


} 
