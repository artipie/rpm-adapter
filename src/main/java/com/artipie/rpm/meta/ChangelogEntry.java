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
package com.artipie.rpm.meta;

import com.google.common.primitives.Ints;
import java.text.ParseException;
import java.text.SimpleDateFormat;
import java.util.Date;
import java.util.Locale;
import java.util.TimeZone;
import java.util.concurrent.TimeUnit;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * Changelog entry.
 *
 * @since 0.8.3
 */
final class ChangelogEntry {

    /**
     * RegEx pattern of changelog entry string.
     */
    private static final Pattern PATTERN = Pattern.compile(
        "\\* (?<date>\\w+ \\w+ \\d+ \\d+) (?<author>[^-]+) (?<content>-.*)",
        Pattern.DOTALL
    );

    /**
     * Origin string.
     */
    private final String origin;

    /**
     * Ctor.
     *
     * @param origin Origin string.
     */
    ChangelogEntry(final String origin) {
        this.origin = origin;
    }

    /**
     * Read author.
     *
     * @return Author string.
     */
    String author() {
        return this.matcher().group("author");
    }

    /**
     * Read date.
     *
     * @return Date in UNIX time.
     */
    int date() {
        final Matcher matcher = this.matcher();
        final String str = matcher.group("date");
        final SimpleDateFormat format = new SimpleDateFormat("EEE MMM dd yyyy", Locale.US);
        format.setTimeZone(TimeZone.getTimeZone("UTC"));
        final Date date;
        try {
            date = format.parse(str);
        } catch (final ParseException ex) {
            throw new IllegalStateException(String.format("Failed to parse date: '%s'", str), ex);
        }
        return Ints.checkedCast(TimeUnit.MILLISECONDS.toSeconds(date.getTime()));
    }

    /**
     * Read content.
     *
     * @return Content string.
     */
    String content() {
        return this.matcher().group("content");
    }

    /**
     * Matches origin string by pattern.
     *
     * @return Matcher.
     */
    private Matcher matcher() {
        final Matcher matcher = PATTERN.matcher(this.origin);
        if (!matcher.matches()) {
            throw new IllegalStateException(String.format("Cannot parse: '%s'", this.origin));
        }
        return matcher;
    }
}
