#!/bin/bash


BUILD_DIR=/opt/apps/peasBuild
GIT_DIR=${BUILD_DIR}/git
JAVA_INCLUDE_PATH=$JAVA_HOME/include


echo "building J2F"
cd ${GIT_DIR}/pcs-j2f
mvn clean package


echo "building FORTRAN library"
cd ${GIT_DIR}/peas-pcs-fortran
export C_INCLUDE_PATH=$JAVA_INCLUDE_PATH:$JAVA_INCLUDE_PATH/linux:$C_INCLUDE_PATH
cd /opt/apps/j2f
${GIT_DIR}/pcs-j2f/src/main/resources/generateFromGit.sh /opt/apps/j2f ${GIT_DIR} 
