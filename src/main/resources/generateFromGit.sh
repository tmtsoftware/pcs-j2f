#!/bin/bash

#verify there is at least one input argument
if [ $# -eq 0 ]
  then
   echo "generateFromGit.sh [<java source dir> <git base dir> ] -- program to generate J2F library and java stubs
    where:
       <dir>  directory with fortran files for input and java output will be in /opt/apps/j2f "
    exit 1
fi


rm $1/J*.java
cp $2/pcs-web/src/main/java/org/tmt/aps/peas/lang/interop/RetVal.java $1
cp $2/pcs-fortran/src/structures.f90 $1
cp $2/pcs-fortran/src/logWrite.f90 $1
java -classpath $2/pcs-j2f/target/j2f-jar-with-dependencies.jar org.tmt.aps.peas.j2f.J2FCodeGenerator $2/pcs-fortran/src/ $1 
cp $1/J*.java $2/pcs-web/src/main/java/org/tmt/aps/peas/lang/interop/




