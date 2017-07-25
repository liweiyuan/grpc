package io.grpc.examples.dao;


import redis.clients.jedis.*;
import redis.clients.jedis.exceptions.JedisDataException;
import redis.clients.util.SafeEncoder;

import java.util.*;

public class RedisClient {
    public  static BinaryClient.LIST_POSITION LIST_POSITION;

    public static final String SCRIPT =
            "local resultKeys={};"
                    + "for k,v in ipairs(KEYS) do "
                    + " local tmp = redis.call('hget', v, 'age');"
                    + " if tmp > ARGV[1] then "
                    + "     table.insert(resultKeys,v);"
                    + " end;"
                    + "end;"
                    + "return resultKeys;";
    String script1 = "return {KEYS[1],KEYS[2],ARGV[1],ARGV[2],ARGV[3]}";
    private Jedis jedis;//非切片额客户端连接
    private JedisPool jedisPool;//非切片连接池
    private ShardedJedis shardedJedis;//切片额客户端连接
    private ShardedJedisPool shardedJedisPool;//切片连接池
    public RedisClient()
    {
        initialPool();
        initialShardedPool();
        shardedJedis = shardedJedisPool.getResource();
        jedis = jedisPool.getResource();
    }

    /**
     * 初始化非切片池
     */
    private void initialPool()
    {
        // 池基本配置
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(200);
        config.setMaxIdle(50);
        config.setMaxWaitMillis(10000l);
        config.setTestOnBorrow(false);

        jedisPool = new JedisPool(config,"192.168.2.129",6379);
    }

    /**
     * 初始化切片池
     */
    private void initialShardedPool()
    {
        // 池基本配置
        JedisPoolConfig config = new JedisPoolConfig();
        config.setMaxTotal(200);
        config.setMaxIdle(50);
        config.setMaxWaitMillis(10000l);
        config.setTestOnBorrow(false);
        // slave链接
        List<JedisShardInfo> shards = new ArrayList<JedisShardInfo>();
        shards.add(new JedisShardInfo("192.168.2.129", 6379, "master"));

        // 构造池
        shardedJedisPool = new ShardedJedisPool(config, shards);
    }


