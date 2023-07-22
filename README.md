
***详细说明请参考:***  
[http://doc.anyline.org/](http://doc.anyline.org/)  
下载的源码在执行前，首先要配置好maven环境,请参考[【maven配置】](http://doc.anyline.org/ss/35_1148)

关于多数据源，请先阅读   
[【三种方式注册数据源】](http://doc.anyline.org/aa/a9_3451)
[【三种方式切换数据源】](http://doc.anyline.org/aa/64_3449)
[【多数据源事务控制】](http://doc.anyline.org/ss/23_1189)  
低代码平台、数据中台等场景需要生成SQL(只生成不执行)参考  
[【JDBCAdapter】](http://doc.anyline.org/ss/01_1193)
或
[【AnylineDao】](https://gitee.com/anyline/anyline/blob/master/anyline-data-jdbc/src/main/java/org/anyline/dao/init/springjdbc/DefaultDao.java)  

***快速开始请参考示例源码(各种各样最简单的hello world):***  
[https://gitee.com/anyline/anyline-simple](https://gitee.com/anyline/anyline-simple)


***一个字都不想看，就想直接启动项目的下载这个源码:***  
[https://gitee.com/anyline/anyline-simple-clear](https://gitee.com/anyline/anyline-simple-clear)

有问题请不要自行百度，因为百度收录的内容有可能过期或版本不一致,有问题请联系  

[<img src="http://cdn.anyline.org/img/user/alq.png" width="150">](http://shang.qq.com/wpa/qunwpa?idkey=279fe968c371670fa9791a9ff8686f86dbac0b5edba8021a660b313e2dd863ad)
&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;<img src="http://cdn.anyline.org/img/user/alg.jpg" width="150">  
&nbsp;&nbsp;&nbsp;QQ群(86020680)&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
或&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;&nbsp;
微信群

## 简介
AnyLine的核心是一个基于spring-jdbc生态的兼容各种数据库的(No-ORM)DBUtil。   
  其重点是:
- 以最简单、快速、动态、统一的方生成或式执行DML与DDL,读写表结构、索引等元数据。


### 与ORM最明显的区别是：    
- 1）摒弃了各种繁琐呆板的实体类。以及相关的service/dao/mapping      
- 2）强化了针对结果集的数据二次处理能力  
### 换个方式说：
- 1）让数据库操作更简单  
   不要一动就是一整套的service/dao/mapping/VOPODTO有用没用的各种O，生成个SQL各种判断遍历。
- 2）结果集的数学计算尽量作到一键...一键...  
   而不是像ORM提供的entity,map,list除了提供个get/set/foreach，稍微有点用的又要麻烦程序员各种判断各种遍历

经常用于一些有动态需求的场景，如数据中台、低代码开发平台、可视化数据源、数据清洗、动态表单等。参考【[适用场景](http://doc.anyline.org/ss/ed_14)】

## 为什么还要要有AnyLine  
现成的spring-data-jdbc,hibernate,mybatis还不满足么   
如果只是简单的CURD,那确实不需要，但实际应用中发现太难了。

### 一、既要 简单方便。  
 机械重复的工作不要让开发人员费心。  

#### 动态场景  
如果我们需要开发一个数据中台时或者一个数据清洗插件时，这时还不知道会有什么数据有什么实体，
那只能定义一个高度抽象的实体了，想来想去也只有Map可以胜任。    
当我们需要用Map处理数据或数据运算时，如删除所有值为空的属性包括,"",null,"null","\n","   "等各种乱七八糟的情况, 计算类型=1的这部分maps的标准方差，  
这时就会发现Map太抽象了，要处理的细节太多了。除了GET/SET好像也没别的可施展了。  

再比如多数据源的情况下，helloword里经常是在方法配置个拦截器来切换数据源。  
在同一个方法里还能切换个数据源了？  
数据中台里有可能有几百几千个数据源，还得配上几千个方法？  
数据源是用户动态提交的呢怎么拦截呢？  

#### 重复工作  
比如一个订单可能有几十上百列的数据，每个分析师需要根据不同的列查询。  
有那么几十列上同时需要<>=!=IN FIND_IN_SET多种查询方式算正常吧  
不能让开发人员挨个写一遍吧，写一遍是没问题，但修改起来可就不是一遍两遍的事了    
所以需要提供一个字典让用户自己去配置，低代码开发平台、自定义报表、动态查询条件应该经常有这个需求。  
当用户提交上来一个列名、一个运算算、一组值，怎么执行SQL呢，不能在代码中各种判断吧，如果=怎么合成SQL，如果IN怎么合成SQL  

#### 多方言  
DML方面hibernate还可以处理，DDL呢？国产库呢？  

### 二、又要 在适当的时机暴露足够低层接口和数据结构  
没用的东西少封装，不要在需要的时候再费事拆开，碍手碍脚。  
比如datasource/sql还需要暴露出来  
不能因为封装成entity了就不管数据类型了  

当然这种问题很难有定论，只能在实际应用过程中根据情况取舍。  
可以参考【[适用场景](http://doc.anyline.org/ss/ed_14)】和【[实战对比](http://doc.anyline.org/ss/c9_1153)】中的示例  
造型之前，当然要搞明白优势劣势，参考【[优势劣势](http://doc.anyline.org/aa/24_3712)】   

## 误解
当然我们并不是要抛弃Entity或ORM，相反的 AnyLine源码中也使用了多达几十个Entity   
在一些 **可预知的 固定的** 场景下，Entity的优势还是不可替代的  
程序员应该有分辨场景的能力  
AnyLine希望程序员手中多一个数据库操作的利器，而不是被各种模式各种hello world限制


## 如何使用
数据操作***不要***再从生成xml/dao/service以及各种配置各种O开始  
默认的service已经提供了大部分的数据库操作功能。  
操作过程大致如下:
```
DataSet set = service.querys("HR_USER(ID,NM)", 
    condition(true,"anyline根据约定自动生成的=,in,like等查询条件"));  
```
这里的查询条件不再需要各种配置,各种if else foreach标签  
Anyline会自动生成,生成规则可以【参考】这里的[【约定规则】](http://doc.anyline.org/s?id=p298pn6e9o1r5gv78acvic1e624c62387f2c45dd13bb112b34176fad5a868fa6a4)  
分页也不需要另外的插件，更不需要繁琐的计算和配置，指定true或false即可


## 如何集成

只需要一个依赖、一个注解即可实现与springboot,netty等框架项目完美整合，参考【入门系列】
大概的方式就是在需要操作数据库的地方注入AnylineService
接下来service就可以完成大部分的数据库操作了。常用示例可以参考【[示例代码](https://gitee.com/anyline/anyline-simple)】

## 兼容
如果实现放不下那些已存在的各种XOOO  
DataSet与Entity之间可以相互转换  
或者这样:
```
EntitySet<User> = service.querys(User.class, 
    condition(true,"anyline根据约定自动生成的查询条件")); 
//true：表示需要分页
//为什么不用返回的是一个EntitySet而不是List?
//因为分页情况下,EntitySet中包含了分页数据,而List不行。
//无论是否分页都返回相同的数据结构，而不需要根据是否分页实现两个接口返回不同的数据结构



//也可以这样(如果真要这样就不要用anyline了,还是用MyBatis,Hibernate之类吧)
public class UserService extends AnylinseService<User> 
userService.querys(condition(true,"anyline根据约定自动生成的查询条件")); 
```


##  关于数据库的适配
直接看示例(代码都是一样的、可以用来测试一下自己的数据库是否被支持)  
[【源码】](https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect)

<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-mysql"> <img alt="MySQL" src="http://cdn.anyline.org/img/logo/mysql.png" width="100" />MySQL </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-postgresql"> <img alt="PostgreSQL" src="http://cdn.anyline.org/img/logo/postgres.png" width="100" />PostgreSQL </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-oracle"> <img alt="Oracle 11G" src="http://cdn.anyline.org/img/logo/oracle.png" width="100" />Oracle </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-mssql"> <img alt="SQL Server" src="http://cdn.anyline.org/img/logo/mssql.jpg" width="100" />SQL Server </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-mariadb"> <img alt="MariaDB" src="http://cdn.anyline.org/img/logo/mariadb.png" width="100" />MariaDB </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-db2">IBM DB2</a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-clickhouse"> <img alt="clickhouse" src="http://cdn.anyline.org/img/logo/clickhouse.jpg" width="100" />clickhouse </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-sqlite"> <img alt="sqlite" src="http://cdn.anyline.org/img/logo/sqlite.jpg" width="100" />sqlite </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-dm"> <img alt="达梦" src="http://cdn.anyline.org/img/logo/dm.webp" width="100" />达梦 </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-tdengine"> <img alt="tdengine" src="http://cdn.anyline.org/img/logo/tdengine.png" width="100" />tdengine </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-derby"> <img alt="derby" src="http://cdn.anyline.org/img/logo/derby.webp" width="100" />derby </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-h2"> <img alt="H2" src="http://cdn.anyline.org/img/logo/h2db.png" width="100" />H2 </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-hsqldb"> <img alt="hsqldb" src="http://cdn.anyline.org/img/logo/hsqldb.webp" width="100" />hsqldb </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-kingbase"> <img alt="人大金仓" src="http://cdn.anyline.org/img/logo/kingbase.png" width="100" />人大金仓 </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-opengauss"> <img alt="OpenGauss" src="http://cdn.anyline.org/img/logo/opengauss.png" width="100" />OpenGauss </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-neo4j"> <img alt="Neo4j" src="http://cdn.anyline.org/img/logo/neo4j.webp" width="100" />Neo4j </a> 
<a style="display:inline-block;" href="https://gitee.com/anyline/anyline-simple/tree/master/anyline-simple-data-jdbc-dialect/anyline-simple-data-jdbc-highgo"> <img alt="瀚高" src="http://cdn.anyline.org/img/logo/hgdb.webp" width="100" />瀚高 </a> 
<a style="display:inline-block;" href=""> <img alt="南大通用" src="http://cdn.anyline.org/img/logo/gbase.jpg" width="100" />南大通用 </a> 
<a style="display:inline-block;" href=""> <img alt="cassandra" src="http://cdn.anyline.org/img/logo/cassandra.png" width="100" />cassandra </a> 
<a style="display:inline-block;" href=""> <img alt="oceanbase" src="http://cdn.anyline.org/img/logo/oceanbase.webp" width="100" />oceanbase </a> 
<a style="display:inline-block;" href=""> <img alt="神舟通用" src="http://cdn.anyline.org/img/logo/oscar.png" width="100" />神舟通用 </a> 
<a style="display:inline-block;" href=""> <img alt="polardb" src="http://cdn.anyline.org/img/logo/polardb.webp" width="100" />polardb </a> 
<a style="display:inline-block;" href=""> <img alt="questdb" src="http://cdn.anyline.org/img/logo/questdb.png" width="100" />questdb </a> 
<a style="display:inline-block;" href=""> <img alt="timescale" src="http://cdn.anyline.org/img/logo/timescale.svg" width="100" />timescale </a> 
<a style="display:inline-block;" href=""> <img alt="海量数据" src="http://cdn.anyline.org/img/logo/vastdata.png" width="100" />Vastbase(海量数据)</a> 
<a style="display:inline-block;" href=""> <img alt="恒生电子" src="http://cdn.anyline.org/img/logo/lightdb.png" width="100" />LightDB(恒生电子)</a> 
<a style="display:inline-block;" href=""> <img alt="万里数据库" src="http://cdn.anyline.org/img/logo/greatdb.png" width="100" />greatdb(万里数据库)</a> 
<a style="display:inline-block;" href=""> <img alt="云和恩墨" src="http://cdn.anyline.org/img/logo/mogdb.png" width="100" />mogdb(云和恩墨)</a> 
<a style="display:inline-block;" href=""> <img alt="中兴GoldenDB" src="http://cdn.anyline.org/img/logo/zte.webp" width="100"/>GoldenDB(中兴)</a> 
<a style="display:inline-block;" href=""> <img alt="GaiaDB-X" src="http://cdn.anyline.org/img/logo/bdy.jpg" />GaiaDB-X(百度云)</a> 
<a style="display:inline-block;" href=""> <img alt="TiDB" src="http://cdn.anyline.org/img/logo/tidb.svg" width="100" />TiDB</a> 
<a style="display:inline-block;" href=""> <img alt="AntDB" src="http://cdn.anyline.org/img/logo/antdb.png" width="100" />AntDB(亚信)</a>
<a style="display:inline-block;" href=""> <img alt="citus" src="http://cdn.anyline.org/img/logo/citus.png" width="100"/>citus</a> 
<a style="display:inline-block;" href=""> <img alt="TDSQL" src="http://cdn.anyline.org/img/logo/tencent.ico" />TDSQL(TBase)(腾讯云)</a> 
<a style="display:inline-block;" href=""> <img alt="磐维数据库" src="http://cdn.anyline.org/img/logo/10086.png" width="100"/>磐维数据库(中国移动)</a> 
<a style="display:inline-block;" href=""> <img alt="中国联通cudb" src="http://cdn.anyline.org/img/logo/chinaunicom.png" width="100"/>CUDB(中国联通)</a> 
<a style="display:inline-block;" href=""> <img alt="沐融信息科技" src="http://cdn.anyline.org/img/logo/murongtech.png" width="100"/>MuDB(沐融)</a> 
<a style="display:inline-block;" href=""> <img alt="北京酷克数据" src="http://cdn.anyline.org/img/logo/hashdata.png" width="100"/>HashData(酷克)</a> 
<a style="display:inline-block;" href=""> <img alt="热璞" src="http://cdn.anyline.org/img/logo/hotdb.png" width="100"/>HotDB(热璞)</a> 
<a style="display:inline-block;" href=""> <img alt="优炫" src="http://cdn.anyline.org/img/logo/uxdb.png" width="100"/>UXDB(优炫)</a> 
<a style="display:inline-block;" href=""> <img alt="星环" src="http://cdn.anyline.org/img/logo/kundb.png" width="100"/>KunDB(星环)</a> 
<a style="display:inline-block;" href=""> <img alt="StarDB" src="http://cdn.anyline.org/img/logo/stardb.png" width="100"/>StarDB(京东)</a> 
<a style="display:inline-block;" href=""> <img alt="YiDB" src="http://cdn.anyline.org/img/logo/yidb.png" width="100"/>YiDB(天翼数智)</a> 
<a style="display:inline-block;" href=""> <img alt="UbiSQL" src="http://cdn.anyline.org/img/logo/ubisql.webp" width="100"/>UbiSQL(平安科技)</a> 
<a style="display:inline-block;" href=""> <img alt="华胜信泰" src="http://cdn.anyline.org/img/logo/xigemadb.jpg" width="100"/>xigemaDB(华胜信泰)</a> 
<a style="display:inline-block;" href=""> <img alt="星瑞格" src="http://cdn.anyline.org/img/logo/sinodb.png" width="100"/>SinoDB(星瑞格)</a> 
<a style="display:inline-block;" href=""> <img alt="CockroachDB" src="http://cdn.anyline.org/img/logo/cockroachdb.png" width="100"/>CockroachDB</a> 
<a style="display:inline-block;" href=""> <img alt="InfluxDB" src="http://cdn.anyline.org/img/logo/influxdata.svg" width="100"/>InfluxDB</a> 
<a style="display:inline-block;" href=""> <img alt="Informix" src="http://cdn.anyline.org/img/logo/informix.webp" width="100"/>Informix</a> 
<a style="display:inline-block;" href=""> <img alt="MongoDB" src="http://cdn.anyline.org/img/logo/mongodb.svg" width="100"/>MongoDB</a> 
<a style="display:inline-block;" href=""> <img alt="MogoDB" src="http://cdn.anyline.org/img/logo/mogdb.png" width="100"/>MogoDB</a> 
<a style="display:inline-block;" href=""> <img alt="RethinkDB" src="http://cdn.anyline.org/img/logo/rethinkdb.png" width="100"/>RethinkDB</a> 
<a style="display:inline-block;" href=""> <img alt="SAP HANA" src="http://cdn.anyline.org/img/logo/hana.png" width="100"/>SAP HANA</a> 

没有示例的看这个目录下有没有 [【anyline-data-jdbc-dialect】](https://gitee.com/anyline/anyline/tree/master/anyline-data-jdbc-dialect)还没有的请联系群管理员
