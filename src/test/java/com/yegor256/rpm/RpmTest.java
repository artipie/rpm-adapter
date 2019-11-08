/**
 * The MIT License (MIT)
 *
 * Copyright (c) 2019 Yegor Bugayenko
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy
 * of this software and associated documentation files (the "Software"), to deal
 * in the Software without restriction, including without limitation the rights
 * to use, copy, modify, merge, publish, distribute, sublicense, and/or sell
 * copies of the Software, and to permit persons to whom the Software is
 * furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included
 * in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND, EXPRESS OR
 * IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY,
 * FITNESS FOR A PARTICULAR PURPOSE AND NON-INFRINGEMENT. IN NO EVENT SHALL THE
 * AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER
 * LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM,
 * OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE
 * SOFTWARE.
 */
package com.yegor256.rpm;

import com.jcabi.matchers.XhtmlMatchers;
import com.jcabi.xml.XMLDocument;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import org.cactoos.Scalar;
import org.cactoos.experimental.Threads;
import org.cactoos.iterable.Repeated;
import org.hamcrest.MatcherAssert;
import org.hamcrest.Matchers;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;

/**
 * Test case for {@link Rpm}.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.0.3
 */
public final class RpmTest {

    /**
     * Temp folder for all tests.
     */
    @Rule
    @SuppressWarnings("PMD.BeanMembersShouldSerialize")
    public TemporaryFolder folder = new TemporaryFolder();

    /**
     * Rpm storage works, in many threads.
     * @throws Exception If some problem inside
     */
    @Test
    @SuppressWarnings("unchecked")
    public void addsSingleRpm() throws Exception {
        final Storage storage = new Storage.Simple(
            this.folder.newFolder().toPath()
        );
        final Path bin = this.folder.newFile("x.rpm").toPath();
        final String key = "nginx-module-xslt-1.16.1-1.el7.ngx.x86_64.rpm";
        Files.copy(
            RpmITCase.class.getResourceAsStream(
                String.format("/%s", key)
            ),
            bin,
            StandardCopyOption.REPLACE_EXISTING
        );
        storage.save(key, bin);
        final Rpm rpm = new Rpm(storage);
        final int threads = 10;
        MatcherAssert.assertThat(
            new Threads<>(
                threads,
                new Repeated<Scalar<Boolean>>(
                    threads,
                    () -> {
                        rpm.update(key);
                        return true;
                    }
                )
            ),
            Matchers.iterableWithSize(threads)
        );
        final Path primary = this.folder.newFile("primary.xml").toPath();
        storage.load("repodata/primary.xml", primary);
        MatcherAssert.assertThat(
            new XMLDocument(new String(Files.readAllBytes(primary))),
            XhtmlMatchers.hasXPath(
                "/ns1:metadata/ns1:package[ns1:name='nginx-module-xslt']",
                "http://linux.duke.edu/metadata/common"
            )
        );
    }

}
