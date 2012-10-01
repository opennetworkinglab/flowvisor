#!/usr/bin/perl -w

$FIX='-fix';
$SHOW='-show';


$cmd = shift @ARGV or usage("need to specify -show or -fix");

usage("invalid command $cmd") unless ($cmd eq $FIX or $cmd eq $SHOW);

$linecount=0;
foreach $file (@ARGV)
{
	open F, "$file" or die "open (readonly) : $!";
	if($cmd eq $FIX)
	{
		open OUT, ">$file.tmp" or die "open (writing): $!";
	}
	while(<F>)
	{
		$linecount++;
		if($cmd eq $FIX)
		{
			s/(\s+)(\s)$/$2/;
			print OUT;
		}
		else 
		{
			print "$file:$linecount : $_" if(/\s+\s$/);
		}
	}
	if($cmd eq $FIX)
	{
		close OUT;
		if( -s "$file.tmp")
		{
			rename("$file.tmp","$file");
		}
		else
		{
			die "$file.tmp zero length... not moving into place!: $!";
		}
	}
}


sub usage
{
	my $msg = join(" ",@_);
	print STDERR << "EOF";
fix_trailing_whitespace.pl <-show|-fix> file1 [file2 [...]]
	-show will list lines with trailing whitespace
	-fix  will remove the trailing white space
EOF
	print STDERR "\n\n$msg\n";
	exit(1);
}
