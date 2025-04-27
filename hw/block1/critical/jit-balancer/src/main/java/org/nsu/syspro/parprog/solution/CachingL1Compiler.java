package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.*;

public class CachingL1Compiler extends CachingCompiler {

    @Override
    void update(MethodID methodID, CompiledMethod code) {
        lock.writeLock().lock();
        try {
            cache.put(methodID.id(), code);
        } finally {
            lock.writeLock().unlock();
        }
    }

    @Override
    CompiledMethod ask(MethodID methodID) {
        lock.readLock().lock();
        try {
            return cache.getOrDefault(methodID.id(), null);
        } finally {
            lock.readLock().unlock();
        }
    }
}
