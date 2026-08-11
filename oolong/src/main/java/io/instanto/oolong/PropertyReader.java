package io.instanto.oolong;

/**
 * Reads a named property of one type, without reflection at runtime.
 *
 * <p>TeaVM strips reflection, so a property cannot be looked up by name in the browser. An
 * implementation of this interface is generated while TeaVM compiles the test: it knows the type's
 * accessors and fields already, and picks between them with an ordinary string comparison.
 *
 * <p>Obtained from {@code OolongMatchers.objectContaining}, which is where the type is named.
 */
public interface PropertyReader<T> {

  /** Reads one property, which must be one of {@link #names()}. */
  Object read(T target, String property);

  /** Whether a value is of the type this reader was generated for. */
  boolean canRead(Object target);

  /** Every property this reader can read, which is what a failure message should list. */
  String[] names();
}