    public void show() {
        KeyOperate();
        StringOperate();
        ListOperate();
        SetOperate();
        SortedSetOperate();
        HashOperate();
        ScriptOperate();
        jedisPool.returnResource(jedis);
        shardedJedisPool.returnResource(shardedJedis);
    }
    public String SerAndCon(){

        jedis.set("key001","value001");
        jedis.set("key002","value002");
        jedis.set("key003","value003");

        String funcName="";
        long startTimeDBsize=System.currentTimeMillis();
        jedis.dbSize();
        long endTimeDBsize=System.currentTimeMillis();
        long durationDBsize=endTimeDBsize-startTimeDBsize;
        funcName=funcName+"Server DBsize,DBsize执行时间："+durationDBsize+"</br>";

        long startTimeDB=System.currentTimeMillis();
        jedis.flushDB();
        long endTimeDB=System.currentTimeMillis();
        long durationDB=endTimeDB-startTimeDB;
        funcName=funcName+"Server FlushDB,FlushDB执行时间："+durationDB+"</br>";

        long startTimeCp=System.currentTimeMillis();
        jedis.ping();
        long endTimeCp=System.currentTimeMillis();
        long durationCp=endTimeCp-startTimeCp;
        funcName=funcName+"Connection ping,ping执行时间："+durationCp+"</br>";

        jedis.set("key004","value004");
        jedis.set("key005","value005");

        long startTimeFlushAll=System.currentTimeMillis();
        jedis.flushAll();
        long endTimeFlushAll=System.currentTimeMillis();
        long durationFlushAll=endTimeFlushAll-startTimeFlushAll;
        funcName=funcName+"Server FlushAll,FlushAll执行时间："+durationFlushAll+"</br>";

        long startTimeS=System.currentTimeMillis();
        jedis.select(0);
        long endTimeS=System.currentTimeMillis();
        long durationS=endTimeS-startTimeS;
        funcName=funcName+"Connection Select,Select执行时间："+durationS+"</br>";


        long startTimeQuit=System.currentTimeMillis();
        jedis.quit();
        long endTimeQuit=System.currentTimeMillis();
        long durationQuit=endTimeQuit-startTimeQuit;
        funcName=funcName+"Connection Quit,Quit执行时间："+durationQuit+"</br>";

        return  funcName;

    }
    public String KeyOperate() {

        String funcName="";
        //System.out.println("======================key==========================");
        // 清空数据
        jedis.flushDB();
        // 判断key否存在
        long startTimeExists=System.currentTimeMillis();
        shardedJedis.exists("key999");
        long endTimeExists=System.currentTimeMillis();
        long durationExists=endTimeExists-startTimeExists;
        funcName=funcName+"exists,exists的执行时间："+durationExists+"</br>";
        shardedJedis.set("key001", "value001");
        //System.out.println("判断key001是否存在："+shardedJedis.exists("key001"));
        // 输出系统中所有的key
        //System.out.println("新增key002,value002键值对："+shardedJedis.set("key002", "value002"));
        //System.out.println("系统中所有键如下：");
        long startTimeKeys=System.currentTimeMillis();
        Set<String> keys = jedis.keys("*");
        long endTimeKeys=System.currentTimeMillis();
        long durationKeys=endTimeKeys-startTimeKeys;
        funcName=funcName+"keys,keys的执行时间："+durationKeys+"</br>";
        Iterator<String> it=keys.iterator() ;
        while(it.hasNext()){
            String key = it.next();
            //System.out.println(key);
        }
        // 删除某个key,若key不存在，则忽略该命令。
        long startTimeDel=System.currentTimeMillis();
        jedis.del("key002");
        long endTimeDel=System.currentTimeMillis();
        long durationDel=endTimeDel-startTimeDel;
        funcName=funcName+"del,del的执行时间："+durationDel+"</br>";
        //System.out.println("判断key002是否存在："+shardedJedis.exists("key002"));
        // 设置 key001的过期时间
        long startTimeExpire=System.currentTimeMillis();
        jedis.expire("key001", 5);
        long endTimeExpire=System.currentTimeMillis();
        long durationExpire=endTimeExpire-startTimeExpire;
        funcName=funcName+"expire,expire的执行时间："+durationExpire+"</br>";
        try{
            Thread.sleep(2000);
        }
        catch (InterruptedException e){
        }
        // 查看某个key的剩余生存时间,单位【秒】.永久生存或者不存在的都返回-1
        long startTimeTtl=System.currentTimeMillis();
        jedis.ttl("key001");
        long endTimeTtl=System.currentTimeMillis();
        long durationTtl=endTimeTtl-startTimeTtl;
        funcName=funcName+"ttl,ttl的执行时间："+durationTtl+"</br>";
        // 移除某个key的生存时间
        long startTimePersist=System.currentTimeMillis();
       jedis.persist("key001");
        long endTimePersist=System.currentTimeMillis();
        long durationPersist=endTimePersist-startTimePersist;
        funcName=funcName+"persist,persist的执行时间："+durationPersist+"</br>";
        //System.out.println("查看key001的剩余生存时间："+jedis.ttl("key001"));
        // 查看key所储存的值的类型
        long startTimeType=System.currentTimeMillis();
        jedis.type("key001");
        long endTimeType=System.currentTimeMillis();
        long durationType=endTimeType-startTimeType;
        funcName=funcName+"type,type的执行时间："+durationType+"</br>";
          /*
           * 一些其他方法：1、修改键名：jedis.rename("key6", "key0");
           *             2、将当前db的key移动到给定的db当中：jedis.move("foo", 1)
           */
        long startTimeRename=System.currentTimeMillis();
        jedis.rename("key001","newkey");
        long endTimeRename=System.currentTimeMillis();
        long durationRename=endTimeRename-startTimeRename;
        funcName=funcName+"rename,rename的执行时间："+durationRename+"</br>";
        long startTimePexpire=System.currentTimeMillis();
        //jedis.pexpire("newkey",1000l);
        long endTimePexpire=System.currentTimeMillis();
        long durationPexpire=endTimePexpire-startTimePexpire;
        funcName=funcName+"pexpire,pexpire的执行时间："+durationPexpire+"</br>";

        return funcName;
    }

