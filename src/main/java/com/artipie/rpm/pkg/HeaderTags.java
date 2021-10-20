/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.ArtipieException;
import java.util.List;
import java.util.Optional;
import java.util.regex.Matcher;
import java.util.regex.Pattern;
import org.redline_rpm.header.Header;

/**
 * Helper object to read metadata header tags from RPM package.
 *
 * @since 0.6
 */
@SuppressWarnings("PMD.TooManyMethods")
public final class HeaderTags {

    /**
     * Metadata.
     */
    private final Package.Meta meta;

    /**
     * Ctor.
     * @param meta Metadata
     */
    public HeaderTags(final Package.Meta meta) {
        this.meta = meta;
    }

    /**
     * Get the name header.
     * @return Value of header tag NAME.
     */
    public String name() {
        return this.meta.header(Header.HeaderTag.NAME).asString("");
    }

    /**
     * Get the arch header.
     * @return Value of header tag ARCH.
     */
    public String arch() {
        return this.meta.header(Header.HeaderTag.ARCH).asString("");
    }

    /**
     * Get the epoch header.
     * @return Value of header tag EPOCH.
     */
    public int epoch() {
        return this.meta.header(Header.HeaderTag.EPOCH).asInt(0);
    }

    /**
     * Get the version header.
     * @return Value of header tag VERSION.
     */
    public String version() {
        return this.meta.header(Header.HeaderTag.VERSION).asString("");
    }

    /**
     * Get the release header.
     * @return Value of header tag RELEASE.
     */
    public String release() {
        return this.meta.header(Header.HeaderTag.RELEASE).asString("");
    }

    /**
     * Get the summary header.
     * @return Value of header tag SUMMARY.
     */
    public String summary() {
        return this.meta.header(Header.HeaderTag.SUMMARY).asString("");
    }

    /**
     * Get the description header.
     * @return Value of header tag DESCRIPTION.
     */
    public String description() {
        return this.meta.header(Header.HeaderTag.DESCRIPTION).asString("");
    }

    /**
     * Get the package header.
     * @return Value of header tag PACKAGER.
     */
    public String packager() {
        return this.meta.header(Header.HeaderTag.PACKAGER).asString("");
    }

    /**
     * Get the url header.
     * @return Value of header tag URL.
     */
    public String url() {
        return this.meta.header(Header.HeaderTag.URL).asString("");
    }

    /**
     * Get the filemtimes header.
     * @return Value of header tag FILEMTIMES.
     */
    public int fileTimes() {
        return this.meta.header(Header.HeaderTag.FILEMTIMES).asInt(0);
    }

    /**
     * Get the build time header.
     * @return Value of header tag BUILDTIME.
     */
    public int buildTime() {
        return this.meta.header(Header.HeaderTag.BUILDTIME).asInt(0);
    }

    /**
     * Get the size header.
     * @return Value of header tag SIZE.
     */
    public int installedSize() {
        return this.meta.header(Header.HeaderTag.SIZE).asInt(0);
    }

    /**
     * Get the archive size header.
     * @return Value of header tag ARCHIVESIZE.
     */
    public int archiveSize() {
        return this.meta.header(Header.HeaderTag.ARCHIVESIZE).asInt(0);
    }

    /**
     * Get the license header.
     * @return Value of header tag LICENSE.
     */
    public String license() {
        return this.meta.header(Header.HeaderTag.LICENSE).asString("");
    }

    /**
     * Get the vendor header.
     * @return Value of header tag VENDOR.
     */
    public String vendor() {
        return this.meta.header(Header.HeaderTag.VENDOR).asString("");
    }

    /**
     * Get the group header.
     * @return Value of header tag GROUP.
     */
    public String group() {
        return this.meta.header(Header.HeaderTag.GROUP).asString("");
    }

    /**
     * Get the build host header.
     * @return Value of header tag BUILDHOST.
     */
    public String buildHost() {
        return this.meta.header(Header.HeaderTag.BUILDHOST).asString("");
    }

    /**
     * Get the source RPM header.
     * @return Value of header tag SOURCERPM.
     */
    public String sourceRmp() {
        return this.meta.header(Header.HeaderTag.SOURCERPM).asString("");
    }

    /**
     * Get the provides libraries names.
     * @return Value of header tag PROVIDENAME.
     */
    public List<String> providesNames() {
        return this.meta.header(Header.HeaderTag.PROVIDENAME).asStrings();
    }

    /**
     * Get the provides libraries versions.
     * @return Value of header tag PROVIDEVERSION.
     */
    public List<String> providesVer() {
        return this.meta.header(Header.HeaderTag.PROVIDEVERSION).asStrings();
    }

    /**
     * Get the require name header.
     * @return Value of header tag REQUIRENAME.
     */
    public List<String> requires() {
        return this.meta.header(Header.HeaderTag.REQUIRENAME).asStrings();
    }

    /**
     * Get the require version header.
     * @return Value of header tag REQUIREVERSION.
     */
    public List<String> requiresVer() {
        return this.meta.header(Header.HeaderTag.REQUIREVERSION).asStrings();
    }

    /**
     * Get the base names header.
     * @return Value of header tag BASENAMES.
     */
    public List<String> baseNames() {
        return this.meta.header(Header.HeaderTag.BASENAMES).asStrings();
    }

    /**
     * Get the dir names header.
     * @return Value of header tag DIRNAMES.
     */
    public List<String> dirNames() {
        return this.meta.header(Header.HeaderTag.DIRNAMES).asStrings();
    }

    /**
     * Get the dir indexes header.
     * @return Value of header tag DIRINDEXES.
     */
    public int[] dirIndexes() {
        return this.meta.header(Header.HeaderTag.DIRINDEXES).asInts();
    }

    /**
     * Get the changelog header.
     * @return Value of header tag CHANGELOG.
     */
    public List<String> changelog() {
        return this.meta.header(Header.HeaderTag.CHANGELOG).asStrings();
    }

    /**
     * Rpm package version, format is [epoch]:[version]-[release].
     * @since 1.9
     */
    static final class Version {

        /**
         * Version format pattern.
         */
        private static final Pattern PTRN =
            Pattern.compile("((?<epoch>\\d+):)?(?<ver>[\\w.]+)(-(?<rel>.*))?");

        /**
         * Value from version header.
         */
        private final String val;

        /**
         * Ctor.
         * @param val Value from version header
         */
        Version(final String val) {
            this.val = val;
        }

        /**
         * Return version value.
         * @return String version
         */
        public String ver() {
            return this.part("ver").orElseThrow(
                () -> new ArtipieException(new IllegalArgumentException("Invalid version value"))
            );
        }

        /**
         * Release value.
         * @return String release, empty if not present
         */
        public Optional<String> rel() {
            return this.part("rel");
        }

        /**
         * Epoch value or default 0.
         * @return String epoch
         */
        public String epoch() {
            return this.part("epoch").orElse("0");
        }

        /**
         * Get version part by name.
         * @param name Part group name, see {@link Version#PTRN}
         * @return Part value if found
         * @throws IllegalArgumentException If does not match
         */
        private Optional<String> part(final String name) {
            final Matcher matcher = Version.PTRN.matcher(this.val);
            if (matcher.matches()) {
                return Optional.ofNullable(matcher.group(name));
            }
            throw new ArtipieException(new IllegalArgumentException("Provided version is invalid"));
        }
    }
}
