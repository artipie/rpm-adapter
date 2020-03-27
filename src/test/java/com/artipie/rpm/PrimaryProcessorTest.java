/*
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

package com.artipie.rpm;

import com.artipie.asto.Key;
import io.reactivex.Observable;
import java.io.IOException;
import java.net.URISyntaxException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import javax.xml.stream.XMLStreamException;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

/**
 * Stub for actual PrimaryProcess test. Does not perform any checks.
 * @since 0.4
 */
public final class PrimaryProcessorTest {
    /**
     * Temporary file.
     */
    private Path tmp;

    /**
     * PrimaryProcessor instance.
     */
    private PrimaryProcessor primary;

    @Test
    public void processSeveralPackages() {
        Observable.fromArray(
            "aom-1.0.0-8.20190810git9666276.el8.aarch64.rpm",
            "nginx-1.16.1-1.el8.ngx.x86_64.rpm"
        ).flatMapCompletable(
            key -> {
                final Pkg pkg = this.pkg(key);
                return this.primary.processNext(new Key.From(key), pkg);
            }
        ).andThen(
            this.primary.complete()
        ).blockingAwait();
    }

    @BeforeEach
    void setUp() throws IOException, XMLStreamException {
        this.tmp = Files.createTempFile("primary", ".xml");
        this.primary = new PrimaryProcessor(this.tmp, Digest.SHA256);
    }

    @AfterEach
    void tearDown() throws IOException {
        Files.delete(this.tmp);
    }

    private Pkg pkg(final String name) throws URISyntaxException {
        return new Pkg(
            Paths.get(
                PrimaryProcessorTest.class.getResource(
                    String.format("/%s", name)
                ).toURI()
            )
        );
    }
}
