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
import java.util.function.Consumer;
import java.util.function.Function;
import java.util.function.Predicate;

import static java.util.concurrent.CompletableFuture.failedStage;
import static java.util.function.Function.identity;

/**
 * The failed result of some operation. In this context, <em>failure</em> means that the operation
 * threw an exception.
 *
 * @implNote this record can never contain a <b>fatal</b> exception;
 * see {@link Failure#isFatal(Throwable)} for more information.
 *
 * @param cause the exception that was caught while executing the operation (never {@code null})
 * @param <T> the return type of the operation (or {@link Void} if the operation is {@code void})
 */
public record Failure<T>(Throwable cause) implements Try<T>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

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
    public <U> U fold(Function<? super Throwable, ? extends U> onFailure, Function<? super T, ? extends U> onSuccess) {
        return onFailure.apply(getCause());
    }

    @Override
    public Try<T> peek(Consumer<? super Throwable> onFailure, Consumer<? super T> onSuccess) {
        onFailure.accept(getCause());
        return this;
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
    public Try<T> recover(Function<? super Throwable, ? extends T> fn) {
        return Try.of(() -> fn.apply(getCause()));
    }

    @Override
    public <X extends Throwable> Try<T> recover(Class<X> exceptionType, Function<? super X, ? extends T> fn) {
        if (exceptionType.isInstance(getCause())) {
            return Try.of(() -> fn.apply(exceptionType.cast(getCause())));
        }

        return this;
    }

    @Override
    public Try<T> recoverWith(Function<? super Throwable, ? extends Try<T>> fn) {
        try {
            return fn.apply(getCause());
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    @Override
    public <X extends Throwable> Try<T> recoverWith(Class<X> exceptionType, Function<? super X, ? extends Try<T>> fn) {
        if (exceptionType.isInstance(getCause())) {
            return Try.of(() -> fn.apply(exceptionType.cast(getCause()))).flatMap(identity());
        }

        return this;
    }

    /**
     * @return {@code true} if {@code throwable} is fatal and should never be caught,
     * {@code false} otherwise
     */
    private static boolean isFatal(Throwable throwable) {
        return throwable instanceof InterruptedException
                || throwable instanceof LinkageError
                || throwable instanceof VirtualMachineError;
    }

    /**
     * Throws a checked exception as if it were unchecked by tricking the compiler
     */
    @SuppressWarnings("unchecked")
    private static <T extends Throwable> void sneakyThrow(Throwable t) throws T {
        throw (T) t;
    }
}
