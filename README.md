bin: 和hdfs， yarn, mapred

本地hadoop100：数据存储在linux本地 -> 几乎不用，测试偶尔用

伪分布hadoop101：数据存储在HDFS -> 公司比较差钱

完全分布式：

hadoop102，hadoop103，hadoop104，数据存储在HDFS，多台服务器工作 -> 企业里大量使用

## 完全分布式运行模式（开发重点）

1. 准备3台客户机
2. 安装JDK
3. 配置环境变量
4. 安装Hadoop
5. 配置环境变量



编写集群分发脚本xsync

1. scp（secure copy) 安全拷贝

   1. scp定义

      scp可以实现服务器与服务器之间的数据拷贝 （from server1 to server2)

   2. 基本语法

      `scp    -r   $pdir/$fname          $user@$host:$pdir/$fname`

      命令   递归   要拷贝的文件路径/名称    目的地用户@主机：目的地路径/名称

      从hadoop102复制文件到103

      从102操作

      `scp -r /opt/module/hadoop-3.1.3 awdawd@hadoop103:/opt/module/`

      从103操作

      `scp -r awdawd@hadoop102:/opt/module/hadoop-3.1.3 /opt/module/`

      从103操作：令数据从102拷贝到104

      `scp -r awdawd@hadoop102:/opt/module/* awdawd@hadoop104:/opt/module/`

2. rsync 同步工具

   1. rsync主要用于备份和镜像，具有速度快、避免复制和相同内容和支持符号链接的有点

      rsync和scp的区别：用rsync做文件的复制要比scp速度快，rsync只对差异文件做更新、scp是把所有文件都复制过去

   2. 基本语法

      `rsync -av       $pdir/$fname         $user@$host:$pdir/$fname`

      命令   选项参数   要拷贝的文件路径/名称    目的地用户@主机：目的地路径/名称

      `-a` 归档拷贝

      `-v` 显示复制过程

   3. 例

      1. 删除hadoop103中的wcinput

      2. 同步hadoop102中的/opt/module/hadoop-3.1.3到hadoop103

          `rsync -av /opt/module/hadoop-3.1.3 awdawd@hadoop103:/opt/module/hadoop-3.1.3`

3. xsync 集群分发脚本

   1. 需求：循环赋值文件到所有节点的相同目录下

   2. 需求分析

      1. rsync命令原始拷贝：

         `rsync -av /opt/module awdawd@hadoop103:/opt`

      2. 期望脚本：

         xsync要同步的文件名称

         期望脚本在任何路径都能使用（脚本放在声明了全局环境变量的路径）

         ```shell
         #!/bin/bash
         
         #1. 判斷參數個數
         if [$# -lt 1]
         then
           echo Not Enough Argument!
           exit;
         fi
         
         #2. 遍歷集群所有機器
         for host in hadoop102 hadoop103 hadoop104
         do
           echo =====================  $host =====================
           #3. 遍歷所有目錄，挨個發送
         
           for file in $@
             do
               #4. 判斷文件是否存在
               if [-e $file]
               then
                 #5. 獲取父目錄
                 pdir=$(cd -P $ (dirname $file); pwd)
         
                 #6. 獲取當前文件名稱
                 fname=$(basename $file)
                 ssh $host "mkdir -p $pdir"
                 rsync -av $pdir/$fname $host:$pdir
               else
                 echo $file does not exists!
               fi
           done
         done
         ```

免密登錄原理

A服務器               B服務器

1）ssh-key-gen

生成密鑰對

公鑰（A）

私鑰（A）

生成公鑰和私鑰：

```shell
ssh-keygen -t rsa
```

然後三次回車

複製公鑰到hadoop103

```shell
ssh-copy-id hadoop103
```

hadoop集群配置

1. 集群部署規劃

   注意

   1. NameNode和SecondaryNameNode不要安裝在同一臺服務器，因爲它們都比較耗内存

   2. ResourceManager也很消耗内存，不要和NameNode、SecondaryNameNode配置在同一臺機器上

      <img src="./images/image-20230110110358651.png" alt="image-20230110110358651" style="zoom: 80%;" />

