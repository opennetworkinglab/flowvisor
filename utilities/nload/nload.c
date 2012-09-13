/*
 * nload
 *
 * Copyright (C) 2002, Rob Sherwood <capveg@myleft.net>
 * 	$Id: nload.c,v 1.12 2004/01/07 19:00:50 capveg Exp $
 *
 * This program is free software; you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation; either version 2 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place, Suite 330, Boston, MA  02111-1307  USA
 *
 */

#include <pcap.h> 
#include <stdio.h> 
#include <stdlib.h> 
#include <unistd.h>
#include <signal.h>
#include <string.h>
#include <pthread.h>


#define MYSNAPLEN	256
#ifndef BUFLEN
#define BUFLEN 8192
#endif
unsigned long BYTE_COUNT;
unsigned long PKT_COUNT;
unsigned long BYTE_AVG;
unsigned long PKT_AVG;
unsigned long ITERATIONS;
unsigned long MAX_ITERATIONS=60;
long msSleep=1000;
double BYTE_MAX;
unsigned long PKT_MAX;
#define DEFAULT_FILTER ""

#define MIN(x,y) ((x)<(y)?(x):(y))

void packetHandler(u_char *, const struct pcap_pkthdr *, const u_char *);
void handleAlarm(int);
void usage(char *,char *);
void * printerThread(void *);

int LinedOutput=0;
pthread_t PrinterThread;


int main(int argc, char * argv[]) {
	char *dev=NULL, errbuf[PCAP_ERRBUF_SIZE];
	pcap_t *handle;
	struct bpf_program filter;
	bpf_u_int32 mask=0, net=0;
	char FILTER_STR[BUFLEN]; 
	int i;

	BYTE_COUNT=0;
	PKT_COUNT=0;
	BYTE_AVG=0;
	PKT_AVG=0;
	ITERATIONS=0;
	BYTE_MAX=0;
	PKT_MAX=0;
	/* parse args */
	while(argc>1){	// horrible args parsing, should use getopt()
		if((!strcmp("-h",argv[1])) ||
			(!strcmp("--help",argv[1])))
			usage(NULL,NULL);
		else if(!strcmp("-l",argv[1])){
			LinedOutput=1;
			argc--; argv++;
		} else if(!strcmp("-i",argv[1])){
			if(argc>2){
				dev=argv[2];
				argc--; argv++;
				argc--; argv++;
			} else {
				usage("-i takes an option",NULL);
			}
		} else if(!strcmp("-t",argv[1])){
			if(argc>2){
				msSleep=atoi(argv[2]);
				argc--; argv++;
				argc--; argv++;
			} else {
				usage("-t takes an option",NULL);
			}

		} else if(!strncmp("-",argv[1],1)){
			usage("unknown option",argv[1]);
		} else break;
	}
	/* copy remainning args to filter string*/
	memset(&FILTER_STR,0,BUFLEN);
	if(argc>1){
		for(i=1;i<argc;i++){
			strncat(FILTER_STR,argv[i],BUFLEN);
			strncat(FILTER_STR," ",1);
		}
	} else {
		strncpy(FILTER_STR,DEFAULT_FILTER,strlen(DEFAULT_FILTER));
	}
		

	if(!dev) dev = pcap_lookupdev(errbuf);
	signal(SIGALRM,handleAlarm);
	if(!dev){
		fprintf(stderr,"ER: pcap_lookupdev: %s\n",errbuf);
		exit(1);
	}

	if(pcap_lookupnet(dev,&net,&mask,errbuf) == -1){
		fprintf(stderr,"WARN: pcap_lookupnet: %s; ",errbuf);
		fprintf(stderr,"filter rules might fail\n");
	}
	fprintf(stderr,"Device: %s", dev);
	if(strcmp(FILTER_STR,""))
		fprintf(stderr,"\tFilter: %s", FILTER_STR);
	fprintf(stderr,"\n");
	handle = pcap_open_live(dev, MYSNAPLEN, 1, 0, errbuf);
	if(!handle){
		fprintf(stderr,"ER: pcap_open_live: %s\n",errbuf);
		exit(3);
	}
	if(pcap_compile(handle,&filter,FILTER_STR,1,net)==-1){
		fprintf(stderr,"ER: pcap_compile: %s\n",errbuf);
		exit(4);
	}

	if(pcap_setfilter(handle,&filter ) == -1){
		fprintf(stderr,"ER: pcap_setfilter: %s\n",errbuf);
		exit(5);
	}
	
	if(msSleep>=1000)
		alarm(1);
	else 
		pthread_create(&PrinterThread,NULL,printerThread,NULL);
	if(pcap_loop(handle,-1,packetHandler,NULL) == -1){
		fprintf(stderr,"ER: pcap_loop returned -1\n");
		exit(6);
	}


	return(0);
}


void packetHandler(u_char * ignore, const struct pcap_pkthdr *hdr
		, const u_char * packet){
	ignore = ignore;
	packet=packet;
	BYTE_COUNT+=hdr->len;
	PKT_COUNT++;

}

void handleAlarm(int ignore){
	ignore=ignore;
	BYTE_AVG+=BYTE_COUNT;
	PKT_AVG+=PKT_COUNT;
	ITERATIONS++;
	if(BYTE_MAX<((double)BYTE_COUNT/1024))
		BYTE_MAX=(double)BYTE_COUNT/1024;
	if(PKT_MAX<((double)PKT_COUNT))
		PKT_MAX=(double)PKT_COUNT;
	printf("\r%8.2f KB/S %ld p/s ",
                (double)BYTE_COUNT/1024,
                PKT_COUNT);
	// if we have less than a tenth of
	// 	the number of iterations we want
	// 	wait before printing an average
	if(ITERATIONS>(MAX_ITERATIONS/10))
		printf("\t\tAVG=%8.2f KB/S %6.2f p/s", 
			(double)BYTE_AVG/(1024*ITERATIONS),
			(double)PKT_AVG/(ITERATIONS)
            );
	else 
		printf("\t\t\tAVG=%8s KB/S", "-");
	printf("\tMAX=%8.2f %ld p/s%s", BYTE_MAX,PKT_MAX,LinedOutput?"\n":"");
	// once we grow too big, cut our data in half
	//  	to hopefully prevent wrapping
	if(ITERATIONS>MAX_ITERATIONS){
		ITERATIONS=ITERATIONS/2;
		BYTE_AVG=BYTE_AVG/2;
		PKT_AVG=PKT_AVG/2;
	}

	fflush(stdout);
	BYTE_COUNT=0;
	PKT_COUNT=0;
	alarm(1);
}

void usage(char *s1, char *s2){
	if(s1) fprintf(stderr,"%s",s1);
	if(s2) fprintf(stderr," %s",s2);
	fprintf(stderr,"\n");
	fprintf(stderr,
		"Usage:\n\tnload [-t ms] [-l] [-i device] [-h] [tcpdump-filter-string]\n");
	exit(-1);
}

void * printerThread(void * ignore){
	struct timeval start,now;
	struct timespec sleep;
	long msElapsed;

	gettimeofday(&start,NULL);
	sleep.tv_sec=0;
	sleep.tv_nsec=msSleep*1000000;
	while(1){
		nanosleep(&sleep,NULL);
		gettimeofday(&now,NULL);
		msElapsed=1000*(now.tv_sec-start.tv_sec)+(now.tv_usec-start.tv_usec)/1000;
		// print in a totally different format :)
		printf("%ld %ld %ld\n",msElapsed, BYTE_COUNT, PKT_COUNT);
		BYTE_COUNT=0;	// reset count
		PKT_COUNT=0;	// reset count
	}
	return NULL;
}
