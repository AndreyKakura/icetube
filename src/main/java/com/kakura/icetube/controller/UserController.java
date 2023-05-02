package com.kakura.icetube.controller;

import com.kakura.icetube.dto.ChangePasswordDto;
import com.kakura.icetube.dto.UserDto;
import com.kakura.icetube.dto.VideoDto;
import com.kakura.icetube.exception.BadRequestException;
import com.kakura.icetube.service.BlackListService;
import com.kakura.icetube.service.CustomUserDetailsService;
import com.kakura.icetube.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.cache.CacheManager;
import org.springframework.context.support.DefaultMessageSourceResolvable;
import org.springframework.http.HttpHeaders;
import org.springframework.http.ResponseCookie;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.util.WebUtils;

import java.util.List;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/user")
@RequiredArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;

    private final BlackListService blackListService;

    @Value("${jwtRefreshCookieName}")
    private String REFRESH_COOKIE_NAME;

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

    @GetMapping("/current")
    public UserDto getCurrentUser() {
        return userService.getCurrentUserDto();
    }

    @PostMapping("/changepassword")
    @PreAuthorize("hasRole('ROLE_USER')")
    public ResponseEntity<?> changePassword(@RequestBody @Valid ChangePasswordDto changePasswordDto,
                                            BindingResult bindingResult, HttpServletRequest request) {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException(bindingResult.getFieldErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("; ")));
        }

        userService.changePassword(changePasswordDto);

        Cookie refreshCookie = WebUtils.getCookie(request, REFRESH_COOKIE_NAME);
        if (refreshCookie != null) {
            String refreshToken = refreshCookie.getValue();
            blackListService.evictJwtFromRefreshList(refreshToken);
        }

        ResponseCookie emptyRefreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, null)
                .path("/api/auth/").maxAge(0)
                .httpOnly(true).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, emptyRefreshCookie.toString())
                .build();
    }
}
