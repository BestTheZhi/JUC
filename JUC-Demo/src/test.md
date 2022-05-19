# MyBatis

![1](https://images.weserv.nl/?url=raw.githubusercontent.com/BestTheZhi/images/master/FrameWork/1.png)

## 一、简介

### 1、什么是MyBatis?

- MyBatis 是一款优秀的**持久层**框架。
- 它支持自定义 SQL、存储过程以及高级映射。
- MyBatis 免除了几乎所有的 JDBC 代码以及设置参数和获取结果集的工作。
- MyBatis 可以通过简单的 XML 或注解来配置和映射原始类型、接口和 Java POJO（Plain Old Java Objects，普通老式 Java 对象 即实体类）为数据库中的记录。
- MyBatis 本是apache的一个开源项目iBatis, 2010年这个项目由apache software foundation 迁移到了google code，并且改名为MyBatis 。2013年11月迁移到Github。



### 2、如何获取MyBatis？

- Maven仓库

```xml
<dependency>
  <groupId>org.mybatis</groupId>
  <artifactId>mybatis</artifactId>
  <version>3.5.4</version>
</dependency>
```

- Github : https://github.com/mybatis/mybatis-3
  - release (下载) : https://github.com/mybatis/mybatis-3/releases
- 中文文档 : https://mybatis.org/mybatis-3/zh/index.html



### 3、相关介绍

- 持久化
  - 数据持久化就是将程序的数据在瞬时状态转化为持久状态的过程
  - 因为内存是**断电即失**的
  - 实现持久化:数据库(Jdbc) , io文件
- 持久层
  - 完成持久化工作的代码块
  - 它的层界限十分明显 (即基本用不到其他方面的知识)



### 4、MyBatis的好处

- 简单易学：本身就很小且简单。没有任何第三方依赖，最简单安装只要两个jar文件+配置几个sql映射文件易于学习，易于使用，通过文档和源代码，可以比较完全的掌握它的设计思路和实现。
- 灵活：mybatis不会对应用程序或者数据库的现有设计强加任何影响。 sql写在xml里，便于统一管理和优化。通过sql语句可以满足操作数据库的所有需求。
- 解除sql与程序代码的耦合：通过提供DAO层，将业务逻辑和数据访问逻辑分离，使系统的设计更清晰，更易维护，更易单元测试。sql和代码的分离，提高了可维护性。
- 提供映射标签，支持对象与数据库的orm字段关系映射
- 提供对象关系映射标签，支持对象关系组建维护
- 提供xml标签，支持编写动态sql。



## 二、MyBatis的使用

### 1、数据库环境搭建

```sql
create database `mybatis`;

use `mybatis`;

drop table if exists `user`;
create table `user`(
  `id` int(20) not null AUTO_INCREMENT,
  `name` varchar(30) default null,
  `pwd` varchar(30) default null,
  primary key(`id`)
)engine=innodb default charset=utf8;

insert into `user`(`id`,`name`,`pwd`) values
(1,'等风',`123456`),
(2,'小狗',`123456`),
(3,'志神',`123456`)

/* 编写代码时 注意区分  `code1`  'code2' */
```



### 2、创建IDEA项目 (父工程)

>一个父工程 (MyBatis)  删除 src 目录  在其下创建许多子模块

- 新建一个普通的maven项目
- 导入相关的maven依赖

```xml
<dependencies>
  <!-- mysql驱动 -->
  <dependency>
    <groupId>mysql</groupId>
    <artifactId>mysql-connector-java</artifactId>
    <version>8.0.25</version>
  </dependency>

  <!-- MyBatis -->
  <dependency>
    <groupId>org.mybatis</groupId>
    <artifactId>mybatis</artifactId>
    <version>3.5.4</version>
  </dependency>

  <!-- junit -->
  <dependency>
    <groupId>junit</groupId>
    <artifactId>junit</artifactId>
    <version>4.12</version>
    <scope>test</scope>
  </dependency>
  
  <dependency>
    <groupId>org.projectlombok</groupId>
    <artifactId>lombok</artifactId>
    <version>1.18.6</version>
  </dependency>
</dependencies>

<!-- 资源过滤 是的src下的配置文件可以被加载出来 -->
<build>
  <resources>
    <resource>
      <directory>src/main/java</directory>
      <includes>
        <include>**/*.xml</include>
        <include>**/*.properties</include>
      </includes>
    </resource>
    <resource>
      <directory>src/main/resources</directory>
      <includes>
        <include>*.xml</include>
        <include>*.properties</include>
      </includes>
    </resource>
  </resources>
</build>
```



### 3、 MyBatis入门

>模块一： mybatis-01

#### a. mybatis 核心配置文件

>XML 配置文件中包含了对 MyBatis 系统的核心设置，包括获取数据库连接实例的数据源（DataSource）以及决定事务作用域和控制方式的事务管理器（TransactionManager）。

```xml
<!-- mybatis-config.xml -->

<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">

<!-- configuration核心配置文件 -->
<configuration>
    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="com.mysql.cj.jdbc.Driver"/>
                <property name="url" value="jdbc:mysql:///mybatis?useUnicode=true&amp;characterEncoding=UTF-8&amp;useSSL=FALSE&amp;serverTimezone=UTC"/>
                <property name="username" value="root"/>
                <property name="password" value="123456"/>
            </dataSource>
        </environment>
    </environments>

    <!-- 每一个Mapper.xml都需要在MyBatis核心配置文件中注册! -->
    <mappers>
      	<!--resource 资源路径 用 / -->
        <mapper resource="com/ZHILIU/repository/UserMapper.xml"/>
    </mappers>


</configuration>
```



#### b. 编写工具类MyBatisUtils

>每个基于 MyBatis 的应用都是以一个 SqlSessionFactory 的实例为核心的。SqlSessionFactory 的实例可以通过 SqlSessionFactoryBuilder 获得。而 SqlSessionFactoryBuilder 则可以从 XML 配置文件或一个预先配置的 Configuration 实例来构建出 SqlSessionFactory 实例。

>有了 SqlSessionFactory，我们可以从中获得 SqlSession 的实例。SqlSession 提供了在数据库执行 SQL 命令所需的所有方法。可以通过 SqlSession 实例来直接执行已映射的 SQL 语句。

```java
//SqlSessionFactory  -->  SqlSession
public class MyBatisUtils {

    private static SqlSessionFactory sqlSessionFactory;

    static {
        String resource = "mybatis-config.xml";
        try {
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SqlSession getSqlSession(){
        return sqlSessionFactory.openSession();
    }

}
```

#### c. 实体类User

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private long id;
    private String name;
    private String pwd;
}
```

#### d. UserRepository

>接口repository中定义 实体类与对应数据库表 中的业务规范

```java
public interface UserRepository {
    List<User> getUserList();
}
```

#### e. UserMapper.xml

>对接repository 映射SQL语句

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Mapper 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">

<!-- namespace=绑定一个对应的repository/Dao/Mapper接口 -->
<mapper namespace="com.ZHILIU.repository.UserRepository">
  
  	<!-- id 绑定repository 中的方法 -->
    <!-- resultType 为返回实体类的全类名-->
    <select id="getUserList" resultType="com.ZHILIU.entity.User">
        select * from user
    </select>
  
</mapper>
```

#### f. 测试

