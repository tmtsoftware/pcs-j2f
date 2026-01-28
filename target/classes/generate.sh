#!/bin/bash

export GIT_HOME=~/Desktop/Prototyping/

rm -rf /opt/apps/j2f/staging/*
mkdir -p /opt/apps/j2f/staging/org/tmt/aps/peas/lang/interop
cp $GIT_HOME/pcs-fortran/src/main/java/org/tmt/aps/peas/lang/interop/RetVal.java $1/org/tmt/aps/peas/lang/interop/
cp $GIT_HOME/pcs-fortran/src/structures.f90 $1
cp $GIT_HOME/pcs-fortran/src/logWrite.f90 $1

java -classpath $GIT_HOME/pcs-j2f/target/j2f-jar-with-dependencies.jar org.tmt.aps.peas.j2f.J2FCodeGenerator ~/Desktop/Prototyping/pcs-fortran/src /opt/apps/j2f/staging
   



