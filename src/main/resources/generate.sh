#!/bin/bash

#verify there is at least one input argument
if [ $# -eq 0 ]
  then
   echo "generate.sh [-e or  <dir>] -- program to generate J2F library and java stubs
    where:
       -e  Is for generation using eclipse workspace for fortran input and java output
       <dir>  directory with fortran files for input and java output will be in /opt/apps/j2f "
    exit 1
fi

rm -rf /tmp/tempDir*

if [[ $1 = "-e" ]]; then
   rm /opt/apps/j2f/J*.java
   java -classpath j2f-0.0.1-SNAPSHOT.jar org.tmt.aps.peas.j2f.J2FCodeGenerator ~/workspace/peas-pcs-fortran/src/
   cp /opt/apps/j2f/J*.java ~/workspace/pcs-web/src/main/java/org/tmt/aps/peas/lang/interop/
else
   java -classpath j2f-0.0.1-SNAPSHOT.jar org.tmt.aps.peas.j2f.J2FCodeGenerator $1
fi