2. 配置文件説明

   hadoop配置文件分兩類：默認配置文件和自定義配置文件，只有用戶想修改某一默認配置時，才需要修改自定義配置文件，更改相應屬性值。

   1. 默認配置文件：

      <img src="./images/image-20230110111557375.png" alt="image-20230110111557375" style="zoom:80%;" />

   2. 自定義配置文件：

      core-site.xml, hdfs-site.xml, yarn-site.xml, mapred-site.xml 四個配置文件存放在$HADOOP_HOME/etc/hadoop這個路綫上，用戶可以根據項目進行修改配置

   3. 配置集群

      1. 核心配置文件

         配置core-site.xml

         ```shell
         cd $HADOOP_HOME/etc/hadoop
         vim core-site.xml
         ```

         文件内容如下

         ```xml
         <?xml version="1.0" encoding="UTF-8"?>
         <?xml-stylesheet type="text/xsl" href="configuration.xsl">
         
         <configuration>
           <!--指定NameNode的地址-->
           <property>
             <name>fs.defaultFS</name>
             <value>hdfs://hadoop102:8020</value>
           </property>
         
           <!--指定hadoop數據的存儲目錄-->
           <property>
             <name>hadoop.tmp.dir</name>
             <value>/opt/module/hadoop-3.1.3/data</value>
           </property>
         
           <!--配置HDFS網頁登錄使用的靜態用戶為awdawd-->
           <property>
             <name>hadoop.http.staticuser.user</name>
             <value>awdawd</value>
           </property>
         </configuration>
         ```

      2. HDFS配置文件

         配置hdfs-site.xml

         ```shell
         vim hdfs-site.xml
         ```

         文件内容如下

         ```xml
         <?xml version="1.0" encoding="UTF-8"?>
         <?xml-stylesheet type="text/xsl" href="configuration.xsl">
         
         <configuration>
           <!-- nn wb端訪問地址 -->
           <property>
             <name>dfs.namenode.http-address</name>
             <value>hadoop102:9870</value>
           </property>
         
           <!-- 2nn web端訪問地址 -->
           <property>
             <name>dfs.namenode.secondary.http-address</name>
             <value>hadoop104:9868</value>
           </property>
         </configuration>
         ```

      3. YARN配置文件

         ```shell
         vim yarn-site.xml
         ```

         文件内容如下

         ```xml
         <?xml version="1.0" encoding="UTF-8"?>
         <?xml-stylesheet type="text/xsl" href="configuration.xsl">
         
         <configuration>
           <!-- 指定MR走shuffle -->
           <property>
             <name>yarn.nodemanager.aux-services</name>
             <value>mapreduce_shuffle</value>
           </property>
         
           <!-- 指定ResourceManager的地址 -->
           <property>
             <name>yarn.resourcemanager.hostname</name>
             <value>hadoop103</value>
           </property>
         
           <!-- 環境變量的繼承 -->
           <property>
             <name>yarn.nodemanager.env-whitelist</name>
             <value>JAVA_HOME,HADOOP_COMMON_HOME,HADOOP_HDFS_HOME,HADOOP_CONF_DIR,CLASSPATH_PREPEND_DISTCACHE,HADOOP_YARN_HOME,HADOOP_MAPRED_HOME</value>
           </property>
         </configuration>
         ```

      4. MapReduce配置文件

         配置mapred-site.xml

         ```shell
         vim mapred-site.xml
         ```

         文件内容如下

         ```xml
         <?xml version="1.0" encoding="UTF-8"?>
         <?xml-stylesheet type="text/xsl" href="configuration.xsl">
         
         <configuration>
           <!-- 指定 MapReduce 程序運行在Yarn上 -->
           <property>
             <name>mapreduce.framework.name</name>
             <value>yarn</value>
           </property>
         </configuration>
         ```

   4. 在集群上分發配置好的Hadoop配置文件

      ```shell
      [awdawd@hadoop102 hadoop]$  xsync /opt/module/hadoop-3.1.3/etc/hadoop/
      ```

   5. 去103和104上查看文件分發情況

      ```shell
      [awdawd@hadoop103 ~]$  cat /opt/module/hadoop-3.1.3/etc/hadoop/core-site.xml
      [awdawd@hadoop104 ~]$  cat /opt/module/hadoop-3.1.3/etc/hadoop/core-site.xml
      ```

