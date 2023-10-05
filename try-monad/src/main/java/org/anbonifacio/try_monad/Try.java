/*
 * Copyright 2023 Antonio Bonifacio
 *
 * Permission is hereby granted, free of charge, to any person obtaining a copy of this software and associated documentation files (the “Software”), to deal in the Software without restriction, including without limitation the rights to use, copy, modify, merge, publish, distribute, sublicense, and/or sell copies of the Software, and to permit persons to whom the Software is furnished to do so, subject to the following conditions:
 *
 * The above copyright notice and this permission notice shall be included in all copies or substantial portions of the Software.
 *
 * THE SOFTWARE IS PROVIDED “AS IS”, WITHOUT WARRANTY OF ANY KIND, EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT. IN NO EVENT SHALL THE AUTHORS OR COPYRIGHT HOLDERS BE LIABLE FOR ANY CLAIM, DAMAGES OR OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE, ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR OTHER DEALINGS IN THE SOFTWARE.
 *
 */

package org.anbonifacio.try_monad;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

public sealed interface Try<T> permits Success, Failure {

    static <T> Try<T> of(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        try {
            return new Success<>(supplier.get());
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    static <T> Try<T> ofCallable(Callable<? extends T> callable) {
        Objects.requireNonNull(callable, "callable is null");
        try {
            return new Success<>(callable.call());
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    static Try<Void> ofRunnable(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null");
        try {
            runnable.run();
            return new Success<>(null);
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    default Try<T> andFinally(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null");
        try {
            runnable.run();
            return this;
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    static <T> Try<T> success(T value) {
        return new Success<>(value);
    }

    static <T> Try<T> failure(Throwable exception) {
        return new Failure<>(exception);
    }

    <U> Try<U> flatMap(Function<? super T, ? extends Try<? extends U>> mapper);

    <U> Try<U> map(Function<? super T, ? extends U> mapper);

    <U> U fold(Function<? super Throwable, ? extends U> ifFail, Function<? super T, ? extends U> f);

    boolean isFailure();

    boolean isSuccess();

    Optional<T> getSuccess();

    Optional<T> toOptional();

    CompletionStage<T> toCompletionStage();

    Optional<Throwable> getFailure();

    T get();

    Throwable getCause();

    Try<T> filter(Predicate<? super T> p);

    @SuppressWarnings("unchecked")
    default Try<T> orElseTry(Supplier<Try<? extends T>> fn) {
        return isSuccess() ? this : (Try<T>) fn.get();
    }

    @SuppressWarnings("unchecked")
    default Try<T> orElseTry(Try<? extends T> other) {
        return isSuccess() ? this : (Try<T>) other;
    }

    default T orElse(T other) {
        return isSuccess() ? this.get() : other;
    }

    default T orElseGet(Supplier<? extends T> fn) {
        return isSuccess() ? this.get() : fn.get();
    }

    default <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isFailure()) {
            throw exceptionSupplier.get();
        } else {
            return this.get();
        }
    }

    Try<T> recover(Function<? super Throwable, T> fn);

    Try<T> recoverWith(Function<? super Throwable, Try<T>> fn);
}
