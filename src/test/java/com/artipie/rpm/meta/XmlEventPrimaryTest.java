/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import com.artipie.rpm.Digest;
import com.artipie.rpm.pkg.FilePackage;
import com.artipie.rpm.pkg.FilePackageHeader;
import com.fasterxml.aalto.stax.OutputFactoryImpl;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.file.Path;
import javax.xml.stream.XMLEventFactory;
import javax.xml.stream.XMLEventWriter;
import javax.xml.stream.XMLStreamException;
import org.hamcrest.MatcherAssert;
import org.junit.jupiter.api.Test;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link XmlEvent.Primary}.
 * @since 1.5
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class XmlEventPrimaryTest {

    @Test
    void writesPackageInfo() throws XMLStreamException, IOException {
        final Path file = new TestResource("abc-1.01-26.git20200127.fc32.ppc64le.rpm").asPath();
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final XMLEventWriter writer = new OutputFactoryImpl().createXMLEventWriter(bout);
        final XMLEventFactory events = XMLEventFactory.newFactory();
        writer.add(events.createStartDocument());
        writer.add(events.createStartElement("", "", "metadata"));
        writer.add(events.createNamespace("http://linux.duke.edu/metadata/common"));
        writer.add(events.createNamespace("rpm", "http://linux.duke.edu/metadata/rpm"));
        new XmlEvent.Primary().add(
            writer,
            new FilePackage.Headers(new FilePackageHeader(file).header(), file, Digest.SHA256)
        );
        writer.add(events.createEndElement("", "", "metadata"));
        writer.close();
        MatcherAssert.assertThat(
            bout.toByteArray(),
            CompareMatcher.isIdenticalTo(
                String.join(
                    "\n",
                    //@checkstyle LineLengthCheck (80 lines)
                    "<?xml version='1.0' encoding='UTF-8'?>",
                    "<metadata xmlns=\"http://linux.duke.edu/metadata/common\" xmlns:rpm=\"http://linux.duke.edu/metadata/rpm\">",
                    "<package type=\"rpm\">",
                    "    <name>abc</name>",
                    "    <arch>ppc64le</arch>",
                    "    <version epoch=\"0\" ver=\"1.01\" rel=\"26.git20200127.fc32\"/>",
                    "    <checksum type=\"sha256\" pkgid=\"YES\">b9d10ae3485a5c5f71f0afb1eaf682bfbea4ea667cc3c3975057d6e3d8f2e905</checksum>",
                    "    <summary>Sequential logic synthesis and formal verification</summary>",
                    "    <description>ABC is a growing software system for synthesis and verification of",
                    "            binary sequential logic circuits appearing in synchronous hardware",
                    "            designs. ABC combines scalable logic optimization based on And-Inverter",
                    "            Graphs (AIGs), optimal-delay DAG-based technology mapping for look-up",
                    "            tables and standard cells, and innovative algorithms for sequential",
                    "            synthesis and verification.",
                    "",
                    "            ABC provides an experimental implementation of these algorithms and a",
                    "            programming environment for building similar applications. Future",
                    "            development will focus on improving the algorithms and making most of",
                    "            the packages stand-alone. This will allow the user to customize ABC for",
                    "            their needs as if it were a toolbox rather than a complete tool.</description>",
                    "    <packager>Fedora Project</packager>",
                    "    <url>http://www.eecs.berkeley.edu/~alanmi/abc/abc.htm</url>",
                    "    <time file=\"1590055722\" build=\"1580489894\"/>",
                    "    <size package=\"19285\" installed=\"80136\" archive=\"0\"/>",
                    "    <location href=\"abc-1.01-26.git20200127.fc32.ppc64le.rpm\"/>",
                    "    <format>",
                    "        <rpm:license>MIT</rpm:license>",
                    "        <rpm:vendor>Fedora Project</rpm:vendor>",
                    "        <rpm:group>Unspecified</rpm:group>",
                    "        <rpm:buildhost>buildvm-ppc64le-04.ppc.fedoraproject.org</rpm:buildhost>",
                    "        <rpm:sourcerpm>abc-1.01-26.git20200127.fc32.src.rpm</rpm:sourcerpm>",
                    "        <rpm:header-range start=\"4504\" end=\"10412\"/>",
                    "        <rpm:provides>",
                    "            <rpm:entry name=\"abc\" ver=\"1.01\" epoch=\"0\" rel=\"26.git20200127.fc32\"/>",
                    "            <rpm:entry name=\"abc(ppc-64)\" ver=\"1.01\" epoch=\"0\" rel=\"26.git20200127.fc32\"/>",
                    "        </rpm:provides>",
                    "        <rpm:requires>",
                    "            <rpm:entry name=\"abc-libs(ppc-64)\" ver=\"1.01\" epoch=\"0\" rel=\"26.git20200127.fc32\"/>",
                    "            <rpm:entry name=\"libabc.so.0()(64bit)\" ver=\"1.01\" epoch=\"0\" rel=\"26.git20200127.fc32\"/>",
                    "            <rpm:entry name=\"libc.so.6()(64bit)\"/>",
                    "            <rpm:entry name=\"libc.so.6(GLIBC_2.17)(64bit)\"/>",
                    "            <rpm:entry name=\"rtld(GNU_HASH)\"/>",
                    "        </rpm:requires>",
                    "        <file>/usr/bin/abc</file>",
                    "        <file type=\"dir\">/usr/lib/.build-id/cb</file>",
                    "        <file>/usr/lib/.build-id/cb/263ca8bdb2d581b1a12e604c0e30635e69c889</file>",
                    "        <file type=\"dir\">/usr/share/doc/abc</file>",
                    "        <file>/usr/share/doc/abc/README.md</file>",
                    "        <file>/usr/share/doc/abc/readmeaig</file>",
                    "        <file>/usr/share/man/man1/abc.1.gz</file>",
                    "    </format>",
                    "</package>",
                    "</metadata>"
                ).getBytes()
            )
                .ignoreWhitespace()
                .ignoreElementContentWhitespace()
                .normalizeWhitespace()
                .withAttributeFilter(attr -> !"file".equals(attr.getName()))
        );
    }

}
