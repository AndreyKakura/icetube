package com.kakura.icetube.service;

import com.kakura.icetube.config.CacheConfiguration;
import com.kakura.icetube.pojo.BlackListRefreshJwtPair;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.junit.jupiter.MockitoExtension;
import org.springframework.boot.autoconfigure.cache.CacheAutoConfiguration;
import org.springframework.boot.autoconfigure.data.redis.RedisAutoConfiguration;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.cache.annotation.EnableCaching;
import org.springframework.context.annotation.Import;
import org.springframework.test.context.ContextConfiguration;
import org.springframework.test.context.junit.jupiter.SpringExtension;

@Import({ CacheConfiguration.class, BlackListService.class})
@ExtendWith(SpringExtension.class)
@ExtendWith(MockitoExtension.class)
@EnableCaching
//@ImportAutoConfiguration(classes = {
//        CacheAutoConfiguration.class,
//        RedisAutoConfiguration.class
//})
@ContextConfiguration(classes = {
        CacheAutoConfiguration.class,
        RedisAutoConfiguration.class
})
@SpringBootTest
class BlackListServiceTest {

//    @Autowired
//    private BlackListService blackListService;

    @InjectMocks
    private BlackListService blackListService;

    @Test
    void testCachingWhenGetJwtPairFromBlackListThenJwtPairReturnFromCache() {
        BlackListRefreshJwtPair oldJwtPair = new BlackListRefreshJwtPair("oldToken", "newToken");
        blackListService.addJwtPairToBlackList(oldJwtPair);

        BlackListRefreshJwtPair uncachedJwtPair = new BlackListRefreshJwtPair("oldToken", null);

        BlackListRefreshJwtPair jwtPairFromCache = blackListService.getJwtPairFromBlackList(uncachedJwtPair);

        assert (jwtPairFromCache != null);

    }

    @Test
    void addJwtToBlackList() {
    }

    @Test
    void getJwtFromBlackList() {
    }

    @Test
    void addJwtToRefreshList() {
    }

    @Test
    void getJwtFromRefreshList() {
    }

    @Test
    void evictJwtFromRefreshList() {
    }
}