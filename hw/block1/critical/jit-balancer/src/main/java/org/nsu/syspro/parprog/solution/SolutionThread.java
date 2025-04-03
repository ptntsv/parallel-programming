package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.*;
import java.util.concurrent.ThreadFactory;
import java.util.concurrent.locks.Condition;
import java.util.concurrent.locks.Lock;
import java.util.concurrent.locks.ReentrantLock;

public class SolutionThread extends UserThread {

    /**
     * Thread factory utility class.
     */
    static class SolutionThreadFactory implements ThreadFactory {
        @Override
        public Thread newThread(Runnable runnable) {
            return new L2CompilerThread(runnable);
        }
    }

    private final ThreadFactory threadFactory;
    /**
     * Thread local field that contains frequency of execution of certain method.
     */
    private final Map<Long, Long> localHotness = new HashMap<>();
    /**
     * Thread global caching map that contains precompiled methods with L1 compiler.
     */
    static final Map<Long, CompiledMethod> globalL1 = new HashMap<>();
    /**
     * Thread global caching map that contains precompiled methods with L2 compiler.
     */
    static final Map<Long, CompiledMethod> globalL2 = new HashMap<>();
    /**
     * Buffer used by specific thread for L2 compilation.
     */
    static final Deque<CompiledMethod> compilingDone = new ArrayDeque<>();
    /**
     * Lock for compilingDone.
     */
    static Lock lock = new ReentrantLock();
    /**
     * Conditional variable for compilingDone.
     */
    static Condition condition = lock.newCondition();


    /**
     * Method that compiles(L2) given method, produces it into buffer and notifies.
     *
     * @param methodID Method to compile.
     * @param N        Maximum size of compilingDone buffer.
     */
    private void produceL2Compilation(MethodID methodID, long N) {
        lock.lock();
        try {
            while (compilingDone.size() > N) {
                condition.wait();
            }
            CompiledMethod code = compiler.compile_l2(methodID);
            compilingDone.add(code);
            condition.signal();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }


    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
        threadFactory = new SolutionThreadFactory();
    }

    /**
     * Method that updates global cache with L1 compiled methods.
     *
     * @param methodID Method to compile.
     */
    private void updateL1(MethodID methodID) {
        CompiledMethod code = compiler.compile_l1(methodID);
        synchronized (globalL1) {
            globalL1.put(methodID.id(), code);
        }
    }

    /**
     * Consumes L2 compiled methods from buffer and updates global cache.
     *
     * @param methodID Method to compile.
     */
    private void updateL2(MethodID methodID) {
        lock.lock();
        try {
            while (compilingDone.isEmpty()) {
                condition.await();
            }
            CompiledMethod code = compilingDone.pop();
            synchronized (globalL2) {
                globalL2.put(methodID.id(), code);
            }

        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }

    @Override
    public ExecutionResult executeMethod(MethodID methodID) {
        final Long id = methodID.id();
        long lHotLevel = localHotness.getOrDefault(id, 0L);
        localHotness.put(id, lHotLevel + 1);

        CompiledMethod code;
        synchronized (globalL2) {
            code = globalL2.getOrDefault(methodID.id(), null);
            if (code != null) return exec.execute(code);
        }
        if (lHotLevel > 90_000) {
            var compThread = threadFactory.newThread(() -> {
                produceL2Compilation(methodID, 100);
            });
            compThread.start();
            updateL2(methodID);
        }
        synchronized (globalL1) {
            code = globalL1.getOrDefault(methodID.id(), null);
            if (code != null) return exec.execute(code);
        }
        if (lHotLevel > 9_000) {
            updateL1(methodID);
        }
        return exec.interpret(methodID);
    }

    /**
     * Specific thread to L2 compilation.
     */
    static class L2CompilerThread extends Thread {
        private final Runnable runnable;

        L2CompilerThread(Runnable runnable) {
            this.runnable = runnable;
        }

        @Override
        public void run() {
            this.runnable.run();
        }
    }
}