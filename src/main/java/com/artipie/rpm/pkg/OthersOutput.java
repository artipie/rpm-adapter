/*
 * The MIT License (MIT)
 *
 * Copyright (c) 2020 artipie.com
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
package com.artipie.rpm.pkg;

import com.artipie.rpm.meta.XmlOthers;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import javax.xml.stream.XMLStreamException;

/**
 * Generates other.xml metadata file.
 * @since 0.4
 * @todo #124:30min Make this class be able to work this already existing others.xml by
 *  adding corresponding ctor and test it.
 */
public final class OthersOutput implements PackageOutput.FileOutput {

    /**
     * Path to write other.xml.
     */
    private final Path path;

    /**
     * Temporary file to partly processed other.xml.
     */
    private final Path tmp;

    /**
     * XML of others.
     */
    private final XmlOthers xml;

    /**
     * Ctor.
     * @param file Path to write filelists.xml
     */
    public OthersOutput(final Path file) {
        this.path = file;
        this.tmp = file.getParent().resolve(
            String.format("%s.part", file.getFileName().toString())
        );
        this.xml = new XmlOthers(this.tmp);
    }

    /**
     * Starts processing of RPMs.
     * @return Self
     * @throws IOException On error
     */
    public OthersOutput start() throws IOException {
        try {
            this.xml.startPackages();
        } catch (final XMLStreamException err) {
            throw new IOException("Failed to start packages", err);
        }
        return this;
    }

    @Override
    public void accept(final Package.Meta meta) throws IOException {
        final HeaderTags tags = new HeaderTags(meta);
        try {
            this.xml.addPackage(
                tags.name(), tags.arch(),
                meta.checksum().hex()
            ).version(tags.epoch(), tags.version(), tags.release())
                .changelog().close();
        } catch (final XMLStreamException err) {
            throw new IOException("Failed to add package", err);
        }
    }

    @Override
    public void close() throws IOException {
        this.xml.close();
        Files.move(this.tmp, this.path, StandardCopyOption.REPLACE_EXISTING);
    }

    @Override
    public Path file() {
        return this.path;
    }
}
