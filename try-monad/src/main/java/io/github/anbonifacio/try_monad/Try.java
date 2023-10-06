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

package io.github.anbonifacio.try_monad;

import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * The interface Try.
 *
 * @param <T>  the type parameter
 */
public sealed interface Try<T> permits Success, Failure {

    /**
     * Of try.
     *
     * @param <T>  the type parameter
     * @param supplier the supplier
     * @return the try
     */
    static <T> Try<T> of(Supplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        try {
            return new Success<>(supplier.get());
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Of callable try.
     *
     * @param <T>  the type parameter
     * @param callable the callable
     * @return the try
     */
    static <T> Try<T> ofCallable(Callable<? extends T> callable) {
        Objects.requireNonNull(callable, "callable is null");
        try {
            return new Success<>(callable.call());
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Of runnable try.
     *
     * @param runnable the runnable
     * @return the try
     */
    static Try<Void> ofRunnable(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null");
        try {
            runnable.run();
            return new Success<>(null);
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * And finally try.
     *
     * @param runnable the runnable
     * @return the try
     */
    default Try<T> andFinally(Runnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null");
        try {
            runnable.run();
            return this;
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Success try.
     *
     * @param <T>  the type parameter
     * @param value the value
     * @return the try
     */
    static <T> Try<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Failure try.
     *
     * @param <T>  the type parameter
     * @param exception the exception
     * @return the try
     */
    static <T> Try<T> failure(Throwable exception) {
        return new Failure<>(exception);
    }

    /**
     * Flat map try.
     *
     * @param <U>  the type parameter
     * @param mapper the mapper
     * @return the try
     */
    <U> Try<U> flatMap(Function<? super T, ? extends Try<? extends U>> mapper);

    /**
     * Map try.
     *
     * @param <U>  the type parameter
     * @param mapper the mapper
     * @return the try
     */
    <U> Try<U> map(Function<? super T, ? extends U> mapper);

    /**
     * Fold u.
     *
     * @param <U>  the type parameter
     * @param ifFail the if fail
     * @param f the f
     * @return the u
     */
    <U> U fold(Function<? super Throwable, ? extends U> ifFail, Function<? super T, ? extends U> f);

    /**
     * Is failure boolean.
     *
     * @return the boolean
     */
    boolean isFailure();

    /**
     * Is success boolean.
     *
     * @return the boolean
     */
    boolean isSuccess();

    /**
     * Gets success.
     *
     * @return the success
     */
    Optional<T> getSuccess();

    /**
     * To optional optional.
     *
     * @return the optional
     */
    Optional<T> toOptional();

    /**
     * To completion stage completion stage.
     *
     * @return the completion stage
     */
    CompletionStage<T> toCompletionStage();

    /**
     * Gets failure.
     *
     * @return the failure
     */
    Optional<Throwable> getFailure();

    /**
     * Get t.
     *
     * @return the t
     */
    T get();

    /**
     * Gets cause.
     *
     * @return the cause
     */
    Throwable getCause();

    /**
     * Filter try.
     *
     * @param p the p
     * @return the try
     */
    Try<T> filter(Predicate<? super T> p);

    /**
     * Or else try try.
     *
     * @param fn the fn
     * @return the try
     */
    @SuppressWarnings("unchecked")
    default Try<T> orElseTry(Supplier<Try<? extends T>> fn) {
        return isSuccess() ? this : (Try<T>) fn.get();
    }

    /**
     * Or else try try.
     *
     * @param other the other
     * @return the try
     */
    @SuppressWarnings("unchecked")
    default Try<T> orElseTry(Try<? extends T> other) {
        return isSuccess() ? this : (Try<T>) other;
    }

    /**
     * Or else t.
     *
     * @param other the other
     * @return the t
     */
    default T orElse(T other) {
        return isSuccess() ? this.get() : other;
    }

    /**
     * Or else get t.
     *
     * @param fn the fn
     * @return the t
     */
    default T orElseGet(Supplier<? extends T> fn) {
        return isSuccess() ? this.get() : fn.get();
    }

    /**
     * Or else throw t.
     *
     * @param <X>  the type parameter
     * @param exceptionSupplier the exception supplier
     * @return the t
     * @throws X the x
     */
    default <X extends Throwable> T orElseThrow(Supplier<? extends X> exceptionSupplier) throws X {
        if (isFailure()) {
            throw exceptionSupplier.get();
        } else {
            return this.get();
        }
    }

    /**
     * Recover try.
     *
     * @param fn the fn
     * @return the try
     */
    Try<T> recover(Function<? super Throwable, T> fn);

    /**
     * Recover with try.
     *
     * @param fn the fn
     * @return the try
     */
    Try<T> recoverWith(Function<? super Throwable, Try<T>> fn);
}
