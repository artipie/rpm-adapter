[![DevOps By Rultor.com](http://www.rultor.com/b/yegor256/rpm-files)](http://www.rultor.com/p/yegor256/rpm-files)
[![We recommend IntelliJ IDEA](https://www.elegantobjects.org/intellij-idea.svg)](https://www.jetbrains.com/idea/)

[![Javadoc](http://www.javadoc.io/badge/com.yegor256/rpm-files.svg)](http://www.javadoc.io/doc/com.yegor256/rpm-files)
[![License](https://img.shields.io/badge/license-MIT-green.svg)](https://github.com/yegor256/rpm-files/blob/master/LICENSE.txt)
[![Hits-of-Code](https://hitsofcode.com/github/yegor256/rpm-files)](https://hitsofcode.com/view/github/yegor256/rpm-files)
[![Maven Central](https://img.shields.io/maven-central/v/com.yegor256/rpm-files.svg)](https://maven-badges.herokuapp.com/maven-central/com.yegor256/rpm-files)
[![PDD status](http://www.0pdd.com/svg?name=yegor256/rpm-files)](http://www.0pdd.com/p?name=yegor256/rpm-files)

Turns your binary storage (files, S3 objects, anything) into an RPM repository.

Similar solutions:

  * [Artifactory](https://www.jfrog.com/confluence/display/RTF/RPM+Repositories)
  * [Pulp](https://pulp-rpm.readthedocs.io/en/latest/)

Some valuable references:

  * [RPM format](https://rpm-packaging-guide.github.io/)
  * [_Yum repository internals_](https://blog.packagecloud.io/eng/2015/07/20/yum-repository-internals/) (blog post)

This is the dependency you need:

```xml
<dependency>
  <groupId>com.yegor256</groupId>
  <artifactId>rpm-files</artifactId>
  <version>[...]</version>
</dependency>
```

Then, you implement `com.yegor256.rpm.Storage` interface.

Then, you make an instance of `Rpm` class with your storage
as an argument. Finally, you publish your artifacts via this
object, or manipulate them:

```java
import com.yegor256.rpm.Rpm;
Rpm rpm = new Rpm(storage);
rpm.publish(file); // add a single RPM artifact to the repo
// Do you need more operations? Submit a ticket.
```

Should work.

## How to contribute

Fork repository, make changes, send us a pull request. We will review
your changes and apply them to the `master` branch shortly, provided
they don't violate our quality standards. To avoid frustration, before
sending us your pull request please run full Maven build:

```
$ mvn clean install -Pqulice
```

To avoid build errors use Maven 3.2+.
