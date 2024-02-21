# lanraragi_tag

### 批量刷新lanraragi的中漫画的标签

参数解释

`-h --host 服务地址 默认 http://127.0.0.1:3000`

`-p --password 管理员密码 必填`

命令示例

`java -jar .\lanraragi_tag-jar-with-dependencies.jar -p 123456 -h http://127.0.0.1:3000`

### 原理

根据默认自动生成的 date_added 标签查询新增加的漫画并使用正则匹配获取标签并进行更新