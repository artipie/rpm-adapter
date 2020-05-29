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

import com.jcabi.log.Logger;
import java.lang.reflect.Method;
import org.junit.jupiter.api.extension.AfterTestExecutionCallback;
import org.junit.jupiter.api.extension.BeforeTestExecutionCallback;
import org.junit.jupiter.api.extension.ExtensionContext;
import org.junit.jupiter.api.extension.ExtensionContext.Namespace;
import org.junit.jupiter.api.extension.ExtensionContext.Store;

/**
 * Junit extension to measure test time execution.
 * @since 1.0
 * @checkstyle JavadocVariableCheck (500 lines)
 */
@SuppressWarnings("PMD.GuardLogStatement")
public final class TimingExtension implements BeforeTestExecutionCallback,
    AfterTestExecutionCallback {

    private static final String START_TIME = "start time";

    @Override
    public void beforeTestExecution(final ExtensionContext context) {
        this.getStore(context).put(TimingExtension.START_TIME, System.currentTimeMillis());
    }

    @Override
    public void afterTestExecution(final ExtensionContext context) {
        final Method method = context.getRequiredTestMethod();
        final long start = this.getStore(context)
            .remove(TimingExtension.START_TIME, long.class);
        Logger.info(
            context.getRequiredTestClass(),
            "Method %s took %[ms]s",
            method.getName(),
            System.currentTimeMillis() - start
        );
    }

    /**
     * Returns store from context.
     * @param context Extension context
     * @return Junit store
     */
    private Store getStore(final ExtensionContext context) {
        return context.getStore(
            Namespace.create(this.getClass(), context.getRequiredTestMethod())
        );
    }

}
