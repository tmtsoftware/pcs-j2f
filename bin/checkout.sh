#!/bin/bash

BUILD_DIR=/opt/apps/peasBuild
GIT_DIR=${BUILD_DIR}/git

# pull into the GIT_DIR subdirectories for J2F and Fortran
cd ${GIT_DIR}/pcs-j2f
git pull
cd ${GIT_DIR}/pcs-fortran
git pull

