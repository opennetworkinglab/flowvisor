#!/bin/sh

if [ $# -ne 1 ] ; then
    echo "Usage: $0 version-string" >&2
    exit 1
fi
if [ ! -f Makefile ] ; then 
    echo "Run me in the base of the source directory; no Makefile found" >&2
    exit 1
fi

if [ `id -u` != 0 ]; then
    echo "You're not running as root (well, uid=0) so will likely get errors" >&2
fi

version=$1
ARCH=`dpkg --print-architecture`

base=`pwd`
 rm -rf $base/pkgbuild/root
make prefix=/usr root=$base/pkgbuild/root fvuser=flowvisor fvgroup=flowvisor install

 mkdir -p $base/pkgbuild/root/etc/init.d
# cp ./scripts/fv-startup.sh $base/pkgbuild/root/etc/init.d/flowvisor
 chmod 755 $base/pkgbuild/root/etc/init.d/flowvisor

 mkdir -p $base/pkgbuild/root/DEBIAN
cd $base/pkgbuild/root/DEBIAN
 cat > control << EOF
Package: flowvisor
Version: $version
Architecture: $ARCH
Maintainer: Ali Al-Shabibi <alshabib@stanford.edu>
Section: misc
Priority: optional
Description: The OpenFlow FlowVisor
Depends: openjdk-6-jre
EOF

cd ..
# chown -Rh root .
# chgrp -Rh root .
# find . \( -type f -o -type d \) -exec chmod ugo-w {} \;
# chown -Rh openflow:openflow ./usr/etc/flowvisor
# chmod 2775 ./usr/etc/flowvisor
# chmod u+w ./usr/etc/flowvisor/*
# chmod u+w DEBIAN

cd ..
dir=unstable/binary-${ARCH}
mkdir -p $base/scripts/DEB/$dir
ctlfile="root/DEBIAN/control"
pkgname=$(grep "^Package:" ${ctlfile} | awk '{print $2}')
arch=$(grep "^Architecture:" ${ctlfile} | awk '{print $2}')
tgtfile="${pkgname}_${version}_${arch}.deb"
dpkg -b root $tgtfile
cp $tgtfile $base/scripts/DEB/$dir
cd  $base/scripts/DEB
make