```java
@Test
    public void test1(){
        //1.获取sqlSession对象
        SqlSession sqlSession = MyBatisUtils.getSqlSession();

        //2.获取MaBatis实例化的repository接口对象
        UserRepository userRepository = sqlSession.getMapper(UserRepository.class);
        //3.映射器实例 执行
        List<User> userList = userRepository.getUserList();

//        List<Object> objects = sqlSession.selectList("com.ZHILIU.repository.UserRepository.getUserList");
//        List<User> userList = sqlSession.selectList("com.ZHILIU.repository.UserRepository.getUserList");

        System.out.println(userList);
        
        //4.关闭SqlSession
        sqlSession.close();

    }
```



> **注意**：1.Mapper不注册 抛异常:org.apache.ibatis.binding.BindingException: Type interface com.ZHILIU.repository.UserRepository is not known to the MapperRegistry.
>
> 2.异常 : Could not find resource com/ZHILIU/repository/UserMapper.xml  是资源过滤问题，IDEA没有加载src下的xml文件，要在pom.xml <build></bulid> 中添加相关配置。

```xml
<resources>
  <resource>
    <directory>src/main/java</directory>
    <includes>
      <include>**/*.xml</include>
      <include>**/*.properties</include>
    </includes>
  </resource>
  <resource>
    <directory>src/main/resources</directory>
    <includes>
      <include>*.xml</include>
      <include>*.properties</include>
    </includes>
  </resource>
</resources>
```

#### g. 总结

- mybatis-config.xml 核心配置文件 配置核心设置 以及 注册Mapper
- User实体类 与数据库中表对应
- UserRepository/UserMapper 接口定义与数据库交互时的规范 (映射语句)。
- UserMapper.xml中绑定UserMapper接口 及 用标签与Mapper中的映射语句绑定并实现sql
- MyBatisUtils 中获取 SqlSession

##### SqlSessionFactoryBuilder

 	这个类可以被实例化、使用和丢弃，一旦创建了 SqlSessionFactory，就不再需要它了。 因此 SqlSessionFactoryBuilder 实例的最佳作用域是方法作用域（也就是局部方法变量）。

##### SqlSessionFactory

​	SqlSessionFactory 一旦被创建就应该在应用的运行期间一直存在，不要重复创建多次。因此 SqlSessionFactory 的最佳作用域是应用作用域。最简单的就是使用**单例模式**或者**静态单例模式**。

##### SqlSession

​	每个线程都应该有它自己的 SqlSession 实例。SqlSession 的实例不是线程安全的，因此是不能被共享。**每次收到 HTTP 请求，就可以打开一个 SqlSession，返回一个响应后，就关闭它。** 这个关闭操作很重要，为了确保每次都能执行关闭操作，你应该把这个关闭操作放到 finally 块中。 下面的示例就是一个确保 SqlSession 关闭的标准模式：

```java
try (SqlSession session = sqlSessionFactory.openSession()) {
  // 你的应用逻辑代码
}
```

#### h. 理解

SqlSessionFactoryBulider.bulid(InputStream ins)  --->  SqlSessionFactory

SqlSessionFactory.openSession  ---> SqlSession

SqlSession.getMapper(Repository.class)  --> userMapper (映射器实例)

映射器是一些绑定映射语句的接口。映射器接口的实例是从 SqlSession 中获得的。

在Mapper.xml中绑定映射器，并且在核心配置文件中注册Mapper就可以通过SqlSeeion.getMapper(Repository.class)获取接口的实现类 (映射器实例)。

获得了 映射器实例 就可以直接调用该接口实例中的方法与数据库进行交互。



## 三、CRUD操作

UserMapper.xml

```java
public interface UserRepository {
    //获取全部用户
    List<User> getUserList();

    //按照id查询用户
    User getUserById(long id);

    //添加一个用户
    int addUser(User user);

    //修改用户
    int updateUser(User user);

    //删除用户
    int deleteById(long id);
}
```



#### 1、select   查询语句

```xml
<select id="getUserList" resultType="com.ZHILIU.entity.User">
  select * from user;
</select>

<select id="getUserById" parameterType="long" resultType="com.ZHILIU.entity.User">
  select * from user where id = #{id};
</select>
```

#### 2、insert  添加语句

```xml
<!-- insert update delete 返回值都是int类型  就不用写-->
<insert id="addUser" parameterType="com.ZHILIU.entity.User">
  insert into user(id,name,pwd) values(#{id},#{name},#{pwd});
</insert>
```

#### 3、update 修改语句

```xml
<update id="updateUser" parameterType="com.ZHILIU.entity.User">
  update user set name = #{name}, pwd = #{pwd} where id = #{id};
</update>
```

#### 4、delete  删除语句

```xml
<delete id="deleteById" parameterType="long" >
  delete from user where id = #{id};
</delete>
```



**.xml中属性**:

- namespace中绑定Mapper接口，即要与接口名相同!


- id : 为绑定接口中的方法名。
- resultType ：SQL语句的返回值。

>insert update delete 返回值都是int类型  就不用写

- parameterType : 为参数类型。 **单个参数不写也可以自动映射**



**注意** : 增删改需要提交事务 sqlSession.commit();



#### 5、"万能Map"

>Map 的 别名为 map ,  paramType="map"

假如，我们实体类或者数据库中表、字段、或者参数过多，我们可以考虑使用map

```java
//万能Map
int addUser1(Map<String,Object> map);

//万能Map
int updateUser1(Map<String,Object> map);
```

```xml
<!-- 用map传递key -->
<insert id="addUser1" parameterType="map">
  insert into user (id, name)
  values (#{userId},#{userName});
</insert>

<update id="updateUser1" parameterType="map">
  update user set name = #{userName} where id = #{userId};
</update>
```

```java
Map<String, Object> map = new HashMap<>();
map.put("userId",5);
map.put("userName","GodZii");

System.out.println(userMapper.addUser1(map));
System.out.println(userMapper.updateUser1(map));
```

Map传递参数，直接在sql中取出key值。

对象传递参数，直接在sql中取出对象的属性。

只有一个基本类型参数的情况下，直接在sql中取到，或者可以省略paramType。

多个参数就用Map，或者注解。



#### 6、模糊查询

```java
//模糊查询
List<User> getUserLike(String value);
```

```xml
<!-- 模糊查询  -->
<select id="getUserLike" parameterType="String" resultType="com.ZHILIU.entity.User">
select * from user where name like "%"#{value}"%";
</select>
```

```java
System.out.println(userMapper.getUserLike("小"));
```

- 可以再java代码执行的时候，就加上通配符 "%小%" 。
- 也可以在sql拼接中使用通配符 "%"#{value}"%" .

>思考防止sql注入!



## 四、XML配置解析

>模块二： mybatis-02

### 1、核心配置文件(mybatis-config.xml)

MyBatis 的配置文件包含了会深深影响 MyBatis 行为的设置和属性信息。

配置文档的顶层结构如下：

- configuration（配置）

  - **properties（属性）**
  - settings（设置）
  - **typeAliases（类型别名）**
  - typeHandlers（类型处理器）
  - objectFactory（对象工厂）
  - plugins（插件）
  - environments（环境配置）

    - environment（环境变量）
      - transactionManager（事务管理器）
      - dataSource（数据源）
  - databaseIdProvider（数据库厂商标识）
  - mappers（映射器）



### 2、环境配置(environments)

MyBatis 可以配置成适应多种环境。

不过要记住：尽管可以配置多个环境，但每个 SqlSessionFactory 实例只能选择一种环境。

在 <environments default="default"> 中选择默认环境。

MyBatis 默认的事务管理器是JDBC ( 知道有个MANAGED)。

数据源默认是连接池 POOLED  (有个UNPOOLED、JNDI)。

### 3、属性(properties)

