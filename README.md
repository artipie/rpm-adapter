<img src="https://www.artipie.com/logo.svg" width="64px" height="64px"/>

[![EO principles respected here](https://www.elegantobjects.org/badge.svg)](https://www.elegantobjects.org)
[![DevOps By Rultor.com](http://www.rultor.com/b/artipie/rpm-adapter)](http://www.rultor.com/p/artipie/rpm-adapter)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Build Status](https://img.shields.io/travis/yegor256/rpm-files/master.svg)](https://travis-ci.org/yegor256/rpm-files)
[![Javadoc](http://www.javadoc.io/badge/com.artipie/rpm-adapter.svg)](http://www.javadoc.io/doc/com.artipie/rpm-adapter)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/com.artipie/rpm-adapter/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/artipie/rpm-adapter)](https://hitsofcode.com/view/github/artipie/rpm-adapter)
[![Maven Central](https://img.shields.io/maven-central/v/com.artipie/rpm-adapter.svg)](https://maven-badges.herokuapp.com/maven-central/com.artipie/rpm-adapter)
[![PDD status](http://www.0pdd.com/svg?name=artipie/rpm-adapter)](http://www.0pdd.com/p?name=artipie/rpm-adapter)

This Java library turns your binary storage
(files, S3 objects, anything) into an RPM repository.
You may add it to your binary storage and it will become
a fully-functionable RPM repository, which
[`yum`](https://en.wikipedia.org/wiki/Yum_%28software%29) and
[`dnf`](https://en.wikipedia.org/wiki/DNF_%28software%29)
will perfectly understand.

Similar solutions:

  * [Artifactory](https://www.jfrog.com/confluence/display/RTF/RPM+Repositories)
  * [Pulp](https://pulp-rpm.readthedocs.io/en/latest/)

Some valuable references:

  * [RPM format](https://rpm-packaging-guide.github.io/)
  * [Yum repository internals](https://blog.packagecloud.io/eng/2015/07/20/yum-repository-internals/) (blog post)
  * [YUM repository and package management: Complete Tutorial](https://www.slashroot.in/yum-repository-and-package-management-complete-tutorial) (blog post)
  * [The Origin of RPM Content](https://docs.pulpproject.org/en/2.9/plugins/pulp_rpm/tech-reference/rpm.html)

This is the dependency you need:

```xml
<dependency>
  <groupId>com.artipie</groupId>
  <artifactId>rpm-adapter</artifactId>
  <version>[...]</version>
</dependency>
```
 
Then, you implement `com.yegor256.rpm.Storage` interface.

Then, you make an instance of `Rpm` class with your storage
as an argument. Finally, you put your artifacts to the storage
and instruct `Rpm` to update the meta info:

```java
import com.artipie.rpm.Rpm;
Rpm rpm = new Rpm(storage);
rpm.update("test/my.rpm");
```

To update the whole repository with all RPM files use `batchUpdate` method of `Rpm` class:
```java
import com.artipie.rpm.Rpm;
Rpm rpm = new Rpm(storage);
rpm.batchUpdate("test");
```

Read the [Javadoc](http://www.javadoc.io/doc/com.yegor256/rpm-files)
for more technical details.

### Naming policy

RPM may use different names to store metadata files in the package,
by default `NamingPolicy.DEFAULT` will be used, to change naming policy use
secondary constructor of `Rpm` to configure it. For instance to add `SHA1` prefixes for metadata files
use `new Rpm(storage, new NamingPolicy.Sha1Prefixed())`.

## How it works?

First, you upload your `.rpm` artifact to the repository. Then,
you call `update()` and these four system XML files are updated
in the `repodata` directory:
`repomd.xml`, `primary.xml.gz`, `filelists.xml.gz`, and `others.xml.gz`.

Examples of these files you can find in
[this repo](https://download.docker.com/linux/centos/7/source/stable/repodata/).

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.