    public String StringOperate() {
        String funcName="";
        //System.out.println("======================String_1==========================");
        // 清空数据
        jedis.flushDB();

        //System.out.println("=============增=============");
        long startTimeSet=System.currentTimeMillis();
        jedis.set("key001","value001");
        jedis.set("key002","value002");
        jedis.set("key003","value003");
        long endTimeSet=System.currentTimeMillis();
        long durationSet=endTimeSet-startTimeSet;
        funcName=funcName+"set,set三个值的时间："+durationSet+"</br>";
        //System.out.println("已新增的3个键值对如下：");
        long startTimeGet=System.currentTimeMillis();
        jedis.get("key001");
        jedis.get("key002");
        jedis.get("key003");
        long endTimeGet=System.currentTimeMillis();
        long durationGet=endTimeGet-startTimeGet;
        funcName=funcName+"get,get三个值的时间："+durationGet+"</br>";
        //System.out.println("=============删=============");
        jedis.del("key003");
        //System.out.println("获取key003键对应的值："+jedis.get("key003"));

       // System.out.println("=============改=============");
        //1、直接覆盖原来的数据
        //System.out.println("直接覆盖key001原来的数据："+jedis.set("key001","value001-update"));
        //System.out.println("获取key001对应的新值："+jedis.get("key001"));
        //2、直接覆盖原来的数据
        long startTimeAppend=System.currentTimeMillis();
       jedis.append("key002","+appendString");
        long endTimeAppend=System.currentTimeMillis();
        long durationAppend=endTimeAppend-startTimeAppend;
        funcName=funcName+"append,append三个值的时间："+durationAppend+"</br>";
        //System.out.println("获取key002对应的新值"+jedis.get("key002"));

       // System.out.println("=============增，删，查（多个）=============");
        /**
         * mset,mget同时新增，修改，查询多个键值对
         * 等价于：
         * jedis.set("name","ssss");
         * jedis.set("jarorwar","xxxx");
         */
        long startTimeMset=System.currentTimeMillis();
        jedis.mset("key201","value201",
                "key202","value202","key203","value203","key204","value204");
        long endTimeMset=System.currentTimeMillis();
        long durationMset=endTimeMset-startTimeMset;
        funcName=funcName+"mset,mset四对值的时间："+durationMset+"</br>";
        long startTimeMget=System.currentTimeMillis();
        jedis.mget("key201","key202","key203","key204");
        long endTimeMget=System.currentTimeMillis();
        long durationMget=endTimeMget-startTimeMget;
        funcName=funcName+"mget,mget四对值的时间："+durationMget+"</br>";
        jedis.del(new String[]{"key201", "key202"});
/*        System.out.println("一次性获取key201,key202,key203,key204各自对应的值："+
                jedis.mget("key201","key202","key203","key204"));
        System.out.println();*/

        //jedis具备的功能shardedJedis中也可直接使用，下面测试一些前面没用过的方法
        //System.out.println("======================String_2==========================");
        // 清空数据
        jedis.flushDB();

        //System.out.println("=============新增键值对时防止覆盖原先值=============");
        long startTimeSetnx=System.currentTimeMillis();
        shardedJedis.setnx("key301", "value301");
        long endTimeSetnx=System.currentTimeMillis();
        long durationSetnx=endTimeSetnx-startTimeSetnx;
        funcName=funcName+"setnx,setnx的执行时间："+durationSetnx+"</br>";
/*        System.out.println("原先key302不存在时，新增key302："+shardedJedis.setnx("key302", "value302"));
        System.out.println("当key302存在时，尝试新增key302："+shardedJedis.setnx("key302", "value302_new"));
        System.out.println("获取key301对应的值："+shardedJedis.get("key301"));
        System.out.println("获取key302对应的值："+shardedJedis.get("key302"));

        System.out.println("=============超过有效期键值对被删除=============");*/
        // 设置key的有效期，并存储数据
        long startTimeSetex=System.currentTimeMillis();
        shardedJedis.setex("key303", 2, "key303-2second");
        long endTimeSetex=System.currentTimeMillis();
        long durationSetex=endTimeSetex-startTimeSetex;
        funcName=funcName+"setex,setex的执行时间："+durationSetex+"</br>";
        //System.out.println("获取key303对应的值："+shardedJedis.get("key303"));
        try{
            Thread.sleep(3000);
        }
        catch (InterruptedException e){
        }
        //System.out.println("3秒之后，获取key303对应的值："+shardedJedis.get("key303"));

        //System.out.println("=============获取原值，更新为新值一步完成=============");
        long startTimeGetset=System.currentTimeMillis();
        shardedJedis.getSet("key303", "value302-after-getset");

        long endTimeGetset=System.currentTimeMillis();
        long durationGetset=endTimeGetset-startTimeGetset;
        funcName=funcName+"getset,getset的执行时间："+durationGetset+"</br>";
        //System.out.println("key302新值："+shardedJedis.get("key302"));

        //System.out.println("=============获取子串=============");
        //System.out.println("获取key302对应值中的子串："+shardedJedis.getrange("key302", 5, 7));

        return funcName;
    }

