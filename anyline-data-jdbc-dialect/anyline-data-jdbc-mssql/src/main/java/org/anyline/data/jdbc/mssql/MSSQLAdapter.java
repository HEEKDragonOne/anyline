 
package org.anyline.data.jdbc.mssql;

import org.anyline.dao.AnylineDao;
import org.anyline.data.adapter.JDBCAdapter;
import org.anyline.data.adapter.SQLAdapter;
import org.anyline.data.entity.*;
import org.anyline.data.prepare.auto.init.DefaultTextPrepare;
import org.anyline.data.run.Run;
import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.entity.OrderStore;
import org.anyline.entity.PageNavi;
import org.anyline.entity.data.DatabaseType;
import org.anyline.proxy.EntityAdapterProxy;
import org.anyline.util.BasicUtil;
import org.anyline.util.ConfigTable;
import org.anyline.util.SQLUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import java.sql.Connection;
import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

@Repository("anyline.data.jdbc.adapter.mssql") 
public class MSSQLAdapter extends SQLAdapter implements JDBCAdapter, InitializingBean {
	 
	@Autowired(required = false) 
	@Qualifier("anyline.dao") 
	protected AnylineDao dao; 
	 
	public DatabaseType type(){
		return DatabaseType.MSSQL; 
	}

	@Value("${anyline.jdbc.delimiter.mssql:}")
	private String delimiter;

	@Override
	public void afterPropertiesSet()  {
		setDelimiter(delimiter);
	}

	private static String dbVersion = ConfigTable.getString("DATABASE_VERSION"); 
	public MSSQLAdapter(){
		super();
		delimiterFr = "[";
		delimiterTo = "]";
		for (MSSQLColumnTypeAlias alias: MSSQLColumnTypeAlias.values()){
			types.put(alias.name(), alias.standard());
		}
	}

	private String getDbVersion(){ 
		if(null == dbVersion){ 
			DataSet set = dao.querys(new DefaultTextPrepare("SELECT @@VERSION AS VS"));
			if(set.size()>0){ 
				dbVersion = set.getString(0,"VS","")+"";
				dbVersion = dbVersion.toUpperCase().replaceAll("\\s{2,}", ""); 
				 
				if(null != dbVersion && dbVersion.contains("SERVER2000")){ 
					dbVersion = "2000"; 
				}else{ 
					dbVersion = "2005"; 
				} 
			}else{ 
				dbVersion = "2005"; 
			} 
		} 
		return dbVersion; 
	}

	/* *****************************************************************************************************
	 *
	 * 											DML
	 *
	 * ****************************************************************************************************/
	/** 
	 * 查询SQL 
	 * Run 反转调用
	 * @param run  run
	 * @return String
	 */ 
	@Override 
	public String parseFinalQuery(Run run){
		StringBuilder builder = new StringBuilder(); 
		String cols = run.getQueryColumns(); 
		PageNavi navi = run.getPageNavi(); 
		String sql = run.getBaseQuery(); 
		OrderStore orders = run.getOrderStore(); 
		int first = 0; 
		int last = 0; 
		String order = ""; 
		if(null != orders){ 
			order = orders.getRunText(getDelimiterFr()+getDelimiterTo());
		} 
		if(null != navi){ 
			first = navi.getFirstRow(); 
			last = navi.getLastRow(); 
		} 
		if(first == 0 && null != navi){ 
			// top
			builder.append("SELECT TOP ").append(last+1).append(" "+cols+" FROM(\n"); 
			builder.append(sql).append("\n) AS _TAB_O \n"); 
			builder.append(order); 
			return builder.toString(); 
		} 
		if(null == navi){ 
			builder.append(sql).append("\n").append(order); 
		}else{ 
			// 分页
			if("2000".equals(getDbVersion())){ 
				int rows = navi.getPageRows(); 
				if(rows * navi.getCurPage() > navi.getTotalRow()){ 
					// 最后一页不足10条
					rows = navi.getTotalRow() % navi.getPageRows(); 
				} 
				String asc = order; 
				String desc = order.replace("ASC", "<A_ORDER>"); 
				desc = desc.replace("DESC", "ASC"); 
				desc = desc.replace("<A_ORDER>", "DESC"); 
				builder.append("SELECT "+cols+" FROM (\n "); 
				builder.append("SELECT TOP ").append(rows).append(" * FROM (\n"); 
				builder.append("SELECT TOP ").append(navi.getPageRows()*navi.getCurPage()).append(" * "); 
				builder.append(" FROM (" + sql + ") AS T0 ").append(asc).append("\n"); 
				builder.append(") AS T1 ").append(desc).append("\n"); 
				builder.append(") AS T2").append(asc); 
			}else{ 
				// 2005 及以上
				if(BasicUtil.isEmpty(order)){ 
					order = "ORDER BY "+ ConfigTable.DEFAULT_PRIMARY_KEY;
				} 
				builder.append("SELECT "+cols+" FROM( \n"); 
				builder.append("SELECT _TAB_I.* ,ROW_NUMBER() OVER(") 
				.append(order) 
				.append(") AS PAGE_ROW_NUMBER_ \n");
				builder.append("FROM( \n"); 
				builder.append(sql); 
				builder.append(") AS _TAB_I \n"); 
				builder.append(") AS _TAB_O WHERE PAGE_ROW_NUMBER_ BETWEEN "+(first+1)+" AND "+(last+1));
			} 
			 
		} 
		 
		return builder.toString(); 
		 
	}

