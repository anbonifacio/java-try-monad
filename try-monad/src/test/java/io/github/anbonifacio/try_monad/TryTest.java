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

import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.Arguments;
import org.junit.jupiter.params.provider.MethodSource;

import java.io.IOException;
import java.io.InterruptedIOException;
import java.sql.SQLException;
import java.util.NoSuchElementException;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Function;
import java.util.stream.Stream;

import static java.lang.Integer.toBinaryString;
import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatException;
import static org.assertj.core.api.Assertions.assertThatExceptionOfType;
import static org.assertj.core.api.Assertions.assertThatRuntimeException;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.junit.jupiter.params.provider.Arguments.arguments;

class TryTest {
    private static Stream<Arguments> fatal_errors() {
        return Stream.of(
                arguments(new InternalError()),
                arguments(new LinkageError()),
                arguments(new ClassFormatError()),
                arguments(new OutOfMemoryError()));
    }

    @Test
    void shouldExecuteAndFinallyOnSuccess() {
        var count = new AtomicInteger();
        Try.ofRunnable(() -> count.set(0)).andFinally(() -> count.set(1));
        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    void shouldExecuteAndFinallyOnFailure() {
        var count = new AtomicInteger();
        Try.ofRunnable(() -> {
                    throw new IllegalStateException("FAILURE");
                })
                .andFinally(() -> count.set(1));
        assertThat(count.get()).isEqualTo(1);
    }

    @Test
    void shouldExecuteAndFinallyTryOnFailureWithFailure() {
        var result = Try.of(() -> {
                    throw new IllegalStateException("FAILURE");
                })
                .andFinally(() -> {
                    throw new IllegalArgumentException("FAILURE");
                });
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getFailure()).containsInstanceOf(IllegalArgumentException.class);
    }

    // -- orElse

    @Test
    void shouldReturnSelfOnOrElseIfSuccess() {
        var success = Try.success(42);
        assertThat(success.orElseTry(Try.success(0))).isSameAs(success);
    }

    @Test
    void shouldReturnSelfOnOrElseSupplierIfSuccess() {
        var success = Try.success(42);
        assertThat(success.orElseTry(() -> Try.success(0))).isSameAs(success);
    }

    @Test
    void shouldReturnAlternativeOnOrElseIfFailure() {
        var success = Try.success(42);
        assertThat(Try.failure(new RuntimeException()).orElseTry(success)).isSameAs(success);
    }

    @Test
    void shouldReturnAlternativeOnOrElseSupplierIfFailure() {
        var success = Try.success(42);
        assertThat(Try.failure(new RuntimeException()).orElseGet(() -> success)).isSameAs(success);
    }

    // -- Try.of

    @Test
    void shouldCreateSuccessWhenCallingTryOfSupplier() {
        assertThat(Try.of(() -> 1)).isInstanceOf(Success.class);
    }