    public String ListOperate() {
        String funcName="";
        //System.out.println("======================list==========================");
        // 清空数据
        jedis.flushDB();
        //System.out.println("=============增=============");
        long startTimeLpush=System.currentTimeMillis();
        shardedJedis.lpush("stringlists", "vector");
        shardedJedis.lpush("stringlists", "ArrayList");
        shardedJedis.lpush("stringlists", "vector");
        shardedJedis.lpush("stringlists", "vector");
        shardedJedis.lpush("stringlists", "LinkedList");
        shardedJedis.lpush("stringlists", "MapList");
        shardedJedis.lpush("stringlists", "SerialList");
        shardedJedis.lpush("stringlists", "HashList");
        long endTimeLpush=System.currentTimeMillis();
        long durationLpush=endTimeLpush-startTimeLpush;
        funcName=funcName+"lpush,lpush8个值的执行时间："+durationLpush+"</br>";
        long startTimeRpush=System.currentTimeMillis();
        shardedJedis.rpush("numberlists", "3");
        shardedJedis.rpush("numberlists", "1");
        shardedJedis.rpush("numberlists", "5");
        shardedJedis.rpush("numberlists", "2");
        long endTimeRpush=System.currentTimeMillis();
        long durationRpush=endTimeRpush-startTimeRpush;
        funcName=funcName+"rpush,rpush4个值的执行时间："+durationRpush+"</br>";
        long startTimeLrange=System.currentTimeMillis();
        shardedJedis.lrange("stringlists", 0, -1);
        long endTimeLrange=System.currentTimeMillis();
        long durationLrange=endTimeLrange-startTimeLrange;
        funcName=funcName+"lrange,lrange 1个key8个value的时间："+durationLrange+"</br>";
        shardedJedis.lrange("numberlists", 0, -1);
        //System.out.println("=============删=============");
        // 删除列表指定的值 ，第二个参数为删除的个数（有重复时），后add进去的值先被删，类似于出栈
        long startTimeLremove=System.currentTimeMillis();
        shardedJedis.lrem("stringlists", 2, "vector");
        long endTimeLremove=System.currentTimeMillis();
        long durationLremove=endTimeLremove-startTimeLremove;
        funcName=funcName+"lrem,lrem一个值的时间："+durationLremove+"</br>";
        //shardedJedis.lrange("stringlists", 0, -1);
        // 删除区间以外的数据
        long startTimeLtrim=System.currentTimeMillis();
       shardedJedis.ltrim("stringlists", 0, 3);
        long endTimeLtrim=System.currentTimeMillis();
        long durationLtrim=endTimeLtrim-startTimeLtrim;
        funcName=funcName+"ltrim,ltrim的执行时间："+durationLremove+"</br>";
        //shardedJedis.lrange("stringlists", 0, -1);
        // 列表元素出栈
        long startTimeLpop=System.currentTimeMillis();
       shardedJedis.lpop("stringlists");
        long endTimeLpop=System.currentTimeMillis();
        long durationLpop=endTimeLpop-startTimeLpop;
        funcName=funcName+"lpop,lpop的执行时间："+durationLpop+"</br>";
        //shardedJedis.lrange("stringlists", 0, -1);

        //System.out.println("=============改=============");
        // 修改列表中指定下标的值
        long startTimeLset=System.currentTimeMillis();
        shardedJedis.lset("stringlists", 0, "hello list!");
        long endTimeLset=System.currentTimeMillis();
        long durationLset=endTimeLset-startTimeLset;
        funcName=funcName+"lset,lset的执行时间："+durationLset+"</br>";
        //shardedJedis.lrange("stringlists", 0, -1);
        //System.out.println("=============查=============");
        // 数组长度
        long startTimeLlen=System.currentTimeMillis();
        shardedJedis.llen("stringlists");
        long endTimeLlen=System.currentTimeMillis();
        long durationLlen=endTimeLset-startTimeLset;
        funcName=funcName+"llen,llen的执行时间："+durationLlen+"</br>";
        shardedJedis.llen("numberlists");
        // 排序
          /*
           * list中存字符串时必须指定参数为alpha，如果不使用SortingParams，而是直接使用sort("list")，
           * 会出现"ERR One or more scores can't be converted into double"
           */
        SortingParams sortingParameters = new SortingParams();
        sortingParameters.alpha();
        sortingParameters.limit(0, 3);
       shardedJedis.sort("stringlists",sortingParameters);
        shardedJedis.sort("numberlists");
        // 子串：  start为元素下标，end也为元素下标；-1代表倒数一个元素，-2代表倒数第二个元素
        //shardedJedis.lrange("stringlists", 1, -1);
        // 获取列表指定下标的值
        long startTimeLindex=System.currentTimeMillis();
        shardedJedis.lindex("stringlists", 2);
        long endTimeLindex=System.currentTimeMillis();
        long durationLindex=endTimeLset-startTimeLset;
        funcName=funcName+"lindex,lindex的执行时间："+durationLindex+"</br>";
        return  funcName;
    }

