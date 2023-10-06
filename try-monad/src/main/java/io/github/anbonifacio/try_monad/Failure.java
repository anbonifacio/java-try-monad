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

import java.io.Serial;
import java.io.Serializable;
import java.util.NoSuchElementException;
import java.util.Objects;
import java.util.Optional;
import java.util.concurrent.CompletionStage;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.concurrent.CompletableFuture.failedStage;

/**
 * The type Failure.
 *
 * @param <T>  the type parameter
 */
public record Failure<T>(Throwable cause) implements Try<T>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    /**
     * Instantiates a new Failure.
     *
     * @param cause the cause
     */
    public Failure {
        Objects.requireNonNull(cause, "cause is null");
        if (isFatal(cause)) {
            sneakyThrow(cause);
        }
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> Try<U> flatMap(Function<? super T, ? extends Try<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        return (Failure<U>) this;
    }

    @Override
    @SuppressWarnings("unchecked")
    public <U> Try<U> map(Function<? super T, ? extends U> mapper) {
        return (Failure<U>) this;
    }

    @Override
    public <U> U fold(Function<? super Throwable, ? extends U> ifFail, Function<? super T, ? extends U> f) {
        return ifFail.apply(getCause());
    }

    @Override
    public boolean isFailure() {
        return true;
    }

    @Override
    public boolean isSuccess() {
        return false;
    }

    @Override
    public Optional<T> getSuccess() {
        return Optional.empty();
    }

    @Override
    public Optional<T> toOptional() {
        return Optional.empty();
    }

    @Override
    public CompletionStage<T> toCompletionStage() {
        return failedStage(cause);
    }

    @Override
    public Optional<Throwable> getFailure() {
        return Optional.of(cause);
    }

    @Override
    public T get() {
        throw new NoSuchElementException("Try is Failure", cause);
    }

    @Override
    public Throwable getCause() {
        return cause;
    }

    @Override
    public Try<T> filter(Predicate<? super T> p) {
        return this;
    }

    @Override
    public Try<T> recover(Function<? super Throwable, T> fn) {
        return Try.of(() -> fn.apply(getCause()));
    }

    @Override
    public Try<T> recoverWith(Function<? super Throwable, Try<T>> fn) {
        try {
            return fn.apply(getCause());
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    private static boolean isFatal(Throwable throwable) {
        return throwable instanceof InterruptedException
                || throwable instanceof LinkageError
                || throwable instanceof VirtualMachineError;
    }

    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
