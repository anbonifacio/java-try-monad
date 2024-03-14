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

import static java.util.concurrent.CompletableFuture.completedStage;

/**
 * The successful result of some operation. In this context, <em>success</em> means that the operation
 * completed without throwing any exception.
 *
 * @param value the result of the operation (or {@code null} if the operation is {@code void})
 * @param <T> the type of the result (or {@link Void} if the operation is {@code void})
 */
public record Success<T>(T value) implements Try<T>, Serializable {
    @Serial
    private static final long serialVersionUID = 1L;

    @Override
    @SuppressWarnings("unchecked")
    public <U> Try<U> flatMap(Function<? super T, ? extends Try<? extends U>> mapper) {
        Objects.requireNonNull(mapper, "mapper is null");
        try {
            return (Try<U>) mapper.apply(get());
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    @Override
    public <U> Try<U> map(Function<? super T, ? extends U> mapper) {
        try {
            return new Success<>(mapper.apply(get()));
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    @Override
    public <U> U fold(Function<? super Throwable, ? extends U> ifFail, Function<? super T, ? extends U> f) {
        return f.apply(get());
    }

    @Override
    public boolean isFailure() {
        return false;
    }

    @Override
    public boolean isSuccess() {
        return true;
    }

    @Override
    public Optional<T> getSuccess() {
        return Optional.of(value);
    }

    @Override
    public CompletionStage<T> toCompletionStage() {
        return completedStage(value);
    }

    @Override
    public Optional<Throwable> getFailure() {
        return Optional.empty();
    }

    @Override
    public T get() {
        return value;
    }

    @Override
    public Throwable getCause() {
        return new NoSuchElementException("getCause on Success");
    }

    @Override
    public Try<T> filter(Predicate<? super T> p) {
        Objects.requireNonNull(p, "predicate is null");
        try {
            if (p.test(get())) {
                return this;
            } else {
                return Try.failure(new NoSuchElementException("Predicate does not hold for " + get()));
            }
        } catch (Throwable t) {
            return new Failure<>(t);
        }
    }

    @Override
    public Try<T> recover(Function<? super Throwable, T> fn) {
        return this;
    }

    @Override
    public Try<T> recoverWith(Function<? super Throwable, Try<T>> fn) {
        return this;
    }
}