    public String SetOperate() {
        String funcName="";
        //System.out.println("======================set==========================");
        // 清空数据
        jedis.flushDB();
        long startTimeSadd=System.currentTimeMillis();
        //System.out.println("=============增=============");
        jedis.sadd("sets", "element001");
        jedis.sadd("sets", "element002");
        jedis.sadd("sets", "element003");
        jedis.sadd("sets", "element004");
        long endTimeSadd=System.currentTimeMillis();
        long durationSadd=endTimeSadd-startTimeSadd;
        funcName=funcName+"sadd,sadd4个值的执行时间："+durationSadd+"</br>";
        long startTimeSmembers=System.currentTimeMillis();
        jedis.smembers("sets");
        long endTimeSmembers=System.currentTimeMillis();
        long durationSmembers=endTimeSmembers-startTimeSmembers;
        funcName=funcName+"smembers,smembers执行时间："+durationSmembers+"</br>";
        //System.out.println("=============删=============");
        long startTimeSrem=System.currentTimeMillis();
        jedis.srem("sets", "element003");
        long endTimeSrem=System.currentTimeMillis();
        long durationSrem=endTimeSrem-startTimeSrem;
        funcName=funcName+"srem,srem执行时间："+durationSrem+"</br>";
        //System.out.println("查看sets集合中的所有元素:"+jedis.smembers("sets"));
          /*System.out.println("sets集合中任意位置的元素出栈："+jedis.spop("sets"));//注：出栈元素位置居然不定？--无实际意义
          System.out.println("查看sets集合中的所有元素:"+jedis.smembers("sets"));*/
        //System.out.println();

        //System.out.println("=============改=============");
        //System.out.println();

        //System.out.println("=============查=============");
        long startTimeSismember=System.currentTimeMillis();
        jedis.sismember("sets", "element001");
        long endTimeSismember=System.currentTimeMillis();
        long durationSismember=endTimeSismember-startTimeSismember;
        funcName=funcName+"sismember,sismember执行时间："+durationSismember+"</br>";

        long startTimeScard=System.currentTimeMillis();
        jedis.scard("sets");
        long endTimeScard=System.currentTimeMillis();
        long durationScard=endTimeScard-startTimeScard;
        funcName=funcName+"scard,执行时间："+durationScard+"</br>";
        //System.out.println("循环查询获取sets中的每个元素：");
        Set<String> set = jedis.smembers("sets");
        Iterator<String> it=set.iterator() ;
        while(it.hasNext()){
            Object obj=it.next();
            System.out.println(obj);
        }
        //System.out.println();

        //System.out.println("=============集合运算=============");
        jedis.sadd("sets1", "element001");
        jedis.sadd("sets1", "element002");
        jedis.sadd("sets1", "element003");
        jedis.sadd("sets2", "element002");
        jedis.sadd("sets2", "element003");
        jedis.sadd("sets2", "element004");
        jedis.smembers("sets1");
        jedis.smembers("sets2");
        long startTimeSinter=System.currentTimeMillis();
        jedis.sinter("sets1", "sets2");
        long endTimeSinter=System.currentTimeMillis();
        long durationSinter=endTimeSinter-startTimeSinter;
        funcName=funcName+"sinter,执行时间："+durationSinter+"</br>";
        long startTimeSunion=System.currentTimeMillis();
        jedis.sunion("sets1", "sets2");
        long endTimeSunion=System.currentTimeMillis();
        long durationSunion=endTimeSunion-startTimeSunion;
        funcName=funcName+"sinter,执行时间："+durationSunion+"</br>";
        long startTimeSdiff=System.currentTimeMillis();
        jedis.sdiff("sets1", "sets2");//差集：set1中有，set2中没有的元素
        long endTimeSdiff=System.currentTimeMillis();
        long durationSdiff=endTimeSdiff-startTimeSdiff;
        funcName=funcName+"sdiff,执行时间："+durationSdiff+"</br>";
        return funcName;
    }

