#!/bin/bash

#verify there is at least one input argument
if [ $# -ne 1 ]
  then
   echo "fortran-lib-publish.sh [-l] [-p version] [-i version]
   where:
      no arguments deploys the most recent installed build
      -l: lists all installed build versions
      -p version: builds and publishes specified version, does not install it. (ex: -v 2.1.3")
      -i version: installs the published version, if no version number is supplied, the latest version is installed (ex: -i 2.1.3)

    exit 1
fi

MAVEN_REPO_HOME=${HOME}/.m2/repository
BUILD_DIR=/opt/apps/peasBuild
FORTRAN_LIB_DIR=/opt/apps/lib
GIT_DIR=${BUILD_DIR}/git
JAVA_INCLUDE_PATH=$JAVA_HOME/include



for var in "$@"
do
    if [ $var = "-l" ]; then
	LIST=1
	elif [ $var = "-i" ]; then
	INSTALL=1
	
	shift
    elif [ $var = "-p" ]; then
	NEXT_VAR="PUBLISH"
	shift
    else  
        if [ "$NEXT_VAR" == "PUBLISH" ]; then
          VERSION=$var
          NEXT_VAR="" 
       fi
    fi
done

# list
if [ -z "$LIST" ]; then
   echo ""
else
   java -cp $GIT_DIR/pcs-web/target/classes org.tmt.aps.peas.tools.VersionTool -l $MAVEN_REPO_HOME/org/tmt/libpeas
fi

# publish
if [ -z "$PUBLISH" ]; then
   echo ""
else

   if [ -z "$VERSION" ]; then
      echo "no version specified, please specify a version"    
   else

	  echo "renaming current library to libpeas.so.current"
	  mv ${FORTRAN_LIB_DIR}/libpeas.so ${FORTRAN_LIB_DIR}/libpeas.so.current
		
	  echo "building J2F"
	  cd ${GIT_DIR}/pcs-j2f
	  mvn clean package
		
		
	  echo "building FORTRAN library"
	  cd ${GIT_DIR}/pcs-fortran
	  export C_INCLUDE_PATH=$JAVA_INCLUDE_PATH:$JAVA_INCLUDE_PATH/linux:$C_INCLUDE_PATH
	  cd /opt/apps/j2f
	  ${GIT_DIR}/pcs-j2f/src/main/resources/generateFromGit.sh /opt/apps/j2f ${GIT_DIR} 


      echo $VERSION
      cd $FORTRAN_LIB_DIR
      mvn install:install-file -Dfile=libpeas.so -DgroupId=org.tmt -DartifactId=libpeas -Dversion=$VERSION -Dpackaging=so   
   fi
   
   
   echo "restoring current library to libpeas.so"
   mv ${FORTRAN_LIB_DIR}/libpeas.so.current ${FORTRAN_LIB_DIR}/libpeas.so   
   
fi


# deploy 
if [ -z "$INSTALL" ]; then
   echo ""
else
   if [ -z "$VERSION" ]; then
      echo "using latest installed version for deploy"
      VERSION="$(java -cp $GIT_DIR/pcs-web/target/classes org.tmt.aps.peas.tools.VersionTool -v $MAVEN_REPO_HOME/org/tmt/libpeas 2>&1)"
   fi

   echo $INSTALL
   
   cp $MAVEN_REPO_HOME/org/tmt/libpeas/$VERSION/*.so ${FORTRAN_LIB_DIR}/libpeas.so
   
fi

