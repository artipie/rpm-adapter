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

import com.jcabi.log.Logger;
import com.jcabi.xml.XMLDocument;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import org.w3c.dom.Node;
import org.xembly.Directives;
import org.xembly.Xembler;

/**
 * One update to an XML file.
 *
 * @author Yegor Bugayenko (yegor256@gmail.com)
 * @version $Id$
 * @since 0.1
 */
final class Update {

    /**
     * The path of XML.
     */
    private final Path xml;

    /**
     * Ctor.
     * @param path The path of XML file
     */
    Update(final Path path) {
        this.xml = path;
    }

    /**
     * Apply an update.
     *
     * @param dirs Directives
     * @throws IOException If fails
     */
    public void apply(final Directives dirs) throws IOException {
        final Node output;
        if (this.xml.toFile().exists() && this.xml.toFile().length() > 0L) {
            output = new Xembler(dirs).applyQuietly(
                new XMLDocument(this.xml.toFile()).node()
            );
        } else {
            output = new Xembler(dirs).domQuietly();
        }
        final String doc = new XMLDocument(output).toString();
        Files.write(this.xml, doc.getBytes(StandardCharsets.UTF_8));
        Logger.debug(this, "Saved:\n%s", doc);
    }

}