    public String SortedSetOperate() {
        String funcName="";
        //System.out.println("======================zset==========================");
        // 清空数据
        jedis.flushDB();
        long startTimeZadd=System.currentTimeMillis();
        //System.out.println("=============增=============");
       shardedJedis.zadd("zset", 7.0, "element001");
       shardedJedis.zadd("zset", 8.0, "element002");
       shardedJedis.zadd("zset", 2.0, "element003");
       shardedJedis.zadd("zset", 3.0, "element004");
        long endTimeZadd=System.currentTimeMillis();
        long durationZadd=endTimeZadd-startTimeZadd;
        funcName=funcName+"zadd,zadd插入四条值的执行时间："+durationZadd+"</br>";
        long startTimeZrange=System.currentTimeMillis();
       shardedJedis.zrange("zset", 0, -1);//按照权重值排序
        long endTimeZrange=System.currentTimeMillis();
        long durationZrange=endTimeZrange-startTimeZrange;
        funcName=funcName+"zrange,zrange执行时间："+durationZrange+"</br>";


       // System.out.println("=============删=============");
        long startTimeZrem=System.currentTimeMillis();
        shardedJedis.zrem("zset", "element002");
        long endTimeZrem=System.currentTimeMillis();
        long durationZrem=endTimeZrem-startTimeZrem;
        funcName=funcName+"zrem,zrem执行时间："+durationZrem+"</br>";
       // System.out.println("zset集合中的所有元素："+shardedJedis.zrange("zset", 0, -1));
        //System.out.println();

        //System.out.println("=============改=============");
        //System.out.println();

        System.out.println("=============查=============");
        long startTimeZcard=System.currentTimeMillis();
        shardedJedis.zcard("zset");
        long endTimeZcard=System.currentTimeMillis();
        long durationZcard=endTimeZcard-startTimeZcard;
        funcName=funcName+"zcard,zcard执行时间："+durationZcard+"</br>";

        long startTimeZcount=System.currentTimeMillis();
       shardedJedis.zcount("zset", 1.0, 5.0);
        long endTimeZcount=System.currentTimeMillis();
        long durationZcount=endTimeZcount-startTimeZcount;
        funcName=funcName+"zcount,zcount执行时间："+durationZcount+"</br>";

        long startTimeZscore=System.currentTimeMillis();
       shardedJedis.zscore("zset", "element004");
        long endTimeZscore=System.currentTimeMillis();
        long durationZscore=endTimeZscore-startTimeZscore;
        funcName=funcName+"zscore,zscore执行时间："+durationZscore+"</br>";

        long startTimeZrevrange=System.currentTimeMillis();
        shardedJedis.zrevrange("zset",1,2);
        long endTimeZrevrange=System.currentTimeMillis();
        long durationZrevrange=endTimeZrevrange-startTimeZrevrange;
        funcName=funcName+"zrevrange,zrevrange执行时间："+durationZrevrange+"</br>";

        long startTimeZrank=System.currentTimeMillis();
        shardedJedis.zrank("zset", "element004");
        long endTimeZrank=System.currentTimeMillis();
        long durationZrank=endTimeZrank-startTimeZrank;
        funcName=funcName+"zrank,zrank执行时间："+durationZrank+"</br>";

        return funcName;
    }

