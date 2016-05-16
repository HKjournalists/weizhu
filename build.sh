#!/bin/bash

GIT_NAME=`git rev-parse --abbrev-ref HEAD`
GIT_HASH=`git rev-parse --short HEAD`
GIT_TIME=`git show -s --format=%ci HEAD | cut -d':' -f 1,2 | sed 's/[- :]//g'`
GIT_AUTHOR=`git show -s --format=%ce HEAD`

BUILD_NAME=${GIT_NAME}-${GIT_TIME}-${GIT_HASH}

echo "BUILD_NAME: ${BUILD_NAME}" 

mvn clean package

MVN_PKG_RET=$?
if [ ! ${MVN_PKG_RET} -eq 0 ]; then
  exit 1
fi

BUILD_DIR=./build

if [ -d ${BUILD_DIR} ]; then
  rm -fr ${BUILD_DIR}
fi

mkdir ${BUILD_DIR}

mv -v webapp/admin/target/weizhu-admin-webapp-1.0.0-SNAPSHOT.war         ${BUILD_DIR}/weizhu-admin-webapp-${BUILD_NAME}.war
mv -v webapp/mobile/target/weizhu-mobile-webapp-1.0.0-SNAPSHOT.war       ${BUILD_DIR}/weizhu-mobile-webapp-${BUILD_NAME}.war
mv -v webapp/demo/target/weizhu-demo-webapp-1.0.0-SNAPSHOT.war           ${BUILD_DIR}/weizhu-demo-webapp-${BUILD_NAME}.war
mv -v webapp/upload/target/weizhu-upload-webapp-1.0.0-SNAPSHOT.war       ${BUILD_DIR}/weizhu-upload-webapp-${BUILD_NAME}.war
mv -v webapp/boss/target/weizhu-boss-webapp-1.0.0-SNAPSHOT.war           ${BUILD_DIR}/weizhu-boss-webapp-${BUILD_NAME}.war

mv -v server/all/target/weizhu-all-server-SHADED.jar                     ${BUILD_DIR}/weizhu-all-server-${BUILD_NAME}.jar 

mv -v server/api/target/weizhu-api-server-SHADED.jar                     ${BUILD_DIR}/weizhu-api-server-${BUILD_NAME}.jar
mv -v server/common-logic/target/weizhu-common-logic-server-SHADED.jar   ${BUILD_DIR}/weizhu-common-logic-server-${BUILD_NAME}.jar
mv -v server/company-logic/target/weizhu-company-logic-server-SHADED.jar ${BUILD_DIR}/weizhu-company-logic-server-${BUILD_NAME}.jar
mv -v server/company-proxy/target/weizhu-company-proxy-server-SHADED.jar ${BUILD_DIR}/weizhu-company-proxy-server-${BUILD_NAME}.jar
mv -v server/conn/target/weizhu-conn-server-SHADED.jar                   ${BUILD_DIR}/weizhu-conn-server-${BUILD_NAME}.jar
mv -v server/external/target/weizhu-external-server-SHADED.jar           ${BUILD_DIR}/weizhu-external-server-${BUILD_NAME}.jar
mv -v server/boss/target/weizhu-boss-server-SHADED.jar                   ${BUILD_DIR}/weizhu-boss-server-${BUILD_NAME}.jar
mv -v server/push/target/weizhu-push-server-SHADED.jar                   ${BUILD_DIR}/weizhu-push-server-${BUILD_NAME}.jar
mv -v server/webapp/target/weizhu-webapp-server-SHADED.jar               ${BUILD_DIR}/weizhu-webapp-server-${BUILD_NAME}.jar
mv -v server/upload/target/weizhu-upload-server-SHADED.jar               ${BUILD_DIR}/weizhu-upload-server-${BUILD_NAME}.jar
mv -v server/data/target/weizhu-data-server-SHADED.jar                   ${BUILD_DIR}/weizhu-data-server-${BUILD_NAME}.jar

mv -v cli/utils/target/weizhu-utils-cli-SHADED.jar                       ${BUILD_DIR}/weizhu-utils-cli-${BUILD_NAME}.jar
