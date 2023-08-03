package org.anyline.dao.init.springjdbc;


import org.anyline.data.runtime.DataRuntime;

public class FixDao<E> extends DefaultDao<E>{

    private String datasource;

    public String getDatasource() {
        return datasource;
    }

    public void setDatasource(String datasource) {
        this.datasource = datasource;
    }

    /**
     * 是否固定数据源
     * @return boolean
     */
    public boolean isFix(){
        return true;
    }
    protected DataRuntime runtime(){
        if(null != runtime){
            return runtime;
        }else{
            throw new RuntimeException("未设置运行环境");
        }/*
        runtime = RuntimeHolder.getRuntime(datasource);
        return runtime;*/
    }
}
