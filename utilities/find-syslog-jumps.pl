#!/usr/bin/perl -w

$lasttime = 0;
$delta = 2;     # in seconds
while(<>) {
    next unless(/flowvisor:/);
    chomp;
    @line = split;
    @time = split /:/, $line[2];
    $nowtime = $time[0] * 3600 + $time[1] * 60 + $time[2];
    if ( $lasttime > 0 and (($nowtime - $lasttime) > $delta)) {
        print "---------------------\n";
        print $lastline, "\n";
        print $_, "\n";
    }
# Jul 15 09:50:32 expedient flowvisor: ALERT-slicer_naxos-33102_ID__expedient_clemson_edu_9_dpid=06:d6:00:26:f1:3f:e4:80: STARVING: handling event took 16ms: org.flowvisor.events.FVIOEvent@4077db

    if (/STARVING: handling took (\d+)ms:/){
        print "###### ", $_, "\n" if ($1 > 1500);
    }
    $lasttime = $nowtime;
    $lastline = $_;
}
