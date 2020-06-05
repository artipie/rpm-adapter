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

import org.hamcrest.MatcherAssert;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link ChangelogEntry}.
 *
 * @since 0.8.3
 */
class ChangelogEntryTest {

    /**
     * Entry tested.
     */
    private final ChangelogEntry entry = new ChangelogEntry(
        "* Wed May 13 2020 John Doe <johndoe@artipie.org> - 0.1-2\n- Second artipie package"
    );

    @Test
    void shouldParseAuthor() {
        MatcherAssert.assertThat(
            this.entry.author(),
            new IsEqual<>("John Doe <johndoe@artipie.org>")
        );
    }

    @Test
    @SuppressWarnings("PMD.UseUnderscoresInNumericLiterals")
    void shouldParseDate() {
        final int unixtime = 1589328000;
        MatcherAssert.assertThat(
            this.entry.date(),
            new IsEqual<>(unixtime)
        );
    }

    @Test
    void shouldParseContent() {
        MatcherAssert.assertThat(
            this.entry.content(),
            new IsEqual<>("- 0.1-2\n- Second artipie package")
        );
    }
}
