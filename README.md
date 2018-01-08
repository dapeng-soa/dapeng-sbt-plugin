# sbt-dapeng 插件使用指南

标签（空格分隔）： 未分类

目录：
    > 1. sbt-dapeng插件能干什么
    > 2. sbt-dapeng插件怎么用
    >   2.1 使用sbt-dapeng快速启动测试hello项目
    >   2.2 自定义自己的项目
    > 3. sbt-dapeng提供的指令详细说明
    >   3.1 ```sbt new```
    >   3.2 ```sbt docker```
    >   3.3 ```compile``` (内置了thrift生成功能)
    >   3.4 ```sbt runContainer```
    >   3.5 ```sbt test```

### sbt-dapeng插件能干什么
    sbt-dapeng 插件提供了以下几个功能
> 1. 一个指令(sbt new xxx)：即可生成一个服务代码
> 2. 一个指令(sbt compie)：即可基于thrift文件生成所需的接口以及实体对象
> 3. 一个指令(sbt docker)：即可构建docker镜像
> 4. 一个指令(sbt runContainer)：即可运行服务项目
> 5. 一个指令(sbt test)： 即可测试服务接口

### sbt-dapeng 插件怎么用
#### 测试环境需求（后续会升级到正式版）:
1. 确保java 的版本为 1.8
2. 确保scala的版本为 2.12.2 或以上
3. 确保sbt 的版本为  1.0.0 或以上
4. 下载isuwang-soa项目: 在根目录执行 ```mvn clean install```
5. 下载sbt-dapeng项目: 在master 分支, 执行 ```sbt publishLocal```
ps: 最好安装一下 sbt 代理，能加速sbt 的下载依赖以及构建:  ```https://github.com/Centaur/repox```

#### 1. 使用sbt-dapeng快速启动测试hello项目
下面我们来看一下如何使用sbt-dapeng来快速测试Hello项目

##### 1.1.  创建项目
首先，cd到你要创建项目的目录，(以~/dev/github 为例)
然后执行 ```sbt new isuwang/dapeng-soa.g8``` , 会出现以下画面, 填入对应参数即可创建HelloWorld拉
```sh
> sbt new isuwang/dapeng-soa.g8
[info] Loading settings from idea.sbt ...
[info] Loading global plugins from /yourHomePath/.sbt/1.0/plugins
[info] Set current project to github (in build file:/yourProjectPath/)

this is a template to genreate dapeng api service

name [api]: bbq     # yourProjectName
version ["0.1-SNAPSHOT"]:  # yourProjectVersion
scalaVersion ["2.12.2"]:   # yourScalaVersion
organization [com.isuwang]: # your project GroupId
resources [resources]:      # defaultValue, no need to modify
api [HelloWorld-api]:           # defaultTemplateValue, no need to modify
service [HelloWorld-service]:  # defaultTemplateValue, no need to modify
java [java]:                    # defaultTemplateValue, no need to modify
scala [scala]:                  # defaultTemplateValue, no need to modify
docker [docker]:
servicePackage [com.isuwang.soa]: # default root package modify

Template applied in ./helloworld
```

