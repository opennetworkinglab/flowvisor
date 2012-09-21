#!/bin/sh


REV=$1
REPO=git://github.com/OPENNETWORKINGLAB/flowvisor-test.git

git clone $REPO
cd flowvisor-test
git checkout $REV

cd tools/munger
make install
cd ../../tests

./fvt --fv-cmd=../../scripts/flowvisor-emma.sh


