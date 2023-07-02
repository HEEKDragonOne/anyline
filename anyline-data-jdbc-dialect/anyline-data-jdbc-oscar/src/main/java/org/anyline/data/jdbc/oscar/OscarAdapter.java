package org.anyline.data.jdbc.oscar;

import org.anyline.data.adapter.JDBCAdapter;
import org.anyline.data.adapter.init.SQLAdapter;
import org.anyline.data.run.Run;
import org.anyline.data.run.SimpleRun;
import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.entity.OrderStore;
import org.anyline.entity.PageNavi;
import org.anyline.entity.generator.PrimaryGenerator;
import org.anyline.metadata.*;
import org.anyline.metadata.type.DatabaseType;
import org.anyline.proxy.EntityAdapterProxy;
import org.anyline.util.BasicUtil;
import org.anyline.util.BeanUtil;
import org.anyline.util.SQLUtil;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.support.KeyHolder;
import org.springframework.jdbc.support.rowset.SqlRowSet;
import org.springframework.stereotype.Repository;

import java.sql.DatabaseMetaData;
import java.sql.ResultSet;
import java.util.*;

@Repository("anyline.data.jdbc.adapter.oscar")
public class OscarAdapter extends SQLAdapter implements JDBCAdapter, InitializingBean {
	
	public static boolean IS_GET_SEQUENCE_VALUE_BEFORE_INSERT = false;
	public DatabaseType type(){
		return DatabaseType.oscar;
	} 
	public OscarAdapter(){
		delimiterFr = "";
		delimiterTo = "";

		for (OscarColumnTypeAlias alias : OscarColumnTypeAlias.values()) {
			types.put(alias.name(), alias.standard());
		}
	}
	@Value("${anyline.data.jdbc.delimiter.oscar:}")
	private String delimiter;

