package com.kakura.icetube.controller;

import com.kakura.icetube.dto.UserDto;
import com.kakura.icetube.dto.VideoDto;
import com.kakura.icetube.service.BlackListService;
import com.kakura.icetube.service.CustomUserDetailsService;
import com.kakura.icetube.service.UserService;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.cache.CacheManager;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;


    @PostMapping("/subscribe/{id}")
    public boolean subscribeToUser(@PathVariable("id") Long id) {
        userService.subscribeToUser(id);
        return true;
    }

    @PostMapping("/unsubscribe/{id}")
    public boolean unsubscribeFromUser(@PathVariable("id") Long id) {
        userService.unsubscribeFromUser(id);
        return false;
    }

    @GetMapping("/subscriptions")
    public List<UserDto> getSubscriptions() {
        return userService.getSubscriptions();
    }

    @GetMapping(value = "/likedvideos")
    public List<VideoDto> getLiked() {
        return userService.getLikedVideos();
    }

    @GetMapping("/{userId}")
    public UserDto getById(@PathVariable("userId") Long userId) {
        return userService.getById(userId);
    }

}
