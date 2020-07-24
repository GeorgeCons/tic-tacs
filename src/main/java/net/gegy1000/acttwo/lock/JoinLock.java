package net.gegy1000.acttwo.lock;

import net.gegy1000.justnow.Waker;

public final class JoinLock implements Lock {
    private final Lock[] locks;

    public JoinLock(Lock[] locks) {
        this.locks = locks;
    }

    @Override
    public boolean tryAcquire() {
        for (int i = 0; i < this.locks.length; i++) {
            Lock lock = this.locks[i];
            if (lock != null && !lock.tryAcquire()) {
                this.releaseUpTo(i);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean tryAcquireAsync(LockWaiter waiter, Waker waker) {
        for (int i = 0; i < this.locks.length; i++) {
            Lock lock = this.locks[i];
            if (lock != null && !lock.tryAcquireAsync(waiter, waker)) {
                this.releaseUpTo(i);
                return false;
            }
        }
        return true;
    }

    @Override
    public boolean canAcquire() {
        for (Lock lock : this.locks) {
            if (lock != null && !lock.canAcquire()) {
                return false;
            }
        }

        return true;
    }

    @Override
    public void release() {
        for (Lock lock : this.locks) {
            if (lock != null) {
                lock.release();
            }
        }
    }

    private void releaseUpTo(int endIndex) {
        for (int i = 0; i < endIndex; i++) {
            Lock lock = this.locks[i];
            if (lock != null) {
                lock.release();
            }
        }
    }
}