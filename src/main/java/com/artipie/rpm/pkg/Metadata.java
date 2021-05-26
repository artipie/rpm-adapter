/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.pkg;

import com.artipie.rpm.Digest;
import com.artipie.rpm.meta.XmlRepomd;
import java.io.IOException;
import java.nio.file.Path;
import java.util.List;

/**
 * Metadata output.
 * @since 0.8
 */
public interface Metadata extends PackageOutput {

    /**
     * Brushes metadata by cleaning not existing packages and setting packages count.
     * @param ids Ids of the packages to clear
     * @throws IOException When error occurs
     */
    void brush(List<String> ids) throws IOException;

    /**
     * Save metadata to repomd, produce gzipped output.
     * @param repodata Repository repodata
     * @param digest Digest
     * @param repomd Repomd to update
     * @return Gzip metadata file
     * @throws IOException On error
     * @checkstyle ParameterNumberCheck (3 lines)
     */
    Path save(Repodata repodata, Digest digest, XmlRepomd repomd)
        throws IOException;

    /**
     * Underling metadata file output.
     * @return File output
     */
    PackageOutput.FileOutput output();

}
