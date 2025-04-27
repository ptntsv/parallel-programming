package org.example;

import java.util.concurrent.atomic.AtomicReference;

public class ReentrantTASlock {

    private static class State {

        final Thread holder;
        final long cnt;

        private State(Thread newHolder, long newCnt) {
            holder = newHolder;
            cnt = newCnt;
        }
    }

    private final AtomicReference<State> state = new AtomicReference<>(new State(null, 0));

    public void lock() {
        var currentThread = Thread.currentThread();
        while (true) {
            var currentState = state.get();
            if (currentState.holder == null) {
                if (state.compareAndSet(currentState, new State(currentThread, 1))) {
                    return;
                }
            } else if (currentState.holder == currentThread) {
                if (state.compareAndSet(currentState,
                    new State(currentThread, currentState.cnt + 1))) {
                    return;
                }
            }
        }
    }

    public void unlock() {
        var currentThread = Thread.currentThread();
        while (true) {
            var currentState = state.get();
            if (currentState.holder == currentThread) {
                long currentCnt = state.get().cnt;
                var newState = (currentCnt == 1) ? new State(null, 0)
                    : new State(currentThread, currentCnt - 1);
                if (state.compareAndSet(currentState, newState)) {
                    return;
                }
            } else {
                throw new RuntimeException("Trying to acquire the lock from outer thread");
            }
        }
    }
}