可以通过properties属性来实现引用配置文件

这些属性可以在外部进行配置，并可以进行动态替换。既可以在典型的 Java 属性文件【.properties】中配置这些属性，也可以在 properties 元素的子元素中设置。

>db.properties

```properties
driver=com.mysql.cj.jdbc.Driver
url=jdbc:mysql:///mybatis?useUnicode=true&characterEncoding=UTF8&useSSL=FALSE&serverTimezone=UTC
username=root
password=123456
```

> 在 mybatis-config.xml 中属性的顺序
>
> The content of element type "configuration" must match "(properties?,settings?,typeAliases?,typeHandlers?,objectFactory?,objectWrapperFactory?,reflectorFactory?,plugins?,environments?,databaseIdProvider?,mappers?)".

mybatis-config.xml

```xml
<!--  引入外部配置文件  -->
<properties resource="db.properties">

</properties>

<environments default="development">
  <environment id="development">
    <transactionManager type="JDBC"/>
    <dataSource type="POOLED">
      <property name="driver" value="${driver}"/>
      <property name="url" value="${url}"/>
      <property name="username" value="${username}"/>
      <property name="password" value="${password}"/>
    </dataSource>
  </environment>
</environments>
```

- 可以直接引入外部文件
- 可以再其中增加一些属性配置
- 如果有标签中的属性与配置文件中的属性字段相同，则外部配置文件优先



### 4、类型别名 (typeAlianses)

类型别名可为 Java 类型设置一个缩写名字，降低冗余的全限定类名书写。

```xml
<!--可以给实体类起别名-->
<typeAliases>
  <typeAlias type="com.ZHILIU.entity.User" alias="User"/>
</typeAliases>
```

可也以指定一个包名，MyBatis会在包名下面搜索需要的Java Bean

这些实体类的默认别名就是这个类的类名，首字母小写。若有注解，则别名为其注解值。

```xml
<typeAliases>
  <package name="com.ZHILIU.entity"/>
</typeAliases>
```

通过注解起别名 (扫描包)

```java
@Alias("User")
public class User{
  //
}
```

> Java 类型内建的类型别名
>
> https://mybatis.org/mybatis-3/zh/configuration.html#typeAliases



###5、设置（settings）

>https://mybatis.org/mybatis-3/zh/configuration.html#settings

- cacheEnabled
- lazyLoadingEnabled
- logImpl



### 6、其他配置 (*了解)

- typeHandlers（类型处理器）
- objectFactory（对象工厂）
- plugins（插件）
  - mybatis-generator-core
  - mybatis-plus
  - 通用mapper



### 7、映射器 (mappers)

MapperRegistry：注册绑定Mapper文件；

**方式一**：

```xml
<mappers>
    <mapper resource="com/ZHILIU/repository/UserMapper.xml"/>
</mappers>
```

方式二：使用class文件绑定注册

```xml
<mappers>
  <mapper class="com.ZHILIU.repository.UserMapper"/>
</mappers>
```

方式三：使用扫描包进行注入绑定

```xml
<mappers>
  <package name="com.ZHILIU.repository"/>
</mappers>
```



注意：(方式二和方式三)

- 接口和它的Mapper配置文件必须同名。
- 接口和它的Mapper配置文件必须在同一个包下。





## 五、初识ResultMap

> 模块三：mabatis-03

> https://mybatis.org/mybatis-3/zh/sqlmap-xml.html#Result_Maps



#### 实体类属性名 和 数据库表字段名不一致

User

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User {
    private long id;
    private String name;
    private String password;
}
```

数据库 User表中字段  id , name ,pwd

查询结果   :    User ( id=1, name=等风, password=null )



解决方法：

- 起别名

```xml
<select id="getUserById" parameterType="long" resultType="com.ZHILIU.entity.User">
  select id,name,pwd as password from user where id = #{id};
</select>
```

- **使用ResultMap**

```xml
<!-- 结果集映射 -->
<resultMap id="userMap" type="User">
  <!-- column为数据库表中的字段 property为实体类中的属性 -->
  		<!-- 相同不需要映射 -->
  <!--        <result column="id" property="id"/>-->
  <!--        <result column="name" property="name"/>-->
  <result column="pwd" property="password"/>
</resultMap>

<select id="getUserById" resultMap="userMap">
  select * from user where id = #{id};
</select>
```



如果这个世界总是这么简单就好了......



## 六、日志

如果一个数据库操作，出现了异常，我们需要排错。日志就是最好的助手！

sout 、debug

现在可以用 日志工厂 ！

**logImpl** ：指定 MyBatis 所用日志的具体实现，未指定时将自动查找。

- SLF4J
- **LOG4J(deprecated since 3.5.9)**
- LOG4J2
- JDK_LOGGING
- COMMONS_LOGGING
- **STDOUT_LOGGING**
- NO_LOGGING



在MyBatis中具体使用哪个日志实现，在设置中设定。



### 6.1、日志工厂

>模块四：mybatis-04

**STDOUT_LOGGING**

```xml
<settings>
  <!-- 标准的日志工厂实现  -->
  <setting name="logImpl" value="STDOUT_LOGGING"/>
</settings>
```

sout

```tex
Opening JDBC Connection
Created connection 599984672.
Setting autocommit to false on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@23c30a20]
==>  Preparing: select * from user where id = ?; 
==> Parameters: 1(Long)
<==    Columns: id, name, pwd
<==        Row: 1, 等风, 123456
<==      Total: 1
User(id=1, name=等风, pwd=123456)
Resetting autocommit to true on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@23c30a20]
Closing JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@23c30a20]
Returned connection 599984672 to pool.
```



### 6.2、Log4j

- Log4j是Apache的一个开源项目，通过使用Log4j，我们可以控制日志信息输送的目的地是控制台、文件、GUI组件。
- 我们也可以控制每一条日志的输出格式。
- 通过定义每一条日志信息的级别，我们能够更加细致地控制日志的生成过程。
- 可以通过一个配置文件来灵活地进行配置，而不需要修改应用的代码。



1.导入maven依赖

```xml
<!-- https://mvnrepository.com/artifact/log4j/log4j -->
<dependency>
    <groupId>log4j</groupId>
    <artifactId>log4j</artifactId>
    <version>1.2.17</version>
</dependency>
```

2.log4j.propertis

```properties
# 将等级为DEBUG的日执信息输出到console和file，console和file的定义在下
log4j.rootLogger=debug,console ,file

# 控制台输出相关配置
log4j.appender.console=org.apache.log4j.ConsoleAppender
log4j.appender.console.Target=System.out
log4j.appender.console.Threshold=DEBUG
log4j.appender.console.layout=org.apache.log4j.PatternLayout
log4j.appender.console.layout.ConversionPattern=[%c]-%m%n

# 文件输出相关配置
log4j.appender.file=org.apache.log4j.FileAppender
log4j.appender.file.File=./logs/ZHILIU.log
log4j.appender.file.Append=true
#log4j.appender.file.MaxFileSize=1mb
log4j.appender.file.Threshold=DEBUG
log4j.appender.file.layout=org.apache.log4j.PatternLayout
log4j.appender.file.layout.ConversionPattern=[ %p ] %-d{yyyy-MM-dd HH:mm:ss} [ %t ] - %m%n

