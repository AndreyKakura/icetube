package com.kakura.icetube.service;

import com.kakura.icetube.config.CacheConfiguration;
import com.kakura.icetube.pojo.BlackListRefreshJwtPair;
import org.springframework.cache.annotation.CacheEvict;
import org.springframework.cache.annotation.CachePut;
import org.springframework.cache.annotation.Cacheable;
import org.springframework.stereotype.Service;

@Service
public class BlackListService {

    @CachePut(value = CacheConfiguration.BLACKLIST_CACHE_NAME, key = "#jwtPair.oldRefreshToken")
    public BlackListRefreshJwtPair addJwtPairToBlackList(BlackListRefreshJwtPair jwtPair) {
        return jwtPair;
    }

    @Cacheable(value = CacheConfiguration.BLACKLIST_CACHE_NAME, key = "#jwtPair.oldRefreshToken", unless = "#result == null")
    public BlackListRefreshJwtPair getJwtPairFromBlackList(BlackListRefreshJwtPair jwtPair) {
        return null;
    }

    @CachePut(value = CacheConfiguration.REFRESH_CACHE_NAME, key = "#jwt")
    public String addJwtToRefreshList(String jwt) {
        return jwt;
    }

    @Cacheable(value = CacheConfiguration.REFRESH_CACHE_NAME, key = "#jwt", unless = "#result == null"/*, sync = true*/)
    public String getJwtFromRefreshList(String jwt) {
        return null;
    }

    @CacheEvict(value = CacheConfiguration.REFRESH_CACHE_NAME, key = "#jwt")
    public void evictJwtFromRefreshList(String jwt) {
    }

}