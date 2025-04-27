package org.nsu.syspro.parprog.solution;

import org.nsu.syspro.parprog.external.CompiledMethod;
import org.nsu.syspro.parprog.external.MethodID;

import java.util.HashMap;
import java.util.Map;
import java.util.concurrent.locks.ReadWriteLock;
import java.util.concurrent.locks.ReentrantReadWriteLock;

public abstract class CachingCompiler {
    protected final Map<Long, CompiledMethod> cache = new HashMap<>();
    protected final ReadWriteLock lock = new ReentrantReadWriteLock();

    abstract void update(MethodID methodID, CompiledMethod code);

    abstract CompiledMethod ask(MethodID methodID);
}
