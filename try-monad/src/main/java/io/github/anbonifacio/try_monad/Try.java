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

import io.github.anbonifacio.try_monad.interfaces.checked.CheckedRunnable;
import io.github.anbonifacio.try_monad.interfaces.checked.CheckedSupplier;

import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.Callable;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;
import java.util.function.Supplier;

/**
 * Functional version of the imperative {@code try-catch-finally} construct
 *
 * @param <T> result of the tried operation (or {@link Void} if no result is expected)
 *
 * @implNote some <b>fatal</b> exceptions will never be caught by this class and will be thrown
 * directly back to the caller. These are exceptions that should never be caught anyway
 * (see {@link Failure#isFatal(Throwable)} to see which exceptions are considered fatal).
 */
public sealed interface Try<T> permits Success, Failure {

    /**
     * Creates a new {@link Try} containing the result of {@code supplier.call()}.
     *
     * @return if {@code supplier} does not throw any exception, a {@link Success} containing the
     * value returned by {@code supplier};
     * <p>otherwise, a {@link Failure} containing the exception thrown by {@code supplier}.
     *
     * @throws NullPointerException if {@code runnable} is null
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
     * Variant of {@link #of(Supplier)} that allows the given {@code supplier} to throw checked
     * exceptions.
     */
    static <T> Try<T> of(CheckedSupplier<? extends T> supplier) {
        Objects.requireNonNull(supplier, "supplier is null");
        try {
            return new Success<>(supplier.checkedGet());
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Creates a new {@link Try} containing the result of {@code callable.call()}.
     *
     * @return if {@code callable} does not throw any exception, a {@link Success} containing the
     * value returned by {@code callable};
     * <p>otherwise, a {@link Failure} containing the exception thrown by {@code callable}.
     *
     * @throws NullPointerException if {@code runnable} is null
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
     * Creates a new {@link Try} containing the result of {@code runnable.run()}.
     *
     * @return if {@code runnable} does not throw any exception, a {@link Success} containing the {@code null};
     * <p>otherwise, a {@link Failure} containing the exception thrown by {@code runnable}.
     *
     * @throws NullPointerException if {@code runnable} is null
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
     * Variant of {@link #ofRunnable(Runnable)} that allows the given {@code runnable} to throw
     * checked exceptions
     */
    static Try<Void> ofRunnable(CheckedRunnable runnable) {
        Objects.requireNonNull(runnable, "runnable is null");
        try {
            runnable.checkedRun();
            return new Success<>(null);
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    /**
     * Just like a {@code finally} block, runs the given {@code runnable} regardless of whether this
     * is a {@link Success} or a {@link Failure}.
     *
     * @return itself, or a {@link Failure} containing the exception caught while running
     * {@code runnable}, if any is thrown.
     *
     * @throws NullPointerException if {@code runnable} is null
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
     * Creates a new {@link Success} with the given {@code value} as result.
     */
    static <T> Try<T> success(T value) {
        return new Success<>(value);
    }

    /**
     * Creates a new {@link Failure} with the given {@code exception} as cause.
     */
    static <T> Try<T> failure(Throwable exception) {
        return new Failure<>(exception);
    }

    /**
     * @return itself if this is a {@link Failure};
     * <p>if this is a {@link Success}, either:<ul>
     *     <li>the result of applying the given {@code mapper} to its {@link #get() result}.
     *     <li>a {@link Failure} containing the caught exception, if {@code mapper} throws any.
     * </ul>
     */
    <U> Try<U> flatMap(Function<? super T, ? extends Try<? extends U>> mapper);

    /**
     * @return itself if this is a {@link Failure};
     * <p>if this is a {@link Success}, either:<ul>
     *     <li>a {@link Success} containing the result of applying the given {@code mapper} to its result
     *     <li>a {@link Failure} containing the caught exception, if {@code mapper} throws any.
     * </ul>
     */
    <U> Try<U> map(Function<? super T, ? extends U> mapper);

    /**
     * @return the result of applying {@code ifFail} to the caught exception if this is a {@link Failure};
     *         <p>the result of applying {@code f} to the result if this is a {@link Success}
     */
    <U> U fold(Function<? super Throwable, ? extends U> ifFail, Function<? super T, ? extends U> f);

    boolean isFailure();

    boolean isSuccess();

    /**
     * @return {@link Optional} containing the result if this is a {@link Success};
     * <p>{@link Optional#empty()} if this is a {@link Failure}.
     */
    Optional<T> getSuccess();

    /**
     * @return a completed {@link CompletionStage} containing the result if this is a {@link Success};
     * <p>a failed {@link CompletionStage} containing the caught exception if this is a {@link Failure}
     */
    CompletionStage<T> toCompletionStage();

    /**
     * @return {@link Optional} containing the caught exception if this is a {@link Failure},
     * {@link Optional#empty()} if this is a {@link Success}
     */
    Optional<Throwable> getFailure();

    /**
     * @return the result if this is a {@link Success}
     *
     * @throws NoSuchElementException if this is a {@link Failure}; the caught exception
     * will be set as the cause
     */
    T get() throws NoSuchElementException;

    /**
     * @return the caught exception if this is a {@link Failure}
     * @throws NoSuchElementException if this is a {@link Success}
     */
    Throwable getCause() throws NoSuchElementException;

    /**
     * @return itself, it this is a {@link Failure};
     * <p>if this is a {@link Success}:
     * <ul>
     *     <li>if the result satisfies {@code p}, returns itself
     *     <li>if the result does <b>NOT</b> satisfy {@code p},
     *     returns a {@link Failure} containing a {@link NoSuchElementException}
     *     <li>if {@code p} throws any exception, returns a {@link Failure} containing the caught
     *     exception.
     * </ul>
     */
    Try<T> filter(Predicate<? super T> p);

    /**
     * @return itself, if this is a {@link Success}
     * <p>{@code fn.get()}, if this is a {@link Failure}
     */
    @SuppressWarnings("unchecked")
    default Try<T> orElseTry(Supplier<Try<? extends T>> fn) {
        return isSuccess() ? this : (Try<T>) fn.get();
    }

    /**
     * @return itself, if this is a {@link Success}
     * <p>{@code other}, if this is a {@link Failure}
     */
    @SuppressWarnings("unchecked")
    default Try<T> orElseTry(Try<? extends T> other) {
        return isSuccess() ? this : (Try<T>) other;
    }

    /**
     * @return the {@link #get() result}, if this is a {@link Success}
     * <p>{@code other}, if this is a {@link Failure}
     */
    default T orElse(T other) {
        return isSuccess() ? this.get() : other;
    }

    /**
     * @return the {@link #get() result}, if this is a {@link Success}
     * <p>{@code fn.get()}, if this is a {@link Failure}
     */
    default T orElseGet(Supplier<? extends T> fn) {
        return isSuccess() ? this.get() : fn.get();
    }

    /**
     * @return the {@link #get() result}, if this is a {@link Success}.
     * @throws X if this is a {@link Failure} (throws the result of {@code exceptionMapper.apply(getCause())})
     */
    default <X extends Throwable> T orElseThrow(Function<? super Throwable, ? extends X> exceptionMapper) throws X {
        if (isFailure()) {
            throw exceptionMapper.apply(getCause());
        } else {
            return this.get();
        }
    }

    /**
     * @return itself if this is a {@link Success};
     * <p>if this is a {@link Failure}, either:<ul>
     *     <li>a {@link Success} containing the result of applying the given {@code fn} to the {@link #getCause() cause}.
     *     <li>a {@link Failure} containing the caught exception, if {@code fn} throws any.
     * </ul>
     */
    Try<T> recover(Function<? super Throwable, T> fn);

    /**
     * @return itself if this is a {@link Success};
     * <p>if this is a {@link Failure}, either:<ul>
     *     <li>the result of applying the given {@code fn} to the {@link #getCause() cause}.
     *     <li>a {@link Failure} containing the caught exception, if {@code fn} throws any.
     * </ul>
     */
    Try<T> recoverWith(Function<? super Throwable, Try<T>> fn);
}