# 日志输出级别
log4j.logger.org.apache=DEBUG
log4j.logger.java.sql=DEBUG
log4j.logger.java.sql.Connection=DEBUG
log4j.logger.java.sql.Statement=DEBUG
log4j.logger.java.sql.PreparedStatement=DEBUG
log4j.logger.java.sql.ResultSet=DEBUG
```

3.mybatis-config.xml

```xml
<settings>
  <!-- 标准的日志工厂实现  -->
  <!--        <setting name="logImpl" value="STDOUT_LOGGING"/>-->
  <setting name="logImpl" value="LOG4J"/>
</settings>
```

4.测试 sout

```tex
[org.apache.ibatis.logging.LogFactory]-Logging initialized using 'class org.apache.ibatis.logging.log4j.Log4jImpl' adapter.
[org.apache.ibatis.logging.LogFactory]-Logging initialized using 'class org.apache.ibatis.logging.log4j.Log4jImpl' adapter.
[org.apache.ibatis.datasource.pooled.PooledDataSource]-PooledDataSource forcefully closed/removed all connections.
[org.apache.ibatis.datasource.pooled.PooledDataSource]-PooledDataSource forcefully closed/removed all connections.
[org.apache.ibatis.datasource.pooled.PooledDataSource]-PooledDataSource forcefully closed/removed all connections.
[org.apache.ibatis.datasource.pooled.PooledDataSource]-PooledDataSource forcefully closed/removed all connections.
[org.apache.ibatis.transaction.jdbc.JdbcTransaction]-Opening JDBC Connection
[org.apache.ibatis.datasource.pooled.PooledDataSource]-Created connection 589273327.
[org.apache.ibatis.transaction.jdbc.JdbcTransaction]-Setting autocommit to false on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@231f98ef]
[com.ZHILIU.repository.UserMapper.getUserById]-==>  Preparing: select * from user where id = ?; 
[com.ZHILIU.repository.UserMapper.getUserById]-==> Parameters: 1(Long)
[com.ZHILIU.repository.UserMapper.getUserById]-<==      Total: 1
User(id=1, name=等风, pwd=123456)
[org.apache.ibatis.transaction.jdbc.JdbcTransaction]-Resetting autocommit to true on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@231f98ef]
[org.apache.ibatis.transaction.jdbc.JdbcTransaction]-Closing JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@231f98ef]
[org.apache.ibatis.datasource.pooled.PooledDataSource]-Returned connection 589273327 to pool.
```

5.log4j 日志文件 .txt

```tex
[ DEBUG ] 2022-01-12 11:47:53 [ main ] - Logging initialized using 'class org.apache.ibatis.logging.log4j.Log4jImpl' adapter.
[ DEBUG ] 2022-01-12 11:47:53 [ main ] - Logging initialized using 'class org.apache.ibatis.logging.log4j.Log4jImpl' adapter.
[ DEBUG ] 2022-01-12 11:47:53 [ main ] - PooledDataSource forcefully closed/removed all connections.
[ DEBUG ] 2022-01-12 11:47:53 [ main ] - PooledDataSource forcefully closed/removed all connections.
[ DEBUG ] 2022-01-12 11:47:53 [ main ] - PooledDataSource forcefully closed/removed all connections.
[ DEBUG ] 2022-01-12 11:47:53 [ main ] - PooledDataSource forcefully closed/removed all connections.
[ DEBUG ] 2022-01-12 11:47:53 [ main ] - Opening JDBC Connection
[ DEBUG ] 2022-01-12 11:47:54 [ main ] - Created connection 87060781.
[ DEBUG ] 2022-01-12 11:47:54 [ main ] - Setting autocommit to false on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@530712d]
[ DEBUG ] 2022-01-12 11:47:54 [ main ] - ==>  Preparing: select * from user where id = ?; 
[ DEBUG ] 2022-01-12 11:47:54 [ main ] - ==> Parameters: 1(Long)
[ DEBUG ] 2022-01-12 11:47:54 [ main ] - <==      Total: 1
[ DEBUG ] 2022-01-12 11:47:54 [ main ] - Resetting autocommit to true on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@530712d]
[ DEBUG ] 2022-01-12 11:47:54 [ main ] - Closing JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@530712d]
[ DEBUG ] 2022-01-12 11:47:54 [ main ] - Returned connection 87060781 to pool.
```



**简单使用：**

Test.java

```java
import org.apache.log4j.Logger;

public class UserMapperTest {

    static Logger logger = Logger.getLogger(UserMapperTest.class);

    @Test
    public void testLog4j(){
        //日志级别
        logger.info("info : testLog4j()...");
        logger.debug("debug : testLog4j()...");
        logger.error("error : testLog4j()...");
    }
}
```

console.sout

```tex
[com.ZHILIU.repository.UserMapperTest]-info : testLog4j()...
[com.ZHILIU.repository.UserMapperTest]-debug : testLog4j()...
[com.ZHILIU.repository.UserMapperTest]-error : testLog4j()...
```

日志文件.log

```tex
[ INFO ] 2022-01-12 11:56:40 [ main ] - info : testLog4j()...
[ DEBUG ] 2022-01-12 11:56:40 [ main ] - debug : testLog4j()...
[ ERROR ] 2022-01-12 11:56:40 [ main ] - error : testLog4j()...
```



## 七、分页

分页是为了减少数据的处理量

### 使用Limit分页

语法：select * from user limit  startIndex , pageSize;

1.UserMap

```java
//分页
List<User> getUserLimit(Map<String,Integer> map);
```

2.UserMapper.xml

```xml
<select id="getUserLimit" resultType="User" parameterType="map">
  select * from user limit #{startIndex},#{pageSize};
</select>
```

3.@Test

```java
@Test
public void testLimit(){
  SqlSession sqlSession = MyBatisUtils.getSqlSession();
  UserMapper userMapper = sqlSession.getMapper(UserMapper.class);
  
  Map<String,Integer> map = new HashMap<>();
  map.put("startIndex",1);
  map.put("pageSize",3);
  
  List<User> userLis = userMapper.getUserLimit(map);
  System.out.println(userLis);

  sqlSession.close();
}
```



### 使用RowBounds实现（*了解）

UserMapper

```java
//分页rowBounds
List<User> getUserByRowBounds();
```

UserMapper.xml

```xml
<select id="getUserByRowBounds" resultType="User">
  select * from user;
</select>
```

@Test

```java
@Test
public void testRowBounds(){
  SqlSession sqlSession = MyBatisUtils.getSqlSession();
  UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

  RowBounds rowBounds = new RowBounds(1,3);
  List<User> userList = sqlSession.selectList("com.ZHILIU.repository.UserMapper.getUserByRowBounds", null, rowBounds);
  System.out.println(userList);

  sqlSession.close();

}
```



### MyBatis 分页插件 PageHelper（*了解）



## 八、注解开发

> 模块五：mybatis-05

​	使用注解来映射简单语句会使代码显得更加简洁，并且不需要Mapper.xml，但对于稍微复杂一点的语句，Java 注解不仅力不从心，还会让你本就复杂的 SQL 语句更加混乱不堪。 因此，如果你需要做一些很复杂的操作，最好用 XML 来映射语句。



- 可以再工具类创建的时候实现自动提交事务

```java
public class MyBatisUtils {

    private static SqlSessionFactory sqlSessionFactory;

    static {
        String resource = "mybatis-config.xml";
        try {
            InputStream inputStream = Resources.getResourceAsStream(resource);
            sqlSessionFactory = new SqlSessionFactoryBuilder().build(inputStream);
        } catch (IOException e) {
            e.printStackTrace();
        }
    }

    public static SqlSession getSqlSession(){
      	//实现自动提交事务
        return sqlSessionFactory.openSession(true);
    }

}
```



UserMapper

```java
public interface UserMapper {

