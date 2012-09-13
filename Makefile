# Because I am old and crotchety and my fingers can't stop from running 
#	`make` commands

.PHONY: docs doc all test tests count install clean

all:
	ant

docs:
	ant javadoc

doc:
	ant javadoc

test: tests

tests: all unit-tests regression-tests 

unit-tests:
	ant tests

regression-tests:
	make -C regress tests

count: 
	@find src -name \*.java | xargs wc -l | sort -n

install: all
	./scripts/install-script.sh

whitespace:
	./scripts/fix_trailing_whitespace.pl -fix `find src -name \*.java`

clean:
	ant clean
	rm -rf pkgbuild
