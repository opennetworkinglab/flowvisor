#!/bin/sh


REV=$1
REPO=git://github.com/OPENNETWORKINGLAB/flowvisor-test.git
./scripts/fvconfig.sh generateCert

git clone $REPO
cd flowvisor-test
git checkout $REV

cd tests

./fvt --verbose --fv-cmd=../../scripts/flowvisor-emma.sh
