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
package com.artipie.rpm;

import org.apache.commons.cli.Option;

/**
 * Rpm repository configuration options. These options are used in rpm-adapter CLI mode and
 * on Artipie storage configuration level.
 * @since 0.10
 */
public enum RpmOptions {

    /**
     * Digest option.
     */
    DIGEST(
        "digest", "dgst",
        "(optional, default sha256) configures Digest instance for Rpm: sha256 or sha1"
    ),

    /**
     * Naming policy option.
     */
    NAMING_POLICY(
        "naming-policy", "np",
        "(optional, default plain) configures NamingPolicy for Rpm: plain, sha256 or sha1"
    ),

    /**
     * FileLists option.
     */
    FILELISTS(
        "filelists", "fl",
        "(optional, default true) includes File Lists for Rpm: true or false"
    );

    /**
     * Option full name.
     */
    private final String name;

    /**
     * Command line argument.
     */
    private final String arg;

    /**
     * Description.
     */
    private final String desc;

    /**
     * Ctor.
     * @param name Option full ame
     * @param opt Option
     * @param desc Description
     */
    RpmOptions(final String name, final String opt, final String desc) {
        this.name = name;
        this.arg = opt;
        this.desc = desc;
    }

    /**
     * Option name.
     * @return String name
     */
    public final String optionName() {
        return this.name;
    }

    /**
     * Builds command line option.
     * @return Instance of {@link Option}.
     */
    public final Option option() {
        return Option.builder(this.name.substring(0, 1))
            .argName(this.arg)
            .longOpt(this.name)
            .desc(this.desc)
            .hasArg()
            .build();
    }
}
