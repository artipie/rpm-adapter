/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.asto;

import com.artipie.asto.Key;
import com.artipie.asto.Storage;
import com.artipie.asto.blocking.BlockingStorage;
import com.artipie.rpm.meta.XmlPackage;
import java.io.ByteArrayInputStream;
import java.io.IOException;
import java.util.zip.GZIPInputStream;
import org.testcontainers.shaded.org.apache.commons.io.IOUtils;

/**
 * Reads and unpacks metadata.
 *
 * @since 1.9.4
 */
public class MetadataBytes {

    /**
     * Storage.
     */
    private final Storage storage;

    /**
     * Key.
     */
    private final Key key;

    /**
     * Ctor.
     * @param storage Storage
     * @param type Type of metadata
     */
    public MetadataBytes(final Storage storage, final XmlPackage type) {
        this(
            storage,
            MetadataBytes.findKey(storage, type)
        );
    }

    /**
     * Ctor.
     * @param storage Storage
     * @param key Key
     */
    public MetadataBytes(final Storage storage, final Key key) {
        this.storage = storage;
        this.key = key;
    }

    public byte[] value() throws IOException {
        return IOUtils.toByteArray(
            new GZIPInputStream(
                new ByteArrayInputStream(new BlockingStorage(this.storage).value(key))
            )
        );
    }

    private static Key findKey(final Storage storage, final XmlPackage type) {
        return new BlockingStorage(storage)
            .list(new Key.From("metadata")).stream()
            .filter(
                item -> item.string().contains(type.lowercase())
            )
            .findFirst()
            .get();
    }
}
