# nginx 反向代理配置

## 全局配置

```
user  nginx;
worker_processes  4;

error_log  /var/log/nginx/error.log warn;
pid        /var/run/nginx.pid;


events {
    worker_connections  4096;
}


http {
    include       /etc/nginx/mime.types;
    default_type  application/octet-stream;

    log_format  main  '$remote_addr - $remote_user [$time_local] "$request" '
                      '$status $body_bytes_sent "$http_referer" '
                      '"$http_user_agent" "$http_x_forwarded_for"';

    access_log  /var/log/nginx/access.log  main;

    sendfile        on;
    #tcp_nopush     on;

    keepalive_timeout  65;

    gzip              on;
    gzip_min_length   1k;
    gzip_buffers      4 16k;
    gzip_http_version 1.1;
    gzip_comp_level   5;
    gzip_types        text/plain text/css text/xml text/javascript application/x-javascript application/json application/xml;
    gzip_disable      "MSIE [1-6].";
    gzip_vary         on;

    include /etc/nginx/conf.d/*.conf;
}
```

## http请求转发

```
server {
    listen       80;
    server_name  test;

    #charset koi8-r;

    access_log  /data/weizhu/nginx/logs/test.access.log  main;
    error_log   /data/weizhu/nginx/logs/test.error.log;

    location / {
        root   /usr/share/nginx/html;
        index  index.html index.htm;
    }

    location ^~ /test/ {
        proxy_pass             http://10.51.110.92:18084;
        proxy_redirect         off;
        proxy_set_header       Host               $host;
        proxy_set_header       X-Real-IP          $remote_addr;
        proxy_set_header       X-Forwarded-For    $proxy_add_x_forwarded_for;
        proxy_set_header       X-Forwarded-Proto  $scheme;
        proxy_connect_timeout  240;
        proxy_send_timeout     240;
        proxy_read_timeout     240;
    }

    #error_page  404              /404.html;
    
    # redirect server error pages to the static page /50x.html
    #
    error_page   500 502 503 504  /50x.html;
    location = /50x.html {
        root   html;
    }
}

```

## https请求转发

[参考文章](http://www.tuicool.com/articles/IvAVnqA)

```
server {
    listen       443 ssl spdy;
    server_name  test;
    
    ssl on;
    ssl_certificate      /usr/local/weizhu/ssl/test.crt;
    ssl_certificate_key  /usr/local/weizhu/ssl/test.key_nopass;
    
    ssl_session_cache    shared:SSL:10m;
    ssl_session_timeout  10m;
    
    ssl_prefer_server_ciphers  on;
    ssl_ciphers                ECDHE-ECDSA-AES256-GCM-SHA384:ECDHE-RSA-AES256-GCM-SHA384:ECDHE-ECDSA-AES256-SHA384:ECDHE-RSA-AES256-SHA384:ECDHE-ECDSA-AES128-GCM-SHA256:ECDHE-RSA-AES128-GCM-SHA256:ECDHE-ECDSA-AES128-SHA256:ECDHE-RSA-AES128-SHA256:ECDHE-ECDSA-RC4-SHA:!ECDHE-RSA-RC4-SHA:ECDH-ECDSA-RC4-SHA:ECDH-RSA-RC4-SHA:ECDHE-RSA-AES256-SHA:!RC4-SHA:HIGH:!aNULL:!eNULL:!LOW:!3DES:!MD5:!EXP:!CBC:!EDH:!kEDH:!PSK:!SRP:!kECDH;
    ssl_protocols              TLSv1 TLSv1.1 TLSv1.2;
    
    location / {
        root   html;
        index  index.html index.htm;
    }

    location ^~ /admin/ {
        proxy_pass             http://10.51.110.92:18082;
        proxy_redirect         off;
        proxy_set_header       Host               $host;
        proxy_set_header       X-Real-IP          $remote_addr;
        proxy_set_header       X-Forwarded-For    $proxy_add_x_forwarded_for;
        proxy_set_header       X-Forwarded-Proto  $scheme;
        proxy_connect_timeout  240;
        proxy_send_timeout     240;
        proxy_read_timeout     240;
    }
}
```

## 静态文件

```
server {
    listen       8080;
    server_name  test;
    
    #charset koi8-r;

    access_log  /data/weizhu/nginx/logs/test.access.log  main;
    error_log   /data/weizhu/nginx/logs/test.error.log;
    
    location / {
        root   /data/weizhu/weizhu-upload;
        index  index.html index.htm;
    }
    
    location ~* "^/image/(original|thumb60|thumb120|thumb240|thumb480)/([0-9a-f]{2})([0-9a-f]{30})\.(jpg|png|gif)$" {
    	root       /data/weizhu/weizhu-upload;
        try_files  /image/$1/$2/$2$3.$4 =404;
    }
    
    location ~* "^/video/([0-9a-f]{2})([0-9a-f]{30})\.(mp4|avi)$" {
    	root       /data/weizhu/weizhu-upload;
        try_files  /video/$1/$1$2.$3 =404;
    }
}

```