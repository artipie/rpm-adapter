/*
 * The MIT License (MIT) Copyright (c) 2020-2021 artipie.com
 * https://github.com/artipie/rpm-adapter/LICENSE.txt
 */
package com.artipie.rpm.meta;

import com.artipie.asto.test.TestResource;
import org.cactoos.list.ListOf;
import org.hamcrest.MatcherAssert;
import org.hamcrest.collection.IsIterableContainingInAnyOrder;
import org.hamcrest.core.IsEqual;
import org.junit.jupiter.api.Test;

/**
 * Test for {@link XmlPrimaryChecksums}.
 * @since 0.8
 */
public class XmlPrimaryChecksumsTest {

    @Test
    void readsChecksums() {
        MatcherAssert.assertThat(
            new XmlPrimaryChecksums(
                new TestResource("repodata/primary.xml.example").asPath()
            ).read(),
            new IsIterableContainingInAnyOrder<>(
                new ListOf<org.hamcrest.Matcher<? super String>>(
                    new IsEqual<>(
                        "7eaefd1cb4f9740558da7f12f9cb5a6141a47f5d064a98d46c29959869af1a44"
                    ),
                    new IsEqual<>(
                        "54f1d9a1114fa85cd748174c57986004857b800fe9545fbf23af53f4791b31e2"
                    )
                )
            )
        );
    }

}