    public String ScriptOperate(){

        //System.out.println("======================script==========================");
        //清空数据
        jedis.flushDB();

        String funcName="";
        jedis.set("foo", "bar");

        long startTimeEval=System.currentTimeMillis();
        jedis.eval("return redis.call('get','foo')");
        long endTimeEval=System.currentTimeMillis();
        long durationEval=endTimeEval-startTimeEval;
        funcName=funcName+"eval,eval的执行时间："+durationEval+"</br>";

        long startTimeScriptFlush=System.currentTimeMillis();
        jedis.scriptFlush();
        long endTimeScriptFlush=System.currentTimeMillis();
        long durationScriptFlush=endTimeScriptFlush-startTimeScriptFlush;
        funcName=funcName+"scriptFlush,scriptFlush的执行时间："+durationScriptFlush+"</br>";

        //jedis.scriptLoad("return redis.call('get','foo')");
       /* long startTimeScriptLoad=System.currentTimeMillis();
        jedis.scriptLoad(SCRIPT);
        long endTimeScriptLoad=System.currentTimeMillis();
        long durationScriptLoad=endTimeScriptLoad-startTimeScriptLoad;
        funcName=funcName+"scriptLoad,scriptLoad的执行时间："+durationScriptLoad+"</br>";*/


        long startTimeScriptLoad=System.currentTimeMillis();
        jedis.scriptLoad(SafeEncoder.encode("return redis.call('get','foo')"));
        long endTimeScriptLoad=System.currentTimeMillis();
        long durationScriptLoad=endTimeScriptLoad-startTimeScriptLoad;
        funcName=funcName+"scriptLoad,scriptLoad的执行时间："+durationScriptLoad+"</br>";

        long startTimeScriptExists=System.currentTimeMillis();
        List<Long> exists = jedis
                .scriptExists(SafeEncoder.encode("6b1bf486c81ceb7edf3c093f4c48582e38c0e791"));
        long endTimeScriptExists=System.currentTimeMillis();
        long durationScriptExists=endTimeScriptExists-startTimeScriptExists;
        funcName=funcName+"scriptExists,scriptExists的执行时间："+durationScriptExists+"</br>";

        long startTimeScriptKill=System.currentTimeMillis();
        try {
            jedis.scriptKill();
        } catch (JedisDataException e) {
            e.getMessage().contains("No scripts in execution right now.");
        }
        long endTimeScriptKill=System.currentTimeMillis();
        long durationScriptKill=endTimeScriptKill-startTimeScriptKill;
        funcName=funcName+"scriptKill,scriptKill的执行时间："+durationScriptKill+"</br>";

       /* long startTimeScriptExists=System.currentTimeMillis();
        List<Boolean> exists = jedis.scriptExists("ffffffffffffffffffffffffffffffffffffffff",
                "6b1bf486c81ceb7edf3c093f4c48582e38c0e791");
        long endTimeScriptExists=System.currentTimeMillis();
        long durationScriptExists=endTimeScriptExists-startTimeScriptExists;
        funcName=funcName+"scriptExists,scriptExists的执行时间："+durationScriptExists+"</br>";*/

        //System.out.println(exists.get(0));
        //System.out.println(exists.get(1));



        List<String> keys = new ArrayList<String>();
        keys.add("key1");
        keys.add("key2");

        List<String> args = new ArrayList<String>();
        args.add("first");
        args.add("second");
        args.add("third");
        long startTimeEvalsha=System.currentTimeMillis();
        List<String> response = (List<String>) jedis.eval(script1, keys, args);
        long endTimeEvalsha=System.currentTimeMillis();
        long durationEvalsha=endTimeEvalsha-startTimeEvalsha;
        funcName=funcName+"evalsha,evalsha的执行时间："+durationEvalsha+"</br>";

        /*long startTimeScriptKill=System.currentTimeMillis();
        jedis.scriptKill();
        long endTimeScriptKill=System.currentTimeMillis();
        long durationScriptKill=endTimeScriptKill-startTimeScriptKill;
        funcName=funcName+"scriptKill,scriptKill的执行时间："+durationScriptKill+"</br>";*/

        return funcName;
    }