	/**
	 * 根据DataSet创建批量INSERT RunPrepare
	 * 2000版本单独处理  insert into tab(nm) select 1 union all select 2
	 * @param template JdbcTemplate
	 * @param run run
	 * @param dest 表 如果不指定则根据set解析
	 * @param set 集合
	 * @param keys 需插入的列
	 */
	@Override
	public void createInserts(JdbcTemplate template, Run run, String dest, DataSet set, List<String> keys){
		if(!"2000".equals(getDbVersion())){
			super.createInserts(template, run, dest, set, keys);
			return;
		}
		StringBuilder builder = run.getBuilder();
		if(null == builder){
			builder = new StringBuilder();
			run.setBuilder(builder);
		}
		builder.append("INSERT INTO ").append(parseTable(dest));
		builder.append("(");

		int keySize = keys.size();
		for(int i=0; i<keySize; i++){
			String key = keys.get(i);
			SQLUtil.delimiter(builder, key, getDelimiterFr(), getDelimiterTo());
			if(i<keySize-1){
				builder.append(",");
			}
		}
		builder.append(")");
		int dataSize = set.size();
		for(int i=0; i<dataSize; i++){
			DataRow row = set.getRow(i);
			if(null == row){
				continue;
			}
			if(row.hasPrimaryKeys() && BasicUtil.isEmpty(row.getPrimaryValue())){
				List<String> pks = row.getPrimaryKeys();
				if(null == pks){
					pks = new ArrayList<>();
				}
				if(pks.size() ==0){
					pks.add(ConfigTable.DEFAULT_PRIMARY_KEY);
				}
				createPrimaryValue(row, type(),dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""), pks, null);
			}
			builder.append("\n SELECT ");
			insertValue(template, run, row, true, false,false, keys);
			if(i<dataSize-1){
				//多行数据之间的分隔符
				builder.append("\n UNION ALL ");
			}
		}
	}

	/**
	 * 根据Collection创建批量INSERT RunPrepare
	 * 2000版本单独处理  insert into tab(nm) select 1 union all select 2
	 * @param template JdbcTemplate
	 * @param run run
	 * @param dest 表 如果不指定则根据set解析
	 * @param list 集合
	 * @param keys 需插入的列
	 */
	@Override
	public void createInserts(JdbcTemplate template, Run run, String dest, Collection list,  List<String> keys){
		if(!"2000".equals(getDbVersion())){
			super.createInserts(template, run, dest, list, keys);
			return;
		}
		StringBuilder builder = run.getBuilder();
		if(null == builder){
			builder = new StringBuilder();
			run.setBuilder(builder);
		}
		if(list instanceof DataSet){
			DataSet set = (DataSet) list;
			createInserts(template, run, dest, set, keys);
			return;
		}
		builder.append("INSERT INTO ").append(parseTable(dest));
		builder.append("(");

		int keySize = keys.size();
		for(int i=0; i<keySize; i++){
			String key = keys.get(i);
			SQLUtil.delimiter(builder, key, getDelimiterFr(), getDelimiterTo());
			if(i<keySize-1){
				builder.append(",");
			}
		}
		builder.append(")\n ");
		int dataSize = list.size();
		int idx = 0;
		for(Object obj:list){
			builder.append("\n SELECT ");
			if(obj instanceof DataRow) {
				DataRow row = (DataRow)obj;
				if (row.hasPrimaryKeys() && BasicUtil.isEmpty(row.getPrimaryValue())) {
					createPrimaryValue(row, type(), dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""), row.getPrimaryKeys(), null);
				}
				insertValue(template, run, row, true, false,false, keys);
			}else{
				if(EntityAdapterProxy.hasAdapter()){
					EntityAdapterProxy.createPrimaryValue(obj);
				}else{
					createPrimaryValue(obj, type(),dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""), null, null);
				}
				insertValue(template, run, obj, true, false, false, keys);
			}
			if(idx<dataSize-1){
				//多行数据之间的分隔符
				builder.append("\n UNION ALL ");

			}
			idx ++;
		}
	}

	@Override 
	public String parseExists(Run run){
		String sql = "IF EXISTS(\n" + run.getBuilder().toString() +"\n) SELECT 1 AS IS_EXISTS ELSE SELECT 0 AS IS_EXISTS"; 
		sql = sql.replaceAll("WHERE\\s*1=1\\s*AND", "WHERE"); 
		return sql; 
	}
	@Override
	public String concat(String ... args){
		return concatAdd(args);
	}



	/* *****************************************************************************************************************
	 *
	 * 													metadata
	 *
	 * =================================================================================================================
	 * table			: 表
	 * master table		: 主表
	 * partition table	: 分区表
	 * column			: 列
	 * tag				: 标签
	 * index			: 索引
	 * constraint		: 约束
	 *
	 ******************************************************************************************************************/

	@Override
	public void checkSchema(Connection con, Table table){
		try {
			if (null == table.getCatalog()) {
				table.setCatalog(con.getCatalog());
			}
			if (null == table.getSchema()) {
				//table.setSchema(con.getSchema());
				table.setSchema("dbo");
			}
		}catch (Exception e){
		}
		table.setCheckSchemaTime(new Date());
	}

	/* *****************************************************************************************************************
	 * 													table
	 * -----------------------------------------------------------------------------------------------------------------
	 * public List<String> buildQueryTableRunSQL(String catalog, String schema, String pattern, String types)
	 * public List<String> buildQueryTableCommentRunSQL(String catalog, String schema, String pattern, String types)
	 * public LinkedHashMap<String, Table> tables(int index, boolean create, String catalog, String schema, LinkedHashMap<String, Table> tables, DataSet set) throws Exception
	 * public LinkedHashMap<String, Table> tables(boolean create, LinkedHashMap<String, Table> tables, DatabaseMetaData dbmd, String catalog, String schema, String pattern, String ... types) throws Exception
	 * public LinkedHashMap<String, Table> comments(int index, boolean create, String catalog, String schema, LinkedHashMap<String, Table> tables, DataSet set) throws Exception
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
	public List<String> buildQueryTableRunSQL(String catalog, String schema, String pattern, String types) throws Exception{
		return super.buildQueryTableRunSQL(catalog, schema, pattern, types);
	}

	/**
	 * 查询表备注
	 * @param catalog catalog
	 * @param schema schema
	 * @param pattern pattern
	 * @param types types
	 * @return String
	 */
	@Override
	public List<String> buildQueryTableCommentRunSQL(String catalog, String schema, String pattern, String types) throws Exception{
		List<String> sqls = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT TBS.NAME AS TABLE_NAME ,DS.VALUE AS TABLE_COMMENT\n");
		builder.append("FROM SYS.EXTENDED_PROPERTIES DS\n");
		builder.append("LEFT JOIN SYS.SYSOBJECTS TBS ON DS.MAJOR_ID=TBS.ID \n");
		builder.append("WHERE  DS.MINOR_ID=0 \n");
		if(BasicUtil.isNotEmpty(pattern)){
			builder.append("TBS.NAME = '").append(pattern).append("'");
		}
		sqls.add(builder.toString());
		return sqls;
	}
	@Override
	public LinkedHashMap<String, Table> tables(int index, boolean create, String catalog, String schema, LinkedHashMap<String, Table> tables, DataSet set) throws Exception{
		return super.tables(index, create, catalog, schema, tables, set);
	}
	@Override
	public LinkedHashMap<String, Table> tables(boolean create, LinkedHashMap<String, Table> tables, DatabaseMetaData dbmd, String catalog, String schema, String pattern, String ... types) throws Exception{
		return super.tables(create, tables, dbmd, catalog, schema, pattern, types);
	}

	/* *****************************************************************************************************************
	 * 													master table
	 * -----------------------------------------------------------------------------------------------------------------
	 * public List<String> buildQueryMasterTableRunSQL(String catalog, String schema, String pattern, String types);
	 * public LinkedHashMap<String, MasterTable> mtables(int index, boolean create, String catalog, String schema, LinkedHashMap<String, MasterTable> tables, DataSet set) throws Exception;
	 * public LinkedHashMap<String, MasterTable> mtables(boolean create, LinkedHashMap<String, MasterTable> tables, DatabaseMetaData dbmd, String catalog, String schema, String pattern, String ... types) throws Exception;
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
	public List<String> buildQueryMasterTableRunSQL(String catalog, String schema, String pattern, String types) throws Exception{
		return super.buildQueryMasterTableRunSQL(catalog, schema, pattern, types);
	}

	/**
	 * 从jdbc结果中提取表结构
	 * ResultSet set = con.getMetaData().getTables()
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param catalog catalog
	 * @param schema schema
	 * @param dbmd DatabaseMetaData
	 * @param pattern pattern
	 * @param types types
	 * @return List
	 */
	@Override
	public LinkedHashMap<String, MasterTable> mtables(boolean create, LinkedHashMap<String, MasterTable> tables, DatabaseMetaData dbmd, String catalog, String schema, String pattern, String ... types) throws Exception{
		return super.mtables(create, tables, dbmd, catalog, schema, pattern, types);
	}


	/**
	 * 从上一步生成的SQL查询结果中 提取表结构
	 * @param index 第几条SQL
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param catalog catalog
	 * @param schema schema
	 * @param tables 上一步查询结果
	 * @param set set
	 * @return tables
	 * @throws Exception 异常
	 */
	@Override
	public LinkedHashMap<String, MasterTable> mtables(int index, boolean create, String catalog, String schema, LinkedHashMap<String, MasterTable> tables, DataSet set) throws Exception{
		return super.mtables(index, create, catalog, schema, tables, set);
	}


	/* *****************************************************************************************************************
	 * 													partition table
	 * -----------------------------------------------------------------------------------------------------------------
	 * public List<String> buildQueryPartitionTableRunSQL(String catalog, String schema, String pattern, String types);
	 * public List<String> buildQueryPartitionTableRunSQL(MasterTable master, Map<String,Object> tags, String name);
	 * public List<String> buildQueryPartitionTableRunSQL(MasterTable master, Map<String,Object> tags);
	 * public LinkedHashMap<String, PartitionTable> ptables(int total, int index, boolean create, MasterTable master, String catalog, String schema, LinkedHashMap<String, PartitionTable> tables, DataSet set) throws Exception;
	 * public LinkedHashMap<String, PartitionTable> ptables(boolean create, LinkedHashMap<String, PartitionTable> tables, DatabaseMetaData dbmd, String catalog, String schema, MasterTable master) throws Exception;
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
	public List<String> buildQueryPartitionTableRunSQL(String catalog, String schema, String pattern, String types) throws Exception{
		return super.buildQueryPartitionTableRunSQL(catalog, schema, pattern, types);
	}
	@Override
	public List<String> buildQueryPartitionTableRunSQL(MasterTable master, Map<String,Object> tags, String name) throws Exception{
		return super.buildQueryPartitionTableRunSQL(master, tags, name);
	}
	@Override
	public List<String> buildQueryPartitionTableRunSQL(MasterTable master, Map<String,Object> tags) throws Exception{
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
	 * @param set set
	 * @return tables
	 * @throws Exception 异常
	 */
	@Override
	public LinkedHashMap<String, PartitionTable> ptables(int total, int index, boolean create, MasterTable master, String catalog, String schema, LinkedHashMap<String, PartitionTable> tables, DataSet set) throws Exception{
		return super.ptables(total, index, create, master, catalog, schema, tables, set);
	}

	/**
	 * 根据JDBC
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param master 主表
	 * @param catalog catalog
	 * @param schema schema
	 * @param tables 上一步查询结果
	 * @param dbmd DatabaseMetaData
	 * @return tables
	 * @throws Exception 异常
	 */
	@Override
	public LinkedHashMap<String, PartitionTable> ptables(boolean create, LinkedHashMap<String, PartitionTable> tables, DatabaseMetaData dbmd, String catalog, String schema, MasterTable master) throws Exception{
		return super.ptables(create, tables, dbmd, catalog, schema, master);
	}


	/* *****************************************************************************************************************
	 * 													column
	 * -----------------------------------------------------------------------------------------------------------------
	 * public List<String> buildQueryColumnRunSQL(Table table, boolean metadata);
	 * public LinkedHashMap<String, Column> columns(int index, boolean create, Table table, LinkedHashMap<String, Column> columns, DataSet set) throws Exception;
	 * public LinkedHashMap<String, Column> columns(boolean create, LinkedHashMap<String, Column> columns, Table table, SqlRowSet set) throws Exception;
	 * public LinkedHashMap<String, Column> columns(boolean create, LinkedHashMap<String, Column> columns, DatabaseMetaData dbmd, Table table, String pattern) throws Exception;
	 ******************************************************************************************************************/

	/**
	 * 查询表上的列
	 * @param table 表
	 * @return sql
	 */
	@Override
	public List<String> buildQueryColumnRunSQL(Table table, boolean metadata) throws Exception{
		List<String> sqls = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT A.NAME AS TABLE_NAME,C.NAME AS COLUMN_NAME, B.VALUE COLUMN_COMMENT , C.PRECISION, C.SCALE, C.COLLATION_NAME, C.IS_NULLABLE, C.MAX_LENGTH  \n");
		builder.append("FROM SYSOBJECTS A \n");
		builder.append("LEFT JOIN  SYS.COLUMNS C ON A.ID = C.OBJECT_ID\n");
		builder.append("LEFT JOIN SYS.EXTENDED_PROPERTIES B ON B.MAJOR_ID = A.ID AND B.MINOR_ID = C.COLUMN_ID\n");
		if(null != table){
			builder.append("WHERE A.NAME='").append(table.getName()).append("'");
		}
		sqls.add(builder.toString());
		return sqls;
	}

	/**
	 *
	 * @param index 第几条SQL 对照 buildQueryColumnRunSQL返回顺序
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param table 表
	 * @param columns 上一步查询结果
	 * @param set set
	 * @return columns columns
	 * @throws Exception 异常
	 */
	@Override
	public LinkedHashMap<String, Column> columns(int index, boolean create, Table table, LinkedHashMap<String, Column> columns, DataSet set) throws Exception{
		return super.columns(index, create, table, columns, set);
	}
	@Override
	public LinkedHashMap<String, Column> columns(boolean create, LinkedHashMap<String, Column> columns, Table table, SqlRowSet set) throws Exception{
		return super.columns(create, columns, table, set);
	}
	@Override
	public LinkedHashMap<String, Column> columns(boolean create, LinkedHashMap<String, Column> columns, DatabaseMetaData dbmd, Table table, String pattern) throws Exception{
		return super.columns(create, columns, dbmd, table, pattern);
	}


	/* *****************************************************************************************************************
	 * 													tag
	 * -----------------------------------------------------------------------------------------------------------------
	 * public List<String> buildQueryTagRunSQL(Table table, boolean metadata);
	 * public LinkedHashMap<String, Tag> tags(int index, boolean create, Table table, LinkedHashMap<String, Tag> tags, DataSet set) throws Exception;
	 * public LinkedHashMap<String, Tag> tags(boolean create, Table table, LinkedHashMap<String, Tag> tags, SqlRowSet set) throws Exception;
	 * public LinkedHashMap<String, Tag> tags(boolean create, LinkedHashMap<String, Tag> tags, DatabaseMetaData dbmd, Table table, String pattern) throws Exception;
	 ******************************************************************************************************************/
	/**
	 *
	 * @param table 表
	 * @param metadata 是否根据metadata | 查询系统表
	 * @return sqls
	 */
	@Override
	public List<String> buildQueryTagRunSQL(Table table, boolean metadata) throws Exception{
		return super.buildQueryTagRunSQL(table, metadata);
	}

	/**
	 *  根据查询结果集构造Tag
	 * @param index 第几条查询SQL 对照 buildQueryTagRunSQL返回顺序
	 * @param create 上一步没有查到的,这一步是否需要新创建
	 * @param table 表
	 * @param tags 上一步查询结果
	 * @param set set
	 * @return tags tags
	 * @throws Exception 异常
	 */
	@Override
	public LinkedHashMap<String, Tag> tags(int index, boolean create, Table table, LinkedHashMap<String, Tag> tags, DataSet set) throws Exception{
		return super.tags(index, create, table, tags, set);
	}
	@Override
	public LinkedHashMap<String, Tag> tags(boolean create, Table table, LinkedHashMap<String, Tag> tags, SqlRowSet set) throws Exception{
		return super.tags(create, table, tags, set);
	}
	@Override
	public LinkedHashMap<String, Tag> tags(boolean create, LinkedHashMap<String, Tag> tags, DatabaseMetaData dbmd, Table table, String pattern) throws Exception{
		return super.tags(create, tags, dbmd, table, pattern);
	}

	/* *****************************************************************************************************************
	 * 													index
	 * -----------------------------------------------------------------------------------------------------------------
	 * public List<String> buildQueryIndexRunSQL(Table table, boolean metadata);
	 * public LinkedHashMap<String, Index> indexs(int index, boolean create, Table table, LinkedHashMap<String, Index> indexs, DataSet set) throws Exception;
	 * public LinkedHashMap<String, Index> indexs(boolean create, Table table, LinkedHashMap<String, Index> indexs, SqlRowSet set) throws Exception;
	 * public LinkedHashMap<String, Index> indexs(boolean create, LinkedHashMap<String, Index> indexs, DatabaseMetaData dbmd, Table table, boolean unique, boolean approximate) throws Exception;
	 ******************************************************************************************************************/
	/**
	 * 查询表上的列
	 * @param table 表
	 * @param name name
	 * @return sql
	 */
	@Override
	public List<String> buildQueryIndexRunSQL(Table table, String name){
		return super.buildQueryIndexRunSQL(table, name);
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
	public LinkedHashMap<String, Index> indexs(int index, boolean create, Table table, LinkedHashMap<String, Index> indexs, DataSet set) throws Exception{
		return super.indexs(index, create, table, indexs, set);
	}
	@Override
	public LinkedHashMap<String, Index> indexs(boolean create, Table table, LinkedHashMap<String, Index> indexs, SqlRowSet set) throws Exception{
		return super.indexs(create, table, indexs, set);
	}
	@Override
	public LinkedHashMap<String, Index> indexs(boolean create, LinkedHashMap<String, Index> indexs, DatabaseMetaData dbmd, Table table, boolean unique, boolean approximate) throws Exception{
		return super.indexs(create, indexs, dbmd, table, unique, approximate);
	}


	/* *****************************************************************************************************************
	 * 													constraint
	 * -----------------------------------------------------------------------------------------------------------------
	 * public List<String> buildQueryConstraintRunSQL(Table table, boolean metadata);
	 * public LinkedHashMap<String, Constraint> constraints(int constraint, boolean create,  Table table, LinkedHashMap<String, Constraint> constraints, DataSet set) throws Exception;
	 * public LinkedHashMap<String, Constraint> constraints(boolean create, Table table, LinkedHashMap<String, Constraint> constraints, SqlRowSet set) throws Exception;
	 * public LinkedHashMap<String, Constraint> constraints(boolean create, Table table, LinkedHashMap<String, Constraint> constraints, ResultSet set) throws Exception;
	 ******************************************************************************************************************/
	/**
	 * 查询表上的约束
	 * @param table 表
	 * @param metadata 是否根据metadata | 查询系统表
	 * @return sqls
	 */
	@Override
	public List<String> buildQueryConstraintRunSQL(Table table, boolean metadata) throws Exception{
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
	public LinkedHashMap<String, Constraint> constraints(int index , boolean create, Table table, LinkedHashMap<String, Constraint> constraints, DataSet set) throws Exception{

		return super.constraints(index, create, table, constraints, set);
	}
	@Override
	public LinkedHashMap<String, Constraint> constraints(boolean create, Table table, LinkedHashMap<String, Constraint> constraints, SqlRowSet set) throws Exception{
		return super.constraints(create, table, constraints, set);
	}
	@Override
	public LinkedHashMap<String, Constraint> constraints(boolean create, Table table, LinkedHashMap<String, Constraint> constraints, ResultSet set) throws Exception{
		return super.constraints(create, table, constraints, set);
	}






	/* *****************************************************************************************************************
	 *
	 * 													DDL
	 *
	 * =================================================================================================================
	 * table			: 表
	 * master table		: 主表
	 * partition table	: 分区表
	 * column			: 列
	 * tag				: 标签
	 * index			: 索引
	 * constraint		: 约束
	 *
	 ******************************************************************************************************************/

	/* *****************************************************************************************************************
	 * 													table
	 * -----------------------------------------------------------------------------------------------------------------
	 * public List<String> buildCreateRunSQL(Table table);
	 * public String buildCreateCommentRunSQL(Table table);
	 * public List<String> buildAlterRunSQL(Table table);
	 * public String buildRenameRunSQL(Table table);
	 * public String buildChangeCommentRunSQL(Table table);
	 * public String buildDropRunSQL(Table table);
	 * public StringBuilder checkTableExists(StringBuilder builder, boolean exists)
	 * public StringBuilder primary(StringBuilder builder, Table table)
	 * public StringBuilder comment(StringBuilder builder, Table table)
	 * public StringBuilder name(StringBuilder builder, Table table)
	 ******************************************************************************************************************/


	@Override
	public List<String> buildCreateRunSQL(Table table) throws Exception{
		List<String> sqls = super.buildCreateRunSQL(table);
		//如果有备注 单独生成 super中已包含
		/*String sql = buildChangeCommentRunSQL(table);
		if(BasicUtil.isNotEmpty(sql)) {
			sqls.add(sql);
		}*/
		//列备注 super中已包含
		/*for(Column col:table.getColumns().values()){
			String cmt = col.getComment();
			if(BasicUtil.isNotEmpty(cmt)){
				sqls.add(buildChangeCommentRunSQL(col));
			}
		}*/
		return sqls;
	}

	@Override
	public List<String> buildAlterRunSQL(Table table) throws Exception{
		return super.buildAlterRunSQL(table);
	}

	/**
	 * 修改表名
	 * EXEC SP_RENAME 'A', 'B'
	 * @param table 表
	 * @return String
	 */
	@Override
	public String buildRenameRunSQL(Table table) throws Exception {
		StringBuilder builder = new StringBuilder();
		builder.append("EXEC SP_RENAME '").append(table.getName()).append("', '").append(table.getUpdate().getName()).append("'");
		return builder.toString();
	}

	/**
	 * 添加表备注(表创建完成后调用,创建过程能添加备注的不需要实现)
	 * @param table 表
	 * @return sql
	 * @throws Exception 异常
	 */
	public String buildCreateCommentRunSQL(Table table) throws Exception {
		String comment = table.getComment();
		if(BasicUtil.isEmpty(comment)){
			return null;
		}
		StringBuilder builder = new StringBuilder();
		if("2000".equals(getDbVersion())){
			builder.append("EXEC sp_addextendedproperty ");
			builder.append("'MS_Description',");
			builder.append("N'").append(comment).append("',");
			builder.append("'USER',");
			builder.append("'").append(table.getSchema()).append("',");
			builder.append("'TABLE',");
			builder.append("'").append(table.getName()).append("'");
		}else {
			builder.append("EXEC sys.sp_addextendedproperty @name=N'MS_Description'");
			builder.append(",@value=N'").append(comment).append("'");
			builder.append(",@level0type=N'SCHEMA'");
			builder.append(",@level0name=N'").append(table.getSchema()).append("'");
			builder.append(",@level1type=N'TABLE'");
			builder.append(",@level1name=N'").append(table.getName()).append("'");
		}
		return builder.toString();
	}

	@Override
	public String buildChangeCommentRunSQL(Table table) throws Exception{
		String comment = table.getComment();
		if(BasicUtil.isEmpty(comment)){
			return null;
		}
		StringBuilder builder = new StringBuilder();
		if("2000".equals(getDbVersion())){
			builder.append("EXEC sp_updateextendedproperty ");
			builder.append("'MS_Description',");
			builder.append("N'").append(comment).append("',");
			builder.append("'USER',");
			builder.append("'").append(table.getSchema()).append("',");
			builder.append("'TABLE',");
			builder.append("'").append(table.getName()).append("'");
		}else {
			builder.append("EXEC sys.sp_updateextendedproperty @name=N'MS_Description'");
			builder.append(",@value=N'").append(comment).append("'");
			builder.append(",@level0type=N'SCHEMA'");
			builder.append(",@level0name=N'").append(table.getSchema()).append("'");
			builder.append(",@level1type=N'TABLE'");
			builder.append(",@level1name=N'").append(table.getName()).append("'");
		}
		return builder.toString();
	}
	/**
	 * 删除表
	 * @param table 表
	 * @return String
	 */
	@Override
	public String buildDropRunSQL(Table table) throws Exception{
		return super.buildDropRunSQL(table);
	}


	@Override
	public StringBuilder checkTableExists(StringBuilder builder, boolean exists){
		if(exists) {
			// 仅drop时支持
			return super.checkTableExists(builder, exists);
		}else {
			// create时不支持
			return builder;
		}
	}


	/**
	 * 主键
	 * CONSTRAINT [PK_BS_DEV] PRIMARY KEY (ID  ASC)
	 * @param builder builder
	 * @param table 表
	 * @return builder
	 */
	@Override
	public StringBuilder primary(StringBuilder builder, Table table){
		List<Column> pks = table.primarys();
		if(pks.size()>0){
			builder.append(",CONSTRAINT ").append("PK_").append(table.getName()).append(" PRIMARY KEY (");
			int idx = 0;
			for(Column pk:pks){
				if(idx > 0){
					builder.append(",");
				}
				SQLUtil.delimiter(builder, pk.getName(), getDelimiterFr(), getDelimiterTo());
				String order = pk.getOrder();
				if(BasicUtil.isNotEmpty(order)){
					builder.append(" ").append(order);
				}
				idx ++;
			}
			builder.append(")");
		}
		return builder;
	}

	/**
	 * 备注
	 * 需要单独生成添加注释的SQL
	 * @param builder builder
	 * @param table 表
	 * @return builder
	 */
	@Override
	public StringBuilder comment(StringBuilder builder, Table table){
		return builder;
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
	 * 													master table
	 * -----------------------------------------------------------------------------------------------------------------
	 * public List<String> buildCreateRunSQL(MasterTable table);
	 * public String buildCreateCommentRunSQL(MasterTable table)
	 * public List<String> buildAlterRunSQL(MasterTable table);
	 * public String buildDropRunSQL(MasterTable table);
	 * public String buildRenameRunSQL(MasterTable table);
	 * public String buildChangeCommentRunSQL(MasterTable table);
	 ******************************************************************************************************************/
	/**
	 * 创建主表
	 * @param table 表
	 * @return String
	 */
	@Override
	public List<String>  buildCreateRunSQL(MasterTable table) throws Exception{
		return super.buildCreateRunSQL(table);
	}
	@Override
	public List<String> buildAlterRunSQL(MasterTable table) throws Exception{
		return super.buildAlterRunSQL(table);
	}
	@Override
	public String buildDropRunSQL(MasterTable table) throws Exception{
		return super.buildDropRunSQL(table);
	}
	@Override
	public String buildRenameRunSQL(MasterTable table) throws Exception{
		return super.buildRenameRunSQL(table);
	}
	@Override
	public String buildChangeCommentRunSQL(MasterTable table) throws Exception{
		return super.buildChangeCommentRunSQL(table);
	}


	/* *****************************************************************************************************************
	 * 													partition table
	 * -----------------------------------------------------------------------------------------------------------------
	 * public String buildCreateRunSQL(PartitionTable table);
	 * public List<String> buildAlterRunSQL(PartitionTable table);
	 * public String buildDropRunSQL(PartitionTable table);
	 * public String buildRenameRunSQL(PartitionTable table);
	 * public String buildChangeCommentRunSQL(PartitionTable table);
	 ******************************************************************************************************************/
	/**
	 * 创建分区表
	 * @param table 表
	 * @return String
	 */
	@Override
	public List<String>  buildCreateRunSQL(PartitionTable table) throws Exception{
		return super.buildCreateRunSQL(table);
	}
	@Override
	public List<String> buildAlterRunSQL(PartitionTable table) throws Exception{
		return super.buildAlterRunSQL(table);
	}
	@Override
	public String buildDropRunSQL(PartitionTable table) throws Exception{
		return super.buildDropRunSQL(table);
	}
	@Override
	public String buildRenameRunSQL(PartitionTable table) throws Exception{
		return super.buildRenameRunSQL(table);
	}
	@Override
	public String buildChangeCommentRunSQL(PartitionTable table) throws Exception{
		return super.buildChangeCommentRunSQL(table);
	}

	/* *****************************************************************************************************************
	 * 													column
	 * -----------------------------------------------------------------------------------------------------------------
	 * public String alterColumnKeyword()
	 * public List<String> buildAddRunSQL(Column column)
	 * public List<String> buildAlterRunSQL(Column column)
	 * public String buildDropRunSQL(Column column)
	 * public String buildRenameRunSQL(Column column)
	 * public List<String> buildChangeTypeRunSQL(Column column)
	 * public String buildChangeDefaultRunSQL(Column column)
	 * public String buildChangeNullableRunSQL(Column column)
	 * public String buildChangeCommentRunSQL(Column column)
	 * public String buildCreateCommentRunSQL(Column column)
	 * public StringBuilder define(StringBuilder builder, Column column)
	 * public StringBuilder type(StringBuilder builder, Column column)
	 * public boolean isIgnorePrecision(Column column);
	 * public boolean isIgnoreScale(Column column);
	 * public Boolean checkIgnorePrecision(String datatype);
	 * public Boolean checkIgnoreScale(String datatype);
	 * public StringBuilder nullable(StringBuilder builder, Column column)
	 * public StringBuilder charset(StringBuilder builder, Column column)
	 * public StringBuilder defaultValue(StringBuilder builder, Column column)
	 * public StringBuilder increment(StringBuilder builder, Column column)
	 * public StringBuilder onupdate(StringBuilder builder, Column column)
	 * public StringBuilder position(StringBuilder builder, Column column)
	 * public StringBuilder comment(StringBuilder builder, Column column)
	 * public StringBuilder checkColumnExists(StringBuilder builder, boolean exists)
	 ******************************************************************************************************************/
	@Override
	public String alterColumnKeyword(){
		return super.alterColumnKeyword();
	}

	/**
	 * 添加新列
	 * ALTER TABLE TAB_A ADD USER_NAME VARCHAR(10)
	 * @param column 列
	 * @return String
	 */
	@Override
	public List<String> buildAddRunSQL(Column column) throws Exception{
		List<String> sqls = new ArrayList<>();
		column.setCreater(this);
		StringBuilder builder = new StringBuilder();
		Table table = column.getTable();
		builder.append("ALTER TABLE ");
		name(builder, table);
		builder.append(" ADD ");
		SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo()).append(" ");
		define(builder, column);
		sqls.add(builder.toString());
		sqls.add(buildCreateCommentRunSQL(column));
		return sqls;
	}



	/**
	 * 修改列 ALTER TABLE  HR_USER CHANGE UPT_TIME UPT_TIME datetime   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP  comment '修改时间' AFTER ID;
	 * @param column 列
	 * @return sqls
	 */
	@Override
	public List<String> buildAlterRunSQL(Column column) throws Exception{
		return super.buildAlterRunSQL(column);
	}


	/**
	 * 删除列
	 * ALTER TABLE HR_USER DROP COLUMN NAME;
	 * @param column 列
	 * @return String
	 */
	@Override
	public String buildDropRunSQL(Column column) throws Exception{
		return super.buildDropRunSQL(column);
	}


	/**
	 * 修改列名
	 * EXEC sp_rename '表名.列名', '新列名', 'COLUMN'
	 * @param column 列
	 * @return sql
	 */
	@Override
	public String buildRenameRunSQL(Column column) throws Exception{
		StringBuilder builder = new StringBuilder();
		builder.append("EXEC SP_RENAME '").append(column.getTableName()).append(".").append(column.getName()).append("' , '").append(column.getUpdate().getName()).append("','COLUMN' ");
		return builder.toString();
	}

	/**
	 * 修改数据类型
	 * ALTER TABLE T ALTER COLUMN C VARCHAR (2);
	 * @param column 列
	 * @return sql
	 */
	@Override
	public List<String> buildChangeTypeRunSQL(Column column) throws Exception{
		List<String> sqls = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		Column update = column.getUpdate();
		builder.append("ALTER TABLE ");
		name(builder, column.getTable());
		builder.append(" ALTER COLUMN ");
		SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo());
		builder.append(" ");
		type(builder, update);
		nullable(builder, update);
		sqls.add(builder.toString());
		return sqls;
	}
	/**
	 * 修改默认值
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param column 列
	 * @return String
	 */
	@Override
	public String buildChangeDefaultRunSQL(Column column) throws Exception{
		return super.buildChangeDefaultRunSQL(column);
	}

	/**
	 * 修改非空限制
	 * ALTER TABLE T ALTER COLUMN C VARCHAR (20) NOT NULL;
	 * @param column 列
	 * @return String
	 */
	@Override
	public String buildChangeNullableRunSQL(Column column) throws Exception{
		Column update = column.getUpdate();
		int nullable = update.isNullable();
		if(nullable == -1){
			return null;
		}
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		name(builder, column.getTable()).append(" ALTER COLUMN ");
		SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo()).append(" ");
		type(builder, update);
		if(nullable == 0){
			builder.append("NOT");
		}
		builder.append(" NULL");
		return builder.toString();
	}

	/**
	 * 添加表备注(表创建完成后调用,创建过程能添加备注的不需要实现)
	 * @param column 列
	 * @return sql
	 * @throws Exception 异常
	 */
	public String buildCreateCommentRunSQL(Column column) throws Exception {
		String comment = column.getComment();
		if(BasicUtil.isEmpty(comment)){
			return null;
		}
		StringBuilder builder = new StringBuilder();
		String schema = column.getSchema();
		if(BasicUtil.isEmpty(schema)){
			schema = column.getTable().getSchema();
		}
		if("2000".equals(getDbVersion())){
			builder.append("EXEC sp_addextendedproperty ");
			builder.append("'MS_Description',");
			builder.append("N'").append(comment).append("',");
			builder.append("'USER',");
			builder.append("'").append(schema).append("',");
			builder.append("'TABLE',");
			builder.append("'").append(column.getTableName()).append("',");
			builder.append("'COLUMN',");
			builder.append("'").append(column.getName()).append("'");
		}else {
			builder.append("EXEC sys.sp_addextendedproperty @name=N'MS_Description'");
			builder.append(",@value=N'").append(comment).append("'");
			builder.append(",@level0type=N'SCHEMA'");
			builder.append(",@level0name=N'").append(schema).append("'");
			builder.append(",@level1type=N'TABLE'");
			builder.append(",@level1name=N'").append(column.getTableName()).append("'");
			builder.append(",@level2type=N'COLUMN'");
			builder.append(",@level2name=N'").append(column.getName()).append("'");
		}
		return builder.toString();
	}
	/**
	 * 修改备注
	 *  -- 字段加注释
	 * EXEC sys.sp_addextendedproperty @name=N'MS_Description'
	 * , @value=N'注释内容'
	 * , @level0type=N'SCHEMA'
	 * ,@level0name=N'dbo'
	 * , @level1type=N'TABLE'
	 * ,@level1name=N'表名'
	 * , @level2type=N'COLUMN'
	 * ,@level2name=N'字段名'
	 *
	 * @param column 列
	 * @return String
	 */
	@Override
	public String buildChangeCommentRunSQL(Column column) throws Exception{
		String comment = null;
		if(null != column.getUpdate()){
			comment = column.getUpdate().getComment();
		}else {
			comment = column.getComment();
		}
		if(BasicUtil.isEmpty(comment)){
			return null;
		}
		StringBuilder builder = new StringBuilder();
		String schema = column.getSchema();
		if(BasicUtil.isEmpty(schema)){
			schema = column.getTable().getSchema();
		}
		if("2000".equals(getDbVersion())){
			builder.append("EXEC sp_updateextendedproperty ");
			builder.append("'MS_Description',");
			builder.append("N'").append(comment).append("',");
			builder.append("'USER',");
			builder.append("'").append(schema).append("',");
			builder.append("'TABLE',");
			builder.append("'").append(column.getTableName()).append("',");
			builder.append("'COLUMN',");
			builder.append("'").append(column.getName()).append("'");
		}else {
			builder.append("EXEC sys.sp_updateextendedproperty @name=N'MS_Description'");
			builder.append(",@value=N'").append(comment).append("'");
			builder.append(",@level0type=N'SCHEMA'");
			builder.append(",@level0name=N'").append(schema).append("'");
			builder.append(",@level1type=N'TABLE'");
			builder.append(",@level1name=N'").append(column.getTableName()).append("'");
			builder.append(",@level2type=N'COLUMN'");
			builder.append(",@level2name=N'").append(column.getName()).append("'");
		}
		return builder.toString();
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
			builder.append(" IDENTITY(").append(column.getIncrementSeed()).append(",").append(column.getIncrementStep()).append(")");
		}
		return builder;
	}



	/**
	 * 更新行事件
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder onupdate(StringBuilder builder, Column column){
		return super.onupdate(builder, column);
	}

	/**
	 * 位置
	 *
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder position(StringBuilder builder, Column column){
		return super.position(builder, column);
	}
	/**
	 * 备注
	 *
	 * @param builder builder
	 * @param column 列
	 * @return builder
	 */
	@Override
	public StringBuilder comment(StringBuilder builder, Column column){
		return super.comment(builder, column);
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
	 * public String buildAddRunSQL(Tag tag);
	 * public List<String> buildAlterRunSQL(Tag tag);
	 * public String buildDropRunSQL(Tag tag);
	 * public String buildRenameRunSQL(Tag tag);
	 * public String buildChangeDefaultRunSQL(Tag tag);
	 * public String buildChangeNullableRunSQL(Tag tag);
	 * public String buildChangeCommentRunSQL(Tag tag);
	 * public List<String> buildChangeTypeRunSQL(Tag tag);
	 * public StringBuilder checkTagExists(StringBuilder builder, boolean exists)
	 ******************************************************************************************************************/

	/**
	 * 添加标签
	 * ALTER TABLE  HR_USER ADD TAG UPT_TIME datetime CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP comment '修改时间' AFTER ID;
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public String buildAddRunSQL(Tag tag) throws Exception{
		return super.buildAddRunSQL(tag);
	}


	/**
	 * 修改标签 ALTER TABLE  HR_USER CHANGE UPT_TIME UPT_TIME datetime   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP  comment '修改时间' AFTER ID;
	 * @param tag 标签
	 * @return sql
	 */
	@Override
	public List<String> buildAlterRunSQL(Tag tag) throws Exception{
		return super.buildAlterRunSQL(tag);
	}


	/**
	 * 删除标签
	 * ALTER TABLE HR_USER DROP TAG NAME;
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public String buildDropRunSQL(Tag tag) throws Exception{
		return super.buildDropRunSQL(tag);
	}


	/**
	 * 修改标签名
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public String buildRenameRunSQL(Tag tag)  throws Exception{
		return super.buildRenameRunSQL(tag);
	}

	/**
	 * 修改默认值
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public String buildChangeDefaultRunSQL(Tag tag) throws Exception{
		return super.buildChangeDefaultRunSQL(tag);
	}

	/**
	 * 修改非空限制
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public String buildChangeNullableRunSQL(Tag tag) throws Exception{
		return super.buildChangeNullableRunSQL(tag);
	}
	/**
	 * 修改备注
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public String buildChangeCommentRunSQL(Tag tag) throws Exception{
		return super.buildChangeCommentRunSQL(tag);
	}

	/**
	 * 修改数据类型
	 *
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param tag 标签
	 * @return sql
	 */
	@Override
	public List<String> buildChangeTypeRunSQL(Tag tag) throws Exception{
		return super.buildChangeTypeRunSQL(tag);
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
	 * public String buildAddRunSQL(PrimaryKey primary) throws Exception
	 * public List<String> buildAlterRunSQL(PrimaryKey primary) throws Exception
	 * public String buildDropRunSQL(PrimaryKey primary) throws Exception
	 * public String buildRenameRunSQL(PrimaryKey primary) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 添加主键
	 * @param primary 主键
	 * @return String
	 */
	@Override
	public String buildAddRunSQL(PrimaryKey primary) throws Exception{
		StringBuilder builder = new StringBuilder();
		Map<String,Column> columns = primary.getColumns();
		if(columns.size()>0) {
			builder.append("ALTER TABLE ");
			name(builder, primary.getTable());
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
		return builder.toString();
	}
	/**
	 * 修改主键
	 * 有可能生成多条SQL
	 * @param primary 主键
	 * @return List
	 */
	@Override
	public List<String> buildAlterRunSQL(PrimaryKey primary) throws Exception{
		return super.buildAlterRunSQL(primary);
	}

	/**
	 * 删除主键
	 * @param primary 主键
	 * @return String
	 */
	@Override
	public String buildDropRunSQL(PrimaryKey primary) throws Exception{
		StringBuilder builder = new StringBuilder();
		builder.append("ALTER TABLE ");
		name(builder, primary.getTable());
		builder.append(" DROP CONSTRAINT ");
		SQLUtil.delimiter(builder, primary.getName(), getDelimiterFr(), getDelimiterTo());
		return builder.toString();
	}
	/**
	 * 修改主键名
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param primary 主键
	 * @return String
	 */
	@Override
	public String buildRenameRunSQL(PrimaryKey primary) throws Exception{
		return super.buildRenameRunSQL(primary);
	}


	/* *****************************************************************************************************************
	 * 													primary
	 * -----------------------------------------------------------------------------------------------------------------
	 * public List<String> buildQueryPrimaryRunSQL(Table table) throws Exception
	 * public PrimaryKey primary(int index, Table table, DataSet set) throws Exception
	 ******************************************************************************************************************/

	/**
	 * 查询表上的主键
	 * @param table 表
	 * @return sqls
	 */
	@Override
	public List<String> buildQueryPrimaryRunSQL(Table table) throws Exception{
		List<String> list = new ArrayList<>();
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT  *  FROM INFORMATION_SCHEMA.KEY_COLUMN_USAGE \nWHERE TABLE_NAME='").append(table.getName()).append("'");
		String catalog = table.getCatalog();
		if(BasicUtil.isNotEmpty(catalog)){
			builder.append("\nAND TABLE_CATALOG = '").append(catalog).append("'");
		}
		String schema = table.getSchema();
		if(BasicUtil.isNotEmpty(schema)){
			builder.append("\nAND TABLE_SCHEMA = '").append(schema).append("'");
		}
		builder.append("\nORDER BY ORDINAL_POSITION");
		list.add(builder.toString());
		return list;
	}
	/**
	 *  根据查询结果集构造PrimaryKey
	 * @param index 第几条查询SQL 对照 buildQueryIndexRunSQL 返回顺序
	 * @param table 表
	 * @param set sql查询结果
	 * @throws Exception 异常
	 */
	@Override
	public PrimaryKey primary(int index, Table table, DataSet set) throws Exception{
		PrimaryKey primary = table.getPrimaryKey();
		for(DataRow row:set){
			if(null == primary){
				primary = new PrimaryKey();
				primary.setName(row.getString("TABLE_NAME"));
				primary.setTable(table);
			}
			String col = row.getString("COLUMN_NAME");
			Column column = primary.getColumn(col);
			if(null == column){
				column = new Column(col);
			}
			column.setTable(table);
			column.setPosition(row.getInt("ORDINAL_POSITION",0));
			primary.addColumn(column);
		}
		return primary;
	}
	/* *****************************************************************************************************************
	 * 													index
	 * -----------------------------------------------------------------------------------------------------------------
	 * public String buildAddRunSQL(Index index) throws Exception
	 * public List<String> buildAlterRunSQL(Index index) throws Exception
	 * public String buildDropRunSQL(Index index) throws Exception
	 * public String buildRenameRunSQL(Index index) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 添加索引
	 * @param index 索引
	 * @return String
	 */
	@Override
	public String buildAddRunSQL(Index index) throws Exception{
		return super.buildAddRunSQL(index);
	}
	/**
	 * 修改索引
	 * 有可能生成多条SQL
	 * @param index 索引
	 * @return List
	 */
	@Override
	public List<String> buildAlterRunSQL(Index index) throws Exception{
		return super.buildAlterRunSQL(index);
	}

	/**
	 * 删除索引
	 * @param index 索引
	 * @return String
	 */
	@Override
	public String buildDropRunSQL(Index index) throws Exception{
		return super.buildDropRunSQL(index);
	}
	/**
	 * 修改索引名
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param index 索引
	 * @return String
	 */
	@Override
	public String buildRenameRunSQL(Index index) throws Exception{
		return super.buildRenameRunSQL(index);
	}
	/* *****************************************************************************************************************
	 * 													constraint
	 * -----------------------------------------------------------------------------------------------------------------
	 * public String buildAddRunSQL(Constraint constraint) throws Exception
	 * public List<String> buildAlterRunSQL(Constraint constraint) throws Exception
	 * public String buildDropRunSQL(Constraint constraint) throws Exception
	 * public String buildRenameRunSQL(Constraint constraint) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 添加约束
	 * @param constraint 约束
	 * @return String
	 */
	@Override
	public String buildAddRunSQL(Constraint constraint) throws Exception{
		return super.buildAddRunSQL(constraint);
	}
	/**
	 * 修改约束
	 * 有可能生成多条SQL
	 * @param constraint 约束
	 * @return List
	 */
	@Override
	public List<String> buildAlterRunSQL(Constraint constraint) throws Exception{
		return super.buildAlterRunSQL(constraint);
	}

	/**
	 * 删除约束
	 * @param constraint 约束
	 * @return String
	 */
	@Override
	public String buildDropRunSQL(Constraint constraint) throws Exception{
		return super.buildDropRunSQL(constraint);
	}
	/**
	 * 修改约束名
	 * 一般不直接调用,如果需要由buildAlterRunSQL内部统一调用
	 * @param constraint 约束
	 * @return String
	 */
	@Override
	public String buildRenameRunSQL(Constraint constraint) throws Exception{
		return super.buildRenameRunSQL(constraint);
	}


	/* *****************************************************************************************************************
	 *
	 * 													common
	 *------------------------------------------------------------------------------------------------------------------
	 * public boolean isBooleanColumn(Column column)
	 * public  boolean isNumberColumn(Column column)
	 * public boolean isCharColumn(Column column)
	 * public String buildInValue(SQL_BUILD_IN_VALUE value)
	 * public String type(String type)
	 * public String type2class(String type)
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
	public String value(SQL_BUILD_IN_VALUE value){
		if(value == SQL_BUILD_IN_VALUE.CURRENT_TIME){
			return "getdate()";
		}
		return null;
	}




}
