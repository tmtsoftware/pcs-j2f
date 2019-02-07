#!/bin/bash

#verify there is at least one input argument
if [ $# -eq 0 ]
  then
   echo "fortran-lib-publish.sh [-l] [-p version] [-i version]
   where:
      -l: lists all installed build versions
      -p version: builds and publishes specified version, does not install it. (ex: -v 2.1.3)
      -i version: installs the published version, if no version number is supplied, the latest version is installed (ex: -i 2.1.3)"

    exit 1
fi

MAVEN_REPO_HOME=/home/smichaels/.m2/repository
BUILD_DIR=/opt/apps/peasBuild
FORTRAN_LIB_DIR=/opt/apps/lib
FORTRAN_INC_DIR=/opt/apps/include
GIT_DIR=${BUILD_DIR}/git
JAVA_INCLUDE_PATH=$JAVA_HOME/include



for var in "$@"
do
    if [ $var = "-l" ]; then
	LIST=1
	elif [ $var = "-i" ]; then
	NEXT_VAR="INSTALL"
	INSTALL=1
	
	shift
    elif [ $var = "-p" ]; then
	NEXT_VAR="PUBLISH"
	PUBLISH=1
	shift
    else  
        if [ "$NEXT_VAR" == "PUBLISH" ]; then
          VERSION=$var
          NEXT_VAR="" 
	elif [ "$NEXT_VAR" == "INSTALL" ]; then
          VERSION=$var
          NEXT_VAR=""
       fi
    fi
done

# list
if [ -z "$LIST" ]; then
   echo ""
else
   java -cp $GIT_DIR/pcs-j2f/target/classes org.tmt.aps.peas.j2f.tools.VersionTool -l $MAVEN_REPO_HOME/org/tmt/libpeas
fi

# publish
if [ -z "$PUBLISH" ]; then
   echo ""
else

   if [ -z "$VERSION" ]; then
      echo "no version specified, please specify a version"    
   else

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

# jar up the mod files
      cd $FORTRAN_INC_DIR
      jar -cvf mods.jar *.mod
      mvn install:install-file -Dfile=mods.jar -DgroupId=org.tmt -DartifactId=modFiles -Dversion=$VERSION -Dpackaging=jar



   fi
   
   
fi


# deploy 
if [ -z "$INSTALL" ]; then
   echo ""
else
   if [ -z "$VERSION" ]; then
      echo "using latest published version to install"
      VERSION="$(java -cp $GIT_DIR/pcs-j2f/target/classes org.tmt.aps.peas.j2f.tools.VersionTool -v $MAVEN_REPO_HOME/org/tmt/libpeas 2>&1)"
   fi

   cp ${MAVEN_REPO_HOME}/org/tmt/libpeas/$VERSION/*.so ${FORTRAN_LIB_DIR}/libpeas.so

   cp ${MAVEN_REPO_HOME}/org/tmt/modFiles/$VERSION/*.jar ${FORTRAN_INC_DIR}/mods.jar

   cd ${FORTRAN_INC_DIR}

   jar -xvf mods.jar

   cd ${BUILD_DIR}

   echo "Installation of fortran library version $VERSION completed"
   
fi

