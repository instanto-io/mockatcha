/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.teavm;

import static org.junit.Assert.assertEquals;
import static org.junit.Assert.assertFalse;
import static org.junit.Assert.assertTrue;

import java.lang.reflect.Proxy;
import java.util.ArrayList;
import java.util.List;
import org.junit.Before;
import org.junit.Test;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.BasicBlock;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.MethodHolder;
import org.teavm.model.MethodReference;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.Variable;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.InvocationType;
import org.teavm.model.instructions.InvokeInstruction;

/**
 * Checks that a TeaVM build mocks only the types its code names.
 *
 * <p>This runs on the JVM against classes built in TeaVM's own model, as TeaVM hands them to the
 * plugin while it loads them.
 */
public class MockedTypesPluginTest {
  private static final String MOCKATCHA = "io.instanto.mockatcha.Mockatcha";

  private final List<String> errors = new ArrayList<>();

  @Before
  public void forgetEarlierBuilds() {
    MockedTypes.clear();
  }

  @Test
  public void recordsTheClassLiteralPassedToMock() {
    transform(caller("mock", "com.example.Pricing"));

    assertTrue(MockedTypes.isDeclared("com.example.Pricing"));
    assertTrue(errors.isEmpty());
  }

  @Test
  public void recordsTheClassLiteralPassedToSpy() {
    transform(caller("spy", "com.example.Ledger"));

    assertTrue(MockedTypes.isDeclared("com.example.Ledger"));
  }

  @Test
  public void leavesUnnamedTypesUnmocked() {
    transform(caller("mock", "com.example.Pricing"));

    assertFalse(MockedTypes.isDeclared("java.lang.Exception"));
  }

  @Test
  public void refusesAClassThatIsNotALiteral() {
    transform(caller("mock", null));

    assertEquals(1, errors.size());
    assertTrue(errors.get(0), errors.get(0).contains("needs a class literal"));
  }

  @Test
  public void ignoresMockatchaHandingItsCallersClassOn() {
    transform(caller(MOCKATCHA, "mock", null));

    assertTrue(errors.isEmpty());
  }

  /** A test class with one method calling {@code Mockatcha.<factory>}. */
  private static ClassHolder caller(String factory, String literalClass) {
    return caller("com.example.PricingTest", factory, literalClass);
  }

  /**
   * A class with one method calling {@code Mockatcha.<factory>}, passing a class literal, or a
   * class value that is not one when {@code literalClass} is null.
   */
  private static ClassHolder caller(String className, String factory, String literalClass) {
    Program program = new Program();
    program.createVariable();
    BasicBlock block = program.createBasicBlock();
    Variable type = program.createVariable();
    if (literalClass != null) {
      ClassConstantInstruction literal = new ClassConstantInstruction();
      literal.setConstant(ValueType.object(literalClass));
      literal.setReceiver(type);
      block.add(literal);
    }
    InvokeInstruction invoke = new InvokeInstruction();
    invoke.setType(InvocationType.SPECIAL);
    invoke.setMethod(
        new MethodReference(
            MOCKATCHA,
            factory,
            ValueType.object("java.lang.Class"),
            ValueType.object("java.lang.Object")));
    invoke.setArguments(type);
    block.add(invoke);

    MethodHolder method = new MethodHolder("create", ValueType.object("java.lang.Object"));
    method.setProgram(program);
    ClassHolder cls = new ClassHolder(className);
    cls.addMethod(method);
    return cls;
  }

  private void transform(ClassHolder cls) {
    Diagnostics diagnostics =
        (Diagnostics)
            Proxy.newProxyInstance(
                Diagnostics.class.getClassLoader(),
                new Class<?>[] {Diagnostics.class},
                (proxy, method, args) -> {
                  if (method.getName().equals("error")) {
                    errors.add((String) args[1]);
                  }
                  return null;
                });
    ClassHolderTransformerContext context =
        (ClassHolderTransformerContext)
            Proxy.newProxyInstance(
                ClassHolderTransformerContext.class.getClassLoader(),
                new Class<?>[] {ClassHolderTransformerContext.class},
                (proxy, method, args) ->
                    method.getName().equals("getDiagnostics") ? diagnostics : null);
    new MockedTypesPlugin().transformClass(cls, context);
  }
}
