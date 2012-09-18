dir=$(type)/binary-all
all:
	dpkg-scanpackages $(dir)/ | gzip -9c > $(dir)/Packages.gz
	@echo Now copy tree to yuba:
	@echo "tar cpf - . | ssh yuba 'cd /hpn/home/httpd/htdocs/openflow/downloads/GENI/DEB && tar xvf -'"
