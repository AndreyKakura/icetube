package com.kakura.icetube.controller;

import com.kakura.icetube.service.BlackListService;
import com.kakura.icetube.service.CustomUserDetailsService;
import com.kakura.icetube.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;


    @PostMapping("/subscribe/{id}")
    public void subscribeToUser(@PathVariable("id") Long id) {
        userService.subscribeToUser(id);
    }

    @PostMapping("/unsubscribe/{id}")
    public void unsubscribeFromUser(@PathVariable("id") Long id) {
        userService.unsubscribeFromUser(id);
    }

}
