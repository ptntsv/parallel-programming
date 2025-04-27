package org.example;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Expect;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.BB_Result;
import org.openjdk.jcstress.infra.results.B_Result;
import org.openjdk.jcstress.infra.results.Z_Result;

@JCStressTest
@State
@Outcome(id = "true", expect = ACCEPTABLE, desc = "Expected exception thrown")
public class ReentrantTASlockExceptionTest {

    private final static ReentrantTASlock lock = new ReentrantTASlock();

    @Actor
    void actor1(Z_Result r) {
        lock.lock();
        try {
            int x = 42;
        } finally {
            lock.unlock();
        }
        try {
            lock.unlock();
        } catch (RuntimeException e) {
            r.r1 = true;
        }
    }
}
