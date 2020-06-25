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

import org.apache.commons.lang3.NotImplementedException;

/**
 * Repository configuration.
 * @since 0.10
 */
public interface RepoConfig {

    /**
     * Repository digest.
     * @return Digest algorithm
     */
    Digest digest();

    /**
     * Repository naming policy.
     * @return Naming
     */
    NamingPolicy naming();

    /**
     * Is filelists.xml metadata required?
     * @return True if filelists.xml is needed, false otherwise
     */
    boolean filelists();

    /**
     * Repository configuration from yaml.
     * @since 0.10
     * @todo #281:30min Implement this class to read repository settings from yaml.
     *  Format:
     *  |settings:
     *  |  digest: sha256
     *  |  naming-policy: sha1
     *  |  filelists: true
     *  as described in https://github.com/artipie/artipie/issues/227. For yaml parsing use eo-yaml
     *  (check example in artipie/artipie AuthFromYaml class) and accept `YamlMapping` instance
     *  into ctor (and probably keep it as field). Do not forget about test.
     */
    final class FromYaml implements RepoConfig {

        @Override
        public Digest digest() {
            throw new NotImplementedException("Not implemented");
        }

        @Override
        public NamingPolicy naming() {
            throw new NotImplementedException("Not done yet");
        }

        @Override
        public boolean filelists() {
            throw new NotImplementedException("To do");
        }
    }

    /**
     * Simple.
     * @since 0.10
     */
    final class Simple implements RepoConfig {

        /**
         * Digest.
         */
        private final Digest dgst;

        /**
         * Naming policy.
         */
        private final NamingPolicy npolicy;

        /**
         * Is filelist needed?
         */
        private final boolean filelist;

        /**
         * Ctor.
         * @param dgst Digest
         * @param npolicy Naming policy
         * @param filelist Filelist
         */
        public Simple(final Digest dgst, final NamingPolicy npolicy, final boolean filelist) {
            this.dgst = dgst;
            this.npolicy = npolicy;
            this.filelist = filelist;
        }

        /**
         * Ctor.
         */
        public Simple() {
            this(Digest.SHA256, StandardNamingPolicy.PLAIN, false);
        }

        @Override
        public Digest digest() {
            return this.dgst;
        }

        @Override
        public NamingPolicy naming() {
            return this.npolicy;
        }

        @Override
        public boolean filelists() {
            return this.filelist;
        }
    }
}
