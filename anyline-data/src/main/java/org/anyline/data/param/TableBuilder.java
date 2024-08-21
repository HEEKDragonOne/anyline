/*
 * Copyright 2006-2023 www.anyline.org
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package org.anyline.data.param;

import org.anyline.data.prepare.RunPrepare;
import org.anyline.data.prepare.auto.TablePrepare;
import org.anyline.data.prepare.auto.init.DefaultTablePrepare;
import org.anyline.data.entity.Join;
import org.anyline.metadata.Column;
import org.anyline.metadata.Table;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

public class TableBuilder {

    private TablePrepare prepare;
    private ConfigStore configs;
    private LinkedHashMap<String,Column> columns = new LinkedHashMap<>(); //需要查询的列
    private List<Join> joins = new ArrayList<>();//关联表

    public RunPrepare build() {
        if(null == prepare){
            prepare = new DefaultTablePrepare();
        }
        if(null != joins) {
            for (Join join : joins) {
                prepare.join(join);
            }
        }
        if(null != columns) {
            for (Column col : columns.values()) {
                prepare.addColumn(col);
            }
        }
        if(null != configs){
            prepare.condition(configs);
        }
        return prepare;
    }
    public static TableBuilder init() {
        TableBuilder builder = new TableBuilder();
        return builder;
    }
    public static TableBuilder init(String table) {
        TableBuilder builder = new TableBuilder();
        builder.setTable(table);
        return builder;
    }
    public static TableBuilder init(Table table) {
        TableBuilder builder = new TableBuilder();
        builder.setTable(table);
        return builder;
    }
    public static TableBuilder init(TablePrepare prepare) {
        TableBuilder builder = new TableBuilder();
        builder.prepare = prepare;
        return builder;
    }
    public static TableBuilder init(String table, String columns) {
        TableBuilder builder = new TableBuilder();
        builder.setTable(table);
        builder.addColumns(columns);
        return builder;
    }
    public TableBuilder condition(ConfigStore configs) {
        this.configs = configs;
        return this;
    }

    public TableBuilder setTable(String table) {
        this.prepare = new DefaultTablePrepare();
        prepare.setTable(table);
        return this;
    }
    public TableBuilder setTable(Table table) {
        this.prepare = new DefaultTablePrepare();
        prepare.setTable(table);
        return this;
    }
    public TableBuilder addColumn(String column) {
        if(!columns.containsKey(column.toUpperCase())) {
            columns.put(column.toUpperCase(), new Column(column));
        }
        return this;
    }
    public TableBuilder addColumns(String ... columns) {
        if(null != columns) {
            for (String column:columns) {
                addColumn(column);
            }
        }
        return this;
    }

    public TableBuilder join(Join join) {
        joins.add(join);
        return this;
    }
    public TableBuilder join(Join.TYPE type, String table, String ... conditions) {
        return join(type, new Table(table), conditions);
    }
    public TableBuilder join(Join.TYPE type, Table table, String ... conditions) {
        Join join = new Join();
        join.setTable(table);
        join.setType(type);
        join.setConditions(conditions);
        return join(join);
    }
    public TableBuilder inner(Table table, String ... conditions) {
        return join(Join.TYPE.INNER, table.getFullName(), conditions);
    }
    public TableBuilder inner(String table, String ... conditions) {
        return join(Join.TYPE.INNER, table, conditions);
    }
    public TableBuilder left(String table, String ... conditions) {
        return join(Join.TYPE.LEFT, table, conditions);
    }
    public TableBuilder left(Table table, String ... conditions) {
        return join(Join.TYPE.LEFT, table, conditions);
    }
    public TableBuilder right(String table, String ... conditions) {
        return join(Join.TYPE.RIGHT, table, conditions);
    }
    public TableBuilder right(Table table, String ... conditions) {
        return join(Join.TYPE.RIGHT, table, conditions);
    }
    public TableBuilder full(String table, String ... conditions) {
        return join(Join.TYPE.FULL, table, conditions);
    }

    public TableBuilder full(Table table, String ... conditions) {
        return join(Join.TYPE.FULL, table, conditions);
    }


    public TableBuilder join(Join.TYPE type, RunPrepare prepare, String ... conditions) {
        Join join = new Join();
        join.setPrepare(prepare);
        join.setType(type);
        join.setConditions(conditions);
        return join(join);
    }
    public TableBuilder inner(RunPrepare prepare, String ... conditions) {
        return join(Join.TYPE.INNER, prepare, conditions);
    }
    public TableBuilder left(RunPrepare prepare, String ... conditions) {
        return join(Join.TYPE.LEFT, prepare, conditions);
    }
    public TableBuilder right(RunPrepare prepare, String ... conditions) {
        return join(Join.TYPE.RIGHT, prepare, conditions);
    }

    public TableBuilder full(RunPrepare prepare, String ... conditions) {
        return join(Join.TYPE.FULL, prepare, conditions);
    }
}
