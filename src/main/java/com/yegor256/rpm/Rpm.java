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
package com.yegor256.rpm;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;

/**
 * The RPM front.
 *
 * First, you make an instance of this class, providing
 * your storage as an argument:
 *
 * <pre> Rpm rpm = new Rpm(storage);</pre>
 *
 * Then, you put your binary RPM artifact to the storage and call
 * {@link Rpm#update(String)}. This method will parse the RPM package
 * and update all the necessary meta-data files. Right after this,
 * your clients will be able to use the package, via {@code yum}:
 *
 * <pre> rpm.update("nginx.rpm");</pre>
 *
 * That's it.
 *
 * @since 0.1
 */
public final class Rpm {

    /**
     * The storage.
     */
    private final Storage storage;

    /**
     * Ctor.
     * @param stg The storage
     */
    public Rpm(final Storage stg) {
        this.storage = stg;
    }

    /**
     * Update the meta info by this artifact.
     *
     * @param key The name of the file just updated
     * @throws IOException If fails
     */
    public void update(final String key) throws IOException {
        synchronized (this.storage) {
            final Path temp = Files.createTempFile("rpm", ".rpm");
            this.storage.load(key, temp);
            final Pkg pkg = new Pkg(temp);
            final Repomd repomd = new Repomd(this.storage);
            repomd.update(
                "primary",
                file -> new Primary(file).update(key, pkg)
            );
            repomd.update(
                "filelists",
                file -> new Filelists(file).update(pkg)
            );
            repomd.update(
                "other",
                file -> new Other(file).update(pkg)
            );
        }
    }

}
