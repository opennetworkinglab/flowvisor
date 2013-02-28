#!/bin/sh

fv_main=src/org/flowvisor/FlowVisor.java

if [ "X$1" = "X" ] ; then
    echo "Usage: $0 release-type" >&2
    exit 1
fi

rtype=$1

release=`sed -n '/FLOWVISOR_VERSION = [^ ]\+/p' $fv_main | awk {' print $7 '} | sed 's/;//' | sed 's/\"//g'`
version=`echo $release | sed -e 's/^flowvisor-//'`

echo "Making debian package (using sudo)" >&2
sudo ./scripts/make-deb-repo.sh $version $rtype

echo "Making rpm package (using sudo)" >&2
sudo ./scripts/make-rpm-repo.sh $version $rtype

sudo make clean
