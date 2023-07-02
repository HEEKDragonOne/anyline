package org.anyline.data.interceptor;

import org.anyline.data.jdbc.ds.JDBCRuntime;
import org.anyline.data.param.ConfigStore;
import org.anyline.data.prepare.RunPrepare;
import org.anyline.data.run.Run;
import org.anyline.metadata.ACTION;
import org.anyline.metadata.ACTION.SWITCH;
import org.anyline.metadata.Parameter;
import org.anyline.metadata.Procedure;

import java.util.List;

public interface ExecuteInterceptor extends DMInterceptor{

    /**
     * 创建 SQL之前，可以在这一步修改查询条件
     * @param runtime datasource/adapter/jdbctemplate
     * @param prepare 准备数据
     * @param configs  执行条件
     * @param conditions 执行条件
     * @return RESULT
     */
    default ACTION.SWITCH prepare(JDBCRuntime runtime, RunPrepare prepare, ConfigStore configs, String ... conditions){ return SWITCH.CONTINUE;}
    default SWITCH prepare(JDBCRuntime runtime, Procedure procedure){ return SWITCH.CONTINUE;}

    /**
     * 合计总数之前调用，到这一步SQL已创建完成
     * @param runtime datasource/adapter/jdbctemplate
     * @param run 查询SQL(包含SQL体，查询条件，查询参数值)
     * @return RESULT
     */
    default SWITCH before(JDBCRuntime runtime, Run run){ return SWITCH.CONTINUE;}


    /**
     * 合计总数之前调用，到这一步SQL已创建完成
     * @param runtime datasource/adapter/jdbctemplate
     * @param procedure 存储过程
     * @return RESULT
     */
    default SWITCH before(JDBCRuntime runtime, Procedure procedure, String sql, List<Parameter> inputs, List<Parameter> outputs){ return SWITCH.CONTINUE;}
    /**
     * 合计总数之前调用，到这一步SQL已创建完成
     * @param runtime datasource/adapter/jdbctemplate
     * @param result 影响行数
     * @param run 查询SQL(包含SQL体，查询条件，查询参数值)
     * @param millis 耗时
     * @return RESULT
     */
    default SWITCH after(JDBCRuntime runtime, Run run, boolean success, int result, long millis){ return SWITCH.CONTINUE;}
    /**
     * 合计总数之前调用，到这一步SQL已创建完成
     * @param runtime datasource/adapter/jdbctemplate
     * @param result 执行结果
     * @param procedure 存储过程
     * @param millis 耗时
     * @return RESULT
     */
    default SWITCH after(JDBCRuntime runtime, Procedure procedure, String sql, List<Parameter> inputs, List<Parameter> outputs, boolean success, boolean result, long millis){ return SWITCH.CONTINUE;}
}
