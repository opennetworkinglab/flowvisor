#!/bin/sh

# Deb
# Assumes mini-dinstall has initialized (run once like below)
mini-dinstall -c /var/packages/repo/mini-dinstall.conf -b
s3cmd --verbose --acl-public --recursive sync /var/packages/repo/debian/stable s3://updates.onlab.us/debian/
s3cmd --verbose --acl-public --recursive sync /var/packages/repo/debian/staging s3://updates.onlab.us/debian/
s3cmd --verbose --acl-public --recursive sync /var/packages/repo/debian/unstable s3://updates.onlab.us/debian/

# Rpm
# Assumes createrepo has initialized all three directories (run w/o --update)
createrepo --update --database /var/packages/repo/rpm/stable
createrepo --update --database /var/packages/repo/rpm/staging
createrepo --update --database /var/packages/repo/rpm/unstable
s3cmd --verbose --acl-public --recursive sync /var/packages/repo/rpm/stable s3://updates.onlab.us/rpm/
s3cmd --verbose --acl-public --recursive sync /var/packages/repo/rpm/staging s3://updates.onlab.us/rpm/
s3cmd --verbose --acl-public --recursive sync /var/packages/repo/rpm/unstable s3://updates.onlab.us/rpm/
