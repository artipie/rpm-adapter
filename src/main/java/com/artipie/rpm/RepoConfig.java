/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm;

import com.amihaiemil.eoyaml.Yaml;
import com.amihaiemil.eoyaml.YamlMapping;
import java.util.Locale;
import java.util.Optional;

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
     */
    final class FromYaml implements RepoConfig {

        /**
         * Settings.
         */
        private final YamlMapping yaml;

        /**
         * Ctor.
         * @param yaml Yaml settings
         */
        public FromYaml(final YamlMapping yaml) {
            this.yaml = yaml;
        }

        /**
         * Ctor.
         * @param yaml Yaml settings
         */
        public FromYaml(final Optional<YamlMapping> yaml) {
            this(yaml.orElse(Yaml.createYamlMappingBuilder().build()));
        }

        @Override
        public Digest digest() {
            return Optional.ofNullable(this.yaml.string(RpmOptions.DIGEST.optionName()))
                .map(dgst -> Digest.valueOf(dgst.toUpperCase(Locale.US))).orElse(Digest.SHA256);
        }

        @Override
        public NamingPolicy naming() {
            return Optional.ofNullable(this.yaml.string(RpmOptions.NAMING_POLICY.optionName()))
                .map(naming -> StandardNamingPolicy.valueOf(naming.toUpperCase(Locale.US)))
                .orElse(StandardNamingPolicy.SHA256);
        }

        @Override
        public boolean filelists() {
            return !Boolean.FALSE.toString()
                .equals(this.yaml.string(RpmOptions.FILELISTS.optionName()));
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
