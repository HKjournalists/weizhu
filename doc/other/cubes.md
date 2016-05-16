# cubes & cubesviewer 安装使用文档

1. 安装python2.7
    * 更新系统和开发工具集
    
        ```bash
        yum -y update
        yum groupinstall -y 'development tools'
        ```
    * 安装 python 工具需要的额外软件包 SSL, bz2, zlib
    
        ```bash
        yum install -y zlib-devel bzip2-devel openssl-devel xz-libs wget
        ```
    * 源码安装Python 2.7.x
    
    
        ```bash
        wget http://www.python.org/ftp/python/2.7.8/Python-2.7.8.tar.xz
        xz -d Python-2.7.8.tar.xz
        tar -xvf Python-2.7.8.tar
        
        # 进入目录:
        cd Python-2.7.8
        # 运行配置 configure:
        ./configure --prefix=/usr/local
        # 编译安装:
        make
        make altinstall
        ```
    * 安装 setuptools
    
    
        ```bash
        # 获取软件包
        wget --no-check-certificate https://pypi.python.org/packages/source/s/setuptools/setuptools-1.4.2.tar.gz
        # 解压:
        tar -xvf setuptools-1.4.2.tar.gz
        cd setuptools-1.4.2
        # 使用 Python 2.7.8 安装 setuptools
        python2.7 setup.py install
        ```
    * 安装 PIP
    
        ```bash
        curl https://raw.githubusercontent.com/pypa/pip/master/contrib/get-pip.py | python2.7 -
        ```
2. 安装cubes-1.0.1

    ```bash
    pip install cubes
	```
3. 安装sqlalchemy flask mysqlclient

    ```bash
    pip install sqlalchemy
    pip install flask
    # 需要安装 mysql-shared-xxx.rpm
    pip install mysqlclient
    ```
4. 修改/usr/local/bin/slicer 支持UTF-8字符集

    用vi编辑器修改`/usr/local/bin/slicer`,添加以下内容
    ```python
    import sys
    reload(sys)
    sys.setdefaultencoding( "utf-8" )
    ```
5. 下载cubesviewer

    [cubesviewer master.zip](https://github.com/jjmontesl/cubesviewer/archive/master.zip) 
6. 处理cubesviewer导出excel bug

    将`src/web/static/js/cubesviewer/cubesviewer.views.cube.export.js`第92行修改为`params.cut = args.cut.toString();`
    
7. 启动cubes

    执行`slicer serve slicer.ini`
8. 用浏览器打开cubesviewer页面

    `src/htmlviews/gui.html` 输入正确的cubes server 地址即可访问