    @Test
    void shouldCreateFailureWhenCallingTryOfSupplier() {
        assertThat(Try.of(() -> {
                    throw new Error("error");
                }))
                .isInstanceOf(Failure.class);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenCallingTryOfNullSupplier() {
        assertThatThrownBy(() -> Try.of(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("supplier is null");
    }

    // -- Try.fold

    @Test
    void shouldReturnValueIfSuccess() {
        var success = Try.success(42);
        assertThat(success.fold(
                        t -> {
                            throw new AssertionError("Not expected to be called");
                        },
                        Function.identity()))
                .isEqualTo(42);
    }

    @Test
    void shouldReturnAlternateValueIfFailure() {
        var success = Try.failure(new NullPointerException("something was null"));
        assertThat(success.<Integer>fold(t -> 42, a -> {
                    throw new AssertionError("Not expected to be called");
                }))
                .isEqualTo(42);
    }

    // -- Try.peek

    @Test
    void shouldPeekOnSuccess() {
        // given
        var failureRun = new AtomicBoolean(false);
        var successRun = new AtomicBoolean(false);
        var sut = Try.success(42);

        // when
        var result = sut.peek(t -> failureRun.set(true), r -> successRun.set(true));

        // then
        assertThat(result).isSameAs(sut);
        assertThat(failureRun).isFalse();
        assertThat(successRun).isTrue();
    }

    @Test
    void shouldPeekOnFailure() {
        // given
        var failureRun = new AtomicBoolean(false);
        var successRun = new AtomicBoolean(false);
        var sut = Try.failure(new Throwable());

        // when
        var result = sut.peek(t -> failureRun.set(true), r -> successRun.set(true));

        // then
        assertThat(result).isSameAs(sut);
        assertThat(successRun).isFalse();
    }

    // -- Try.ofCallable

    @Test
    void shouldCreateSuccessWhenCallingTryOfCallable() {
        assertThat(Try.ofCallable(() -> 1)).isInstanceOf(Success.class);
    }

    @Test
    void shouldCreateFailureWhenCallingTryOfCallable() {
        assertThat(Try.ofCallable(() -> {
                    throw new Error("error");
                }))
                .isInstanceOf(Failure.class);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenCallingTryOfCallable() {
        assertThatThrownBy(() -> Try.ofCallable(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("callable is null");
    }

    // -- Try.ofRunnable

    @Test
    void shouldCreateSuccessWhenCallingTryRunCheckedRunnable() {
        assertThat(Try.ofRunnable(() -> {})).isInstanceOf(Success.class);
    }

    @Test
    void shouldCreateFailureWhenCallingTryRunCheckedRunnable() {
        assertThat(Try.ofRunnable(() -> {
                    throw new Error("error");
                }))
                .isInstanceOf(Failure.class);
    }

    @Test
    void shouldThrowNullPointerExceptionWhenCallingTryRunCheckedRunnable() {
        assertThatThrownBy(() -> Try.ofRunnable(null))
                .isInstanceOf(NullPointerException.class)
                .hasMessage("runnable is null");
    }

    @Test
    void isSuccessOnSuccessShouldBeTrue() {
        assertThat(Try.of(() -> 1).isSuccess()).isTrue();
        assertThat(Try.of(() -> 1).isFailure()).isFalse();
    }

    @Test
    void isFailureOnFailureShouldBeTrue() {
        assertThat(Try.of(() -> {
                            throw new RuntimeException();
                        })
                        .isFailure())
                .isTrue();
        assertThat(Try.of(() -> {
                            throw new RuntimeException();
                        })
                        .isSuccess())
                .isFalse();
    }

    @Test
    void getOnSuccessShouldReturnTheExpressionResult() {
        assertThat(Try.of(() -> 2).get()).isEqualTo(2);
    }

    @Test
    void getOnFailureShouldThrowRuntimeException() {
        assertThatException().isThrownBy(() -> Try.of(() -> {
                    throw new RuntimeException();
                })
                .get());
    }

    @Test
    void getOrElseOnFailureShouldReturnGivenValue() {
        assertThat(Try.of(() -> {
                            throw new RuntimeException();
                        })
                        .orElse(3))
                .isEqualTo(3);
    }

    @Test
    void getOrElseOnOnSuccessShouldReturnExpressionResult() {
        assertThat(Try.of(() -> 1).orElse(0)).isOne();
    }

    @Test
    void getOrElseOnFailureShouldReturnExpressionResult() {
        assertThat(Try.of(() -> {
                            throw new RuntimeException();
                        })
                        .orElse(3))
                .isEqualTo(3);
    }

    @Test
    void OrElseOnOnSuccessShouldReturnExpressionResult() {
        assertThat(Try.of(() -> 2).orElseTry(() -> Try.of(() -> 3)).get().intValue())
                .isEqualTo(2);
    }

    @Test
    void OrElseOnOnFailureShouldReturnExpressionResultOfTheGivenTry() {
        assertThat(Try.of(() -> {
                            throw new RuntimeException();
                        })
                        .orElseTry(() -> Try.of(() -> 3))
                        .get())
                .isEqualTo(3);
    }

    @Test
    void OrElseTryOnFailureShouldReturnNewFailureIfTheGivenTryFails() {
        var result = Try.of(() -> {
                    throw new RuntimeException();
                })
                .orElseTry(() -> Try.of(() -> {
                    throw new NullPointerException();
                }));
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getFailure()).containsInstanceOf(NullPointerException.class);
    }

    @Test
    void mapOnSuccessShouldReturnSuccess() {
        Try<Integer> result = Try.of(() -> 2).map((r) -> 2 * r);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().intValue()).isEqualTo(4);
    }

    @Test
    void mapOnSuccessShouldReturnSuccessForSubTypes() {
        assertThat(Try.of(() -> 2).map(Integer::toBinaryString).get()).isEqualTo("10");
        assertThat(Try.of(() -> 2).map((r) -> r % 2 == 0).get()).isTrue();
    }

    @Test
    void mapOnSuccessShouldReturnAFailureIfTheMappingFunctionFails() {
        Try<Integer> result = Try.of(() -> 2).map((v) -> v / 0);
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void mapOnFailureShouldJustReturnTheFailure() {
        Try<String> result = Try.of(() -> {
                    throw new RuntimeException();
                })
                .map((v) -> "ignored");
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void flatMapOnSuccessShouldApplyTheGivenFunctionThatSucceeds() {
        var result = Try.of(() -> 2).flatMap((v) -> Try.of(() -> toBinaryString(v)));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo("10");
    }

    @Test
    void flatMapOnSuccessShouldReturnFailureIfTheMappingFunctionFails() {
        var result = Try.of(() -> 1).flatMap((v) -> {
            throw new RuntimeException();
        });
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void flatMapOnSuccessShouldShouldReturnFailureIfApplyingTheMappingFunctionFails() {
        var result = Try.of(() -> 1)
                .flatMap((v) -> Try.of(() -> {
                    throw new RuntimeException();
                }));
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void flatMapOnFailureShouldJustReturnTheFailure() {
        var result = Try.of(() -> {
                    throw new NumberFormatException();
                })
                .flatMap((v -> Try.of(() -> toBinaryString((Integer) v))));

        assertThatExceptionOfType(NoSuchElementException.class)
                .isThrownBy(result::get)
                .withMessage("Try is Failure")
                .withCause(new NumberFormatException());
    }

    @Test
    void filterOnSuccessShouldReturnSuccessIfPredicateIsSatisfied() {
        assertThat(Try.of(() -> 2).filter((v) -> v == 2).isSuccess()).isTrue();
    }

    @Test
    void filterOnSuccessShouldReturnFailureIfPredicateIsNotSatisfied() {
        assertThat(Try.of(() -> 2).filter((v) -> v == 0).isFailure()).isTrue();
    }

    @Test
    void filterOnFailureShouldJustReturnTheFailure() {
        assertThat(Try.of(() -> {
                            throw new RuntimeException();
                        })
                        .filter((v) -> false)
                        .isFailure())
                .isTrue();
    }

    // -- recover/recoverWith

    @Test
    void recoverOnSuccessShouldShouldJustReturnSuccess() {
        var result = Try.of(() -> 2).recover((t) -> 0);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().intValue()).isEqualTo(2);
    }

    @Test
    void typedRecoverOnSuccessShouldShouldJustReturnSuccess() {
        var result = Try.success(2).recover(IllegalStateException.class, t -> 0);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().intValue()).isEqualTo(2);
    }

    @Test
    void typedRecoverOnFailureShouldShouldReturnItselfGivenCauseTypeMismatch() {
        var failure = Try.failure(new IOException("test"));
        var result = failure.recover(IllegalStateException.class, t -> 0);
        assertThat(result).isEqualTo(failure);
    }

    @Test
    void recoverOnFailureShouldReturnSuccessIfRecoverySucceeds() {
        var result = Try.of(() -> {
                    throw new RuntimeException();
                })
                .recover((t) -> 0);
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(0);
    }

    @Test
    void typedRecoverOnFailureShouldReturnSuccessGivenCauseTypeMatchesAndRecoverySucceeds() {
        var result = Try.failure(new IOException("test")).recover(IOException.class, e -> 0);
        assertThat(result).isEqualTo(Try.success(0));
    }

    @Test
    void recoverOnFailureShouldReturnFailureIfRecoveryFails() {
        var result = Try.of(() -> {
                    throw new RuntimeException();
                })
                .recover((t) -> {
                    throw new RuntimeException();
                });
        assertThat(result.isFailure()).isTrue();
    }

    @Test
    void typedRecoverOnFailureShouldReturnFailureIfRecoveryFails() {
        var newCause = new ArrayIndexOutOfBoundsException();
        var result = Try.failure(new NoSuchElementException()).recover(NoSuchElementException.class, e -> {
            throw newCause;
        });
        assertThat(result).isEqualTo(Try.failure(newCause));
    }

    @Test
    void recoverWithOnSuccessShouldJustReturnSuccess() {
        var result = Try.of(() -> 2).recoverWith((t) -> Try.of(() -> 0));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get().intValue()).isEqualTo(2);
    }

    @Test
    void typedRecoverWithOnSuccessShouldJustReturnSuccess() {
        var success = Try.success(2);
        var result = success.recoverWith(IllegalArgumentException.class, t -> Try.success(0));
        assertThat(result).isEqualTo(success);
    }

    @Test
    void typedRecoverWithOnFailureShouldJustReturnItselfGivenCauseTypeMismatch() {
        var failure = Try.failure(new Throwable());
        var result = failure.recoverWith(IllegalArgumentException.class, t -> Try.success(0));
        assertThat(result).isEqualTo(failure);
    }

    @Test
    void recoverWithOnFailureShouldReturnSuccessIfRecoverySucceeds() {
        var result = Try.of(() -> {
                    throw new RuntimeException();
                })
                .recoverWith((t) -> Try.of(() -> 0));
        assertThat(result.isSuccess()).isTrue();
        assertThat(result.get()).isEqualTo(0);
    }

    @Test
    void typedRecoverWithOnFailureShouldReturnSuccessGivenCauseTypeMatchesAndRecoverySucceeds() {
        var expected = Try.success(0);
        var result = Try.<Integer>failure(new ArrayIndexOutOfBoundsException())
                .recoverWith(ArrayIndexOutOfBoundsException.class, e -> expected);
        assertThat(result).isEqualTo(expected);
    }

    @Test
    void recoverWithOnFailureShouldReturnFailureIfRecoveryFails() {
        var result = Try.of(() -> {
                    try {
                        throw new IOException();
                    } catch (IOException e) {
                        throw new RuntimeException(e);
                    }
                })
                .recover((t) -> {
                    if (t instanceof ClassCastException) {
                        return 1;
                    }
                    throw new NullPointerException();
                })
                .recoverWith(t -> {
                    throw new ArrayIndexOutOfBoundsException();
                });
        assertThat(result.isFailure()).isTrue();
        assertThatRuntimeException().isThrownBy(result::get);
    }

    @Test
    void typedRecoverWithOnFailureShouldReturnFailureGivenCauseTypeMatchesAndRecoveryFails() {
        var arrayIndexOutOfBoundsException = new ArrayIndexOutOfBoundsException();
        var result = Try.failure(new IOException())
                .recoverWith(ClassCastException.class, e -> Try.failure(new NullPointerException()))
                .recoverWith(IOException.class, t -> {
                    throw arrayIndexOutOfBoundsException;
                });
        assertThat(result).isEqualTo(Try.failure(arrayIndexOutOfBoundsException));
    }

    @Test
    void shouldThrowNewExceptionWhenOrElseThrowOnFailure() {
        assertThatExceptionOfType(IllegalStateException.class)
                .isThrownBy(() -> Try.failure(new IndexOutOfBoundsException()).orElseThrow(IllegalStateException::new))
                .withCauseInstanceOf(IndexOutOfBoundsException.class);
    }

    @Test
    void shouldNotBeEmptyOnGetSuccess() {
        var success = Try.success("success");

        var result = success.getSuccess();
        assertThat(result).isPresent();
    }

    @Test
    void shouldBeEmptyOnGetSuccess() {
        var success = Try.of(() -> {
            throw new RuntimeException();
        });

        var result = success.getSuccess();
        assertThat(result).isNotPresent();
    }

    @Test
    void getCauseOnSuccessShouldReturnNoSuchElementException() {
        var success = Try.success("success");
        assertThat(success.getCause()).isInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getFailureOnFailureShouldReturnOptionalOfException() {
        var failure = Try.failure(new NoSuchElementException());
        assertThat(failure.getFailure()).containsInstanceOf(NoSuchElementException.class);
    }

    @Test
    void getFailureOnSuccessShouldReturnEmptyOptional() {
        var success = Try.success("success");
        assertThat(success.getFailure()).isEmpty();
    }

    @Test
    void shouldReturnCompletedStageOnSuccess() {
        var success = Try.success("success");
        assertThat(success.toCompletionStage()).isCompleted();
    }

    @Test
    void shouldReturnFailedStageOnFailure() {
        var failure = Try.failure(new NoSuchElementException());
        assertThat(failure.toCompletionStage()).isNotCompleted();
    }

    @ParameterizedTest
    @MethodSource("fatal_errors")
    void shouldThrowIfFatalError(Error ex) {
        assertThatExceptionOfType(ex.getClass())
                .isThrownBy(() -> Try.of(() -> {
                    throw ex;
                }));
    }

    @Test
    void differentSuccessShouldNotBeEqual() {
        var try1 = Try.success(1);
        var try2 = Try.success(2);

        assertThat(try1.equals(try2)).isFalse();
    }

    @Test
    void sameSuccessShouldBeEqual() {
        var try1 = Try.success(1);
        var try2 = Try.success(1);

        assertThat(try1.equals(try2)).isTrue();
    }

    @Test
    void failureAndSuccessShouldNotBeEqual() {
        var try1 = Try.success(1);
        var try2 = Try.failure(new RuntimeException());

        assertThat(try1.equals(try2)).isFalse();
    }

    @Test
    void differentFailuresShouldNotBeEqual() {
        var try1 = Try.failure(new RuntimeException("one"));
        var try2 = Try.failure(new RuntimeException("two"));

        assertThat(try1.equals(try2)).isFalse();
    }

    @Test
    void sameFailureTypeWithDifferentExceptionObjectsShouldNotBeEqual() {
        var try1 = Try.failure(new RuntimeException("one"));
        var try2 = Try.failure(new RuntimeException("one"));

        assertThat(try1.equals(try2)).isFalse();
    }

    @Test
    void sameFailureObjectShouldBeEqual() {
        var ex = new RuntimeException("same");
        var try1 = Try.failure(ex);
        var try2 = Try.failure(ex);

        assertThat(try1.equals(try2)).isTrue();
    }

    @Test
    void sameFailureObjectShouldHaveSameHash() {
        var ex = new RuntimeException("same");
        var try1 = Try.failure(ex);
        var try2 = Try.failure(ex);

        assertThat(try1).hasSameHashCodeAs(try2);
    }

    @Test
    void sameFailureTypeWithDifferentExceptionObjectsShouldHaveDifferentHash() {
        var try1 = Try.failure(new RuntimeException("one"));
        var try2 = Try.failure(new RuntimeException("one"));

        assertThat(try1).doesNotHaveSameHashCodeAs(try2);
    }

    @Test
    void differentSuccessShouldHaveDifferentHash() {
        var try1 = Try.success(1);
        var try2 = Try.success(2);

        assertThat(try1).doesNotHaveSameHashCodeAs(try2);
    }

    @Test
    void failureAndSuccessShouldNotHaveSameHash() {
        var try1 = Try.success(1);
        var try2 = Try.failure(new RuntimeException());

        assertThat(try1).doesNotHaveSameHashCodeAs(try2);
    }

    @Test
    void shouldReturnStringForSuccess() {
        var try1 = Try.success(1);

        assertThat(try1).hasToString("Success[value=1]");
    }

    @Test
    void shouldReturnStringForFailure() {
        var try1 = Try.failure(new InterruptedIOException("TEST"));

        assertThat(try1).hasToString("Failure[cause=java.io.InterruptedIOException: TEST]");
    }

    @Test
    void shouldTryCheckedExceptionForSupplier() {

        var testClass = new CheckedExceptionClassImpl();

        var result = Try.of(testClass::checked);

        assertThat(result.isSuccess()).isTrue();
    }

    private static Stream<Arguments> checkedExceptions() {
        return Stream.of(new Exception(), new IOException(), new SQLException()).map(Arguments::of);
    }

    @ParameterizedTest
    @MethodSource("checkedExceptions")
    void shouldFailSupplierWithThrownCheckedException(Exception checkedException) {
        // when
        var result = Try.of(() -> {
            throw checkedException;
        });

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(checkedException);
    }

    @Test
    void shouldTryCheckedExceptionForRunnable() {

        var testClass = new CheckedExceptionClassImpl();

        var result = Try.ofRunnable(testClass::checked);

        assertThat(result.isSuccess()).isTrue();
    }

    @ParameterizedTest
    @MethodSource("checkedExceptions")
    void shouldFailRunnableWithThrownCheckedException(Exception checkedException) {
        // when
        var result = Try.ofRunnable(() -> {
            throw checkedException;
        });

        // then
        assertThat(result.isFailure()).isTrue();
        assertThat(result.getCause()).isEqualTo(checkedException);
    }

    interface CheckedExceptionClass {
        int checked() throws IOException;
    }

    static class CheckedExceptionClassImpl implements CheckedExceptionClass {
        @Override
        public int checked() throws IOException {
            return 0;
        }
    }
}