  @Select("select * from user")
  List<User> getUserList();

  //一个参数@param可以省略,建议加上
  @Select("select * from user where id = #{uid}")
  User getUserById(@Param("uid") long id);

  //方法存在多个基本类型的参数 **必须** 加上 @param 注解
  @Select("select * from user where id = #{id} and name = #{name}")
  User getUser(@Param("id") long id,@Param("name") String name);

  @Insert("insert into user(id,name,pwd) value(#{id},#{name},#{pwd})")
  int addUser(User user);

  @Update("update user set name=#{name},pwd=#{pwd} where id = #{id}")
  int updateUser(User user);

  @Delete("delete from user where id = #{id}")
  int deleteUser(long id);

}
```

Test

```java
@Test
public void test(){
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  UserMapper userMapper = sqlSession.getMapper(UserMapper.class);

  //        System.out.println(userMapper.getUserList());

  //        System.out.println(userMapper.getUserById(1L));

  //        System.out.println(userMapper.getUser(1L,"等风"));

  //        userMapper.addUser(new User(7,"小志","191919"));

  //        System.out.println(userMapper.updateUser(new User(7, "小智", "191919")));

  System.out.println(userMapper.deleteUser(7L));

  sqlSession.close();
}
```



一定要将接口注册到mybatis-config.xml中

```xml
<mappers>
  <mapper class="com.ZHILIU.repository.UserMapper"/>