	@Override
	public void afterPropertiesSet()  {
		setDelimiter(delimiter);
	}


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
			int limit = navi.getLastRow() - navi.getFirstRow() + 1;
			if(limit < 0){
				limit = 0;
			}
			sql += " LIMIT " + navi.getFirstRow() + "," + limit;
		}
		sql = sql.replaceAll("WHERE\\s*1=1\\s*AND", "WHERE");
		return sql;
	}

	/* *****************************************************************************************************************
	 *
	 * 														common
	 *
	 *  *****************************************************************************************************************/
	public String concat(String ... args){
		return concatFun(args);
	}

	/* *****************************************************************************************************************
	 *
	 * 														DML
	 *
	 *  *****************************************************************************************************************/

	/**
	 * 查询序列cur 或 next value
	 * @param next  是否生成返回下一个序列 false:cur true:next
	 * @param names 序列名
	 * @return String
	 */
	public List<Run> buildQuerySequence(boolean next, String ... names){
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		String key = "CURRVAL";
		if(next){
			key = "NEXTVAL";
		}
		if(null != names && names.length>0) {
			builder.append("SELECT ");
			boolean first = true;
			for (String name : names) {
				if(!first){
					builder.append(",");
				}
				first = false;
				builder.append(name).append(".").append(key).append(" AS ").append(name);
			}
			builder.append(" FROM DUAL");
		}
		return runs;
	}

	@Override
	public String parseExists(Run run){
		String sql = "SELECT 1 AS IS_EXISTS FROM DUAL WHERE  EXISTS(" + run.getBuilder().toString() + ")";
		sql = sql.replaceAll("WHERE\\s*1=1\\s*AND", "WHERE");
		return sql;
	}
	protected void createPrimaryValue(JdbcTemplate template, Collection list, String seq){
		StringBuilder builder = new StringBuilder();
		builder.append("SELECT ").append(seq).append(" AS ID FROM(\n");
		int size = list.size();
		for(int i=0; i<size; i++){
			builder.append("SELECT NULL FROM DUAL\n");
			if(i<size-1){
				builder.append("UNION ALL\n");
			}
		}
		builder.append(") M");
		List<Map<String,Object>> ids = template.queryForList(builder.toString());
		int i=0;
		for(Object obj:list){
			Object value = ids.get(i++).get("ID");
			setPrimaryValue(obj, value);
		}
	}

	/**
	 * 批量插入
	 *
	 * 有序列时 只支持插入同一张表
	 * INSERT INTO CRM_USER(ID, NAME)
	 *  SELECT gloable_seq.nextval  AS ID  , M.* FROM (
	 * 		SELECT  'A1' AS NM FROM  DUAL
	 * 		UNION ALL SELECT    'A2' FROM DUAL
	 * 		UNION ALL SELECT    'A3' FROM DUAL
	 * ) M
	 * @param template JdbcTemplate
	 * @param run run
	 * @param dest dest
	 * @param keys keys
	 */
	@Override
	public void createInserts(JdbcTemplate template, Run run, String dest, DataSet set, List<String> keys){
		if(null == set || set.size() ==0){
			return;
		}
		StringBuilder builder = run.getBuilder();
		DataRow first = set.getRow(0);
		Map<String,String> seqs = new HashMap<>();
		for(String key:keys){
			Object value = first.getStringNvl(key);
			if(null != value && value instanceof String) {
				String str = (String)value;
				if (str.toUpperCase().contains(".NEXTVAL")) {
					if (str.startsWith("${") && str.endsWith("}")) {
						str = str.substring(2, str.length() - 1);
					}
					if(IS_GET_SEQUENCE_VALUE_BEFORE_INSERT) {
						createPrimaryValue(template, set, str);
					}else {
						seqs.put(key, str);
					}
				}
			}
		}

		List<String> pks = null;
		PrimaryGenerator generator = checkPrimaryGenerator(type(),dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""));
		if(null != generator){
			pks = first.getPrimaryKeys();
			BeanUtil.join(true, keys, pks);
		}

		builder.append("INSERT INTO ");
		SQLUtil.delimiter(builder, dest, getDelimiterFr(), getDelimiterTo()).append(" (");
		int keySize = keys.size();
		for(int i=0; i<keySize; i++){
			String key = keys.get(i);
			builder.append(key);
			if(i<keySize-1){
				builder.append(", ");
			}
		}
		builder.append(") \n");
		builder.append("SELECT ");
		for(int i=0; i<keySize; i++){
			String key = keys.get(i);
			String seq = seqs.get(key);
			if(null != seq){
				builder.append(seq);
			}else{
				builder.append("M.").append(key);
			}
			builder.append(" AS ").append(key);
			if(i<keySize-1){
				builder.append(", ");
			}
		}
		builder.append("\nFROM( ");
		keys.removeAll(seqs.keySet());
		int col = 0;
		for(DataRow row:set) {
			if(row.hasPrimaryKeys() && null != primaryGenerator){
				if(null != generator){
					generator.create(row, type(),dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""), pks,  null);
				}
				//createPrimaryValue(row, type(),dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""), row.getPrimaryKeys(),  null);
			}

			if(col > 0){
				builder.append("\n\tUNION ALL");
			}
			builder.append("\n\tSELECT ");
			insertValue(template, run, row, true, true,false, keys);
			builder.append(" FROM DUAL ");
			col ++;
		}
		builder.append(") M ");
	}
	@Override
	public void createInserts(JdbcTemplate template, Run run, String dest, Collection list, List<String> keys){
		if(null == list || list.isEmpty()){
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

		Object first = list.iterator().next();
		Map<String,String> seqs = new HashMap<>();
		for(String key:keys){
			Object value = BeanUtil.getFieldValue(first, key);
			if(null != value && value instanceof String) {
				String str = (String)value;
				if (str.toUpperCase().contains(".NEXTVAL")) {
					if (str.startsWith("${") && str.endsWith("}")) {
						str = str.substring(2, str.length() - 1);
					}
					if(IS_GET_SEQUENCE_VALUE_BEFORE_INSERT) {
						createPrimaryValue(template, list, str);
					}else {
						seqs.put(key, str);
					}
				}
			}
		}

		PrimaryGenerator generator = checkPrimaryGenerator(type(), dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""));
		List<String> pks = null;
		if(null != generator) {
			pks = EntityAdapterProxy.primaryKeys(first.getClass(), true);
			BeanUtil.join(true, keys, pks);
		}
		builder.append("INSERT INTO ");
		SQLUtil.delimiter(builder, dest, getDelimiterFr(), getDelimiterTo()).append(" (");
		int keySize = keys.size();
		for(int i=0; i<keySize; i++){
			String key = keys.get(i);
			builder.append(key);
			if(i<keySize-1){
				builder.append(", ");
			}
		}
		builder.append(") \n");
		builder.append("SELECT ");
		for(int i=0; i<keySize; i++){
			String key = keys.get(i);
			String seq = seqs.get(key);
			if(null != seq){
				builder.append(seq);
			}else{
				builder.append("M.").append(key);
			}
			builder.append(" AS ").append(key);
			if(i<keySize-1){
				builder.append(", ");
			}
		}
		builder.append("\nFROM( ");
		keys.removeAll(seqs.keySet());
		int col = 0;

		for(Object obj:list){
			/*if(obj instanceof DataRow) {
				DataRow row = (DataRow)obj;
				if (row.hasPrimaryKeys() && null != primaryGenerator && BasicUtil.isEmpty(row.getPrimaryValue())) {
					String pk = row.getPrimaryKey();
					if (null == pk) {
						pk = ConfigTable.DEFAULT_PRIMARY_KEY;
					}
					createPrimaryValue(row, type(), dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""), row.getPrimaryKeys(),  null);
				}
			}else{*/
				boolean create = false;
				if(EntityAdapterProxy.hasAdapter()){
					create = EntityAdapterProxy.createPrimaryValue(obj, keys);
				}
				if(!create && null != generator){
					generator.create(obj, type(),dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""), pks,  null);
					//createPrimaryValue(obj, type(),dest.replace(getDelimiterFr(), "").replace(getDelimiterTo(), ""), null,  null);
				}

			//}

			if(col > 0){
				builder.append("\n\tUNION ALL");
			}
			builder.append("\n\tSELECT ");
			insertValue(template, run, obj, true, true,false, keys);
			builder.append(" FROM DUAL ");
			col ++;
		}
		builder.append(") M ");
	}

	/**
	 * 执行 insert
	 * @param template JdbcTemplate
	 * @param random random
	 * @param data entity|DataRow|DataSet
	 * @param sql sql
	 * @param values 占位参数值
	 * @return int 影响行数
	 * @throws Exception 异常
	 */
	@Override
	public int insert(JdbcTemplate template, String random, Object data, String sql, List<Object> values, String[] pks) throws Exception{
		int cnt = 0;
		if(data instanceof Collection) {
			if (null == values || values.isEmpty()) {
				cnt = template.update(sql);
			} else {
				int size = values.size();
				Object[] params = new Object[size];
				for (int i = 0; i < size; i++) {
					params[i] = values.get(i);
				}
				cnt = template.update(sql, params);
			}
		}else{
			//单行的可以返回序列号
			String pk = getPrimayKey(data);
			if(null != pk){
				pks = new String[]{pk};
			}else{
				pks = null;
			}
			cnt = super.insert(template, random, data, sql, values, pks);
		}
		return cnt;
	}

	@Override
	public boolean identity(String random, Object data, KeyHolder keyholder){
		if(data instanceof Collection) {
			return false;
		}else{
			return super.identity(random, data, keyholder);
		}
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


	/* *****************************************************************************************************************
	 * 													table
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryTableRunSQL(String catalog, String schema, String pattern, String types)
	 * List<Run> buildQueryTableCommentRunSQL(String catalog, String schema, String pattern, String types)
	 * <T extends Table> LinkedHashMap<String, T> tables(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> tables, DataSet set) throws Exception
	 * <T extends Table> LinkedHashMap<String, T> tables(boolean create, LinkedHashMap<String, T> tables, DatabaseMetaData dbmd, String catalog, String schema, String pattern, String ... types) throws Exception
	 * <T extends Table> LinkedHashMap<String, T> comments(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> tables, DataSet set) throws Exception
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
//		builder.append("SELECT T.TABLE_NAME, T.OWNER, TC.COMMENTS \n");
//		builder.append("FROM SYS.ALL_ALL_TABLES T \n");
//		builder.append("LEFT JOIN SYS.ALL_TAB_COMMENTS TC  ON  TC.OWNER  = T.OWNER  AND TC.TABLE_NAME  = T.TABLE_NAME \n");
//		builder.append("WHERE T.IOT_NAME IS NULL  AND T.NESTED = 'NO'  AND T.SECONDARY = 'N' ");
//		if(BasicUtil.isNotEmpty(schema)){
//			builder.append("AND T.OWNER = '").append(schema).append("'");
//		}
//		if(BasicUtil.isNotEmpty(pattern)){
//			builder.append(" AND T.TABLE_NAME = '").append(pattern).append("'");
//		}
//
//		runs.add(run);
//		return runs;
		// jack 2023年5月2日 19点55分 由于之前查询表名的方式会意外失效，特进行调整,兼容table和view
		//增加types列用于后期扩展
		builder.append(" SELECT * FROM (" );
		builder.append(" SELECT A.TABLE_NAME, B.COMMENTS, 'TABLE' TABLE_TYPE FROM USER_TABLES A, USER_TAB_COMMENTS B WHERE A.TABLE_NAME = B.TABLE_NAME");
		builder.append(" UNION ALL ");
		builder.append(" SELECT A.VIEW_NAME,  B.COMMENTS, 'VIEW'  TABLE_TYPE FROM USER_VIEWS  A, USER_TAB_COMMENTS B WHERE A.VIEW_NAME = B.TABLE_NAME");
		builder.append(" ) T WHERE 1=1");

		if(BasicUtil.isNotEmpty(pattern)){
			builder.append(" AND TABLE_NAME LIKE '").append(pattern).append("'");
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
		}
		return runs;
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
	public List<Run> buildQueryTableCommentRunSQL(String catalog, String schema, String pattern, String types) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("SELECT * FROM USER_TAB_COMMENTS\n");
		if(BasicUtil.isNotEmpty(pattern)){
			builder.append("WHERE TABLE_NAME = '").append(pattern).append("'");
		}
		return runs;
	}
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
			table.setCatalog(catalog);
			table.setSchema(schema);
			table.setName(name);
			table.setComment(row.getString("COMMENTS"));
			tables.put(name.toUpperCase(), table);
		}
		return tables;
	}
	@Override
	public <T extends Table> LinkedHashMap<String, T> tables(boolean create, LinkedHashMap<String, T> tables, DatabaseMetaData dbmd, String catalog, String schema, String pattern, String ... types) throws Exception{
		return super.tables(create, tables, dbmd, catalog, schema, pattern, types);
	}

	/* *****************************************************************************************************************
	 * 													view
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryViewRunSQL(String catalog, String schema, String pattern, String types)
	 * <T extends View> LinkedHashMap<String, T> views(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> views, DataSet set) throws Exception
	 * <T extends View> LinkedHashMap<String, T> views(boolean create, LinkedHashMap<String, T> views, DatabaseMetaData dbmd, String catalog, String schema, String pattern, String ... types) throws Exception
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
		builder.append("SELECT A.VIEW_NAME,A.TEXT DEFINITION_SQL,  B.COMMENTS, 'VIEW'  TABLE_TYPE FROM USER_VIEWS  A, USER_TAB_COMMENTS B WHERE A.VIEW_NAME = B.TABLE_NAME");
		if(BasicUtil.isNotEmpty(pattern)){
			builder.append(" AND TABLE_NAME LIKE '").append(pattern).append("'");
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
			String name = row.getString("VIEW_NAME");
			T view = views.get(name.toUpperCase());
			if(null == view){
				view = (T)new View();
			}
			view.setCatalog(catalog);
			view.setSchema(schema);
			view.setName(name);
			view.setComment(row.getString("COMMENTS"));
			view.setDefinition(row.getString("DEFINITION_SQL"));
			views.put(name.toUpperCase(), view);
		}
		return views;
	}
	/* *****************************************************************************************************************
	 * 													master table
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryMasterTableRunSQL(String catalog, String schema, String pattern, String types)
	 * <T extends MasterTable> LinkedHashMap<String, T> mtables(int index, boolean create, String catalog, String schema, LinkedHashMap<String, T> tables, DataSet set) throws Exception
	 * <T extends MasterTable> LinkedHashMap<String, T> mtables(boolean create, LinkedHashMap<String, T> tables, DatabaseMetaData dbmd, String catalog, String schema, String pattern, String ... types) throws Exception
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
	 * @param dbmd DatabaseMetaData
	 * @return List
	 */
	@Override
	public <T extends MasterTable> LinkedHashMap<String, T> mtables(boolean create, LinkedHashMap<String, T> tables, DatabaseMetaData dbmd, String catalog, String schema, String pattern, String ... types) throws Exception{
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
	 * <T extends PartitionTable> LinkedHashMap<String,T> ptables(boolean create, LinkedHashMap<String, T> tables, DatabaseMetaData dbmd, String catalog, String schema, MasterTable master) throws Exception
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
	 * @param set set
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
	 * @param dbmd DatabaseMetaData
	 * @return tables
	 * @throws Exception 异常
	 */
	@Override
	public <T extends PartitionTable> LinkedHashMap<String,T> ptables(boolean create, LinkedHashMap<String, T> tables, DatabaseMetaData dbmd, String catalog, String schema, MasterTable master) throws Exception{
		return super.ptables(create, tables, dbmd, catalog, schema, master);
	}


	/* *****************************************************************************************************************
	 * 													column
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryColumnRunSQL(Table table, boolean metadata)
	 * <T extends Column> LinkedHashMap<String, T> columns(int index, boolean create, Table table, LinkedHashMap<String, T> columns, DataSet set) throws Exception
	 * <T extends Column> LinkedHashMap<String, T> columns(boolean create, LinkedHashMap<String, T> columns, Table table, SqlRowSet set) throws Exception
	 * <T extends Column> LinkedHashMap<String, T> columns(boolean create, LinkedHashMap<String, T> columns, DatabaseMetaData dbmd, Table table, String pattern) throws Exception
	 ******************************************************************************************************************/

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
			builder.append("SELECT M.*, F.COMMENTS AS COLUMN_COMMENT FROM USER_TAB_COLUMNS    M \n");
			builder.append("LEFT JOIN USER_COL_COMMENTS F ON M.TABLE_NAME = F.TABLE_NAME AND M.COLUMN_NAME = F.COLUMN_NAME\n");
			if (BasicUtil.isNotEmpty(table)) {
				builder.append("WHERE M.TABLE_NAME = '").append(table.getName()).append("'");
			}
		}
		return runs;
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
	public <T extends Column> LinkedHashMap<String, T> columns(int index, boolean create, Table table, LinkedHashMap<String, T> columns, DataSet set) throws Exception{
		set.removeColumn("CHARACTER_SET_NAME");
		return super.columns(index, create, table, columns, set);
	}
	@Override
	public <T extends Column> LinkedHashMap<String, T> columns(boolean create, LinkedHashMap<String, T> columns, Table table, SqlRowSet set) throws Exception{
		return super.columns(create, columns, table, set);
	}
	@Override
	public <T extends Column> LinkedHashMap<String, T> columns(boolean create, LinkedHashMap<String, T> columns, DatabaseMetaData dbmd, Table table, String pattern) throws Exception{
		return super.columns(create, columns, dbmd, table, pattern);
	}


	/* *****************************************************************************************************************
	 * 													tag
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryTagRunSQL(Table table, boolean metadata)
	 * <T extends Tag> LinkedHashMap<String, T> tags(int index, boolean create, Table table, LinkedHashMap<String, T> tags, DataSet set) throws Exception
	 * <T extends Tag> LinkedHashMap<String, T> tags(boolean create, Table table, LinkedHashMap<String, T> tags, SqlRowSet set) throws Exception
	 * <T extends Tag> LinkedHashMap<String, T> tags(boolean create, LinkedHashMap<String, T> tags, DatabaseMetaData dbmd, Table table, String pattern) throws Exception
	 ******************************************************************************************************************/
	/**
	 *
	 * @param table 表
	 * @param metadata 是否根据metadata | 查询系统表
	 * @return sqls
	 */
	@Override
	public List<Run> buildQueryTagRunSQL(Table table, boolean metadata) throws Exception{
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
	public <T extends Tag> LinkedHashMap<String, T> tags(int index, boolean create, Table table, LinkedHashMap<String, T> tags, DataSet set) throws Exception{
		return super.tags(index, create, table, tags, set);
	}
	@Override
	public <T extends Tag> LinkedHashMap<String, T> tags(boolean create, Table table, LinkedHashMap<String, T> tags, SqlRowSet set) throws Exception{
		return super.tags(create, table, tags, set);
	}
	@Override
	public <T extends Tag> LinkedHashMap<String, T> tags(boolean create, LinkedHashMap<String, T> tags, DatabaseMetaData dbmd, Table table, String pattern) throws Exception{
		return super.tags(create, tags, dbmd, table, pattern);
	}

	/* *****************************************************************************************************************
	 * 													index
	 * -----------------------------------------------------------------------------------------------------------------
	 * List<Run> buildQueryIndexRunSQL(Table table, boolean metadata)
	 * <T extends Index> LinkedHashMap<String, T> indexs(int index, boolean create, Table table, LinkedHashMap<String, T> indexs, DataSet set) throws Exception
	 * <T extends Index> LinkedHashMap<String, T> indexs(boolean create, Table table, LinkedHashMap<String, T> indexs, SqlRowSet set) throws Exception
	 * <T extends Index> LinkedHashMap<String, T> indexs(boolean create, LinkedHashMap<String, T> indexs, DatabaseMetaData dbmd, Table table, boolean unique, boolean approximate) throws Exception
	 ******************************************************************************************************************/
	/**
	 * 查询表上的列
	 * @param table 表
	 * @param name name
	 * @return sql
	 */
	@Override
	public List<Run> buildQueryIndexRunSQL(Table table, String name){
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
	public <T extends Index> LinkedHashMap<String, T> indexs(int index, boolean create, Table table, LinkedHashMap<String, T> indexs, DataSet set) throws Exception{
		return super.indexs(index, create, table, indexs, set);
	}
	@Override
	public <T extends Index> LinkedHashMap<String, T> indexs(boolean create, Table table, LinkedHashMap<String, T> indexs, SqlRowSet set) throws Exception{
		return super.indexs(create, table, indexs, set);
	}
	@Override
	public <T extends Index> LinkedHashMap<String, T> indexs(boolean create, LinkedHashMap<String, T> indexs, DatabaseMetaData dbmd, Table table, boolean unique, boolean approximate) throws Exception{
		return super.indexs(create, indexs, dbmd, table, unique, approximate);
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
		return super.buildQueryTriggerRunSQL(table, events);
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
		return super.triggers(index, create, table, triggers, set);
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
	@Override
	public List<Run> buildAddCommentRunSQL(Table table) throws Exception {
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		if(BasicUtil.isEmpty(table.getComment())){
			return runs;
		}
		builder.append(" COMMENT ON TABLE ");
		name(builder, table);
		builder.append("  IS '").append(table.getComment()).append("'");
		return runs;
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
	public List<Run> buildAlterRunSQL(Table table, Collection<Column> columns) throws Exception{
		return super.buildAlterRunSQL(table, columns);
	}
	/**
	 * 修改表名
	 * ALTER TABLE A RENAME TO B;
	 * @param table 表
	 * @return String
	 */
	@Override
	public List<Run> buildRenameRunSQL(Table table) throws Exception {
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("ALTER TABLE ");
		name(builder, table);
		builder.append(" RENAME TO ");
		//去掉catalog schema前缀
		Table update = new Table(table.getUpdate().getName());
		name(builder, update);
		return runs;
	}

	/**
	 * 修改备注
	 * COMMENT ON TABLE T IS 'ABC';
	 * @param table 表
	 * @return String
	 */
	@Override
	public List<Run> buildChangeCommentRunSQL(Table table) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		String comment = table.getComment();
		if(BasicUtil.isNotEmpty(comment)) {
			builder.append("COMMENT ON TABLE ");
			name(builder, table);
			builder.append(" IS '").append(comment).append("'");
			runs.add(run);
		}
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
		return builder;
	}


	/**
	 * 主键
	 * CONSTRAINT PK_BS_DEV PRIMARY KEY (ID ASC)
	 * @param builder builder
	 * @param table 表
	 * @return builder
	 */
	@Override
	public StringBuilder primary(StringBuilder builder, Table table){
		List<Column> pks = table.primarys();
		if(pks.size()>0){
			builder.append(",CONSTRAINT ").append("PK_").append(table.getName()).append(" PRIMARY KEY (");
			boolean first = true;
			for(Column pk:pks){
				if(!first){
					builder.append(",");
				}
				SQLUtil.delimiter(builder, pk.getName(), getDelimiterFr(), getDelimiterTo());
				String order = pk.getOrder();
				if(null != order){
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
	 * 不支持在创建表时带备注，创建后单独添加 buildAddCommentRunSQL(table)
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
		return super.buildCreateRunSQL(view);
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
		return "ALTER";
	}

	/**
	 * 添加列
	 * ALTER TABLE  HR_USER ADD  UPT_TIME datetime CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP comment '修改时间' AFTER ID;
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
		// Column update = column.getUpdate();
		// if(null == update){
		// 添加列
		builder.append(" ADD ");
		SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo()).append(" ");
		define(builder, column);
		//}
		runs.addAll(buildAddCommentRunSQL(column));
		return runs;
	}

	/**
	 * 修改列 ALTER TABLE  HR_USER CHANGE UPT_TIME UPT_TIME datetime   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP  comment '修改时间' AFTER ID;
	 * @param column 列
	 * @param slice 是否只生成片段(不含alter table部分，用于DDL合并)
	 * @return List
	 */
	@Override
	public List<Run> buildAlterRunSQL(Column column, boolean slice) throws Exception{
		return super.buildAlterRunSQL(column, slice);
	}
	@Override
	public List<Run> buildAlterRunSQL(Column column) throws Exception{
		return buildAlterRunSQL(column, false);
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
	 * ALTER TABLE 表名 RENAME COLUMN RENAME 老列名 TO 新列名
	 * @param column 列
	 * @return String
	 */
	@Override
	public List<Run> buildRenameRunSQL(Column column)  throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		builder.append("ALTER TABLE ");
		name(builder, column.getTable(true));
		builder.append(" RENAME COLUMN ");
		SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo());
		builder.append(" TO ");
		SQLUtil.delimiter(builder, column.getUpdate().getName(), getDelimiterFr(), getDelimiterTo());
		column.setName(column.getUpdate().getName());
		return runs;
	}


	/**
	 * 修改数据类型
	 * 1.ADD NEW COLUMN
	 * 2.FORMAT VALUE
	 * 3.MOVE VALUE
	 * alter table tb modify (name nvarchar2(20))
	 * @param column 列
	 * @return sql
	 */
	public List<Run> buildChangeTypeRunSQL(Column column) throws Exception{
		List<Run> runs = new ArrayList<>();
		Column update = column.getUpdate();
		String name = column.getName();
		String type = column.getTypeName();
		if(type.contains("(")){
			type = type.substring(0,type.indexOf("("));
		}
		String uname = update.getName();
		String utype = update.getTypeName();
		if(uname.endsWith("_TMP_UPDATE_TYPE")){
			runs.addAll(buildDropRunSQL(update));
		}else {
			if (utype != null && utype.contains("(")) {
				utype = utype.substring(0, utype.indexOf("("));
			}
			if (!type.equals(utype)) {
				String tmp_name = column.getName() + "_TMP_UPDATE_TYPE";

				update.setName(tmp_name);
				runs.addAll(buildRenameRunSQL(column));

				update.setName(uname);
				runs.addAll(buildAddRunSQL(update));

				StringBuilder builder = new StringBuilder();
				builder.append("UPDATE ");
				name(builder, column.getTable(true));
				builder.append(" SET ");
				SQLUtil.delimiter(builder, uname, getDelimiterFr(), getDelimiterTo());
				builder.append(" = ");
				SQLUtil.delimiter(builder, tmp_name, getDelimiterFr(), getDelimiterTo());
				runs.add(new SimpleRun(builder));

				column.setName(tmp_name);
				List<Run> drop = buildDropRunSQL(column);
				runs.addAll(drop);

				column.setName(name);
				update.setName(tmp_name);
				column.setNullable(update.isNullable());
			} else {
				StringBuilder builder = new StringBuilder();
				builder.append("ALTER TABLE ");
				name(builder, column.getTable(true));
				builder.append(" MODIFY(");
				SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo()).append(" ");
				type(builder, column.getUpdate());
				builder.append(")");
				runs.add(new SimpleRun(builder));
			}
		}
		// column.setName(name);
		return runs;
	}

	/**
	 * 修改默认值
	 * ALTER TABLE MY_TEST_TABLE MODIFY B DEFAULT 2
	 * @param column 列
	 * @return String
	 */
	@Override
	public List<Run> buildChangeDefaultRunSQL(Column column) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		Object def = null;
		if(null != column.getUpdate()){
			def = column.getUpdate().getDefaultValue();
		}else {
			def = column.getDefaultValue();
		}
		builder.append("ALTER TABLE ");
		name(builder, column.getTable(true)).append(" MODIFY ");
		SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo());
		builder.append(" DEFAULT ");
		if(null != def){
			def = write(column, def, false);
			//format(builder, def);
			builder.append(def);
		}else{
			builder.append(" NULL");
		}
		return runs;
	}

	/**
	 * 修改非空限制
	 * ALTER TABLE T  MODIFY C NOT NULL ;
	 * @param column 列
	 * @return String
	 */
	@Override
	public List<Run> buildChangeNullableRunSQL(Column column) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		int nullable = column.isNullable();
		int uNullable = column.getUpdate().isNullable();
		if(nullable != -1 && uNullable != -1){
			if(nullable == uNullable){
				return runs;
			}

			builder.append("ALTER TABLE ");
			name(builder, column.getTable(true)).append(" MODIFY ");
			SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo());
			if(uNullable == 0){
				builder.append(" NOT ");
			}
			builder.append(" NULL");
			column.setNullable(uNullable);
		}
		return runs;
	}

	/**
	 * 添加表备注(表创建完成后调用,创建过程能添加备注的不需要实现)
	 * @param column 列
	 * @return sql
	 * @throws Exception 异常
	 */
	public List<Run> buildAddCommentRunSQL(Column column) throws Exception {
		return buildChangeCommentRunSQL(column);
	}
	/**
	 * 修改备注
	 * COMMENT ON COLUMN T.ID IS 'ABC'
	 * @param column 列
	 * @return String
	 */
	@Override
	public List<Run> buildChangeCommentRunSQL(Column column) throws Exception{
		List<Run> runs = new ArrayList<>();
		Run run = new SimpleRun();
		runs.add(run);
		StringBuilder builder = run.getBuilder();
		String comment = null;
		if(null != column.getUpdate()){
			comment = column.getUpdate().getComment();
		}else {
			comment = column.getComment();
		}
		if(BasicUtil.isNotEmpty(comment)) {
			builder.append("COMMENT ON COLUMN ");
			name(builder, column.getTable(true)).append(".");
			SQLUtil.delimiter(builder, column.getName(), getDelimiterFr(), getDelimiterTo());
			builder.append(" IS '").append(comment).append("'");
		}
		return runs;
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
		return super.increment(builder, column);
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
	 * List<Run> buildAddRunSQL(Tag tag)
	 * List<Run> buildAlterRunSQL(Tag tag)
	 * List<Run> buildDropRunSQL(Tag tag)
	 * List<Run> buildRenameRunSQL(Tag tag)
	 * List<Run> buildChangeDefaultRunSQL(Tag tag)
	 * List<Run> buildChangeNullableRunSQL(Tag tag)
	 * List<Run> buildChangeCommentRunSQL(Tag tag)
	 * List<Run> buildChangeTypeRunSQL(Tag tag)
	 * StringBuilder checkTagExists(StringBuilder builder, boolean exists)
	 ******************************************************************************************************************/

	/**
	 * 添加标签
	 * ALTER TABLE  HR_USER ADD TAG UPT_TIME datetime CHARACTER SET utf8mb4 COLLATE utf8mb4_0900_ai_ci  DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP comment '修改时间' AFTER ID;
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public List<Run> buildAddRunSQL(Tag tag) throws Exception{
		return super.buildAddRunSQL(tag);
	}


	/**
	 * 修改标签 ALTER TABLE  HR_USER CHANGE UPT_TIME UPT_TIME datetime   DEFAULT NULL ON UPDATE CURRENT_TIMESTAMP  comment '修改时间' AFTER ID;
	 * @param tag 标签
	 * @return List
	 */
	@Override
	public List<Run> buildAlterRunSQL(Tag tag) throws Exception{
		return super.buildAlterRunSQL(tag);
	}


	/**
	 * 删除标签
	 * ALTER TABLE HR_USER DROP TAG NAME;
	 * @param tag 标签
	 * @return String
	 */
	@Override
	public List<Run> buildDropRunSQL(Tag tag) throws Exception{
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
	public List<Run> buildRenameRunSQL(Tag tag)  throws Exception{
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
	public List<Run> buildChangeDefaultRunSQL(Tag tag) throws Exception{
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
	public List<Run> buildChangeNullableRunSQL(Tag tag) throws Exception{
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
	public List<Run> buildChangeCommentRunSQL(Tag tag) throws Exception{
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
	public List<Run> buildChangeTypeRunSQL(Tag tag) throws Exception{
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
			builder.append(" ADD CONSTRAINT ").append(primary.getTableName(true)).append("_PK").append(" PRIMARY KEY(");
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

	/**
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
		builder.append("SELECT COL.* FROM DBA_CONSTRAINTS CON ,DBA_CONS_COLUMNS COL\n");
		builder.append("WHERE CON.CONSTRAINT_NAME = COL.CONSTRAINT_NAME\n");
		builder.append("AND CON.CONSTRAINT_TYPE = 'P'\n");
		builder.append("AND COL.TABLE_NAME = '").append(table.getName()).append("'\n");
		if(BasicUtil.isNotEmpty(table.getSchema())){
			builder.append(" AND COL.OWNER = '").append(table.getSchema()).append("'");
		}
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
		PrimaryKey primary = table.getPrimaryKey();
		for(DataRow row:set){
			if(null == primary){
				primary = new PrimaryKey();
				primary.setName(row.getString("CONSTRAINT_NAME"));
				primary.setTable(table);
			}
			String col = row.getString("COLUMN_NAME");
			Column column = primary.getColumn(col);
			if(null == column){
				column = new Column(col);
			}
			column.setTable(table);
			column.setPosition(row.getInt("POSITION",0));
			primary.addColumn(column);
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
		return super.buildQueryForeignsRunSQL(table);
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
		return super.foreigns(index, table, foreigns, set);
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
		return super.buildDropRunSQL(index);
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
		super.comment(builder, index);
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
	 * void value(StringBuilder builder, Object obj, String key)
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
	public String value(Column column, SQL_BUILD_IN_VALUE value){
		if(value == SQL_BUILD_IN_VALUE.CURRENT_TIME){
			return "sysdate";
		}
		return null;
	}

} 
