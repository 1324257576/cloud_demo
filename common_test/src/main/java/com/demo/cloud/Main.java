package com.demo.cloud;

import com.demo.cloud.lock.DistributedLock;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooKeeper;
import org.junit.Test;

import java.util.concurrent.CountDownLatch;
import java.util.concurrent.TimeUnit;

/**
 * @author lqq
 * @date 2020/4/4
 */
@Slf4j
public class Main {


    @Test
    public void test() throws Exception {
        CountDownLatch cdl = new CountDownLatch(1);

        log.info("try connecting zk");
        String connectionString = String.format("%s:%s", "129.204.21.157", "2181");
        int timeout = 5 * 1000;
        ZooKeeper zooKeeper = new ZooKeeper(connectionString, timeout,
                (event) -> {
                    log.info("event={}", event);
                    if (event.getState() == Watcher.Event.KeeperState.SyncConnected) {
                        log.info("connect success");
                        cdl.countDown();
                    }
                });
        cdl.await();

        log.info("sessionId={}", zooKeeper.getSessionId());
        zooKeeper.close();
    }

    @Test
    public void test1() throws Exception {
        DistributedLock lock = new DistributedLock();
        CountDownLatch cdl = new CountDownLatch(5);
        for (int i = 0; i < 5; i++) {

            new Thread(() -> {
                String path = lock.lock();
                log.info("lock owner");
                try {
                    TimeUnit.SECONDS.sleep(10);
                } catch (InterruptedException ignore) {
                }
                lock.unlock(path);
                cdl.countDown();
            }, "t" + i).start();
        }

        cdl.await();
        lock.closeLock();
        log.info("lock test finish");

    }


    @Test
    public void test2() throws Exception {
        CountDownLatch cdl = new CountDownLatch(3);

        new Thread(() -> {
            try {
                TimeUnit.SECONDS.sleep(10);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cdl.countDown();
        }, "t1").start();
        new Thread(() -> {
            try {
                TimeUnit.MINUTES.sleep(5);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cdl.countDown();
        }, "t2").start();
        new Thread(() -> {
            try {
                TimeUnit.MINUTES.sleep(1);
            } catch (InterruptedException e) {
                e.printStackTrace();
            }
            cdl.countDown();
        }, "t3").start();
        cdl.await();
    }
}
