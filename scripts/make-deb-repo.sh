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
rm -rf $base/pkgbuild
make prefix=/usr root=$base/pkgbuild/root fvuser=$USER fvgroup=$GROUP pkg-install

mkdir -p $base/pkgbuild/root/etc/init.d
# cp ./scripts/fv-startup.sh $base/pkgbuild/root/etc/init.d/flowvisor
chmod 755 $base/pkgbuild/root/etc/init.d/flowvisor

mkdir -p $base/pkgbuild/debian
cd $base/pkgbuild/debian

cat > control << EOF
Source: flowvisor
Maintainer: Ali Al-Shabibi <ali.al-shabibi@onlab.us>
Section: misc
Priority: optional

Package: flowvisor
Version: $version
Architecture: $ARCH
Description: The OpenFlow FlowVisor
Depends: openjdk-6-jre-headless, python (>= 2.6), logrotate
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

# creating flowvisor group if he isn't already there
if ! getent group flowvisor >/dev/null; then
    # Adding system group: flowvisor.
    echo "Creating FlowVisor (flowvisor) group."
    addgroup --system flowvisor >/dev/null
fi

# creating flowvisor user if he isn't already there
if ! getent passwd flowvisor >/dev/null; then
    # Adding system user: flowvisor.
    echo "Creating FlowVisor (flowvisor) user."
    adduser \
      --system \
          --disabled-login \
      --ingroup flowvisor \
      --no-create-home \
      --home /nonexistent \
      --gecos "FlowVisor Hypervisor" \
      --shell /bin/false \
      flowvisor  >/dev/null
fi
EOF

cat > postinst << EOF
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

chown -R flowvisor:flowvisor /etc/flowvisor /usr/libexec/flowvisor
chown flowvisor:flowvisor /usr/bin/fvctl-json /usr/bin/fvctl-xml /usr/sbin/derby-interact /usr/sbin/flowvisor /usr/sbin/fvconfig
chown -R flowvisor:flowvisor /usr/share/db/flowvisor
chown -R flowvisor:flowvisor /usr/share/doc/flowvisor
chown -R flowvisor:flowvisor /var/log/flowvisor


if [ -d /usr/share/db/flowvisor/FlowVisorDB/seg0 ]; then
    echo "FlowVisorDB exists, leaving untouched."
else
    echo "Please run fvconfig generate to create and initialize the database"
fi
EOF

cat > changelog << EOF
flowvisor ($version-1) $type; urgency=low

  * Initial release

 -- Ali Al-Shabibi (FlowVisor) <ali.al-shabibi@onlab.us>  `date --rfc-822`

EOF

cat > rules << EOF
#!/usr/bin/make -f

%:
	dh \$@
EOF

# cat > files << EOF
# flowvisor_$version-1_amd64.deb misc optional
# EOF

cat > compat << EOF
7
EOF

cat > flowvisor.install << EOF
root/etc/* etc/
root/usr/* usr/
root/var/* var/
EOF

chmod 775 preinst
chmod 775 postinst
chmod 775 rules

cd ..

dpkg-buildpackage -b

cd ..

chown jenkins:nogroup *.deb
chown jenkins:nogroup *.changes
mv *.deb /var/packages/repo/debian/mini-dinstall/incoming
mv *.changes /var/packages/repo/debian/mini-dinstall/incoming