</mappers>
```



## 九、复杂条件查询

>模块六：mybatis-06

### 9.1、数据库环境搭建

```sql
drop table if exists `teacher`;
CREATE TABLE `teacher` (
  `id` INT(10) NOT NULL,
  `name` VARCHAR(30) DEFAULT NULL,
  PRIMARY KEY (`id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;

INSERT INTO teacher(`id`, `name`) VALUES (1, '秦老师'); 

drop table if exists `student`;
CREATE TABLE `student` (
  `id` INT(10) NOT NULL,
  `name` VARCHAR(30) DEFAULT NULL,
  `tid` INT(10) DEFAULT NULL,
  PRIMARY KEY (`id`),
  KEY `fktid` (`tid`),
  CONSTRAINT `fktid` FOREIGN KEY (`tid`) REFERENCES `teacher` (`id`)
) ENGINE=INNODB DEFAULT CHARSET=utf8;


INSERT INTO `student` (`id`, `name`, `tid`) VALUES ('1', '小明', '1'); 
INSERT INTO `student` (`id`, `name`, `tid`) VALUES ('2', '小红', '1'); 
INSERT INTO `student` (`id`, `name`, `tid`) VALUES ('3', '小张', '1'); 
INSERT INTO `student` (`id`, `name`, `tid`) VALUES ('4', '小李', '1'); 
INSERT INTO `student` (`id`, `name`, `tid`) VALUES ('5', '小王', '1');
```

### 9.2、IDEA项目中环境

1.实体类 Student，Teacher

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Student {
    private long id;
    private String name;
    //学生tid(外键)关联老师主键id
    private Teacher teacher;
}
```

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Teacher {
    private long id;
    private String name;
    //老师对应多个学生
    private List<Student> students;
}
```

2.Mapper接口 StudentMapper，TeacherMapper

3.xml映射文件 StudentMapper.xml，TeacherMapper.xml

4.mybatis-config.xml  ,  db.properties

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE configuration
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-config.dtd">

<configuration>

    <properties resource="db.properties"/>
    
    <settings>
        <setting name="logImpl" value="STDOUT_LOGGING"/>
    </settings>

    <typeAliases>
        <package name="com.ZHILIU.entity"/>
    </typeAliases>

    <environments default="development">
        <environment id="development">
            <transactionManager type="JDBC"/>
            <dataSource type="POOLED">
                <property name="driver" value="${driver}"/>
                <property name="url" value="${url}"/>
                <property name="username" value="${username}"/>
                <property name="password" value="${password}"/>
            </dataSource>
        </environment>
    </environments>

    <mappers>
        <package name="com.ZHILIU.repository"/>
    </mappers>

</configuration>
```



### 9.3、多对一查询案例

--- 查询所有学生的信息，以及对应老师的信息

```java
public interface StudentMapper {
    //查询所有学生的信息，以及对应老师的信息
    public List<Student> getStudentList1();

    //查询所有学生的信息，以及对应老师的信息
    public List<Student> getStudentList2();
}
```



#### A.按照结果嵌套处理

>联表查询

```xml
<select id="getStudentList1" resultMap="studentList1">
  select s.id as sid,s.name as sname,t.id as tid,t.name as tname
  from student s,teacher t
  where s.tid = t.id;
</select>
<resultMap id="studentList1" type="Student">
  <!-- id是主键 也可换成result标签-->
  <id column="sid" property="id"/>
  <!-- column是查询结果中的列名,property是Java Bean中属性  -->
  <result column="sname" property="name"/>
  <!-- 复杂的属性，对象用association 集合用collection
             javaType="" 指定属性的类型
             ofType="" 集合中的泛型信息     -->
  <association property="teacher" javaType="Teacher">
    <id column="tid" property="id"/>
    <result column="tname" property="name"/>
  </association>
</resultMap>
```

@Test

```java
@Test
public void testStudentMapper(){
  //已经设置自动提交事务
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  StudentMapper mapper = sqlSession.getMapper(StudentMapper.class);

  List<Student> studentList = mapper.getStudentList2();
  for(Student s : studentList ){
    System.out.println(s);
  }

  sqlSession.close();
}
```

sout

```tex
==>  Preparing: select s.id as sid,s.name as sname,t.id as tid,t.name as tname 					from student s,teacher t 
				where s.tid = t.id; 
==> Parameters: 
<==    Columns: sid, sname, tid, tname
<==        Row: 1, 小明, 1, 秦老师
<==        Row: 2, 小红, 1, 秦老师
<==        Row: 3, 小张, 1, 秦老师
<==        Row: 4, 小李, 1, 秦老师
<==        Row: 5, 小王, 1, 秦老师
<==      Total: 5
Student(id=1, name=小明, teacher=Teacher(id=1, name=秦老师, students=null))
Student(id=2, name=小红, teacher=Teacher(id=1, name=秦老师, students=null))
Student(id=3, name=小张, teacher=Teacher(id=1, name=秦老师, students=null))
Student(id=4, name=小李, teacher=Teacher(id=1, name=秦老师, students=null))
Student(id=5, name=小王, teacher=Teacher(id=1, name=秦老师, students=null))
```



#### B.按照查询嵌套处理

> 子查询

```xml
<!--  1.先查出学生信息
      2.根据查询出来的学生的tid，查询对应的老师 -->
<select id="getStudentList2" resultMap="studentList2">
  select * from student;
</select>
<resultMap id="studentList2" type="Student">
  <result property="id" column="id"/>
  <result property="name" column="name"/>
  <association property="teacher" column="tid" javaType="Teacher" select="getTeacherById"/>
</resultMap>

<select id="getTeacherById" resultType="Teacher">
  select * from teacher where id = #{id};
</select>
```

如下写法是**错误**的

```xml
<select id="getStudentList2" resultMap="studentList2">
  select * from student;
</select>
<resultMap id="studentList2" type="Student">
  <result property="id" column="id"/>
  <result property="name" column="name"/>
  <!--  @Select("select * from teacher where id = #{tid}")
              Teacher getTeacherById(@Param("tid") long id);        -->
  <association property="teacher" column="tid" javaType="Teacher" select="getTeacherById"/>
</resultMap>

<!--如上写法是不行的
    Exception: Mapped Statements collection does not contain value for com.ZHILIU.repository.StudentMapper.getTeacherById
    getTeacherById  只能写在它自己的xml文件中 如下：
    -->
```

>​	不要以为TeacherMapper.xml中有 getTeacherById 的查询方法就以为可以直接使用，直接报异常它只能在自己的配置文件中找方法。



sout (不用管students=null 没有映射而已)

```tex
==>  Preparing: select * from student; 
==> Parameters: 
<==    Columns: id, name, tid
<==        Row: 1, 小明, 1
====>  Preparing: select * from teacher where id = ?; 
====> Parameters: 1(Integer)
<====    Columns: id, name
<====        Row: 1, 秦老师
<====      Total: 1
<==        Row: 2, 小红, 1
<==        Row: 3, 小张, 1
<==        Row: 4, 小李, 1
<==        Row: 5, 小王, 1
<==      Total: 5
Student(id=1, name=小明, teacher=Teacher(id=1, name=秦老师, students=null))
Student(id=2, name=小红, teacher=Teacher(id=1, name=秦老师, students=null))
Student(id=3, name=小张, teacher=Teacher(id=1, name=秦老师, students=null))
Student(id=4, name=小李, teacher=Teacher(id=1, name=秦老师, students=null))
Student(id=5, name=小王, teacher=Teacher(id=1, name=秦老师, students=null))
```



### 9.4、一对多查询案例

--- 获取指定老师下的所有学生及老师信息

```java
public interface TeacherMapper {
    //获取指定老师下的所有学生及老师信息
    Teacher getTeacher1(@Param("tid") long id);

    //获取指定老师下的所有学生及老师信息
    Teacher getTeacher2(@Param("tid") long id);
}
```



#### A.按照结果嵌套处理

```xml
<!-- 按照结果嵌套查询 -->
<select id="getTeacher1" resultMap="teacherInfo1">
  select s.id sid,s.name sname,t.name tname,t.id tid
  from student s,teacher t
  where s.tid = t.id and t.id = #{tid}
</select>

<resultMap id="teacherInfo1" type="Teacher">
  <result property="id" column="tid"/>
  <result property="name" column="tname"/>
  <!-- 复杂的属性，对象用association 集合用collection
             javaType="" 指定属性的类型
             ofType="" 集合中的泛型信息     -->
  <collection property="students" ofType="Student">
    <result property="id" column="sid"/>
    <result property="name" column="sname"/>
  </collection>
</resultMap>
```

@Test

```java
@Test
public void testTeacherMapper(){
  //已经设置自动提交事务
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  TeacherMapper mapper = sqlSession.getMapper(TeacherMapper.class);

  Teacher teacher = mapper.getTeacher1(1L);
  System.out.println("Teacher["+teacher.getId()+"---"+teacher.getName()+"]");
  List<Student> students = teacher.getStudents();
  for(Student s : students){
    System.out.println(s);
  }

  sqlSession.close();
}
```

sout

```tex
==>  Preparing: select s.id sid,s.name sname,t.name tname,t.id tid 
	 			from student s,teacher t 
	  			where s.tid = t.id and t.id = ? 
==> Parameters: 1(Long)
<==    Columns: sid, sname, tname, tid
<==        Row: 1, 小明, 秦老师, 1
<==        Row: 2, 小红, 秦老师, 1
<==        Row: 3, 小张, 秦老师, 1
<==        Row: 4, 小李, 秦老师, 1
<==        Row: 5, 小王, 秦老师, 1
<==      Total: 5
Teacher[1---秦老师]
Student(id=1, name=小明, teacher=null)
Student(id=2, name=小红, teacher=null)
Student(id=3, name=小张, teacher=null)
Student(id=4, name=小李, teacher=null)
Student(id=5, name=小王, teacher=null)
```



#### B.按照查询嵌套处理

```xml
<!-- 按照查询嵌套处理 -->
<select id="getTeacher2" resultMap="teacherInfo2">
  select * from teacher where id = #{tid};
</select>

<resultMap id="teacherInfo2" type="Teacher">
  <id column="id" property="id"/>
  <result column="name" property="name"/>
  <collection property="students" column="id" ofType="Student" select="getStudentList"/>
</resultMap>

<select id="getStudentList" resultType="Student" >
  select * from student where tid = #{tid};
</select>
```



sout

```tex
==>  Preparing: select * from teacher where id = ?; 
==> Parameters: 1(Long)
<==    Columns: id, name
<==        Row: 1, 秦老师
====>  Preparing: select * from student where tid = ?; 
====> Parameters: 1(Integer)
<====    Columns: id, name, tid
<====        Row: 1, 小明, 1
<====        Row: 2, 小红, 1
<====        Row: 3, 小张, 1
<====        Row: 4, 小李, 1
<====        Row: 5, 小王, 1
<====      Total: 5
<==      Total: 1
Teacher[1---秦老师]
Student(id=1, name=小明, teacher=null)
Student(id=2, name=小红, teacher=null)
Student(id=3, name=小张, teacher=null)
Student(id=4, name=小李, teacher=null)
Student(id=5, name=小王, teacher=null)
```



## 十、动态SQL

>模块七：mybatis-07

[动态SQL](https://mybatis.org/mybatis-3/zh/dynamic-sql.html)

动态SQL就是指根据不同的条件生成不同的SQL语句。

- if
- choose (when, otherwise)
- trim (where, set)
- foreach



---- 数据库环境搭建

```sql
drop table if exists `blog`;
CREATE TABLE `blog`(
`id` INT(20) NOT NULL AUTO_INCREMENT,
`title` VARCHAR(100) NOT NULL,
`author` VARCHAR(30) NOT NULL,
`create_time` DATETIME NOT NULL,
`views` INT(30) NOT NULL,
PRIMARY KEY (`id`)
)ENGINE=INNODB DEFAULT CHARSET=utf8;
```



--- IDEA项目中环境

Blog.java

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class Blog {
    private long id;
    private String title;
    private String author;
    //数据库表中字段是create_time
    //在mybatis-config.xml中开启驼峰命名自动映射
    private Date createTime;
    private long views;
}
```

mybatis-config.xml

```xml
<settings>
  <setting name="logImpl" value="STDOUT_LOGGING"/>
  <!-- 开启驼峰命名自动映射 -->
  <setting name="mapUnderscoreToCamelCase" value="true"/>
</settings>
```

>mapUnderscoreToCamelCase
>
>是否开启驼峰命名自动映射，即从经典数据库列名 A_COLUMN 映射到经典 Java 属性名 aColumn。
>
>true | **false**



BolgMapper.xml

```xml
public interface BlogMapper {

    @Insert("insert into blog (title,author,create_time,views)" +
            "values(#{title},#{author},#{createTime},#{views})")
    int addBlog(Blog blog);

}
```

插入数据

```java
@Test
public void testInsert(){
  //已经设置自动提交事务
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  BlogMapper mapper = sqlSession.getMapper(BlogMapper.class);
  Blog blog = new Blog();

  blog.setTitle("Mybatis");
  blog.setAuthor("是风动");
  blog.setCreateTime(new Date());
  blog.setViews(9999);
  mapper.addBlog(blog);

  blog.setTitle("Java");
  blog.setAuthor("是幡动");
  blog.setViews(1000);
  mapper.addBlog(blog);

  blog.setTitle("Spring");
  blog.setAuthor("莫敛此轻寒");
  mapper.addBlog(blog);

  blog.setTitle("微服务");
  blog.setAuthor("莫敛此轻寒");
  mapper.addBlog(blog);

  sqlSession.close();

  sqlSession.close();
}
```



### IF

BlogMapper.java

```xml
//测试IF
//参数也可以是Blog blog
List<Blog> getBlogIF(Map<String,String> map);
```

BlogMapper.xml

```xml
<select id="getBlogIF" resultType="Blog">
  select * from blog where 1=1
  
  <!-- 没有1=1的话 where后面直接跟and 会报错
       又或者后面所有条件都不成立 where后面什么都没有  
       所以要配合其他标签使用     -->
  <if test="author != null">
    and author = "%"#{author}"%"
  </if>
  <if test="title != null">
    and title = #{title}
  </if>
</select>
```

@Test

```java
@Test
public void testIF(){
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  BlogMapper mapper = sqlSession.getMapper(BlogMapper.class);

  Map<String,String> map = new HashMap<>();
  map.put("title","微服务");
  map.put("author","轻寒");

  System.out.println(mapper.getBlogIF(map));

  sqlSession.close();
}
```



### where

> *where* 元素只会在子元素返回任何内容的情况下才插入 “WHERE” 子句。而且，若子句的开头为 “AND” 或 “OR”，*where* 元素也会将它们去除。

```xml
<select id="getBlogIF" resultType="Blog">
  select * from blog
  <where>
    <if test="author != null">
      author like "%"#{author}"%"
    </if>
    <if test="title != null">
      and title = #{title}
    </if>
  </where>
</select>
```

### set

> *set* 元素会动态地在行首插入 SET 关键字，并会删掉额外的逗号

```xml
//测试set
int updateBlogSet(Map<String,String> map);
```

```xml
<update id="updateBlogSet"  parameterType="map">
  update blog
  <set>
    <if test="title != null">
      title = #{title},
    </if>
    <if test="author != null">
      author = #{author},
    </if>
    <if test="views != null">
      view = #{view};
    </if>
  </set>
  where id = #{id};
</update>
```

```java
@Test
public void testSet(){
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  BlogMapper mapper = sqlSession.getMapper(BlogMapper.class);

  Map<String,String> map = new HashMap<>();
  //        map.put("title","微服务");
  map.put("author","不是风动");
  map.put("id","3");

  System.out.println(mapper.updateBlogSet(map));

  sqlSession.close();
}
```



### trim

> trim 标签中的 prefix 和 suffix 属性会被⽤于⽣成实际的 SQL 语句，会和标签内部的语句进⾏拼接，如果语句前后出现了 prefixOverrides 或者 suffixOverrides 属性中指定的值，MyBatis 框架会⾃动将其删除。

- 和 *where* 元素等价的自定义 trim 元素为：

``` xml
<trim prefix="WHERE" prefixOverrides="AND |OR ">
  ...
</trim>
```

*prefixOverrides* 属性会忽略通过管道符分隔的文本序列（注意此例中的空格是必要的）。上述例子会移除所有 *prefixOverrides* 属性中指定的内容，并且插入 *prefix* 属性中指定的内容。

- 与 *set* 元素等价的自定义 *trim* 元素为：

```xml
<trim prefix="SET" suffixOverrides=",">
  ...
</trim>
```

覆盖了后缀值设置，并且自定义了前缀值。



### choose,when,otherwise

> 有时候，我们不想使用所有的条件，而只是想从多个条件中选择   **一个**  使用。针对这种情况，MyBatis 提供了 choose 元素，它有点像 Java 中的 switch 语句。

BlogMapper.java

```java
//测试choose
List<Blog> getBlogChoose(Map<String,String> map);
```

BlogMapper.xml

```xml
<select id="getBlogChoose" resultType="Blog">
  select * from blog where
  <choose>
    <when test="title != null">
      title = #{title}
    </when>
    <when test="author != null">
      uthor = #{author}
    </when>
    <otherwise>
      views > 2000
    </otherwise>
  </choose>
</select>
```

@Test

```java
@Test
public void testChoose(){
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  BlogMapper mapper = sqlSession.getMapper(BlogMapper.class);

  Map<String,String> map = new HashMap<>();
  //多个参数也只选择一个
  map.put("title","微服务");
  map.put("author","是风动");

  System.out.println(mapper.getBlogChoose(map));

  sqlSession.close();
}
```

sout

```tex
==>  Preparing: select * from blog where title = ? 
==> Parameters: 微服务(String)
<==    Columns: id, title, author, create_time, views
<==        Row: 4, 微服务, 莫敛此轻寒, 2022-01-13 09:38:52, 9999
<==      Total: 1
[Blog(id=4, title=微服务, author=莫敛此轻寒, createTime=Thu Jan 13 17:38:52 CST 2022, views=9999)]
```



### foreach

>动态 SQL 的另一个常见使用场景是对集合进行遍历（尤其是在构建 IN 条件语句的时候）。
>
>select * from blog where id in (1,2,3);



```java
//测试foreach
List<Blog> getBlogForeach(@Param("ids") List<Integer> ids);
```

>不加 @Param("ids")
>
>org.apache.ibatis.binding.BindingException:
>
> Parameter 'ids' not found. Available parameters are [collection, list]
>
>也可以使用map：
>
>List<Blog> getBlogForeach(Map map);
>
>map.put("ids" , new ArrayList<Integer>())
>
>就可以直接取到 ids



```xml
<!--  select * from blog where id in (1,2,3)  -->
<select id="getBlogForeach" resultType="Blog">
  select * from blog
  <where>
    <foreach collection="ids" item="id" open="id in (" close=")" separator=",">
      #{id}
    </foreach>
  </where>
</select>
```

```java
@Test
public void testForeach(){
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  BlogMapper mapper = sqlSession.getMapper(BlogMapper.class);

  List<Integer> ids = new ArrayList<>();
  ids.add(1);
  ids.add(2);
  System.out.println(mapper.getBlogForeach(ids));

  sqlSession.close();
}
```







### SQL片段

可以将一些功能的部分抽取出来，方便复用

```xml
<sql id="if-author-title">
  <if test="author != null">
    author like "%"#{author}"%"
  </if>
  <if test="title != null">
    and title = #{title}
  </if>
</sql>

<select id="getBlogIF" resultType="Blog">
  select * from blog
  <where>
    <include refid="if-author-title"></include>
  </where>
</select>
```

注意：

- 最好基于单表来定义SQL片段
- 不要存在where标签



## 十一、MyBatis 缓存

> 模块八：mybatis-08

​	使⽤MyBatis缓存可以**减少 Java 应⽤与数据库的交互次数，从⽽提升程序的运⾏效率**。⽐如查询出 id = 1 的对象，第⼀次查询出之后会⾃动将该对象保存到缓存中，当下⼀次查询时，直接从缓存中取出对象即可，⽆需再次访问数据库。



### 一级缓存

> SqlSession 级别，默认开启，并且不能关闭

​	操作数据库时需要创建 SqlSession 对象，在对象中有⼀个 HashMap ⽤于存储缓存数据，不同的SqlSession 之间缓存数据区域是互不影响的。sqlSession关闭，缓存也随着消失。

​	⼀级缓存的作⽤域是 SqlSession 范围的，当在同⼀个 SqlSession 中执⾏两次相同的 SQL 语句事，第⼀次执⾏完毕会将结果保存到缓存中，第⼆次查询时直接从缓存中获取。

​	需要注意的是，如果 SqlSession 执⾏了 DML 操作（insert、update、delete），MyBatis 自动将缓存清空以保证数据的准确性。也可以手动清理缓存 sqlSession.clearCache();



UserMapper.java

```java
public interface UserMapper {

    @Select("select * from user where id = #{id}")
    User getUserById(long id);

    @Update("update user set name = #{name},pwd=#{pwd} where id = #{id}")
    int updateUser(User user);

}
```



@Test  //直接取两次

```java
@Test
public void test1(){
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  UserMapper mapper = sqlSession.getMapper(UserMapper.class);

  System.out.println(mapper.getUserById(1));

  System.out.println("-----------------------");

  System.out.println(mapper.getUserById(1));

  sqlSession.close();
}
```

sout

```tex
Opening JDBC Connection
Created connection 1456339771.
Setting autocommit to false on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@56cdfb3b]
==>  Preparing: select * from user where id = ? 
==> Parameters: 1(Long)
<==    Columns: id, name, pwd
<==        Row: 1, 等风, 123456
<==      Total: 1
User(id=1, name=等风, pwd=123456)
-----------------------
User(id=1, name=等风, pwd=123456)
Resetting autocommit to true on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@56cdfb3b]
Closing JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@56cdfb3b]
Returned connection 1456339771 to pool.
```



@Test  //中间执行DML语句，缓存清除

```java
@Test
public void test1(){
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  UserMapper mapper = sqlSession.getMapper(UserMapper.class);

  System.out.println(mapper.getUserById(1));

  System.out.println("-----------------------");

  mapper.updateUser(new User(5L,"GodZ","123456"));
  
  System.out.println("-----------------------");

  System.out.println(mapper.getUserById(1));

  sqlSession.close();
}
```

sout

```tex
Opening JDBC Connection
Created connection 900636745.
Setting autocommit to false on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@35aea049]
==>  Preparing: select * from user where id = ? 
==> Parameters: 1(Long)
<==    Columns: id, name, pwd
<==        Row: 1, 等风, 123456
<==      Total: 1
User(id=1, name=等风, pwd=123456)
-----------------------
==>  Preparing: update user set name = ?,pwd=? where id = ? 
==> Parameters: GodZ(String), 123456(String), 5(Long)
<==    Updates: 1
-----------------------
==>  Preparing: select * from user where id = ? 
==> Parameters: 1(Long)
<==    Columns: id, name, pwd
<==        Row: 1, 等风, 123456
<==      Total: 1
User(id=1, name=等风, pwd=123456)
Rolling back JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@35aea049]
Resetting autocommit to true on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@35aea049]
Closing JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@35aea049]
Returned connection 900636745 to pool.
```



### 二级缓存

> Mapper 级别，默认关闭，可以开启。

​	使⽤⼆级缓存时，多个 SqlSession 使⽤同⼀个 Mapper 的 SQL 语句操作数据库，得到的数据会存在⼆级缓存区，同样是使⽤ HashMap 进⾏数据存储，相⽐较于⼀级缓存，⼆级缓存的范围更⼤，多个SqlSession 可以共⽤⼆级缓存，⼆级缓存是跨 SqlSession 的。

​	⼆级缓存是多个 SqlSession 共享的，其作⽤域是 Mapper 的同⼀个 namespace，不同的 SqlSession两次执⾏相同的 namespace 下的 SQL 语句，参数也相等，**则当第⼀个sqlsession提交或关闭将数据保存到⼆级缓存中之后**，第⼆次可直接从⼆级缓存中取出数据（即会话关闭了，一级缓存存入二级缓存）。



mybatis-config.xml  配置开启MyBatis ⾃带的⼆级缓存

```xml
<settings>
  <setting name="logImpl" value="STDOUT_LOGGING"/>
  <!-- 开启⼆级缓存  -->
  <setting name="cacheEnabled" value="true"/>
</settings>
```

UserMapper.xml

```xml
<?xml version="1.0" encoding="UTF-8" ?>
<!DOCTYPE mapper
        PUBLIC "-//mybatis.org//DTD Config 3.0//EN"
        "http://mybatis.org/dtd/mybatis-3-mapper.dtd">
<mapper>

    <cache/>

</mapper>
```

实体类实现序列化接⼝

```java
@Data
@NoArgsConstructor
@AllArgsConstructor
public class User implements Serializable {
    private long id;
    private String name;
    private String pwd;
}
```



@Test

```java
@Test
public void test1(){
  SqlSession sqlSession = MyBatisUtils.getSqlSession();

  UserMapper mapper = sqlSession.getMapper(UserMapper.class);
  User user1 = mapper.getUserById(1);
  System.out.println(user1);

  sqlSession.close();

  System.out.println("---------------------");

  sqlSession = MyBatisUtils.getSqlSession();

  mapper = sqlSession.getMapper(UserMapper.class);
  User user2 = mapper.getUserById(1);
  System.out.println(user2);

  System.out.println("---------------------");
  System.out.println("user1 == user2" + (user1 == user2));

  sqlSession.close();
}
```

sout

```tex
Opening JDBC Connection
Created connection 1150963491.
Setting autocommit to false on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@449a4f23]
==>  Preparing: select * from user where id = ?; 
==> Parameters: 1(Long)
<==    Columns: id, name, pwd
<==        Row: 1, 等风, 123456
<==      Total: 1
User(id=1, name=等风, pwd=123456)
Resetting autocommit to true on JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@449a4f23]
Closing JDBC Connection [com.mysql.cj.jdbc.ConnectionImpl@449a4f23]
Returned connection 1150963491 to pool.
---------------------
Cache Hit Ratio [com.ZHILIU.repository.UserMapper]: 0.5
User(id=1, name=等风, pwd=123456)
---------------------
user1 == user2false
```

> User实现序列化可能是导致 user1==user2:false 的原因

查询是先看二级缓存 在看一级缓存 最后查询数据库



### Ehcache二级缓存

Maven依赖

```xml
<dependency>
 <groupId>org.mybatis</groupId>
 <artifactId>mybatis-ehcache</artifactId>
 <version>1.0.0</version>
</dependency>
<dependency>
 <groupId>net.sf.ehcache</groupId>
 <artifactId>ehcache-core</artifactId>
 <version>2.4.3</version>
</dependency>
```

添加Ehcache.xml

```xml
<ehcache xmlns:xsi="http://www.w3.org/2001/XMLSchema-instance"
         xsi:noNamespaceSchemaLocation="../config/ehcache.xsd">
  <diskStore/>
  <defaultCache
                maxElementsInMemory="1000"
                maxElementsOnDisk="10000000"
                eternal="false"
                overflowToDisk="false"
                timeToIdleSeconds="120"
                timeToLiveSeconds="120"
                diskExpiryThreadIntervalSeconds="120"
                memoryStoreEvictionPolicy="LRU">
  </defaultCache>
</ehcache>
```

mybatis-config.xml 配置开启⼆级缓存

```xml
<settings>
 <!-- 开启⼆级缓存 -->
 <setting name="cacheEnabled" value="true"/>
</settings>
```

Mapper.xml 中配置⼆级缓存

```xml
<cache type="org.mybatis.caches.ehcache.EhcacheCache">
  <!-- 缓存创建之后，最后⼀次访问缓存的时间⾄缓存失效的时间间隔 -->
  <property name="timeToIdleSeconds" value="3600"/>
  <!-- 缓存⾃创建时间起⾄失效的时间间隔 -->
  <property name="timeToLiveSeconds" value="3600"/>
  <!-- 缓存回收策略，LRU表示移除近期使⽤最少的对象 -->
  <property name="memoryStoreEvictionPolicy" value="LRU"/>
</cache>
```

实体类不需要实现序列化接口





