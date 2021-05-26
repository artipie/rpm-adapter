/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import java.io.BufferedReader;
import java.io.BufferedWriter;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Joins two meta xml-files.
 * @since 0.9
 */
public final class XmlMetaJoin {

    /**
     * Xml header patter.
     */
    private static final Pattern HEADER = Pattern.compile("<\\?xml.*?>");

    /**
     * How many lines to check for xml header and open tag.
     */
    private static final int MAX = 5;

    /**
     * Tag.
     */
    private final String tag;

    /**
     * Ctor.
     * @param tag Metatag
     */
    public XmlMetaJoin(final String tag) {
        this.tag = tag;
    }

    /**
     * Appends data from part to target.
     * @param target Target
     * @param part File to append
     * @throws IOException On error
     */
    @SuppressWarnings({"PMD.PrematureDeclaration", "PMD.GuardLogStatement"})
    public void merge(final Path target, final Path part) throws IOException {
        final Path res = target.getParent().resolve(
            String.format("%s.merged", target.getFileName().toString())
        );
        try (BufferedWriter out = Files.newBufferedWriter(res)) {
            this.writeFirstPart(target, out);
            this.writeSecondPart(part, out);
        } catch (final IOException err) {
            Files.delete(res);
            throw err;
        }
        Files.move(res, target, StandardCopyOption.REPLACE_EXISTING);
    }

    /**
     * Writes the first part.
     * @param target What to write
     * @param writer Where to write
     * @throws IOException On error
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    private void writeFirstPart(final Path target, final BufferedWriter writer)
        throws IOException {
        try (BufferedReader in = Files.newBufferedReader(target)) {
            String line;
            final String close = String.format("</%s>", this.tag);
            while ((line = in.readLine()) != null) {
                if (line.contains(close)) {
                    line = line.replace(close, "");
                }
                writer.append(line);
                writer.newLine();
            }
        }
    }

    /**
     * Writes the first part.
     * @param target What to write
     * @param writer Where to write
     * @throws IOException On error
     */
    @SuppressWarnings("PMD.AssignmentInOperand")
    private void writeSecondPart(final Path target, final BufferedWriter writer)
        throws IOException {
        try (BufferedReader in = Files.newBufferedReader(target)) {
            String line;
            int cnt = 0;
            boolean found = false;
            final Pattern pattern = Pattern.compile(String.format("<%s.*?>", this.tag));
            while ((line = in.readLine()) != null) {
                if (cnt >= XmlMetaJoin.MAX && !found) {
                    throw new IOException("Failed to merge xml, header not found in part");
                }
                if (cnt < XmlMetaJoin.MAX && !found) {
                    final Matcher mheader = XmlMetaJoin.HEADER.matcher(line);
                    if (mheader.find()) {
                        line = mheader.replaceAll("");
                    }
                    final Matcher matcher = pattern.matcher(line);
                    if (matcher.find()) {
                        line = matcher.replaceAll("");
                        found = true;
                    }
                }
                cnt = cnt + 1;
                writer.append(line);
                writer.newLine();
            }
        }
    }
}
