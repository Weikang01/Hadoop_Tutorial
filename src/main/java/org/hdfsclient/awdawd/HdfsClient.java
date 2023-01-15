package org.hdfsclient.awdawd;

import org.apache.hadoop.conf.Configuration;
import org.apache.hadoop.fs.FileSystem;
import org.apache.hadoop.fs.Path;
import org.junit.After;
import org.junit.Before;
import org.junit.Test;

import java.io.IOException;
import java.net.URI;
import java.net.URISyntaxException;

/**
* @Description: TODO
* @author: Weikang
* @date:
客戶端代碼常用套路
1. 獲取一個客戶端對象
2. 執行相關的操作命令
3. 關閉資源
HDFS  zookeeper
* @Return:
*/
public class HdfsClient {
    private FileSystem fs;

    @Before
    public void init() throws URISyntaxException, IOException, InterruptedException {
        // 連接的集群nn地址
        URI uri = new URI("hdfs://hadoop102:8020");
        // 創建一個配置文件
        Configuration configuration = new Configuration();
        // 用戶
        String user = "awdawd";
        // 獲取客戶端對象
        fs = FileSystem.get(uri, configuration, user);
    }

    @After
    public void close() throws IOException {
        // 關閉資源
        fs.close();
    }

    // 創建目錄
    @Test
    public void testmkdir() throws IOException {
        // 創建一個文件夾
        fs.mkdirs(new Path("/xiyou/huaguoshan1"));
    }

    // 上傳操作
    @Test
    public void testPut() throws IOException {
        // 參數解讀：
        // delSrc：表示刪除原數據； overwrite：是否允許覆蓋； src：原數據路徑；dst：目的地路徑
        fs.copyFromLocalFile
                (false, true,
                        new Path("D:\\github_sync\\IDEA_workspace\\HDFSClient\\HDFSClient\\sunwukong.txt"),
                        new Path("hdfs://hadoop102/xiyou/huaguoshan"));
    }
}
