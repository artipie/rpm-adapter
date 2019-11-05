FROM centos
MAINTAINER Yegor Bugayenko <yegor256@gmail.com>
LABEL Description="Yum utils" Vendor="Yegor Bugayenko" Version="1.0"

RUN yum install -y yum-utils
RUN yum install -y createrepo
