# nginx 配置https自签名证书访问

1. 制作CA证书
    * 生成CA私钥`weizhu_ca.key`，并设置好密码
    
        ```bash
        openssl genrsa -des3 -out weizhu_ca.key 2048
        ```
    * 生成CA根证书（公钥）`weizhu_ca.crt`
    
        ```bash
        openssl req -new -x509 -days 7305 -key weizhu_ca.key -out weizhu_ca.crt
        ```
2. 制作网站证书
    * 生成`test.wehelpu.cn`证书私钥`test.wehelpu.cn.key`,并设置好密码
    
        ```bash
        openssl genrsa -des3 -out test.wehelpu.cn.key 1024
        ```
    * 生成一个不需要输入密码的私钥文件`test.wehelpu.cn_nopass.key`
    
        ```bash
        openssl rsa -in test.wehelpu.cn.key -out test.wehelpu.cn_nopass.key
        ```
    * 生成签名请求`test.wehelpu.cn.csr`，**Common Name 必须和访问的域名一致，否则浏览器会报错**
    
        ```bash
        openssl req -new -key test.wehelpu.cn.key -out test.wehelpu.cn.csr
        # 参考输入
        # Country Name (2 letter code) [AU]:CN
        # State or Province Name (full name) [Some-State]:Beijing
        # Locality Name (eg, city) []:Beijing
        # Organization Name (eg, company) [Internet Widgits Pty Ltd]:weizhu
        # Organizational Unit Name (eg, section) []:weizhu
        # Common Name (e.g. server FQDN or YOUR name) []:test.wehelpu.cn
        # Email Address []:admin@wehelpu.cn
        ```
3. 用CA证书进行签名
    * 创建目录结构
    
        ```bash
        mkdir -p demoCA/newcerts
        touch CA/index.txt
        touch CA/serial
        echo “01″ > CA/serial
        ```
    * 生成签名后的证书`test.wehelpu.cn.crt`
    
        ```bash
        openssl ca -policy policy_anything -days 365 -cert weizhu_ca.crt -keyfile weizhu_ca.key -in test.wehelpu.cn.csr -out test.wehelpu.cn.crt
        ```
4. 配置nginx https访问
    
    ```
    server {
        server_name YOUR_DOMAINNAME_HERE;
        listen 443;
        ssl on;
        ssl_certificate /path/to/test.wehelpu.cn.crt;
        ssl_certificate_key /path/to/test.wehelpu.cn_nopass.key;
        # 其他配置
    }
    ```
5. 配置nginx https双向验证配置
    * 生成client私钥`francislin.key`,并设置好密码
       
        ```
        openssl genrsa -des3 -out francislin.key 1024
        ```
    * 生成client签名请求`francislin.csr`
    
        ```
        openssl req -new -key francislin.key -out francislin.csr
        # 参考输入
        # Country Name (2 letter code) [AU]:CN
        # State or Province Name (full name) [Some-State]:Beijing
        # Locality Name (eg, city) []:Beijing
        # Organization Name (eg, company) [Internet Widgits Pty Ltd]:weizhu
        # Organizational Unit Name (eg, section) []:weizhu
        # Common Name (e.g. server FQDN or YOUR name) []:francislin
        # Email Address []:francislin@wehelpu.cn
        ```
    * 使用CA证书签名`francislin.crt`
        
        ```
        openssl ca -policy policy_anything -days 365 -cert weizhu_ca.crt -keyfile weizhu_ca.key -in francislin.csr -out francislin.crt
        ```
    * 生成浏览器可识别的p12格式`francislin.p12`,并发给对应的用户导入到浏览器中
    
        ```
        openssl pkcs12 -export -clcerts -in francislin.crt -inkey francislin.key -out francislin.p12
        ```
    * 配置nginx
    
        ```
        server {
            server_name YOUR_DOMAINNAME_HERE;
            listen 443;
            ssl on;
            ssl_certificate /path/to/test.wehelpu.cn.crt;   
            ssl_certificate_key /path/to/test.wehelpu.cn_nopass.key;
            ssl_client_certificate /path/to/weizhu_ca.crt
            ssl_verify_client on;
            # 其他配置
            location ^~ /xxx {
                proxy_pass  http://localhost:8080;
                proxy_set_header Host $host;
                proxy_set_header X-Real-IP $remote_addr;
                proxy_set_header X-Real-PORT $remote_port;
                proxy_set_header X-Forwarded-For $proxy_add_x_forwarded_for;
                proxy_set_header X-Forwarded-Proto $scheme
                proxy_set_header X-SSL-Client-Subject-DN $ssl_client_s_dn
                proxy_redirect     off;
            }
        }
        ```
6. 参考
    * [nginx配置ssl加密（单/双向认证、部分https）](http://segmentfault.com/a/1190000002866627)