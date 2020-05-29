构造方法：

```java
public ConcurrentHashMap() {
    
    }


public ConcurrentHashMap(int initialCapacity) {
    if (initialCapacity < 0)
        throw new IllegalArgumentException();
    int cap = ((initialCapacity >= (MAXIMUM_CAPACITY >>> 1)) ?
               MAXIMUM_CAPACITY :
               // tableSizeFor（3/2*initCap+1）
               //tableSizeFor通过>>> 以及|运算将cap-1的最高位1后面的位数都变成1，从而使得cap-1满足2^n-1，返回2的幂次方的数值
               tableSizeFor(initialCapacity + (initialCapacity >>> 1) + 1));
    //存储sizeCtl=cap
    this.sizeCtl = cap;
}
```

此时Node<K,V>[] table依然为null，类似于`HashMap`，使用对table懒初始化的思想。





`put(k，v)`方法

```java
 public V put(K key, V value) {
     return putVal(key, value, false);
 }
```

主要分析`putVal(key,value,onlyIfAbsent)`方法：

```java
final V putVal(K key, V value, boolean onlyIfAbsent) {
    //不允许k或v的值为null
    if (key == null || value == null) throw new NullPointerException();
    //(h ^ (h >>> 16)) & HASH_BITS 
    //通过异或运算，使得hash的高位也参与到index的计算中
    int hash = spread(key.hashCode());
    int binCount = 0;
    //局部变量tab存储table，进入死循环中，跳过循环的条件是将kv的node成功put
    for (Node<K,V>[] tab = table;;) {
        Node<K,V> f; int n, i, fh;
        if (tab == null || (n = tab.length) == 0)
            //如果tab为空或者长度为0，则进行初始化table，并更新tab的值为初始化后的table
            //进行initTable方法的分析
            tab = initTable();
        else if ((f = tabAt(tab, i = (n - 1) & hash)) == null) {
            //i = (n - 1) & hash 通过&运算，求得index。由于n-1的低位都是1，因此该值只与hash的低位有关
            //如果table[i]==null则cas保存node，put成功即break
            if (casTabAt(tab, i, null,
                         new Node<K,V>(hash, key, value, null)))
                break;                   // no lock when adding to empty bin
        }
        else if ((fh = f.hash) == MOVED)
            //如果f=table[i]的hash为MOVED，即表示table正在扩容，通过helpTransfer()方法，当前线程参与到扩容的数据复制过程，
            //进行帮助node的从oldTable到newTable的转移，并更新tab的值为扩容后的table
            //transfer
            tab = helpTransfer(tab, f);
        else {
            V oldVal = null;
            //此时f不为null，对f对象进行加锁
            synchronized (f) {
                //双重检测，可能table[i]被remove等操作
                if (tabAt(tab, i) == f) {
                    if (fh >= 0) {
                        binCount = 1;
                        for (Node<K,V> e = f;; ++binCount) {
                            K ek;
                        //遍历链表，如果查找到链表上存在相同key的node，则根据onlyIfAbsent进行是否替换value，并break链表遍历
                            if (e.hash == hash &&
                                ((ek = e.key) == key ||
                                 (ek != null && key.equals(ek)))) {
                                oldVal = e.val;
                                if (!onlyIfAbsent)
                                    e.val = value;
                                break;
                            }
                            Node<K,V> pred = e;
                            //遍历链表，如果链表上不存在相同key的node，则通过尾插法，将kv的node保存在链表尾部，并break链表遍历
                            if ((e = e.next) == null) {
                                pred.next = new Node<K,V>(hash, key,
                                                          value, null);
                                break;
                            }
                        }
                    }
                    else if (f instanceof TreeBin) {
                        //TreeBin维护着TreeNode<K,V> root根节点，保存的是整个红黑树，这里和HashMap有所区别
//因为对table[i]所在对象加锁，如果以HashMap的方式对TreeNode加锁，通过自平衡后，可能造成table[i]!=f从而破坏这里的synchronized(f)同步逻辑
                        Node<K,V> p;
                        binCount = 2;
                        //putTreeVal()如果查找到相同的key的node，则直接返回node，否则，添加node到tree中
                        if ((p = ((TreeBin<K,V>)f).putTreeVal(hash, key,
                                                              value)) != null) {
                            //如果存在相同key的node，则根据onlyIfAbsent进行替换value
                            oldVal = p.val;
                            if (!onlyIfAbsent)
                                p.val = value;
                        }
                    }
                }
            }
            if (binCount != 0) {
                if (binCount >= TREEIFY_THRESHOLD)
                    //链表长度达到树化阈值，是否进行table扩容，或链表节点的树化
                    //分析treeifyBin方法
                    treeifyBin(tab, i);
                if (oldVal != null)
                    //如果存在相同key的node，则直接返回oldValue，并break
                    return oldVal;
                break;
            }
        }
    }
    //计数+1，根据binCount的值判断是否进行table的resize操作
    //这里的计数使用的思想和LongAdder一样，使用baseCount和counterCells进行计数
    addCount(1L, binCount);
    return null;
}
```



