#!/bin/sh

if [ $# -ne 2 ] ; then
    echo "Usage: $0 version-string build-type" >&2
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
type=$2
ARCH="all"
USER="flowvisor"
GROUP="flowvisor"

base=`pwd`
 rm -rf $base/pkgbuild/root
make prefix=/usr root=$base/pkgbuild/root fvuser=$USER fvgroup=$GROUP pkg-install

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

cat > preinst << EOF
#!/bin/bash -e
#
# summary of how this script can be called:
#        * <new-preinst> install
#        * <new-preinst> install <old-version>
#        * <new-preinst> upgrade <old-version>
#        * <old-preinst> abort-upgrade <new-version>
#

. /usr/share/debconf/confmodule

if [ -n "$DEBIAN_SCRIPT_DEBUG" ]; then set -v -x; DEBIAN_SCRIPT_TRACE=1; fi
${DEBIAN_SCRIPT_TRACE:+ echo "#42#DEBUG# RUNNING $0 $*" 1>&2 }

# creating flowvisr group if he isn't already there
if ! getent group flowvisr >/dev/null; then
    # Adding system group: flowvisr.
    echo "Creating FlowVisor (flowvisr) group."
    addgroup --system flowvisr >/dev/null
fi

# creating flowvisr user if he isn't already there
if ! getent passwd flowvisr >/dev/null; then
    # Adding system user: flowvisr.
    echo "Creating FlowVisor (flowvisr) user."
    adduser \
      --system \
          --disabled-login \
      --ingroup flowvisr \
      --no-create-home \
      --home /nonexistent \
      --gecos "FlowVisor Hypervisor" \
      --shell /bin/false \
      flowvisr  >/dev/null
fi
EOF

cat > postrm << EOF
#!/bin/bash -e
#
# summary of how this script can be called:
#        * <new-preinst> install
#        * <new-preinst> install <old-version>
#        * <new-preinst> upgrade <old-version>
#        * <old-preinst> abort-upgrade <new-version>
#

. /usr/share/debconf/confmodule

if [ -n "$DEBIAN_SCRIPT_DEBUG" ]; then set -v -x; DEBIAN_SCRIPT_TRACE=1; fi
${DEBIAN_SCRIPT_TRACE:+ echo "#42#DEBUG# RUNNING $0 $*" 1>&2 }

# deleting flowvisr user/group
if getent passwd flowvisr > /dev/null; then
    echo "Removing FlowVisor user and group"
    deluser flowvisr > /dev/null
fi
if getent group flowvisr > /dev/null; then
    delgroup --only-if-empty flowvisr > /dev/null
fi
EOF

chmod 775 preinst
chmod 775 postrm

cd ..
# chown -Rh root .
# chgrp -Rh root .
# find . \( -type f -o -type d \) -exec chmod ugo-w {} \;
# chown -Rh openflow:openflow ./usr/etc/flowvisor
# chmod 2775 ./usr/etc/flowvisor
# chmod u+w ./usr/etc/flowvisor/*
# chmod u+w DEBIAN

cd ..
dir=$type/binary-${ARCH}
mkdir -p $base/scripts/DEB/$dir
ctlfile="root/DEBIAN/control"
pkgname=$(grep "^Package:" ${ctlfile} | awk '{print $2}')
arch=$(grep "^Architecture:" ${ctlfile} | awk '{print $2}')
tgtfile="${pkgname}_${version}_${arch}.deb"
dpkg -b root $tgtfile
cp $tgtfile $base/scripts/DEB/$dir
cd  $base/scripts/DEB
make type=$type
