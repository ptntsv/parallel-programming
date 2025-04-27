package org.example;

import static org.openjdk.jcstress.annotations.Expect.ACCEPTABLE;

import org.openjdk.jcstress.annotations.Actor;
import org.openjdk.jcstress.annotations.Arbiter;
import org.openjdk.jcstress.annotations.JCStressTest;
import org.openjdk.jcstress.annotations.Outcome;
import org.openjdk.jcstress.annotations.State;
import org.openjdk.jcstress.infra.results.I_Result;

@JCStressTest
@State
@Outcome(id = "0", expect = ACCEPTABLE, desc = "Thread released lock exact the same times as it acquired it")
public class ReentrantTASlockReentrancyTest {

    private int acquired = 0;
    private final int threshold = 100;
    private boolean release = false;
    private static final ReentrantTASlock lock = new ReentrantTASlock();

    @Actor
    void actor1() {
        lock.lock();
        try {
            if (acquired > threshold) {
                assert (!release);
                release = true;
            }
            if (release) {
                return;
            }
            acquired++;
            actor1();
        } finally {
            if (acquired > 0) {
                acquired--;
            }
            lock.unlock();
        }
    }

    @Arbiter
    void arbiter(I_Result r) {
        r.r1 = acquired;
    }
}
