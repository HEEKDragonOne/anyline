package org.anyline.data.adapter.init;

import org.anyline.data.metadata.persistence.ManyToMany;
import org.anyline.data.metadata.persistence.OneToMany;
import org.anyline.entity.data.Column;
import org.anyline.entity.data.Table;
import org.anyline.proxy.EntityAdapterProxy;
import org.anyline.util.ClassUtil;

import java.lang.annotation.Annotation;
import java.lang.reflect.Field;
import java.lang.reflect.Method;

public class PersistenceAdapter {


    public static OneToMany oneToMany(Field field) throws Exception{
        /*
         *     //考勤记录
         *     @OneToMany(mappedBy = "EMPLOYEE_ID")              // 关联表中与当前表关联的外键(这里可以是列名也可以是AttendanceRecord属性名)
         *     private List<AttendanceRecord> records = null;    //
         *
         *
         *     //考勤记录
         *     @OneToMany(mappedBy = "EMPLOYEE_ID")              //关联表中与当前表关联的外键(这里可以是列名也可以是AttendanceRecord属性名)
         *     private AttendanceRecord[] recordArray  = null;   //
         */
        OneToMany join = new OneToMany();
        join.joinColumn = ClassUtil.parseAnnotationFieldValue(field, "OneToMany.mappedBy");
        join.dependencyClass = ClassUtil.getComponentClass(field);;
        join.joinField = ClassUtil.getField(join.dependencyClass, join.joinColumn);
        if(null == join.joinField){
            //提供的是列名
            join.joinField = EntityAdapterProxy.field(join.dependencyClass, join.joinColumn);
        }
        //检测joinField对应的列表
        if(null != join.joinField){
            Column column = EntityAdapterProxy.column(join.dependencyClass,join.joinField);
            if(null != column){
                join.joinColumn = column.getName();
            }
        }
        Table table = EntityAdapterProxy.table(join.dependencyClass);
        if(null != table){
            join.dependencyTable = table.getName();
        }
        return join;
    }
    public static ManyToMany manyToMany(Field field) throws Exception{
        /*
         *     //多对多关系  一个在多个部门任职
         *     @ManyToMany
         *     @JoinTable(name = "HR_EMPLOYEE_DEPARTMENT"                          //中间关联表
         *             , joinColumns = @JoinColumn(name="EMPLOYEE_ID")             //关联表中与当前表关联的外键
         *             , inverseJoinColumns = @JoinColumn(name="DEPARTMENT_ID"))   //关联表中与当前表关联的外键
         *     private List<Department> departments;//查部门完整信息
         *
         *
         *     @ManyToMany
         *     @JoinTable(name = "HR_EMPLOYEE_DEPARTMENT"                          //中间关联表
         *             , joinColumns = @JoinColumn(name="EMPLOYEE_ID")             //关联表中与当前表关联的外键
         *             , inverseJoinColumns = @JoinColumn(name="DEPARTMENT_ID"))   //关联表中与当前表关联的外键
         *     @Transient
         *     private List<Long> departmentIds;//只查部门主键
         * */
        ManyToMany join = new ManyToMany();
        join.joinTable = ClassUtil.parseAnnotationFieldValue(field, "JoinTable.name");
        Annotation anJoinTable = ClassUtil.getFieldAnnotation(field, "JoinTable");
        if (null != anJoinTable) {
            Method methodJoinColumns = anJoinTable.annotationType().getMethod("joinColumns");
            if (null != methodJoinColumns) {
                Object[] ojoinColumns = (Object[]) methodJoinColumns.invoke(anJoinTable);
                if (null != ojoinColumns && ojoinColumns.length > 0) {
                    Annotation joinColumn = (Annotation) ojoinColumns[0];
                    join.joinColumn = (String) joinColumn.annotationType().getMethod("name").invoke(joinColumn);
                }
            }
            Method methodInverseJoinColumns = anJoinTable.annotationType().getMethod("inverseJoinColumns");
            if (null != methodInverseJoinColumns) {
                Object[] ojoinColumns = (Object[]) methodInverseJoinColumns.invoke(anJoinTable);
                if (null != ojoinColumns && ojoinColumns.length > 0) {
                    Annotation joinColumn = (Annotation) ojoinColumns[0];
                    join.inverseJoinColumn = (String) joinColumn.annotationType().getMethod("name").invoke(joinColumn);
                }
            }
        }
        join.itemClass = ClassUtil.getComponentClass(field);	//Department
        if(!ClassUtil.isPrimitiveClass(join.itemClass) && String.class != join.itemClass){
            //List<Department> departments;
            org.anyline.entity.data.Table table = EntityAdapterProxy.table(join.itemClass);
            if(null != table){
                join.dependencyTable = table.getName();
                Column col = EntityAdapterProxy.primaryKey(join.itemClass);
                if(null != col){
                    join.dependencyPk = col.getName();
                }
            }
        }else{
            //List<Long> departments
            //基础类(只取ID)
        }
        return join;
    }
}
