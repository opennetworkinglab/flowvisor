#!/bin/sh

fv_main=src/org/flowvisor/FlowVisor.java

if [ "X$1" = "X" ] ; then
    echo "Usage: $0 release-name" >&2
    exit 1
fi

release=$1
sed -i "s/FLOWVISOR_VERSION = [^ ]\+/FLOWVISOR_VERSION = \"$release\";/" $fv_main
hg diff $fv_main

read -p "Are these changes what you wanted? 'y' to continue, anything else to abort " res
if [ "X$res" != "Xy" ] ; then
    echo "Aborting; resolve by hand..." >&2
    exit 2
fi


make tests
if [ $? != 0 ] ; then
    echo "make tests seem to have failed... aborting release" >&2
    exit 1
fi

#git commit -m "Release $release" $fv_main
#git tag $release

echo "Making debian package (using sudo)" >&2
version=`echo $release | sed -e 's/^flowvisor-//'`
sudo ./scripts/make-deb.sh $version

echo "If you're happy, do 'git push --tags'" >&2