`initTable()`方法的分析：


```java
private final Node<K,V>[] initTable() {
    Node<K,V>[] tab; int sc;
    //while当table==null
    while ((tab = table) == null || tab.length == 0) {
        //this.sizeCtl初始化值为0或者cap的值
        if ((sc = sizeCtl) < 0)
            Thread.yield(); 
        //cas更新sizeCtl的值为-1，更新成功的线程负责初始化table
        else if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
            try {
                //双重检测
                if ((tab = table) == null || tab.length == 0) {
                    //n=cap或者16(DEFAULT_CAPACITY)
                    int n = (sc > 0) ? sc : DEFAULT_CAPACITY;
                    @SuppressWarnings("unchecked")
                    Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                    table = tab = nt;
                    //sc=0.75*cap，即扩容阈值
                    sc = n - (n >>> 2);
                }
            } finally {
                //更新sizeCtl=扩容阈值
                sizeCtl = sc;
            }
            break;
        }
    }
    return tab;
}
```



`treeifyBin(Node<K,V>[] tab, int index)`方法的分析：

```java
private final void treeifyBin(Node<K,V>[] tab, int index) {
    //index为tab的下标索引
    Node<K,V> b; int n, sc;
    if (tab != null) {
        //MIN_TREEIFY_CAPACITY = 64
        if ((n = tab.length) < MIN_TREEIFY_CAPACITY)
            //进行table扩容
            //分析tryPresize方法
            tryPresize(n << 1);
        else if ((b = tabAt(tab, index)) != null && b.hash >= 0) {
            //对链表树化，对table[i]节点进行加锁
            synchronized (b) {
                //双重检测
                if (tabAt(tab, index) == b) {
                    TreeNode<K,V> hd = null, tl = null;
                    for (Node<K,V> e = b; e != null; e = e.next) {
                        TreeNode<K,V> p =
                            new TreeNode<K,V>(e.hash, e.key, e.val,
                                              null, null);
                        if ((p.prev = tl) == null)
                            hd = p;
                        else
                            tl.next = p;
                        tl = p;
                    }
                    setTabAt(tab, index, new TreeBin<K,V>(hd));
                }
            }
        }
    }
}
```



`tryPresize(int size)`方法分析：

```java
private final void tryPresize(int size) {
    int c = (size >= (MAXIMUM_CAPACITY >>> 1)) ? MAXIMUM_CAPACITY :
    tableSizeFor(size + (size >>> 1) + 1);
    int sc;
    while ((sc = sizeCtl) >= 0) {
        Node<K,V>[] tab = table; int n;
        
        //initTable()
        if (tab == null || (n = tab.length) == 0) {
            n = (sc > c) ? sc : c;
            if (U.compareAndSwapInt(this, SIZECTL, sc, -1)) {
                try {
                    if (table == tab) {
                        @SuppressWarnings("unchecked")
                        Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n];
                        table = nt;
                        sc = n - (n >>> 2);
                    }
                } finally {
                    sizeCtl = sc;
                }
            }
        }
        //如果c未超过扩容阈值或者tab.length达到最大容量，直接break
        else if (c <= sc || n >= MAXIMUM_CAPACITY)
            break;
        else if (tab == table) {
            int rs = resizeStamp(n);
            //如果sizeCtl<0则表示其他线程正在进行table扩容
            if (sc < 0) {
                Node<K,V>[] nt;
                //根据扩容线程达到MAX_RESIZERS或者是否扩容完成或transferIndex不需要对剩余tab[i]转移，
                //判断当前线程是否进行帮助node转移
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                    //帮助节点转移
                    transfer(tab, nt);
            }
            //cas更新sizeCtl为-(1+N)的情况 ：-1正在初始化 -(1+N):正在扩容 0：初始化值 >0：扩容阈值
            //具体的数值暂不进行分析
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                         (rs << RESIZE_STAMP_SHIFT) + 2))
                //当前线程开始进行table扩容
                transfer(tab, null);
        }
    }
}
```

