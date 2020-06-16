package com.tmx.demo.service;

import com.alibaba.fastjson.JSON;
import com.tmx.demo.config.RedisConfig;
import com.tmx.demo.util.LuaScriptReader;
import org.springframework.beans.factory.InitializingBean;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;
import redis.clients.jedis.Jedis;
import redis.clients.jedis.JedisPool;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

@Service
public class RedisService implements InitializingBean {
    
    @Autowired
    JedisPool jedisPool;

    @Autowired
    RedisConfig redisConfig;

    private int uuid;

    private String lock_sha1;

    private String unlock_sha1;

    @Override
    public void afterPropertiesSet() throws Exception {
        StringBuilder lock_builder = LuaScriptReader.read("lock.lua");
        StringBuilder unlock_builder = LuaScriptReader.read("unlock.lua");

        Jedis jedis = jedisPool.getResource();
        lock_sha1 = jedis.scriptLoad(lock_builder.toString());
        unlock_sha1 = jedis.scriptLoad(unlock_builder.toString());

        uuid = redisConfig.getUuid();
    }

    // =============================common============================

    public boolean expire(String key, int seconds) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            if(seconds > 0) {
                jedis.expire(key, seconds);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            jedis.close();
        }
    }

    public Long getExpire(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            Long time = jedis.ttl(key);

            return time;
        } catch (Exception e) {
            e.printStackTrace();
            return 0L;
        } finally {
            jedis.close();
        }
    }

    public boolean exists(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            boolean res = jedis.exists(key);

            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            jedis.close();
        }
    }

    public void delAllKey() {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            Set<String> keySet = jedis.keys("*");
            if (keySet.size() > 0) {
                jedis.del(keySet.toArray(new String[0]));
            }

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }



    // ============================String=============================

    public <T> boolean set(String key, T value, int seconds) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            String jsonValue = toJSON(value);
            jedis.setex(key, seconds, jsonValue);

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            jedis.close();
        }
    }

    public <T> T get(String key, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            String jsonValue = jedis.get(key);
            T res = fromJSON(jsonValue, clazz);

            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            jedis.close();
        }
    }

    public void incr(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.incr(key);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }

    public void decr(String key) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            jedis.decr(key);

        } catch (Exception e) {
            e.printStackTrace();
        } finally {
            jedis.close();
        }
    }



    // ================================Hash=================================

    public <T> boolean hset(String key, String item, T value, int seconds) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();
            String jsonValue = toJSON(value);
            jedis.hset(key, item, jsonValue);
            if (seconds > 0) {
                expire(key, seconds);
            }

            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        } finally {
            jedis.close();
        }
    }

    public <T> T hget(String key, String item, Class<T> clazz) {
        Jedis jedis = null;
        try {
            jedis = jedisPool.getResource();

            String jsonValue = jedis.hget(key, item);
            T res = fromJSON(jsonValue, clazz);

            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            jedis.close();
        }
    }



    // ================================Lua=================================

    public Long lua_lock(String lockName) {
        Jedis jedis = null;
        if (lock_sha1 == null || lock_sha1.length() == 0) {
            return null;
        }
        try {
            jedis = jedisPool.getResource();

            List<String> keys = new ArrayList<>();
            keys.add(lockName);

            List<String> args = new ArrayList<>();
            args.add("600000");
            args.add(String.valueOf(uuid));

            Long res = (Long) jedis.evalsha(lock_sha1, keys, args);

            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            jedis.close();
        }
    }

    public Long lua_unlock(String lockName) {
        Jedis jedis = null;
        if (unlock_sha1 == null || unlock_sha1.length() == 0) {
            return null;
        }
        try {
            jedis = jedisPool.getResource();

            List<String> keys = new ArrayList<>();
            keys.add(lockName);

            List<String> args = new ArrayList<>();
            args.add(String.valueOf(uuid));

            Long res = (Long) jedis.evalsha(unlock_sha1, keys, args);

            return res;
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        } finally {
            jedis.close();
        }
    }


    private <T> String toJSON(T value) {
        if(value == null) {
            return null;
        }
        Class<?> clazz = value.getClass();
        if(clazz == int.class || clazz == Integer.class) {
            return ""+value;
        }else if(clazz == String.class) {
            return (String)value;
        }else if(clazz == long.class || clazz == Long.class) {
            return ""+value;
        }else {
            return JSON.toJSONString(value);
        }
    }

    @SuppressWarnings("unchecked")
    private <T> T fromJSON(String str, Class<T> clazz) {
        if(str == null || str.length() <= 0 || clazz == null) {
            return null;
        }
        if(clazz == int.class || clazz == Integer.class) {
            return (T)Integer.valueOf(str);
        }else if(clazz == String.class) {
            return (T)str;
        }else if(clazz == long.class || clazz == Long.class) {
            return  (T)Long.valueOf(str);
        }else {
            return JSON.toJavaObject(JSON.parseObject(str), clazz);
        }
    }
}
