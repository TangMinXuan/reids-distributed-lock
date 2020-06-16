package com.tmx.demo.controller;

import com.tmx.demo.service.RedisService;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
public class DemoController {

    @Autowired
    RedisService redisService;

    @GetMapping("/demo")
    public String testRedis() {
        String key = "Hello";
        redisService.set(key, "Hello World!", 60);
        String res = redisService.get(key, String.class);
        return res;
    }

    @GetMapping("/delAll")
    public String delAll() {
        redisService.delAllKey();
        return "1";
    }

    @GetMapping("/lock")
    public Long lock() {
        String lockName = "lock_name";
        return redisService.lua_lock(lockName);
    }

    @GetMapping("/unlock")
    public Long unlock() {
        String lockName = "lock_name";
        return redisService.lua_unlock(lockName);
    }
}
