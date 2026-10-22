/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.teavm;

import java.util.Set;
import java.util.concurrent.ConcurrentHashMap;

/**
 * The types a TeaVM build mocks or spies on: the class literals passed to {@code Mockatcha.mock}
 * and {@code Mockatcha.spy}. Mocks are generated for these and nothing else.
 *
 * <p>TeaVM hands the generator every class its analysis says could reach the class argument, and a
 * fast analysis says every class could. Bounding generation by what the program names keeps a
 * build to the mocks it asks for.
 *
 * <p>{@link MockedTypesPlugin} fills it while TeaVM loads classes; {@link TeaVmMocks} reads it while
 * it generates. It is shared through TeaVM's class loader, so it must not be compile-time code, and
 * its methods are public because the generator is loaded by a different class loader.
 */
public final class MockedTypes {
  private static final Set<String> DECLARED = ConcurrentHashMap.newKeySet();

  private MockedTypes() {}

  public static void clear() {
    DECLARED.clear();
  }

  public static void declare(String className) {
    DECLARED.add(className);
  }

  public static boolean isDeclared(String className) {
    return DECLARED.contains(className);
  }
}
