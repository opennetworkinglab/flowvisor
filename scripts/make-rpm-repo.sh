#!/bin/sh

if [ $# -ne 2 ] ; then
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
type=$2
USER="flowvisor"
GROUP="flowvisor"
date=`date +"%a %b %d %Y"`
base=`pwd`

# Clean and setup RPM build environment
mkdir -p $base/pkgbuild/BUILD
mkdir -p $base/pkgbuild/RPMS
mkdir -p $base/pkgbuild/SOURCES
mkdir -p $base/pkgbuild/SPECS
mkdir -p $base/pkgbuild/SRPMS
mkdir -p $base/pkgbuild/BUILDROOT

if [ ! -d $base/pkgbuild/root ] ; then 
    make prefix=/usr root=$base/pkgbuild/BUILDROOT/flowvisor-$version-1.x86_64 fvuser=$USER fvgroup=$GROUP pkg-install;
else
    mv $base/pkgbuild/root $base/pkgbuild/BUILDROOT/flowvisor-$version-1.x86_64;
fi

cat > $base/pkgbuild/SPECS/flowvisor.spec << EOF
# Folder where RPM will be built
%define _topdir			$base/pkgbuild
# Disable rpmbuild macros for binary packages
%global __os_install_post 	%{nil}

Summary:	The OpenFlow FlowVisor
Name:		flowvisor
Version:	$version
Release:	1%{?dist}
Group:		Applications/Engineering
URL:		http://www.flowvisor.org
Packager:	Marc De Leenheer <marc@onlab.us>
Vendor:		Open Networking Laboratory (ON.Lab)
BuildArch:	noarch
BuildRoot:	%{_topdir}/BUILDROOT
Source:		%{name}-%{version}.tar.gz
License:	OpenFlow License
Requires:	java-1.6.0-openjdk, python >= 2.6, logrotate

%description
An OpenFlow controller that acts as a hypervisor/proxy
between a switch and multiple controllers.  Can slice
multiple switches in parallel, effectively slicing a network.

%pre
# creating flowvisor group if he isn't already there
if ! getent group flowvisor >/dev/null; then
    # Adding system group: flowvisor.
    echo "Creating FlowVisor (flowvisor) group."
    /usr/sbin/groupadd -r flowvisor >/dev/null
fi

# creating flowvisor user if he isn't already there
if ! getent passwd flowvisor >/dev/null; then
    # Adding system user: flowvisor.
    echo "Creating FlowVisor (flowvisor) user."
    /usr/sbin/adduser -c FlowVisor -r -d / -g flowvisor -M --shell /sbin/nologin flowvisor  >/dev/null
fi  

%post
if [ -d /usr/share/db/flowvisor/FlowVisorDB/seg0 ]; then
    echo "FlowVisorDB exists, leaving untouched."
else
    echo "Please run fvconfig generate to create and initialize the database"
fi

%files
%defattr(644, root, root, 755)
%config %attr(755, flowvisor, flowvisor) /etc/flowvisor
%attr(755, root, root) /etc/init.d/flowvisor
%attr(755, root, root) /usr/bin/fvctl-json
%attr(755, root, root) /usr/bin/fvctl-xml
%attr(755, root, root) /usr/bin/fvctl
/usr/libexec/flowvisor
%attr(755, root, root) /usr/sbin/derby-interact
%attr(755, root, root) /usr/sbin/flowvisor
%attr(755, root, root) /usr/sbin/fvconfig
%config %attr(755, flowvisor, flowvisor) /usr/share/db/flowvisor
%doc /usr/share/doc/flowvisor
%doc /usr/share/man/man1/fvconfig.1
%doc /usr/share/man/man1/fvctl.1
%doc /usr/share/man/man8/flowvisor.8
%attr(755, flowvisor, flowvisor) /var/log/flowvisor
%config /etc/logrotate.d/flowvisor

%clean
rm -r %buildroot

%changelog
* $date Marc De Leenheer <marc@onlab.us> $version-1
- Initial RPM release.
EOF

# Build binary RPM package (-bb binary only, -bs source only, -ba source and binary)
rpmbuild -bb $base/pkgbuild/SPECS/flowvisor.spec
# Sign RPM
rpm --addsign $base/pkgbuild/RPMS/noarch/flowvisor-$version-1.noarch.rpm

mv $base/pkgbuild/RPMS/noarch/flowvisor-$version-1.noarch.rpm /var/packages/repo/rpm/$type
