package com.kakura.icetube.service;

import com.kakura.icetube.config.CacheConfig;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BlackListService {

    @CachePut(CacheConfig.BLACKLIST_CACHE_NAME)
    public String addJwtToBlackList(String jwt) {
        return jwt;
    }

    @Cacheable(value = CacheConfig.BLACKLIST_CACHE_NAME, unless = "#result == null")
    public String getJwtFromBlackList(String jwt) {
        return null;
    }

}