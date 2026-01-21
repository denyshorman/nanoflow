package io.github.denyshorman.nanoflow.internal.util;

/**
 * Internal utility for throwing checked exceptions without wrapping them in {@link RuntimeException}.
 * <p>
 * This class uses a type-erasure trick to bypass Java's checked exception mechanism,
 * allowing checked exceptions to be thrown without being declared in method signatures.
 * This is used internally by Nanoflow to propagate exceptions to callers without wrapping them.
 *
 * <h2>Implementation Note</h2>
 * This technique exploits the fact that Java's type erasure removes generic type
 * information at runtime, allowing us to cast any {@code Throwable} to a generic
 * type parameter {@code E extends Throwable} and throw it.
 */
final public class SneakyThrow {
    private SneakyThrow() {
    }

    /**
     * Throws the given exception without wrapping it, bypassing checked exception requirements.
     * <p>
     * Despite the {@link RuntimeException} return type, this method never actually returns.
     * The return type is declared to allow this method to be used in contexts where a
     * return value is expected (e.g., {@code throw sneakyThrow(e);}).
     *
     * @param e   the exception to throw; must not be null
     * @param <E> the type of the exception (inferred at compile time, erased at runtime)
     * @return never returns; declared as RuntimeException for syntactic convenience
     * @throws E the given exception, thrown without wrapping
     */
    @SuppressWarnings("unchecked")
    public static <E extends Throwable> RuntimeException sneakyThrow(Throwable e) throws E {
        throw (E) e;
    }
}
