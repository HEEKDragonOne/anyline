package org.anyline.dao.impl;

import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.ConcurrentLinkedDeque;

import org.anyline.entity.DataRow;
import org.anyline.entity.DataSet;
import org.anyline.util.ConfigTable;


public class BatchInsertStore {
	public boolean isRun = false;
	private ConcurrentHashMap<String,ConcurrentLinkedDeque<DataRow>> map = new ConcurrentHashMap<String,ConcurrentLinkedDeque<DataRow>>();
	
	public synchronized void addData(String table, String cols, DataRow data){
		String key = table + "(" + cols +")";
		ConcurrentLinkedDeque<DataRow> rows = map.get(key);
		if(null == rows){
			rows = new ConcurrentLinkedDeque<DataRow>();
			map.put(key, rows);
		}
		rows.add(data);
	}
	/**
	 * 需要保存的数据列表
	 * @return
	 */
	public synchronized DataSet getDatas(){
		int max = ConfigTable.getInt("BATCH_INSERT_MAX_SIZE",100);//一次最多插入
		DataSet list = new DataSet();
		//第一次循环查找数量>=100的数据
				for(ConcurrentLinkedDeque<DataRow> rows :map.values()){
					int size = rows.size();
					if(size >= max){
						int cnt = 0;
						while(cnt < max && !rows.isEmpty()){
							DataRow row = rows.poll();
							if(null != row){
								list.add(row);
							}
						}
						return list;
					}
				}
			//第一次失败后 补充第二次循环查找数量>=1的数据
			for(ConcurrentLinkedDeque<DataRow> rows :map.values()){
				int size = rows.size();
				if(size > 0){
					int cnt = 0;
					while(cnt < max && !rows.isEmpty()){
						DataRow row = rows.poll();
						if(null != row){
							list.add(row);
						}
					}
					return list;
				}
			}
			
		return list;
	}
}
