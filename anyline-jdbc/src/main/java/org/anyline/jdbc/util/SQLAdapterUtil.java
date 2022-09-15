package org.anyline.jdbc.util;

import org.anyline.jdbc.config.db.SQLAdapter;
import org.anyline.jdbc.ds.DataSourceHolder;
import org.anyline.util.SpringContextUtil;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.jdbc.datasource.DataSourceUtils;
import org.springframework.stereotype.Repository;

import javax.sql.DataSource;
import java.sql.Connection;
import java.sql.SQLException;
import java.util.Map;
import java.util.concurrent.ConcurrentHashMap;

@Repository
public class SQLAdapterUtil {

	private static ConcurrentHashMap<String, SQLAdapter> adapters= new ConcurrentHashMap<>();

	@Autowired(required = false)
	public void setAdapters(Map<String, SQLAdapter> map){
		for (SQLAdapter adapter:map.values()){
			adapters.put(adapter.type().getCode(), adapter);
		}
	}

	private static SQLAdapter defaultAdapter = null;	//如果当前项目只有一个adapter则不需要多次识别
	public static SQLAdapter getAdapter(JdbcTemplate jdbc){


		if(null != defaultAdapter){
			return defaultAdapter;
		}
		if(adapters.size() ==1){
			defaultAdapter = adapters.values().iterator().next();
			return defaultAdapter;
		}
		SQLAdapter adapter = null;
		SQLAdapter.DB_TYPE type = DataSourceHolder.dialect();
		if(null != type){
			//根据 别名
			adapter = getAdapter(type.getName());
			if(null != adapter){
				return adapter;
			}
		}

		DataSource ds = null;
		Connection con = null;
		try {
			if(null != jdbc){
				ds = jdbc.getDataSource();
				con = DataSourceUtils.getConnection(ds);
				String name = con.getMetaData().getDatabaseProductName().toLowerCase().replace(" ", "");
				name += con.getMetaData().getURL().toLowerCase();
				//根据url中关键字
				adapter = getAdapter(name);

			}
			if(null == adapter){
				adapter = SpringContextUtil.getBean(SQLAdapter.class);
			}
		} catch (SQLException e) {
			e.printStackTrace();
		}finally {
			if(!DataSourceUtils.isConnectionTransactional(con, ds)){
				DataSourceUtils.releaseConnection(con, ds);
			}
		}
		return adapter;
	}
	private static SQLAdapter getAdapter(String name){
		SQLAdapter adapter = null;
		adapter = adapters.get(name);
		if(null != adapter){
			return adapter;
		}
		if(name.contains("mysql")){
			adapter = adapters.get(SQLAdapter.DB_TYPE.MYSQL.getCode());
		}else if(name.contains("mssql") || name.contains("sqlserver")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.MSSQL.getCode());
		}else if(name.contains("oracle")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.ORACLE.getCode());
		}else if(name.contains("postgresql")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.PostgreSQL.getCode());
		}

		else if(name.contains("clickhouse")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.ClickHouse.getCode());
		}else if(name.contains("db2")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.DB2.getCode());
		}else if(name.contains("derby")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.Derby.getCode());
		}else if(name.contains("dmdbms")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.DM.getCode());
		}else if(name.contains("hgdb") || name.contains("highgo")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.HighGo.getCode());
		}else if(name.contains("kingbase")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.KingBase.getCode());
		}else if(name.contains("oceanbase")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.OceanBase.getCode());
		}else if(name.contains("polardb")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.PolarDB.getCode());
		}else if(name.contains("sqlite")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.SQLite.getCode());
		}else if(name.contains(":h2:")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.H2.getCode());
		}else if(name.contains("hsqldb")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.HSQLDB.getCode());
		}else if(name.contains("taos")){
			adapter =  adapters.get(SQLAdapter.DB_TYPE.TDengine.getCode());
		}
		adapters.put(name, adapter);
		return adapter;
	}
}
