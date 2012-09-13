TARGET=nload
CC=gcc
CFLAGS+=-I/usr/include/pcap 
PLIBS+=-lpcap
LIBS+=-lpthread
DIR=$(TARGET)

CFLAGS+=-g -Wall


SRC=$(wildcard *.c)
OBJS=$(subst .c,.o,$(SRC))
TARBALL=$(patsubst %,$(DIR)/%,$(SRC) Makefile README)

all: $(TARGET)

$(TARGET): $(OBJS)
	$(CC) $(CFLAGS) $(LDFLAGS) -o $(TARGET) $(OBJS) $(LIBS) $(PLIBS)

install:
	cp nload /usr/local/sbin/nload
	chmod 755 /usr/local/sbin/nload

clean:
	rm -f $(TARGET) $(OBJS) core $(TARGET).tgz

%.o: %.c
	$(CC) $(CFLAGS) -c $< 

tar: $(TARGET).tgz
$(TARGET).tgz: 
	(cd ..  &&tar cpzf $(DIR)/$(TARGET).tgz $(TARBALL))
	

