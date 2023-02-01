package org.anyline.entity.data;

public interface Column {


    public String getCatalog();

    public String getClassName();

    public Integer getDisplaySize();

    public String getComment();

    public String getName();
    public String getOriginalName();

    public Integer getType();

    public Table getTable();
    public String getTypeName();

    public Integer getPrecision();

    public String getSchema();
    public String getTableName();

    public int isCaseSensitive();

    public int isCurrency();

    public int isSigned();
    public Integer getScale();

    public int isNullable();

    public int isAutoIncrement();
    public int isPrimaryKey();

    public int isGenerated();

    public Object getDefaultValue();

    public Integer getPosition();

    public String getOrder();

    public String getBefore() ;
    public String getAfter() ;

    public Integer getIncrementSeed();
    public Integer getIncrementStep();
    public int isOnUpdate();

    public String getCharset();

    public String getCollate();
    //完整的数据类型 varchar(32)
    public String getFullType();
}
