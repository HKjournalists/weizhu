# 微助监控相关部署文档

## 安装influxDB

1. [下载influxDB rpm安装包](https://influxdata.com/downloads/#influxdb)
2. 安装rpm包 `rpm -ivh influxdb-xxx.rpm`
3. 配置influxdb配置文件 `/etc/influxdb/influxdb.conf`
    * 关闭report `reporting-disabled = true`
    * 更改存储目录 
    
        ```
        [meta]
          dir = "/data/weizhu/influxdb/meta"
        [data]
          dir = "/data/weizhu/influxdb/data"
          wal-dir = "/data/weizhu/influxdb/wal"
        ```

## 安装grafana

1. [下载grafana rpm安装包](http://grafana.org/download/)
2. 安装rpm包 `rpm -ivh grafana-xxx.rpm`
3. 配置grafana配置文件 `/etc/grafana/grafana.ini`
    * 更改存储目录
    
        ```
        [paths]
        data = /data/weizhu/grafana/data
        logs = /data/weizhu/grafana/logs
        plugins = /data/weizhu/grafana/plugins
        ```
    * 关闭上报
    
        ```
        [analytics]
        reporting_enabled = false
        ```

## 安装collectD (先不用)

http://metrics.dropwizard.io/3.1.0/