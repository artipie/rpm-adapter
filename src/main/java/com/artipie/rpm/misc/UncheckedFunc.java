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
package com.artipie.rpm.misc;

import java.util.function.Function;

/**
 * Unchecked {@link java.util.function.Function}.
 * @param <T> Function type
 * @param <R> Function return type
 * @param <E> Error type
 * @since 0.8
 */
public final class UncheckedFunc<T, R, E extends Throwable> implements Function<T, R> {

    /**
     * Checked version.
     */
    private final Checked<T, R, E> checked;

    /**
     * Ctor.
     * @param checked Checked func
     */
    public UncheckedFunc(final UncheckedFunc.Checked<T, R, E> checked) {
        this.checked = checked;
    }

    @Override
    @SuppressWarnings("PMD.AvoidCatchingThrowable")
    public R apply(final T val) {
        try {
            return this.checked.apply(val);
            // @checkstyle IllegalCatchCheck (1 line)
        } catch (final Throwable err) {
            throw new IllegalStateException(err);
        }
    }

    /**
     * Checked version of consumer.
     * @param <T> Consumer type
     * @param <R> Return type
     * @param <E> Error type
     * @since 0.8
     */
    @FunctionalInterface
    public interface Checked<T, R, E extends Throwable> {

        /**
         * Apply value.
         * @param value Value to accept
         * @return Result
         * @throws E On error
         */
        R apply(T value) throws E;
    }
}
