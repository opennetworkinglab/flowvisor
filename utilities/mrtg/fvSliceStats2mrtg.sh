#!/bin/bash

### usage: fvSliceStats <SLICE_NAME> <DPID>

SED=/bin/sed
GREP=/bin/grep
FVCTL=/var/local/bin/fvctl

slice=$1
dpid=$2

host=`hostname`
uptime=`uptime | sed -e 's/, .*$//'`

stats=`$FVCTL --passwd-file=/mrtg/.fvp getSliceStats $slice 2> /dev/null`

if [ $? -ne 0 ]; then exit 255; fi

switchstats=`echo $stats | $GREP $dpid | $GREP ,PACKET_IN`

if [ $? -ne 0 ]; then exit 254; fi

packetIn=`echo $switchstats | $SED -e 's/^.*PACKET_IN=//' -e 's/,.*$//'`

outstats=`echo $switchstats | $GREP ,PACKET_OUT`

if [ $? -eq 0 ]; then
	packetOut=`echo $outstats | $SED -e 's/^.*PACKET_OUT=//' -e 's/,.*$//' -e 's/ ---.*$//'`
else
	packetOut=0
fi

echo $packetIn
echo $packetOut
echo $uptime
echo $host
