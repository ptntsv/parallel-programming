package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.UserThread;
import org.nsu.syspro.parprog.external.*;

import java.util.*;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.locks.*;

public class SolutionThread extends UserThread {

    private final ExecutorService pool = Executors.newSingleThreadExecutor();

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
     * Map used by specific thread for L2 compilation.
     */
    static final Map<Long, CompiledMethod> compilingDone = new HashMap<>();
    /**
     * Lock for compilingDone.
     */
    static Lock lock = new ReentrantLock();
    /**
     * Conditional variable for compilingDone.
     */
    static Condition condition = lock.newCondition();

    static ReadWriteLock rwLock = new ReentrantReadWriteLock();
    public static CachingCompiler cachingL1Compiler = new CachingL1Compiler();


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
                condition.await();
            }
            CompiledMethod code = compiler.compile_l2(methodID);
            compilingDone.put(methodID.id(), code);
            condition.signal();
        } catch (InterruptedException e) {
            throw new RuntimeException(e);
        } finally {
            lock.unlock();
        }
    }


    public SolutionThread(int compilationThreadBound, ExecutionEngine exec, CompilationEngine compiler, Runnable r) {
        super(compilationThreadBound, exec, compiler, r);
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
            CompiledMethod code = compilingDone.get(methodID.id());
            rwLock.writeLock().lock();
            try {
                globalL2.put(methodID.id(), code);
            } finally {
                rwLock.writeLock().unlock();
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


        rwLock.readLock().lock();
        try {
            code = globalL2.getOrDefault(methodID.id(), null);
            if (code != null) return exec.execute(code);
        } finally {
            rwLock.readLock().unlock();
        }
        if (lHotLevel > 90_000) {
            pool.execute(() -> {
                produceL2Compilation(methodID, 100);
            });
            updateL2(methodID);
        }

        code = cachingL1Compiler.ask(methodID);
        if (code != null) return exec.execute(code);

        if (lHotLevel > 9_000) {
            code = compiler.compile_l1(methodID);
            cachingL1Compiler.update(methodID, code);
            return exec.execute(code);
        }
        return exec.interpret(methodID);
    }
}