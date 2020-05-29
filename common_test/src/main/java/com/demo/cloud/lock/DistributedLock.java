package com.demo.cloud.lock;

import lombok.SneakyThrows;
import lombok.extern.slf4j.Slf4j;
import org.apache.zookeeper.CreateMode;
import org.apache.zookeeper.Watcher;
import org.apache.zookeeper.ZooDefs;
import org.apache.zookeeper.ZooKeeper;
import org.apache.zookeeper.data.Stat;

import java.util.Collections;
import java.util.List;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.locks.LockSupport;

/**
 * @author lqq
 * @date 2020/4/4
 */
@Slf4j
public class DistributedLock {

    public static final String CONNECT_STRING = "129.204.21.157:2181";
    public static final int TIMEOUT = 10 * 10000;

    //作为锁竞争节点的父节点
    public static final String LOCK_PATH = "/lock";


    public static final String SYNC_PATH = "/sync";
    CountDownLatch cdl = new CountDownLatch(1);
    ZooKeeper zooKeeper;

    @SneakyThrows
    public DistributedLock() {

        log.info("try connecting");
        zooKeeper = new ZooKeeper(CONNECT_STRING, TIMEOUT, (e) -> {
            if (e.getType() == Watcher.Event.EventType.None && e.getState() == Watcher.Event.KeeperState.SyncConnected) {
                log.info("connect success");
                cdl.countDown();
            }
        });
        cdl.await();

        //如果多个JVM的应用同时进行多个DistributedLock对象的创建，以下【判断是否存在-》创建】的过程，存在线程安全问题。通过提前创建LOCK_PATH节点可以避免该问题
        Stat stat = zooKeeper.exists(LOCK_PATH, false);
        log.info("lock_path exist result={}", (stat != null));
        if (stat == null) {
            zooKeeper.create(LOCK_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.PERSISTENT);
            log.info("create lock_path node success");

        }


    }

    @SneakyThrows
    public void closeLock() {
        if (zooKeeper != null) {
            // zooKeeper.delete(LOCK_PATH, -1);
            //log.info("delete lock_path node success");
            zooKeeper.close();
            log.info("disconnect success");
        }
    }

    @SneakyThrows
    public String lock() {
        Thread thread = Thread.currentThread();
        String nodeAbsolutePath = zooKeeper.create(LOCK_PATH + SYNC_PATH, new byte[0], ZooDefs.Ids.OPEN_ACL_UNSAFE, CreateMode.EPHEMERAL_SEQUENTIAL);
        log.info("create sync node success,nodeAbsolutePath={}", nodeAbsolutePath);

        String nodePath = nodeAbsolutePath.substring(LOCK_PATH.length() + 1);

        List<String> syncList = zooKeeper.getChildren(LOCK_PATH, false);
        Collections.sort(syncList);
        log.info("syncList:{}", syncList);
        int order = syncList.indexOf(nodePath);
        //此处会对syncList的所有preNode进行多处exists判断（而每一个preNode最多进行一次判断）。如果对getChildren方法添加watcher则会造成多次getChildren判断。
        for (; ; ) {
            log.info("current order:{}", order);
            if (order == 0) {
                return nodeAbsolutePath;
            }

            String preNodePath = syncList.get(--order);
            String preNodeAbPath = String.format("%s%s%s", LOCK_PATH, "/", preNodePath);
            log.info("wait for preNodePath={},preNodeAbPath={}", preNodePath, preNodeAbPath);
            Stat stat = zooKeeper.exists(preNodeAbPath, (e) -> {
                if (e.getType() == Watcher.Event.EventType.NodeDeleted) {
                    log.info("preOrder Sync node delete");
                    LockSupport.unpark(thread);
                }
            });
            if (stat != null) {
                LockSupport.park();
            }


        }


    }


    @SneakyThrows
    public void unlock(String path) {
        log.info("sync node delete,path={} ", path);
        zooKeeper.delete(path, -1);

    }
}
