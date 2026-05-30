package com.easylive.redis;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.data.redis.core.RedisTemplate;
import org.springframework.stereotype.Component;
import org.springframework.util.CollectionUtils;

import javax.annotation.Resource;
import java.util.*;
import java.util.concurrent.TimeUnit;

@Component("redisUtils")
@SuppressWarnings("unchecked") // 压制未经检查的转换警告
public class RedisUtils {

    @Resource
    private RedisTemplate<String, Object> redisTemplate;

    private static final Logger logger = LoggerFactory.getLogger(RedisUtils.class);

    /**
     * 删除缓存
     *
     * @param key 可以传一个值 或多个
     */
    public void delete(String... key) {
        if (key != null && key.length > 0) {
            if (key.length == 1) {
                redisTemplate.delete(key[0]);
            } else {
                redisTemplate.delete((Collection<String>) CollectionUtils.arrayToList(key));
            }
        }
    }

    /**
     * 普通缓存获取
     */
    public Object get(String key) {
        return key == null ? null : redisTemplate.opsForValue().get(key);
    }

    /**
     * 普通缓存放入
     *
     * @param key   键
     * @param value 值
     * @return true成功 false失败
     */
    public boolean set(String key, Object value) {
        try {
            redisTemplate.opsForValue().set(key, value);
            return true;
        } catch (Exception e) {
            logger.error("设置redisKey:{},value:{}失败", key, value);
            return false;
        }
    }

    public boolean keyExists(String key) {
        return redisTemplate.hasKey(key);
    }

    /**
     * 普通缓存放入并设置时间
     *
     * @param key   键
     * @param value 值
     * @param time  时间(毫秒)
     * @return true成功 false 失败
     */
    public boolean setex(String key, Object value, long time) {
        try {
            if (time > 0) {
                redisTemplate.opsForValue().set(key, value, time, TimeUnit.MILLISECONDS);
            } else {
                set(key, value);
            }
            return true;
        } catch (Exception e) {
            logger.error("设置redisKey:{},value:{}失败", key, value);
            return false;
        }
    }

    public boolean expire(String key, long time) {
        try {
            if (time > 0) {
                redisTemplate.expire(key, time, TimeUnit.MILLISECONDS);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * 获取List列表
     */
    // 修改 RedisUtils 里的方法定义
    public <T> List<T> getQueueList(String key) {
        // 内部逻辑...
        return (List<T>) redisTemplate.opsForList().range(key, 0, -1);
    }

    /**
     * List左侧放入
     */
    public boolean lpush(String key, Object value, Long time) {
        try {
            redisTemplate.opsForList().leftPush(key, value);
            if (time != null && time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    public long remove(String key, Object value) {
        try {
            Long remove = redisTemplate.opsForList().remove(key, 1, value);
            return remove == null ? 0 : remove;
        } catch (Exception e) {
            e.printStackTrace();
            return 0;
        }
    }

    public boolean lpushAll(String key, List<Object> values, long time) {
        try {
            redisTemplate.opsForList().leftPushAll(key, values);
            if (time > 0) {
                expire(key, time);
            }
            return true;
        } catch (Exception e) {
            e.printStackTrace();
            return false;
        }
    }

    /**
     * List右侧弹出
     */
    public Object rpop(String key) {
        try {
            return redisTemplate.opsForList().rightPop(key);
        } catch (Exception e) {
            e.printStackTrace();
            return null;
        }
    }

    public Long increment(String key) {
        return redisTemplate.opsForValue().increment(key, 1);
    }

    public Long incrementex(String key, long milliseconds) {
        Long count = redisTemplate.opsForValue().increment(key, 1);
        if (count != null && count == 1) {
            expire(key, milliseconds);
        }
        return count;
    }

    public Long decrement(String key) {
        Long count = redisTemplate.opsForValue().increment(key, -1);
        if (count != null && count <= 0) {
            redisTemplate.delete(key);
        }
        logger.info("key:{},减少数量{}", key, count);
        return count;
    }

    public Set<String> getByKeyPrefix(String keyPrifix) {
        return redisTemplate.keys(keyPrifix + "*");
    }

    /**
     * 批量获取
     */
    public Map<String, Object> getBatch(String keyPrifix) {
        Set<String> keySet = redisTemplate.keys(keyPrifix + "*");
        if (CollectionUtils.isEmpty(keySet)) return new HashMap<>();

        List<String> keyList = new ArrayList<>(keySet);
        List<Object> keyValueList = redisTemplate.opsForValue().multiGet(keyList);

        Map<String, Object> resultMap = new HashMap<>();
        for (int i = 0; i < keyList.size(); i++) {
            resultMap.put(keyList.get(i), keyValueList.get(i));
        }
        return resultMap;
    }

    /**
     * ZSet 增加分数
     */
    public void zaddCount(String key, Object v) {
        redisTemplate.opsForZSet().incrementScore(key, v, 1);
    }

    /**
     * 获取 ZSet 排行榜
     */
    public List<Object> getZSetList(String key, Integer count) {
        Set<Object> topElements = redisTemplate.opsForZSet().reverseRange(key, 0, count);
        return topElements == null ? new ArrayList<>() : new ArrayList<>(topElements);
    }
}