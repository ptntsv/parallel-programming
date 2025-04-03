package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.*;

public class SolutionThread extends UserThread {

    private final Map<Long, Long> localHotness = new HashMap<>();
    static final Map<Long, CompiledMethod> globalL1 = new HashMap<>();

    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
    }

    private void updateL1(MethodID methodID) {
        CompiledMethod code = compiler.compile_l1(methodID);
        synchronized (globalL1) {
            globalL1.put(methodID.id(), code);
        }
    }

    @Override
    public ExecutionResult executeMethod(MethodID methodID) {
        final Long id = methodID.id();
        long lHotLevel = localHotness.getOrDefault(id, 0L);
        localHotness.put(id, lHotLevel + 1);

        CompiledMethod code;
        synchronized (globalL1) {
            code = globalL1.getOrDefault(methodID.id(), null);
        }
        if (code != null) return exec.execute(code);
        if (lHotLevel > 9_000) {
            updateL1(methodID);
        }
        return exec.interpret(methodID);
    }
}