package org.example;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;
import static org.openjdk.jcstress.annotations.Expect.FORBIDDEN;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.II_Result;

@JCStressTest
@State
@Outcome(id = {"1, 1", "2, 2"}, expect = FORBIDDEN, desc = "Mutex failure")
@Outcome(id = {"1, 2", "2, 1"}, expect = ACCEPTABLE, desc = "Mutex works")
public class ReentrantTASlockCounterTest {

    private int v;
    private static final ReentrantTASlock lock = new ReentrantTASlock();

    @Actor
    public void actor1(II_Result r) {
        lock.lock();
        try {
            r.r1 = ++v;
        } finally {
            lock.unlock();
        }
    }

    @Actor
    public void actor2(II_Result r) {
        lock.lock();
        try {
            r.r2 = ++v;
        } finally {
            lock.unlock();
        }
    }
}