<small>`isuwang/dapeng-soa.g8` 是一个基于giter8 创建的模版项目，开发人员无需关心，只需要知道你创建的项目是基于该模版创建的即可。如果想进一步了解，请查看github：[isuwang/dapeng-soa.g8](https://github.com/isuwang/dapeng-soa.g8)
dapeng-soa.g8 内置了一个样例服务（HelloService）, 开发可以参考</small>

##### 1.2. 编译创建好的模板项目
cd到项目根目录 ```cd helloWorld``` 执行 ```sbt compile```
```sh
> sbt compile
[info] Loading settings from idea.sbt ...
[info] Loading global plugins from /Users/jackliang/.sbt/1.0/plugins
[info] Loading settings from build.sbt,plugins.sbt ...
[info] Loading project definition from /Users/jackliang/dev/github/bbq/project
[info] Loading settings from build.sbt ...
[info] Set current project to sbt-idlc (in build file:/Users/jackliang/dev/github/bbq/)
[info] Executing in batch mode. For better performance use sbt 's  shell
scrooge:-gen java -all -in /Users/jackliang/dev/github/bbq/bbq-api/src/main/resources/thrifts -out /Users/jackliang/dev/github/bbq/bbq-api/src/main
scrooge:-gen scala -all -in /Users/jackliang/dev/github/bbq/bbq-api/src/main/resources/thrifts -out /Users/jackliang/dev/github/bbq/bbq-api/src/main
[success] Total time: 1 s, completed 2017-11-11 16:38:46
```

##### 1.3 构建项目镜像
执行 ```sbt docker```, 我们会得到如下镜像:
docker.oa.isuwang.com:5000/product/bbq_service:latest
```sh
> sbt docker
[info] Step 1/7 : FROM docker.oa.isuwang.com:5000/system/dapeng-container:1.2.1
[info]  ---> 73df0043a78c
[info] Step 2/7 : RUN mkdir -p /dapeng-container
[info]  ---> Using cache
[info]  ---> ee2e23a17ebd
[info] Step 3/7 : COPY 0/bbq_service-assembly-0.1-SNAPSHOT.jar /dapeng-container/apps
[info]  ---> b99e8cd2bbc5
[info] Removing intermediate container 1efd6666d8d9
[info] Step 4/7 : COPY 1/startup.sh /dapeng-container/bin/
[info]  ---> 911fa73a3791
[info] Removing intermediate container c1b0bb2efdb4
[info] Step 5/7 : RUN chmod +x /dapeng-container/bin/startup.sh
[info]  ---> Running in bbc571506fdd
[info]  ---> 7823dab2c923
[info] Removing intermediate container bbc571506fdd
[info] Step 6/7 : WORKDIR /dapeng-container/bin
[info]  ---> 2481076bf248
[info] Removing intermediate container 83b73c25edbf
[info] Step 7/7 : CMD /bin/sh -c /dapeng-container/bin/startup.sh && tail -F /dapeng-container/bin/startup.sh
[info]  ---> Running in 65fba4c6e7ca
[info]  ---> 828797c3ec01
[info] Removing intermediate container 65fba4c6e7ca
[info] Successfully built 828797c3ec01
[info] Tagging image 828797c3ec01 with name: docker.oa.isuwang.com:5000/product/bbq_service:latest
```
1.4 启动镜像
常规的启动镜像是配置 dc-all.yml文件，然后配置dc-local.yml巴拉巴拉..., 现在为了快速启动镜像，提供一个docker run 指令来启动镜像, 如果想快速测试可以使用该方法: (记得修改一下你的Ip，文件路径,端口)

```sh
docker run -p yourIp:8999:8999 --name="bbq" -e soa_container_port=8999 -e soa_container_ip=yourIp -e soa_service_port=8999 -e soa_service_ip=yourIp -e soa_zookeeper_host=yourIp:2181 -e LANG=zh_CN.UTF-8 --env-file /yourWorkPath/kscompose/.envs/application.env --add-host soa_zookeeper:yourIp  --add-host db-master:yourIp docker.oa.isuwang.com:5000/product/bbq_service:latest
```
启动完镜像，我们再启动一下zookeeper, mysql
然后我们 ```docker ps ``` 一下, 可以看到bbq已经启动了
``` sh
PORTS                          NAMES
yourIp:8999->8999/tcp          bbq
0.0.0.0:2181->2181/tcp         zookeeper
yourIp:3306->3306/tcp          mysql
```


1.5 测试
条件: 需要服务端项目执行一下 ```sbt publishLocal```
    (ps: 目的是为了拿到服务api的版本)

新建一个项目，
添加依赖: (以前面创建的 hello项目为例, 添加hello-api 以及客户端调用所需的dapeng包)
```java
<dependencies>
        <dependency>
            <groupId>com.isuwang</groupId>
            <artifactId>hello-api_2.12</artifactId>
            <version>0.1-SNAPSHOT</version>
        </dependency>
        <dependency>
            <groupId>com.isuwang</groupId>
            <artifactId>dapeng-remoting-api</artifactId>
            <version>1.2.1</version>
        </dependency>
        <dependency>
            <groupId>com.isuwang</groupId>
            <artifactId>dapeng-core</artifactId>
            <version>1.2.1</version>
        </dependency>
        <dependency>
            <groupId>com.isuwang</groupId>
            <artifactId>dapeng-remoting-netty</artifactId>
            <version>1.2.1</version>
        </dependency>
        <dependency>
            <groupId>com.isuwang</groupId>
            <artifactId>dapeng-registry-zookeeper</artifactId>
            <version>1.2.1</version>
        </dependency>

        <dependency>
            <groupId>com.isuwang</groupId>
            <artifactId>dapeng-spring</artifactId>
            <version>1.2.1</version>
            <exclusions>
                <exclusion>
                    <groupId>org.springframework</groupId>
                    <artifactId>spring-context</artifactId>
                </exclusion>
            </exclusions>
        </dependency>

    </dependencies>
```
写个main方法
```scala
object TestClient2 {

  def main(args: Array[String]): Unit = {
    val hello = HelloServiceClient.sayHello("test")
    println(s" hello : ${hello}")
  }
}
```
就可以看到正常返回了. 至此整个 开发 -> 编译 -> 启动 -> 测试流程结束

1.5.1 测试模版
我们还提供了一个客户端测试模版. 意不意外？惊不惊喜？
只需要执行 ```sbt new isuwang/dapeng-soa-client.g8``` 就能把项目下下来，然后执行```ClientTest``` 的 ```main``` 方法 就能看到测试结果了
<small>（ps： 同上需要注意build.sbt的api依赖版本与你 publishLocal 后生成的api版本要一致）</small>