` transfer(Node<K,V>[] tab, Node<K,V>[] nextTab)`方法分析

```java
private final void transfer(Node<K,V>[] tab, Node<K,V>[] nextTab) {
    int n = tab.length, stride;
    //每个线程进行转移的节点区间的步长
    if ((stride = (NCPU > 1) ? (n >>> 3) / NCPU : n) < MIN_TRANSFER_STRIDE)
        stride = MIN_TRANSFER_STRIDE; // subdivide range
    //如果nextTab传入null，表示需要进行table扩容
    if (nextTab == null) {            // initiating
        try {
            @SuppressWarnings("unchecked")
            //容量*2
            Node<K,V>[] nt = (Node<K,V>[])new Node<?,?>[n << 1];
            nextTab = nt;
        } catch (Throwable ex) {      // try to cope with OOME
            sizeCtl = Integer.MAX_VALUE;
            return;
        }
        //将newTable赋值到this.nextTable
        nextTable = nextTab;
        //需要转移node的数量，即oldTable.length
        transferIndex = n;
    }
    int nextn = nextTab.length;
    ForwardingNode<K,V> fwd = new ForwardingNode<K,V>(nextTab);
    //表示当前线程是否需要在oldTable未有线程进行转移的node区间上继续进行转移
    boolean advance = true;
    boolean finishing = false; // to ensure sweep before committing nextTab
    for (int i = 0, bound = 0;;) {
        Node<K,V> f; int fh;
        while (advance) {
            int nextIndex, nextBound;
            if (--i >= bound || finishing)
                advance = false;
            else if ((nextIndex = transferIndex) <= 0) {
                i = -1;
                advance = false;
            }
            else if (U.compareAndSwapInt
                     (this, TRANSFERINDEX, nextIndex,
                      nextBound = (nextIndex > stride ?
                                   nextIndex - stride : 0))) {
                //计算此次需要转移的区间的下标：transferIndex-stride <= index <=transferIndex-1
                bound = nextBound;
                i = nextIndex - 1;
                advance = false;
            }
        }
        if (i < 0 || i >= n || i + n >= nextn) {
            int sc;
            if (finishing) {
				//当所有的节点转移到newTable上
                nextTable = null;
                table = nextTab;
                //sizeCtl=0.75*newTable.length
                sizeCtl = (n << 1) - (n >>> 1);
                return;
            }
            if (U.compareAndSwapInt(this, SIZECTL, sc = sizeCtl, sc - 1)) {
                if ((sc - 2) != resizeStamp(n) << RESIZE_STAMP_SHIFT)
                    return;
                finishing = advance = true;
                i = n; // recheck before commit
            }
        }
        else if ((f = tabAt(tab, i)) == null)
            advance = casTabAt(tab, i, null, fwd);
        else if ((fh = f.hash) == MOVED)
            advance = true; // already processed
        else {
            //对f加锁，转移节点时进行加锁
            synchronized (f) {
                if (tabAt(tab, i) == f) {
                    Node<K,V> ln, hn;
                    if (fh >= 0) {
                        //和HashMap一样的思想，根据hash的决定index的低位数据的更高一位是否为0或1进行判断在newTable中的下标
                        //如果runBit=0则表示在newTable中的下标为当前在oldTable中的下标，
                        //如果runBit=1,则表示在newTable中的下标为当前在oldTable中的下标加上oldTable.length的值
                        int runBit = fh & n;
                        Node<K,V> lastRun = f;
                        for (Node<K,V> p = f.next; p != null; p = p.next) {
                            int b = p.hash & n;
                            if (b != runBit) {
                                runBit = b;
                                lastRun = p;
                            }
                        }
                        if (runBit == 0) {
                            ln = lastRun;
                            hn = null;
                        }
                        else {
                            hn = lastRun;
                            ln = null;
                        }
                        for (Node<K,V> p = f; p != lastRun; p = p.next) {
                            int ph = p.hash; K pk = p.key; V pv = p.val;
                            if ((ph & n) == 0)
                                ln = new Node<K,V>(ph, pk, pv, ln);
                            else
                                hn = new Node<K,V>(ph, pk, pv, hn);
                        }
                        setTabAt(nextTab, i, ln);
                        setTabAt(nextTab, i + n, hn);
                        //将oldTable[i]设置为ForwardingNode类型的节点，在putVal中使用到
                        setTabAt(tab, i, fwd);
                        advance = true;
                    }
                    else if (f instanceof TreeBin) {
                        //对红黑树的节点也进行转移，在newTable下标的计算方式和链表情况的计算方式一样。
                        TreeBin<K,V> t = (TreeBin<K,V>)f;
                        TreeNode<K,V> lo = null, loTail = null;
                        TreeNode<K,V> hi = null, hiTail = null;
                        int lc = 0, hc = 0;
                        for (Node<K,V> e = t.first; e != null; e = e.next) {
                            int h = e.hash;
                            TreeNode<K,V> p = new TreeNode<K,V>
                                (h, e.key, e.val, null, null);
                            if ((h & n) == 0) {
                                if ((p.prev = loTail) == null)
                                    lo = p;
                                else
                                    loTail.next = p;
                                loTail = p;
                                ++lc;
                            }
                            else {
                                if ((p.prev = hiTail) == null)
                                    hi = p;
                                else
                                    hiTail.next = p;
                                hiTail = p;
                                ++hc;
                            }
                        }
                        ln = (lc <= UNTREEIFY_THRESHOLD) ? untreeify(lo) :
                        (hc != 0) ? new TreeBin<K,V>(lo) : t;
                        hn = (hc <= UNTREEIFY_THRESHOLD) ? untreeify(hi) :
                        (lc != 0) ? new TreeBin<K,V>(hi) : t;
                        setTabAt(nextTab, i, ln);
                        setTabAt(nextTab, i + n, hn);
                        setTabAt(tab, i, fwd);
                        advance = true;
                    }
                }
            }
        }
    }
}
```



