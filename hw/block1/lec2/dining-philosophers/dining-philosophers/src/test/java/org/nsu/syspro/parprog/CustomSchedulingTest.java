package org.nsu.syspro.parprog;

import org.junit.jupiter.api.Timeout;
import org.junit.jupiter.api.condition.EnabledIf;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.nsu.syspro.parprog.base.DefaultFork;
import org.nsu.syspro.parprog.base.DiningTable;
import org.nsu.syspro.parprog.examples.DefaultPhilosopher;
import org.nsu.syspro.parprog.helpers.TestLevels;
import org.nsu.syspro.parprog.interfaces.Fork;

import static org.junit.jupiter.api.Assertions.assertTrue;

public class CustomSchedulingTest extends TestLevels {

    static final class CustomizedPhilosopher extends DefaultPhilosopher {
        @Override
        public void onHungry(Fork left, Fork right) {
            sleepMillis(this.id * 20);
            System.out.println(Thread.currentThread() + " " + this + ": onHungry");
            super.onHungry(left, right);
        }

    }

    static final class CustomizedSlowPhilosopher extends DefaultPhilosopher {
        @Override
        public void countMeal() {
            if (id == 0) sleepSeconds(1);
            super.countMeal();
        }
    }

    static final class CustomizedFork extends DefaultFork {
        @Override
        public void acquire() {
            System.out.println(Thread.currentThread() + " trying to acquire " + this);
            super.acquire();
            System.out.println(Thread.currentThread() + " acquired " + this);
            sleepMillis(100);
        }
    }

    static final class CustomizedFastFork extends DefaultFork {
        @Override
        public void acquire() {
            System.out.println(Thread.currentThread() + " trying to acquire " + this);
            super.acquire();
            System.out.println(Thread.currentThread() + " acquired " + this);
        }
    }

    static final class CustomizedDualPhilosopher extends DefaultPhilosopher {
        @Override
        public void countMeal() {
            if (id % 2 == 0) sleepMillis(10);
            else sleepMillis(100);
            super.countMeal();
        }
    }

    static final class CustomizedTable extends DiningTable<CustomizedPhilosopher, CustomizedFork> {
        public CustomizedTable(int N) {
            super(N);
        }

        @Override
        public CustomizedFork createFork() {
            return new CustomizedFork();
        }

        @Override
        public CustomizedPhilosopher createPhilosopher() {
            return new CustomizedPhilosopher();
        }
    }

    static final class CustomizedDualTable extends DiningTable<CustomizedDualPhilosopher, CustomizedFork> {

        public CustomizedDualTable(int N) {
            super(N);
        }

        @Override
        public CustomizedFork createFork() {
            return new CustomizedFork();
        }

        @Override
        public CustomizedDualPhilosopher createPhilosopher() {
            return new CustomizedDualPhilosopher();
        }
    }

    static final class CustomizedSingleSlowTable extends DiningTable<CustomizedSlowPhilosopher, CustomizedFastFork> {

        public CustomizedSingleSlowTable(int N) {
            super(N);
        }

        @Override
        public CustomizedFastFork createFork() {
            return new CustomizedFastFork();
        }

        @Override
        public CustomizedSlowPhilosopher createPhilosopher() {
            return new CustomizedSlowPhilosopher();
        }
    }

    @EnabledIf("easyEnabled")
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Timeout(2)
    void testDeadlockFreedom(int N) {
        final CustomizedTable table = dine(new CustomizedTable(N), 1);
    }

    @EnabledIf("easyEnabled")
    @ParameterizedTest
    @ValueSource(ints = {5})
    @Timeout(2)
    void testSingleSlow(int N) {
        final CustomizedSingleSlowTable table = dine(new CustomizedSingleSlowTable(N), 1);
        for (int i = 0; i < N; i++) {
            System.out.println(table.philosopherAt(i).meals());
        }
        assertTrue(table.maxMeals() >= 1000);
    }

    @EnabledIf("mediumEnabled")
    @ParameterizedTest
    @ValueSource(ints = {2, 3, 4, 5})
    @Timeout(2)
    void testWeakFairness(int N) {
        final CustomizedDualTable table = dine(new CustomizedDualTable(N), 1);
        assertTrue(table.minMeals() > 0); // every philosopher eat at least oncCustomizedPhilosophere
    }
}
