package io.instanto.oolong;

/**
 * Reads a named property of one type, without reflection at runtime.
 *
 * <p>Generated while TeaVM compiles the test. Obtained from {@code ObjectMatchers.propertiesOf}.
 */
public interface PropertyReader<T> {

  /** Reads one property, which must be one of {@link #names()}. */
  Object read(T target, String property);

  /** Whether a value is of the type this reader was generated for. */
  boolean canRead(Object target);

  /** Every property this reader can read. */
  String[] names();
}