`addCount(long x, int check)`方法分析：

```java
private final void addCount(long x, int check) {
    CounterCell[] as; long b, s;
    if ((as = counterCells) != null ||
        //尝试在baseCount上进行cas更新： baseCount+x
        !U.compareAndSwapLong(this, BASECOUNT, b = baseCount, s = b + x)) {
        //如果在baseCount更新失败
        CounterCell a; long v; int m;
        //表示是否有其他线程进行对counterCell[i]竞争cas
        boolean uncontended = true;
        if (as == null || (m = as.length - 1) < 0 ||
            (a = as[ThreadLocalRandom.getProbe() & m]) == null ||
            !(uncontended =
              U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))) {
            //如果counterCells为null，或者如果counterCells[index]为null，或者对counterCells[index]进行cas累加失败
            //进行fullAddCount分析
            fullAddCount(x, uncontended);
            return;
        }
        if (check <= 1)
            return;
        //通过累加baseCount和counterCells各个元素的值，获取map中元素的个数
        s = sumCount();
    }
    if (check >= 0) {
        Node<K,V>[] tab, nt; int n, sc;
        //如果达到扩容阈值，则进行table扩容流程
        //扩容逻辑：见tryPresize
        while (s >= (long)(sc = sizeCtl) && (tab = table) != null &&
               (n = tab.length) < MAXIMUM_CAPACITY) {
            int rs = resizeStamp(n);
            if (sc < 0) {
                if ((sc >>> RESIZE_STAMP_SHIFT) != rs || sc == rs + 1 ||
                    sc == rs + MAX_RESIZERS || (nt = nextTable) == null ||
                    transferIndex <= 0)
                    break;
                if (U.compareAndSwapInt(this, SIZECTL, sc, sc + 1))
                    transfer(tab, nt);
            }
            
            else if (U.compareAndSwapInt(this, SIZECTL, sc,
                                         (rs << RESIZE_STAMP_SHIFT) + 2))
                transfer(tab, null);
            s = sumCount();
        }
    }
}
```



