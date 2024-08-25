# lanraragi_tag

### 批量刷新lanraragi的中漫画的标签

参数解释

`-h --host 服务地址 默认 http://127.0.0.1:3000`

`-k --key Api Key 必填`

`-t 线程数量 默认为 2`

### 命令示例

    java -jar .\lanraragi_tag-jar-with-dependencies.jar -k 123456 -h http://127.0.0.1:3000

### Docker部署

    docker run -d --name lanraragi_tag -e HOST="http://127.0.0.1:3000" -e KEY="" -e THREAD_NUM="2" -e RUN="TRUE" -e CRON="0 1 * * *" -e TZ=Asia/Shanghai --restart always wushuo894/lanraragi_tag

| 参数         | 作用           | 默认值                   |
|------------|--------------|-----------------------|
| HOST       | lanraragi 地址 | http://127.0.0.1:3000 |
| KEY        | API Key      | 空                     |
| THREAD_NUM | 线程数量         | 2                     |
| RUN        | 启动时运行        | TRUE                  |
| CRON       | 计划任务         | 0 1 * * *             |
| TZ         | 时区           | Asia/Shanghai         |

### 原理

根据默认自动生成的 date_added 标签查询新增加的漫画并使用正则匹配获取标签并进行更新,需开启自动添加时间戳
