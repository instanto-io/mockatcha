/*
 * Copyright 2026 Carl Stainton
 *
 * SPDX-License-Identifier: Apache-2.0
 */
package io.instanto.mockatcha.teavm;

import java.util.HashMap;
import java.util.Map;
import java.util.Set;
import org.teavm.diagnostics.Diagnostics;
import org.teavm.model.BasicBlock;
import org.teavm.model.CallLocation;
import org.teavm.model.ClassHolder;
import org.teavm.model.ClassHolderTransformer;
import org.teavm.model.ClassHolderTransformerContext;
import org.teavm.model.Instruction;
import org.teavm.model.MethodHolder;
import org.teavm.model.Program;
import org.teavm.model.ValueType;
import org.teavm.model.instructions.ClassConstantInstruction;
import org.teavm.model.instructions.InvokeInstruction;
import org.teavm.vm.spi.TeaVMHost;
import org.teavm.vm.spi.TeaVMPlugin;

/**
 * Records the types a TeaVM build mocks, in {@link MockedTypes}, as TeaVM loads each class and
 * before any of its code is analysed.
 *
 * <p>A mocked type is the class literal passed to {@code Mockatcha.mock} or {@code Mockatcha.spy}.
 * A call that passes any other class value is an error: the mocks a build generates must be bounded
 * by what its code names.
 */
public final class MockedTypesPlugin implements TeaVMPlugin, ClassHolderTransformer {
  private static final String MOCKATCHA = "io.instanto.mockatcha.Mockatcha";
  private static final Set<String> FACTORIES = Set.of("mock", "spy");

  @Override
  public void install(TeaVMHost host) {
    MockedTypes.clear();
    host.add(this);
  }

  @Override
  public void transformClass(ClassHolder cls, ClassHolderTransformerContext context) {
    // Mockatcha hands its caller's class on to the generator; its callers name the type.
    if (cls.getName().equals(MOCKATCHA)) {
      return;
    }
    for (MethodHolder method : cls.getMethods()) {
      Program program = method.getProgram();
      if (program != null) {
        recordMockedTypes(method, program, context.getDiagnostics());
      }
    }
  }

  private static void recordMockedTypes(
      MethodHolder method, Program program, Diagnostics diagnostics) {
    Map<Integer, ValueType> literals = new HashMap<>();
    for (BasicBlock block : program.getBasicBlocks()) {
      for (Instruction instruction : block) {
        if (instruction instanceof ClassConstantInstruction literal) {
          literals.put(literal.getReceiver().getIndex(), literal.getConstant());
        }
      }
    }
    for (BasicBlock block : program.getBasicBlocks()) {
      for (Instruction instruction : block) {
        if (instruction instanceof InvokeInstruction invoke
            && invoke.getMethod().getClassName().equals(MOCKATCHA)
            && FACTORIES.contains(invoke.getMethod().getName())
            && !invoke.getArguments().isEmpty()) {
          ValueType type = literals.get(invoke.getArguments().get(0).getIndex());
          if (type instanceof ValueType.Object object) {
            MockedTypes.declare(object.getClassName());
          } else {
            diagnostics.error(
                new CallLocation(method.getReference(), invoke.getLocation()),
                "Mockatcha.{{m0}} needs a class literal under TeaVM, so the mocks a build generates"
                    + " are the ones it names",
                invoke.getMethod());
          }
        }
      }
    }
  }
}