    public String HashOperate() {
        String funcName="";
        //System.out.println("======================hash==========================");
        //清空数据
        jedis.flushDB();
        long startTimeHset=System.currentTimeMillis();

        //System.out.println("=============增=============");
        shardedJedis.hset("hashs", "key001", "value001");
        shardedJedis.hset("hashs", "key002", "value002");
        long endTimeHset=System.currentTimeMillis();
        long durationHset=endTimeHset-startTimeHset;
        funcName=funcName+"hset,hset两组值的执行时间："+durationHset+"</br>";

        //System.out.println("hashs中添加key003和value003键值对："+shardedJedis.hset("hashs", "key003", "value003"));

        long startTimeHincrBy=System.currentTimeMillis();
        shardedJedis.hincrBy("hashs", "key004", 4l);
        long endTimeHincrBy=System.currentTimeMillis();
        long durationHincrBy=endTimeHincrBy-startTimeHincrBy;
        funcName=funcName+"hincrBy,hincrBy的执行时间："+durationHincrBy+"</br>";

        long startTimeHvals=System.currentTimeMillis();
        shardedJedis.hvals("hashs");
        long endTimeHvals=System.currentTimeMillis();
        long durationHvals=endTimeHvals-startTimeHvals;
        funcName=funcName+"hvals,hvals的执行时间："+durationHvals+"</br>";

        //System.out.println("=============删=============");
        long startTimeHdel=System.currentTimeMillis();
        shardedJedis.hdel("hashs", "key002");
        long endTimeHdel=System.currentTimeMillis();
        long durationHdel=endTimeHdel-startTimeHdel;
        funcName=funcName+"hdel,hdel的执行时间："+durationHdel+"</br>";
        //System.out.println("hashs中的所有值："+shardedJedis.hvals("hashs"));

       //System.out.println("=============改=============");
        shardedJedis.hincrBy("hashs", "key004", 100l);
       // System.out.println("=============查=============");
        long startTimeHexists=System.currentTimeMillis();
        shardedJedis.hexists("hashs", "key003");
        long endTimeHexists=System.currentTimeMillis();
        long durationHexists=endTimeHexists-startTimeHexists;
        funcName=funcName+"hexists,hexists的执行时间："+durationHexists+"</br>";

        long startTimeHget=System.currentTimeMillis();
       shardedJedis.hget("hashs", "key004");
        long endTimeHget=System.currentTimeMillis();
        long durationHget=endTimeHget-startTimeHget;
        funcName=funcName+"hget,hget的执行时间："+durationHget+"</br>";
        Map map = new HashMap();
        map.put("cardid", "123456");
        map.put("username", "jzkangta");

        long startTimeHmset=System.currentTimeMillis();
        shardedJedis.hmset("hash",map);
        long endTimeHmset=System.currentTimeMillis();
        long durationHmset=endTimeHmset-startTimeHmset;
        funcName=funcName+"hmset,hmset的执行时间："+durationHmset+"</br>";

        long startTimeHmget=System.currentTimeMillis();
        shardedJedis.hmget("hash", "username");
        long endTimeHmget=System.currentTimeMillis();
        long durationHmget=endTimeHmget-startTimeHmget;
        funcName=funcName+"hmset,hmset的执行时间："+durationHmget+"</br>";

        long startTimeHkeys=System.currentTimeMillis();
        System.out.println("获取hashs中所有的key："+shardedJedis.hkeys("hashs"));
        long endTimeHkeys=System.currentTimeMillis();
        long durationHkeys=endTimeHkeys-startTimeHkeys;
        funcName=funcName+"hkeys,hkeys的执行时间："+durationHkeys+"</br>";


        long startTimeHlen=System.currentTimeMillis();
        shardedJedis.hlen("hashs");
        long endTimeHlen=System.currentTimeMillis();
        long durationHlen=endTimeHlen-startTimeHlen;
        funcName=funcName+"hlen,hlen的执行时间："+durationHlen+"</br>";

        long startTimeHgetall=System.currentTimeMillis();
        shardedJedis.hgetAll("hashs");
        long endTimeHgetall=System.currentTimeMillis();
        long durationHgetall=endTimeHgetall-startTimeHgetall;
        funcName=funcName+"hgetall,hgetall的执行时间："+durationHgetall+"</br>";

        long startTimeHsetnx=System.currentTimeMillis();
        shardedJedis.hsetnx("hash","test","ttt");
        long endTimeHsetnx=System.currentTimeMillis();
        long durationHsetnx=endTimeHgetall-startTimeHgetall;
        funcName=funcName+"hsetnx,hsetnx的执行时间："+durationHsetnx+"</br>";


        long startTimeHscan=System.currentTimeMillis();
        //ScanResult<Map.Entry<String, String>> hashs = shardedJedis.hscan("hashs", String.valueOf(0), new ScanParams());
        long endTimeHscan=System.currentTimeMillis();
        long durationHscan=endTimeHscan-startTimeHscan;
        funcName=funcName+"hscan,hscan的执行时间："+durationHscan+"</br>";

        //System.out.println("获取hashs中所有的value："+shardedJedis.hvals("hashs"));
        //System.out.println();

        return funcName;
    }
}
