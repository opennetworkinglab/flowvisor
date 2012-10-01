#!/bin/sh

fv_main=src/org/flowvisor/FlowVisor.java

if [ "X$1" = "X" ] ; then
    echo "Usage: $0 release-type" >&2
    exit 1
fi

rtype=$1

release=`sed -n '/FLOWVISOR_VERSION = [^ ]\+/p' $fv_main | awk {' print $7 '} | sed 's/;//' | sed 's/\"//g'`

echo "Making debian package (using sudo)" >&2
version=`echo $release | sed -e 's/^flowvisor-//'`
sudo ./scripts/make-deb.sh $version $rtype
sudo make clean
sudo chown -R jenkins:nogroup ./scripts/DEB
