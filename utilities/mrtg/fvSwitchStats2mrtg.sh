#!/bin/bash

### usage: fvSwitchStats <DPID>

SED=/bin/sed
GREP=/bin/grep
FVCTL=/var/local/bin/fvctl

dpid=$1

host=`hostname`
uptime=`uptime | sed -e 's/, .*$//'`

stats=`$FVCTL --passwd-file=/mrtg/.fvp getSwitchStats $dpid 2> /dev/null`

if [ $? -ne 0 ]; then exit 255; fi

totals=`echo $stats | $GREP Total`

packetIn=`echo $totals | $SED -e 's/^.*PACKET_IN=//' -e 's/,.*$//'`
packetOut=`echo $totals | $SED -e 's/^.*PACKET_OUT=//' -e 's/,.*$//'`

echo $packetIn
echo $packetOut
echo $uptime
echo $host
