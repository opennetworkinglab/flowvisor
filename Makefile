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

tests: all setup-db unit-tests rmdb

setup-db:
	./scripts/derby-interact.sh ./scripts/FlowVisorDB.sql > /dev/null

rmdb:
	rm -rf FlowVisorDB

unit-tests:
	ant tests

emma:
	ant emma-report

emma-report: setup-db emma rmdb

regress:
	./scripts/run-tests.sh $(REV)

regression: setup-db regress rmdb regressclean

count: 
	@find src -name \*.java | xargs wc -l | sort -n

install: all
	./scripts/install-script.sh 

pkg-install: all
	./scripts/install-package.sh 


whitespace:
	./scripts/fix_trailing_whitespace.pl -fix `find src -name \*.java`

regressclean:
	rm -rf flowvisor-test

clean:
	ant clean
	rm -rf pkgbuild

emmaclean:
	rm -rf inst
	rm -rf coverage