3. 群起集群

   1. 配置workers

      ```shell
      vim /opt/module/hadoop-3.1.3/etc/hadoop/workers
      ```

      在該文件中增加如下内容：

      ```shell
      hadoop102
      hadoop103
      hadoop104
      ```

      注意：該文件中添加的内容結尾不允許有空格，文件中不允許有空行

      同步所有節點配置文件

      ```shell
      xsync /opt/module/hadoop-3.1.3/etc
      ```

   2. 啓動集群

      1. 如果集群是第一次啓動，需要在hadoop102節點格式化NameNode（注意：格式化NameNode，會產生新的集群id，導致NameNode和DataNode的集群 id 不一致，集群找不到以往數據。如果集群在運行過程中報錯，需要重新格式化NameNode的話，一定要先停止namenode和datanode進程，并且要刪除所有機器的data和logs目錄，然後再進行格式化）

         ```shell
         hdfs namenode -format
         ```

      2. 啓動HDFS

         ```shell
         [awdawd@hadoop102 hadoop-3.1.3]$  sbin/start-dfs.sh
         ```

      3. **在配置了ResourceManager的节点（`hadoop103`）**启动YARN

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  sbin/start-yarn.sh
         ```

      4. Web端查看HDFS的NameNode

         1. 浏览器中输入：
         2. 查看HDFS上存储的数据信息

      5. Web端查看YARN的ResourceManager

         1. 浏览器中输入：http://hadoop103:8088
         2. 查看YARN上运行的Job信息

         ![image-20230110181541443](./images/image-20230110181541443.png)

   3. 集群基本测试

      1. 上传文件到集群

         1. 上传小文件

            ```shell
            [awdawd@hadoop102 ~]$  hadoop fs -mkdir /input
            [awdawd@hadoop102       ~]$  hadoop fs -put $HADOOP_HOME/wcinput/word.txt /input
            ```

         2. 上传大文件

            ```shell
            [awdawd@hadoop102 ~]$  hadoop fs -put /opt/software/jdk-8u212-linux-x64.tar.gz /
            ```

      2. 上传文件后查看文件存放在什么位置

         1. 查看HDFS文件存储路径

            ```shell
            [awdawd@hadoop102 subdir0]$  pwd /opt/module/hadoop-3.1.3/data/dfs/data/current/BP-1436128598-192.168.10.102-1610603670062/current/finalized/subdir0/subdir0
            ```

         2. 查看HDFS在磁盘存储文件内容

            ```shell
            [awdawd@hadoop102 subdir0]$  cat blk_1073741825
            hadoop yarn
            hadoop mapreduce
            awdawd
            awdawd
            ```

         3. 拼接
      
            ```shell
            [awdawd@hadoop102 subdir0]$  cat blk_1073741826>>tmp.tar.gz
            [awdawd@hadoop102 subdir0]$  cat blk_1073741827>>tmp.tar.gz
            [awdawd@hadoop102 subdir0]$  tar -zxvf tmp.tar.gz
            ```
      
         4. 下载
      
            ```shell
            [awdawd@hadoop104 software]$  hadoop fs -get /jdk-8u212-linux-x64.tar.gz ./
            ```
      
         5. 执行 wordcount 程序
      
            ```shell
            [awdawd@hadoop102 hadoop-3.1.3]$  hadoop jar share/hadoop/mapreduce/hadoop-mapreduce-examples-3.1.3.jar wordcount /input /output
            ```
      
      3. 配置历史服务器
      
         为了查看程序的历史运行情况，需要配置一下历史服务器，具体配置步骤如下：
      
         1. 配置 mapred-site.xml
      
            ```shell
            [awdawd@hadoop102 hadoop]$  vim mapred-site.xml
            ```
      
            在该文件里增加如下配置。
      
            ```xml
              <!-- 历史服务器端地址 -->
              <property>
                <name>mapreduce.jobhistory.address</name>
                <value>hadoop102:10020</value>
              </property>
            
              <!-- 历史服务器 web 端地址 -->
              <property>
                <name>mapreduce.jobhistory.webapp.address</name>
                <value>hadoop102:19888</value>
              </property>
            ```
      
         2. 分发配置
      
            ```shell
            [awdawd@hadoop102 hadoop]$  xsync $HADOOP_HOME/etc/hadoop/mapred-site.xml
            ```
      
         3. 在 hadoop102 启动历史服务器
      
            ```shell
            [awdawd@hadoop102 hadoop]$  mapred --daemon start historyserver
            ```
      
         4. 查看历史服务器是否启动
      
            ```shell
            [awdawd@hadoop102 hadoop]$  jps
            ```
      
         5. 查看 JobHistory
      
            http://hadoop102:19888/jobhistory
      
      4. 配置日志的聚集
      
         日志聚集概念：应用运行完成以后，将程序运行日志信息上传到DHFS系统上
      
         ![image-20230111140655701](./images/image-20230111140655701.png)
      
         日志聚集功能的好处：可以方便地查看到程序运行详情，方便开发调试。
      
         注意：开启日志聚集功能，需要重新启动 NodeManager, ResourceManager 和 HistoryServer
      
         开启日志聚集功能具体步骤如下：
      
         1. 配置 yarn-site.xml
      
            ```shell
            [awdawd@hadoop102 hadoop]$  vim yarn-site.xml
            ```
      
            在该文件里面增加如下配置。
      
            ```xml
              <!-- 开启日志聚集功能 -->
              <property>
                <name>yarn.log-aggregation-enable</name>
                <value>true</value>
              </property>
            
              <!-- 设置日志聚集服务器地址 -->
              <property>
                <name>yarn.log.server.url</name>
                <value>hadoop102:19888/jobhistory/logs</value>
              </property>
            
              <!-- 设置日志保留时间为7天 -->
              <property>
                <name>yarn.log-aggregation.retain-seconds</name>
                <value>604800</value>
              </property>
            ```
      
         2. 分发配置
      
            ```shell
            [awdawd@hadoop102 hadoop]$  xsync $HADOOP_HOME/etc/hadoop/yarn-site.xml
            ```
      
         3. 关闭 NodeManager、ResourceManager 和 HistoryServer
      
            ```shell
            [awdawd@hadoop103 hadoop-3.1.3]$  sbin/stop-yarn.sh
            [awdawd@hadoop103   hadoop-3.1.3]$  mapred -- daemon stop historyserver
            ```
      
      5. 集群启动/停止方式总结
      
         1. 各个模块分开启动/停止（配置ssh是前提）**常用**
      
            1. 整体启动/停止 HDFS
      
               ```shell
               start-dfs.sh/stop-dfs.sh
               ```
      
            2. 整体启动/停止 YARN
      
               ```shell
               start-yarn.sh/stop-yarn.sh
               ```
      
         2. 哥哥服务组件逐一启动/停止
      
            1. 分别启动/停止 HDFS 组件
      
               ```shell
               hdfs --daemon start/stop namenode/datanode/secondarynamenode
               ```
      
            2. 启动/停止 YARN
      
               ```shell
               yarn --daemon start/stop resourcemanager/nodemanager
               ```
      
      6. 编写 Hadoop 集群常用脚本
      
         1. Hadoop 集群启停脚本 （包含 HDFS, Yarn, HistoryServer）：myhadoop.sh
      
            ```shell
            [awdawd@hadoop103 ~]$  cd /home/awdawd/bin
            [awdawd@hadoop103 bin]$  vim myhadoop.sh
            ```
      
            * 输入如下内容
      
            ```shell
            #!/bin/bash
            
            if [ $# -lt 1 ]
            then
              echo "No Args Input..."
              exit;
            fi
            
            case $1 in
            "start")
              echo "===================== 启动 hadoop 集群 ====================="
              
              echo "--------------------- 启动 hdfs ---------------------"
              ssh hadoop102 "/opt/module/hadoop-3.1.3/sbin/start-dfs.sh"
              echo "--------------------- 启动 yarn ---------------------"
              ssh hadoop103 "/opt/module/hadoop-3.1.3/sbin/start-yarn.sh"
              echo "--------------------- 启动 historyserver ---------------------"
              ssh hadoop102 "/opt/module/hadoop-3.1.3/bin/mapred --daemon start historyserver"
            ;;
            "stop")
              echo "===================== 关闭 hadoop 集群 ====================="
              
              echo "--------------------- 关闭 historyserver ---------------------"
              ssh hadoop102 "/opt/module/hadoop-3.1.3/bin/mapred --daemon stop historyserver"
              echo "--------------------- 关闭 yarn ---------------------"
              ssh hadoop103 "/opt/module/hadoop-3.1.3/sbin/stop-yarn.sh"
              echo "--------------------- 关闭 hdfs ---------------------"
              ssh hadoop102 "/opt/module/hadoop-3.1.3/sbin/stop-dfs.sh"
            ;;
            *)
              echo "Input Args Error..."
            ;;
            esac
            ```
      
         2. 查看三台服务器 Java 进程脚本：jpsall
      
            ```shell
            [awdawd@hadoop102 ~]$  cd /home/awdawd/bin
            [awdawd@hadoop102 bin]$  vim jpsall
            ```
      
            * 输入以下内容
      
              ```shell
              #!/bin/bash
              
              for host in hadoop102 hadoop103 hadoop104
              do
                echo ===================== $host =====================
                ssh $host jps
              done
              ```
      
            * 保存后退出，然后赋予脚本执行权限
      
              ```shell
              [awdawd@hadoop102 bin]$  chmod +x jpsall
              ```
      
         3. 分发/home/awdawd/bin 目录，保证自定义脚本在三台机器上都可以使用
      
            ```shell
            [awdawd@hadoop102 ~]$  xsync /home/awdawd/bin/
            ```
      
      7. 常用端口号说明
      
         hadoop3.x
      
         ​	HDFS NameNode 内部通讯端口：8020/9000/9820
      
         ​	HDFS NameNode 对用户的查询端口：9870
      
         ​	Yarn 查看任务运行情况的端口：8088
      
         ​	历史服务器：19888
      
         hadoop2.x
      
         ​	HDFS NameNode 内部通讯端口：8020/9000
      
         ​	HDFS NameNode 对用户的查询端口：50070
      
         ​	Yarn 查看任务运行情况的端口：8088
      
         ​	历史服务器：19888
      
      8. 常用的配置文件
      
         hadoop3.x:
      
         ​	core-site.xml
      
         ​	hdfs-site.xml
      
         ​	yarn-site.xml
      
         ​	mapred-site.xml
      
         ​	workers
      
         hadoop2.x
      
         ​	core-site.xml
      
         ​	hdfs-site.xml
      
         ​	yarn-site.xml
      
         ​	mapred-site.xml
      
         ​	slaves
      
      9. 集群时间同步
      
         如果服务器能连接外网，就不需要时间同步
      
         反之，需要时间同步
      
         1. 时间服务器配置（必须 root 用户）
      
            1. 查看所有节点 ntpd 服务状态和开启自启动状态
      
               ```shell
               [awdawd@hadoop102 ~]$  sudo systemctl status ntpd
               [awdawd@hadoop102 ~]$  sudo systemctl start ntpd
               [awdawd@hadoop102 ~]$  sudo systemctl is-enabled ntpd
               [awdawd@hadoop102 ~]$  sudo systemctl enable ntpd
               ```
      
            2. 修改 hadoop102 的 ntp.conf 配置文件
      
               ```shell
               [awdawd@hadoop102 ~]$  sudo vim /etc/ntp.conf
               ```
      
               修改内容如下
      
               1. 修改 1 （授权 192.168.10.0-192.168.10.255 网段上的所有机器可以从这台机器上查询和同步时间）
      
                  ```shell
                  #restrict 192.168.10.0 mask 255.255.255.0 nomodify notrap
                  ```
      
                  为
      
                  ```shell
                  restrict 192.168.10.0 mask 255.255.255.0 nomodify notrap
                  ```
      
               2. 修改 2 （集群在局域网中，不适用其他互联网上的时间）
      
                  ```shell
                  server 0.centos.pool.ntp.org iburst
                  server 1.centos.pool.ntp.org iburst
                  server 2.centos.pool.ntp.org iburst
                  server 3.centos.pool.ntp.org iburst
                  ```
      
                  为
      
                  ```shell
                  #server 0.centos.pool.ntp.org iburst
                  #server 1.centos.pool.ntp.org iburst
                  #server 2.centos.pool.ntp.org iburst
                  #server 3.centos.pool.ntp.org iburst
                  ```
      
               3. 添加3（当该节点丢失和网络连接，依然可以采用本地时间作为服务器为集群中的其他节点提供时间同步）
      
                  ```shell
                  server 127.127.1.0
                  fudge 127.127.1.0 stratum 10
                  ```
      
            3. 修改 hadoop102 的/etc/sysconfig/ntpd 文件
      
               ```shell
               [awdawd@hadoop102 ~]$  sudo vim /etc/sysconfig/ntpd
               ```
         
               增加以下内容
         
               ```conf
               SYNC_HWCLOCK=yes
               ```
         
            4. 重新启动ntpd服务
         
               ```shell
               [awdawd@hadoop102 ~]$  sudo systemctl start ntpd
               ```
         
            5. 设置 ntpd 服务开机启动
         
               ```shell
               [awdawd@hadoop102 ~]$  sudo systectl enable ntpd
               ```
         
         2. 其它机器配置（必须为root用户）
         
            1. 关闭所有节点上 ntp 服务和自启动
         
               ```shell
               [awdawd@hadoop103 ~]$  sudo systemctl stop ntpd
               [awdawd@hadoop103 ~]$  sudo systemctl disable ntpd
               [awdawd@hadoop104 ~]$  sudo systemctl stop ntpd
               [awdawd@hadoop104 ~]$  sudo systemctl disable ntpd
               ```
         
            2. 在其它机器配置1分钟与时间服务器同步一次
         
               ```shell
               [awdawd@hadoop103 ~]$  sudo crontab -e
               ```
         
               编写定时任务如下：
         
               ```shell
               */1 * * * * /usr/sbin/ntpdate hadoop102
               ```
         
            3. 修改任意机器时间
         
               ```shell
               [awdawd@hadoop103 ~]$  sudo date -s "2021-9-11 11:11:11"
               ```
         
            4. 1分钟后查看机器是否与时间服务器同步
         
               ```shell
               [awdawd@hadoop103 ~]$  sudo date
               ```

## 常见错误及解决方案

1. 防火墙没关闭、或者没有启动YARN

   INFO client.RMProxy: Connecting to ResourceManager at hadoop108/192.168.10.108:8032

2. 主机名称配置错误

3. IP地址配置错误

4. ssh没有配置好

5. root用户和awdawd两个用户启动集群不统一

6. 配置文件修改不细心

7. 不识别主机名称

   ```output
   java.net.UnknownHostException: hadoop102: hadoop102 at java.net.InetAddress.getLocalHost(InetAddress.java:1475) at org.apache.hadoop.mapreduce.JobSubmitter.submitJobInternal(JobSubmitter.java:146) at org.apache.hadoop.mapreduce.Job$10,run(Job.java:1290) at org.apache.hadoop.mapreduce.Job$10.run(Job.java:1287) at java.security.AccessController.doPrivileged(Native Method) at javax.security.auth.Subject.doAs(Subject.java:415)
   ```

   解决办法：

   1. 在/etc/hosts 文件中添加 192.168.10.102 hadoop102
   2. 主机名称不要起 hadoop hadoop000等特殊名称

8. DataNode和NameNode进程同时只能工作一个

# Hadoop之HDFS

1. 概述

   1. HDFS产生背景和定义

      随着数据量越来越大，在一个操作系统存不下所有的数据，那么就分配到更多的操作系统管理的磁盘中，但是不方便管理和维护，需要一种系统来管理多台机器上的文件，这就是分布式文件管理系统。HDFS只是分布式文件管理系统中的一种。

      HDFS（Hadoop Distributed File System），它是一个文件系统，用于存储文件，通过目录树来定位文件；其次，它是分布式的，由很多服务器联合起来实现其功能，集群中的服务器有各自的角色。

      HDFS的使用场景：适合一次写入，多次读出的场景。一个文件经过创建、写入和关闭之后就不需要改变。

   2. 优缺点

      优点：

      1. 高容错性

         数据自动保存多个副本。它通过增加副本的形式，提高容错性。

      2. 适合处理大数据

         数据规模：能够处理数据规模达到GB、TB、甚至PB级别的数据；

         文件规模：能够处理百万规模以上的文件数量，数量相当之大。

      3. 可构建在廉价机器上，通过多副本机制，提高可靠性。

      缺点

      1. 不适合低延时数据访问，比如毫秒级的数据存储，是做不到的。

      2. 无法高效对大量小文件进行存储

         存储大量小文件的话，它会占用NameNode大量内存来存储文件目录和块信息。这样是不可取的，因为NameNode的内存总是有限的。

         小文件存储的寻址时间会超过读取时间，它违反了HDFS的设计目标

      3. 不支持并发写入、文件随机修改

         一个文件只能有一个写，不允许多线程同时写

         仅支持数据append（追加），不支持文件随即修改。

   3. 组成

      ![image-20230112133354649](./images/image-20230112133354649.png)

      1. NameNode (nn) : 就是Master，它是一个主管、管理者。
         1. 管理HDFS的名称空间；
         2. 配置副本策略；
         3. 管理数据块（Block）的映射信息；
      2. Secondary NameNode：并非NameNode的热备。当NameNode挂掉的时候，它并不能马上替换NameNode并提供服务。
         1. 辅助NameNode，分担其工作量，比如定期合并Fsimage和Edits，并推送给NameNode；
         2. 在紧急情况下，可辅助恢复NameNode。
      3. Client：客户端
         1. 文件切分。文件上传HDFS的时候，Client将文件切分成一个一个的Block，然后进行上传。
         2. 与NameNode交互，获取文件的位置信息；
         3. 与DataNode交互，读取或者写入数据；
         4. Client提供一些命令来管理HDFS，比如NameNode格式化；
         5. Client可以通过一些命令来访问HDFS，比如对HDFS增删查改操作。
      4. DataNode 就是Slave。NameNode下达命令，DataNode执行实际的操作。
         1. 存储实际的数据块；
         2. 执行数据块的读写操作。

   4. 文件块大小问题

      HDFS中文件在物理上是分块存储（Block），块的大小可以通过配置参数（dfs.blocksize）来规定，默认大小在Hadoop2.x/3.x版本中是128M，1.x版本中是64M。

      2. 如果寻址时间为约10ms，即查找到木雕block的时间为10ms。

      3. 寻址时间为传输时间的1%时，则为最佳状态。（专家）

         因此，传输时间=10ms/0.01=1000ms=1s

      4. 而目前磁盘的传输速率普遍为100MB/s。

      思考：为什么块的大小不能设置太小、也不能设置太大？

      1. HDFS的块设置太小，会增加寻址时间，程序一直在找块的开始位置；
      2. 如果块设置的太大，从磁盘传输数据的时间会明显大于定位这个块开始位置所需的时间。导致程序在处理这块数据时会非常慢。

      总结：HDFS块的大小设置主要取决于磁盘传输速率。

      机械磁盘，100MB/s -> 128MB

      固态，200-300MB/s -> 256MB

2. HDFS的Shell相关操作（开发重点）

   基本语法

   `hadoop fs 具体命令` OR `hdfs dfs 具体命令`

   两个是完全相同的。

   ```shell
   [awdawd@hadoop103 hadoop-3.1.3]$  bin/hadoop fs
   ```

   命令大全

   ```shell
   	[-appendToFile <localsrc> ... <dst>]
   	[-cat [-ignoreCrc] <src> ...]
   	[-checksum <src> ...]
   	[-chgrp [-R] GROUP PATH...]
   	[-chmod [-R] <MODE[,MODE]... | OCTALMODE> PATH...]
   	[-chown [-R] [OWNER][:[GROUP]] PATH...]
   	[-copyFromLocal [-f] [-p] [-l] [-d] [-t <thread count>] <localsrc> ... <dst>]
   	[-copyToLocal [-f] [-p] [-ignoreCrc] [-crc] <src> ... <localdst>]
   	[-count [-q] [-h] [-v] [-t [<storage type>]] [-u] [-x] [-e] <path> ...]
   	[-cp [-f] [-p | -p[topax]] [-d] <src> ... <dst>]
   	[-createSnapshot <snapshotDir> [<snapshotName>]]
   	[-deleteSnapshot <snapshotDir> <snapshotName>]
   	[-df [-h] [<path> ...]]
   	[-du [-s] [-h] [-v] [-x] <path> ...]
   	[-expunge]
   	[-find <path> ... <expression> ...]
   	[-get [-f] [-p] [-ignoreCrc] [-crc] <src> ... <localdst>]
   	[-getfacl [-R] <path>]
   	[-getfattr [-R] {-n name | -d} [-e en] <path>]
   	[-getmerge [-nl] [-skip-empty-file] <src> <localdst>]
   	[-head <file>]
   	[-help [cmd ...]]
   	[-ls [-C] [-d] [-h] [-q] [-R] [-t] [-S] [-r] [-u] [-e] [<path> ...]]
   	[-mkdir [-p] <path> ...]
   	[-moveFromLocal <localsrc> ... <dst>]
   	[-moveToLocal <src> <localdst>]
   	[-mv <src> ... <dst>]
   	[-put [-f] [-p] [-l] [-d] <localsrc> ... <dst>]
   	[-renameSnapshot <snapshotDir> <oldName> <newName>]
   	[-rm [-f] [-r|-R] [-skipTrash] [-safely] <src> ...]
   	[-rmdir [--ignore-fail-on-non-empty] <dir> ...]
   	[-setfacl [-R] [{-b|-k} {-m|-x <acl_spec>} <path>]|[--set <acl_spec> <path>]]
   	[-setfattr {-n name [-v value] | -x name} <path>]
   	[-setrep [-R] [-w] <rep> <path> ...]
   	[-stat [format] <path> ...]
   	[-tail [-f] [-s <sleep interval>] <file>]
   	[-test -[defsz] <path>]
   	[-text [-ignoreCrc] <src> ...]
   	[-touch [-a] [-m] [-t TIMESTAMP ] [-c] <path> ...]
   	[-touchz <path> ...]
   	[-truncate [-w] <length> <path> ...]
   	[-usage [cmd ...]]
   ```

   常用命令實操

   1. 準備工作

      1. 啓動hdoop集群（方便後續測試）

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  sbin/start-dfs.sh
         [awdawd@hadoop103 hadoop-3.1.3]$  sbin/start-yarn.sh
         ```

      2. -help: 輸出這個命令參數

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -help rm
         ```

      3. 創建sanguo文件夾

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -mkdir /sanguo
         ```

   2. 上傳

      1. -moveFromLocal：從本地剪切**粘貼**到HDFS

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  vim shuguo.txt
         輸入：
         shuguo
         
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -moveFromLocal ./shuguo.txt /sanguo
         ```

      2. -copyFromLocal：從本地文件系統中**拷貝**文件到HDFS路徑去

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  vim weiguo.txt
         輸入：
         weiguo
         
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -copyFromLocal weiguo.txt /sanguo
         ```

      3. -put：等同於copyFromLocal，生產環境更習慣用put

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  vim wuguo.txt
         輸入：
         wuguo
         
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -put wuguo.txt /sanguo
         ```

      4. -appendToFile：追加一個文件到已經存在的文件末尾

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  vim liubei.txt
         輸入：
         liubei
         
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -appendToFile liubei.txt /sanguo/shuguo.txt
         ```

   3. 下載

      1. -copyToLocal：從HDFS拷貝到本地

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -copyToLocal
         /sanguo/shuguo.txt ./
         ```

      2. -get：等同於copyToLocal，生產環境更習慣用get

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -get /sanguo/shuguo.txt ./shuguo2
         ```

   4. HDFS直接操作

      1. -ls 顯示目錄信息

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -ls /sanguo
         ```

      2. -cat：顯示文件内容

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -cat /sanguo/shuguo.txt
         ```

      3. -chgrp，-chmod，-chown：Linux文件系統中的用法一樣，修改文件所屬權限

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -chmod 666 /sanguo/shuguo.txt
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -chown awdawd:awdawd /sanguo/shuguo.txt
         ```

      4. -mkdir：創建路徑

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -mkdir /jinguo
         ```

      5. -cp：從HDFS的一個路徑拷貝到HDFS的另一個路徑

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -cp /sanguo/shuguo.txt /jinguo
         ```

      6. -mv：在HDFS目錄中移動文件

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -mv /sanguo/wuguo.txt /jinguo
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -mv /sanguo/weiguo.txt /jinguo
         ```

      7. -tail：顯示一個文件的末尾1kb的數據

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -tail /sanguo/shuguo.txt
         ```

      8. -rm：刪除文件或文件夾

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -rm /sanguo/shuguo.txt
         ```

      9. -rm -r：遞歸刪除目錄及目錄裏面的内容

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -rm -r /sanguo
         ```

      10. -du 統計文件夾的大小信息

         ```shell
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -du -s -h /jinguo
         27  81 /jinguo
         
         [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -du -h /jinguo
         14  42 /jinguo/shuguo.txt
         7  21  /jinguo/weiguo.txt
         6  18  /jinguo/wuguo.txt
         ```

         説明：27表示文件大小：81表示27*3個副本；/jinguo表示查看的目錄

      11. -setrep：設置HDFS中文件的副本數量

          ```shell
          [awdawd@hadoop103 hadoop-3.1.3]$  hadoop fs -setrep 10 /jinguo/shuguo.txt
          ```

          這裏設置的副本數只是記錄在NameNode的元數據中，是否真的會有這麽多副本，還得看DataNode的數量。因爲目前只有3台設備，最多也就3個副本，只有節點數增加到10台時，副本數才能達到10。

3. HDFS的客户端API

   1. 客戶端環境準備

   2. 在IDEA中創建一個Maven工程HdfsClientDemo，並導入相應的依賴坐標+日志添加

      ```xml
      <dependencies>
          <dependency>
              <groupId>org.apache.hadoop</groupId>
              <artifactId>hadoop-client</artifactId>
              <version>3.3.2</version>
          </dependency>
          <dependency>
              <groupId>junit</groupId>
              <artifactId>junit</artifactId>
              <version>4.13.2</version>
          </dependency>
          <dependency>
              <groupId>org.slf4j</groupId>
              <artifactId>slf4j-log4j12</artifactId>
              <version>2.0.5</version>
          </dependency>
      </dependencies>
      ```

      在項目的src/main/resources目錄下，新建一個文件，命名為“log4j.properties”，在文件中填入。

      ```properties
      log4j.rootLogger=INFO, stdout
      log4j.appender.stdout=org.apache.log4j.ConsoleAppender
      log4j.appender.stdout.layout=org.apache.log4j.PatternLayout
      log4j.appender.stdout.layout.ConversionPattern=%d %p [%c] - %m%n
      log4j.appender.logfile=org.apache.log4j.FileAppender
      log4j.appender.logfile.File=target/spring.log
      log4j.appender.logfile.layout=org.apache.log4j.PatternLayout
      log4j.appender.logfile.layout.ConversionPattern=%d %p [%c] - %m%n
      ```

   3. 創建包名：com.hdfsclient.awdawd

   4. 創建HdfsClient類

4. HDFS的读写流程（面试重点）

5. NN和2NN（了解）

6. DataNode工作机制