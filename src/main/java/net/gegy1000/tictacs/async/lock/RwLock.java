package net.gegy1000.tictacs.async.lock;

import net.gegy1000.tictacs.async.LinkedWaiter;
import net.gegy1000.tictacs.async.WaiterQueue;
import net.gegy1000.justnow.Waker;

import java.util.concurrent.atomic.AtomicInteger;

public final class RwLock {
    private static final int FREE = 0;
    private static final int WRITING = -1;

    private final Read read = new Read();
    private final Write write = new Write();

    private final AtomicInteger state = new AtomicInteger(FREE);

    private final WaiterQueue waiters = new WaiterQueue();

    public Lock read() {
        return this.read;
    }

    public Lock write() {
        return this.write;
    }

    boolean tryAcquireRead() {
        while (true) {
            int state = this.state.get();
            if (state == WRITING) {
                return false;
            }

            if (this.state.compareAndSet(state, state + 1)) {
                return true;
            }
        }
    }

    boolean canAcquireRead() {
        return this.state.get() != WRITING;
    }

    boolean tryAcquireWrite() {
        return this.state.compareAndSet(FREE, WRITING);
    }

    boolean canAcquireWrite() {
        return this.state.get() == FREE;
    }

    void releaseWrite() {
        if (!this.state.compareAndSet(WRITING, FREE)) {
            throw new IllegalStateException("write lock not acquired");
        }

        this.waiters.wake();
    }

    void releaseRead() {
        int state = this.state.getAndDecrement();
        if (state <= 0) {
            throw new IllegalStateException("read lock not acquired");
        }

        int readCount = state - 1;
        if (readCount <= 0) {
            this.waiters.wake();
        }
    }

    @Override
    public String toString() {
        int state = this.state.get();
        if (state == FREE) {
            return "RwLock(FREE)";
        } else if (state == WRITING) {
            return "RwLock(WRITING)";
        } else {
            return "RwLock(READING=" + state + ")";
        }
    }

    private final class Read implements Lock {
        @Override
        public boolean tryAcquire() {
            return RwLock.this.tryAcquireRead();
        }

        @Override
        public boolean canAcquire() {
            return RwLock.this.canAcquireRead();
        }

        @Override
        public void release() {
            RwLock.this.releaseRead();
        }

        @Override
        public boolean tryAcquireAsync(LinkedWaiter waiter, Waker waker) {
            if (!this.tryAcquire()) {
                RwLock.this.waiters.registerWaiter(waiter, waker);
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            if (this.canAcquire()) {
                return "RwLock.Read(FREE)";
            } else {
                return "RwLock.Read(LOCKED)";
            }
        }
    }

    private final class Write implements Lock {
        @Override
        public boolean tryAcquire() {
            return RwLock.this.tryAcquireWrite();
        }

        @Override
        public boolean canAcquire() {
            return RwLock.this.canAcquireWrite();
        }

        @Override
        public void release() {
            RwLock.this.releaseWrite();
        }

        @Override
        public boolean tryAcquireAsync(LinkedWaiter waiter, Waker waker) {
            if (!this.tryAcquire()) {
                RwLock.this.waiters.registerWaiter(waiter, waker);
                return false;
            }

            return true;
        }

        @Override
        public String toString() {
            if (this.canAcquire()) {
                return "RwLock.Write(FREE)";
            } else {
                return "RwLock.Write(LOCKED)";
            }
        }
    }
}
