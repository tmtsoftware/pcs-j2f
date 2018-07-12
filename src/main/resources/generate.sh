#!/bin/bash

#verify there is at least one input argument
if [ $# -eq 0 ]
  then
   echo "generate.sh [-e or  <fortran dir>, <java source dir> ] -- program to generate J2F library and java stubs
    where:
       -e  Is for generation using eclipse workspace for fortran input and java output
       <dir>  directory with fortran files for input and java output will be in /opt/apps/j2f "
    exit 1
fi


if [[ $1 = "-e" ]]; then
   rm $2/J*.java
   cp ~/git/pcs-web/src/main/java/org/tmt/aps/peas/lang/interop/RetVal.java $2
   cp ~/git/pcs-fortran/src/structures.f90 $2
   cp ~/git/pcs-fortran/src/logWrite.f90 $2
   java -classpath ~/git/pcs-j2f/target/j2f-jar-with-dependencies.jar org.tmt.aps.peas.j2f.J2FCodeGenerator ~/git/pcs-fortran/src/ $2 
   cp $2/J*.java ~/git/pcs-web/src/main/java/org/tmt/aps/peas/lang/interop/
else
   java -classpath j2f-jar-with-dependencies.jar org.tmt.aps.peas.j2f.J2FCodeGenerator $1 $2
fi