`fullAddCount(long x, boolean wasUncontended)`方法分析：

```java
private final void fullAddCount(long x, boolean wasUncontended) {
    //wasUncontended=false
    int h;
    if ((h = ThreadLocalRandom.getProbe()) == 0) {
        ThreadLocalRandom.localInit();      // force initialization
        h = ThreadLocalRandom.getProbe();
        wasUncontended = true;
    }
    boolean collide = false;                // True if last slot nonempty
    for (;;) {
        CounterCell[] as; CounterCell a; int n; long v;
        if ((as = counterCells) != null && (n = as.length) > 0) {
            //此时counterCells已经初始化好
            //使用(n - 1) & h计算counterCells下标
            if ((a = as[(n - 1) & h]) == null) {
                //当对应下标为null，尝试赋值new CounterCell(x)
                //cellsBusy作用：对counterCells数组做更新操作时加锁，
                //值：0表示无锁，1表示：cells数组初始化/扩容/为数组中为null的元素，赋值new CounterCell(x)
                if (cellsBusy == 0) {            // Try to attach new Cell
                    CounterCell r = new CounterCell(x); // Optimistic create
                    if (cellsBusy == 0 &&
                        U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                        boolean created = false;
                        try {               // Recheck under lock
                            CounterCell[] rs; int m, j;
                            //检查对应下标的元素是否仍然为null，如果是则赋值
                            if ((rs = counterCells) != null &&
                                (m = rs.length) > 0 &&
                                rs[j = (m - 1) & h] == null) {
                                rs[j] = r;
                                created = true;
                            }
                        } finally {
                            cellsBusy = 0;
                        }
                        if (created)
                            break;
                        continue;           // Slot is now non-empty
                    }
                }
                collide = false;
            }
            else if (!wasUncontended)       // CAS already known to fail
                wasUncontended = true;      // Continue after rehash
            else if (U.compareAndSwapLong(a, CELLVALUE, v = a.value, v + x))
                //a=counterCells[index]，cas更新a的值
                break;
            
            //n= counterCells.length
            else if (counterCells != as || n >= NCPU)
                collide = false;            // At max size or stale
            else if (!collide)
                collide = true;
            //在第二次cas设置counterCells[index]的累加失败后，进行counterCells扩容：容量*=2
            else if (cellsBusy == 0 &&
                     U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
                try {
                    if (counterCells == as) {// Expand table unless stale
                        CounterCell[] rs = new CounterCell[n << 1];
                        for (int i = 0; i < n; ++i)
                            rs[i] = as[i];
                        counterCells = rs;
                    }
                } finally {
                    cellsBusy = 0;
                }
                collide = false;
                continue;                   // Retry with expanded table
            }
            h = ThreadLocalRandom.advanceProbe(h);
        }
        //此时counterCells==null，并且cellsBusy==0，进行cas替换cellsBusy，成功则表示当前线程进行counterCells的初始化过程
        else if (cellsBusy == 0 && counterCells == as &&
                 U.compareAndSwapInt(this, CELLSBUSY, 0, 1)) {
            boolean init = false;
            try {                           // Initialize table
                if (counterCells == as) {
                    //初始化长度为2
                    CounterCell[] rs = new CounterCell[2];
                    //根据h&(counterCells-1)求得下标，并将计数值x累加到当前下标的CounterCell
                    rs[h & 1] = new CounterCell(x);
                    counterCells = rs;
                    init = true;
                }
            } finally {
                cellsBusy = 0;
            }
            if (init)
                break;
        }
        //如果counterCells==null，当前线程也无法进行counterCells的初始化，则尝试对baseCount进行cas更新，如果成功则break返回
        else if (U.compareAndSwapLong(this, BASECOUNT, v = baseCount, v + x))
            break;                          // Fall back on using base
    }
}
```

