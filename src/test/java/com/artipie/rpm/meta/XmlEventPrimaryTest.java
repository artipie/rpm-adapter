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
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.CsvSource;
import org.xmlunit.matchers.CompareMatcher;

/**
 * Test for {@link XmlEventPrimary}.
 * @since 1.5
 */
@SuppressWarnings("PMD.AvoidDuplicateLiterals")
class XmlEventPrimaryTest {

    @ParameterizedTest
    @CsvSource({
        "abc-1.01-26.git20200127.fc32.ppc64le.rpm,abc_res.xml",
        "libnss-mymachines2-245-1.x86_64.rpm,libnss_res.xml",
        "openssh-server-7.4p1-16.h16.eulerosv2r7.x86_64.rpm,openssh_res.xml",
        "httpd-2.4.6-80.1.h8.eulerosv2r7.x86_64.rpm,httpd_res.xml",
        "felix-framework-4.2.1-5.el7.noarch.rpm,felix-framework-res.xml",
        "ant-1.9.4-2.el7.noarch.rpm,ant_res.xml",
        "dbus-1.6.12-17.el7.x86_64.rpm,dbus_res.xml",
        "compat-db47-4.7.25-28.el7.i686.rpm,compat_res.xml"
    })
    void writesPackageInfo(final String rpm, final String res) throws XMLStreamException,
        IOException {
        final Path file = new TestResource(rpm).asPath();
        final ByteArrayOutputStream bout = new ByteArrayOutputStream();
        final XMLEventWriter writer = new OutputFactoryImpl().createXMLEventWriter(bout);
        final XMLEventFactory events = XMLEventFactory.newFactory();
        writer.add(events.createStartDocument());
        writer.add(events.createStartElement("", "", "metadata"));
        writer.add(events.createNamespace("http://linux.duke.edu/metadata/common"));
        writer.add(events.createNamespace("rpm", "http://linux.duke.edu/metadata/rpm"));
        new XmlEventPrimary().add(
            writer,
            new FilePackage.Headers(new FilePackageHeader(file).header(), file, Digest.SHA256)
        );
        writer.add(events.createEndElement("", "", "metadata"));
        writer.close();
        MatcherAssert.assertThat(
            bout.toByteArray(),
            CompareMatcher.isIdenticalTo(
                new TestResource(String.format("XmlEventPrimaryTest/%s", res)).asBytes()
            )
                .ignoreWhitespace()
                .ignoreElementContentWhitespace()
                .normalizeWhitespace()
        );
    }

}
