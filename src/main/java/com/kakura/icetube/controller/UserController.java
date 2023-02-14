package com.kakura.icetube.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.exceptions.JWTVerificationException;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.kakura.icetube.dto.AuthResponseDto;
import com.kakura.icetube.dto.LoginDto;
import com.kakura.icetube.dto.RegistrationDto;
import com.kakura.icetube.dto.UserDto;
import com.kakura.icetube.exception.BadRequestException;
import com.kakura.icetube.exception.NotFoundException;
import com.kakura.icetube.exception.UnauthorizedException;
import com.kakura.icetube.model.Role;
import com.kakura.icetube.model.User;
import com.kakura.icetube.pojo.BlackListRefreshJwtPair;
import com.kakura.icetube.service.BlackListService;
import com.kakura.icetube.service.CustomUserDetailsService;
import com.kakura.icetube.service.UserService;
import jakarta.servlet.http.Cookie;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
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
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.validation.BindingResult;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;
import org.springframework.web.util.WebUtils;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.Map;
import java.util.stream.Collectors;

@RestController
@RequestMapping("/api/auth")
@RequiredArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;

    private final CustomUserDetailsService customUserDetailsService;

    private final AuthenticationManager authenticationManager;

    private final BlackListService blackListService;

    private final CacheManager cacheManager;

    @Value("${jwt_secret}")
    private String SECRET_KEY;

    @Value("${access_token_expiration_millis}")
    private int ACCESS_TOKEN_EXPIRATION_TIME;

    @Value("${refresh_token_expiration_millis}")
    private int REFRESH_TOKEN_EXPIRATION_TIME;

    @Value("${jwtRefreshCookieName}")
    private String REFRESH_COOKIE_NAME;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@RequestBody @Valid RegistrationDto registrationDto, BindingResult bindingResult) {
        if (bindingResult.hasErrors()) {
            throw new BadRequestException(bindingResult.getFieldErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("; ")));
        }
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/auth/register").toUriString());
        return ResponseEntity.created(uri).body(userService.saveUser(registrationDto));
    }

    @PostMapping("/token")
    public ResponseEntity<?> getToken(@RequestBody @Valid LoginDto loginDto, BindingResult bindingResult, HttpServletRequest request, HttpServletResponse response) {
        log.info("/token");
        if (bindingResult.hasErrors()) {
            throw new BadRequestException(bindingResult.getFieldErrors().stream()
                    .map(DefaultMessageSourceResolvable::getDefaultMessage).collect(Collectors.joining("; ")));
        }
        String username = loginDto.getUsername();
        String password = loginDto.getPassword();
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);

        authenticationManager.authenticate(authenticationToken);

        User user = userService.getUserByUsername(username)
                .orElseThrow(() -> new NotFoundException("Cannot find user by username: " + username));

        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes());
        String accessToken = JWT.create()
                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .withIssuer(request.getRequestURL().toString())
                .withClaim("roles", user.getRoles().stream().map(Role::getName).toList())
                .withClaim("type", "accessToken")
                .sign(algorithm);

        String refreshToken = JWT.create()
                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .withIssuer(request.getRequestURL().toString())
                .withClaim("type", "refreshToken")
                .sign(algorithm);

        blackListService.addJwtToRefreshList(refreshToken);

        ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                .path("/api/auth/").maxAge(REFRESH_TOKEN_EXPIRATION_TIME / 1000)
                .httpOnly(true).build();

        AuthResponseDto authResponseDto = new AuthResponseDto(accessToken, user.getId(),
                user.getUsername(), user.getName(), user.getSurname(),
                user.getRoles().stream().map(Role::getName).toList());

        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                .body(authResponseDto);
    }

    @GetMapping("/refresh")
    public ResponseEntity<?> refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        log.info("/refresh");
//        String authorizationHeader = request.getHeader(AUTHORIZATION);
//        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
//            String oldRefreshToken = authorizationHeader.substring("Bearer ".length());
        Cookie oldRefreshCookie = WebUtils.getCookie(request, REFRESH_COOKIE_NAME);

        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes());
        JWTVerifier verifier = JWT.require(algorithm).build();
        DecodedJWT decodedJWT;

        if (oldRefreshCookie != null) {
            String oldRefreshToken = oldRefreshCookie.getValue();

            try {
                decodedJWT = verifier.verify(oldRefreshToken);
                if (!decodedJWT.getClaim("type").asString().equals("refreshToken")) {
                    throw new UnauthorizedException("Trying to use access token as refresh token");
                }


//                blackListService.evictJwtFromRefreshList(oldRefreshToken); //test

                String refreshTokenFromDb = blackListService.getJwtFromRefreshList(oldRefreshToken);
                if (refreshTokenFromDb == null) {
                    throw new UnauthorizedException("Refresh token was stolen");
                }


                //                blackListService.addJwtPairToBlackList(new BlackListRefreshJwtPair(oldRefreshToken, "fasf")); //test

                BlackListRefreshJwtPair blackListPair = blackListService.
                        getJwtPairFromBlackList(new BlackListRefreshJwtPair(oldRefreshToken, null));

                if (blackListPair != null) {
                    blackListService.evictJwtFromRefreshList(blackListPair.getRefreshToken());
                    throw new UnauthorizedException("Refresh token is blacklisted");
                }

                String username = decodedJWT.getSubject();
                User user = userService.getUserByUsername(username)
                        .orElseThrow(() -> new NotFoundException("Cannot find user by username: " + username));

                String accessToken = JWT.create()
                        .withSubject(user.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                        .withClaim("type", "accessToken")
                        .sign(algorithm);

                String refreshToken = JWT.create()
                        .withSubject(user.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("type", "refreshToken")
                        .sign(algorithm);

                blackListService.addJwtPairToBlackList(new BlackListRefreshJwtPair(oldRefreshToken, refreshToken));

                blackListService.addJwtToRefreshList(refreshToken);

                ResponseCookie refreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, refreshToken)
                        .path("/api/auth/").maxAge(REFRESH_TOKEN_EXPIRATION_TIME / 1000)
                        .httpOnly(true).build();

                AuthResponseDto authResponseDto = new AuthResponseDto(accessToken, user.getId(),
                        user.getUsername(), user.getName(), user.getSurname(),
                        user.getRoles().stream().map(Role::getName).toList());

                return ResponseEntity.ok()
                        .header(HttpHeaders.SET_COOKIE, refreshCookie.toString())
                        .body(authResponseDto);
            } catch (JWTVerificationException e) {
                throw new UnauthorizedException(e.getMessage());
            }
        } else {
            throw new UnauthorizedException("Refresh  cookie token is missing");
        }

    }

    @GetMapping("/logout")
    public ResponseEntity<?> logout(HttpServletRequest request, HttpServletResponse response) {

        Cookie refreshCookie = WebUtils.getCookie(request, REFRESH_COOKIE_NAME);
        System.out.println(refreshCookie);
        if (refreshCookie != null) {
            String refreshToken = refreshCookie.getValue();
            System.out.println(refreshCookie);
            blackListService.evictJwtFromRefreshList(refreshToken);
        }

        ResponseCookie emptyRefreshCookie = ResponseCookie.from(REFRESH_COOKIE_NAME, null)
                .path("/api/auth/").maxAge(0)
                .httpOnly(true).build();
        return ResponseEntity.ok()
                .header(HttpHeaders.SET_COOKIE, emptyRefreshCookie.toString())
                .build();
    }

    @GetMapping("/testuser")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Map<String, String> testUser() {
        log.info("/testuser");
        return Map.of("this", "is for USER");
    }

    @GetMapping("/testadmin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Map<String, String> testAdmin() {
        return Map.of("this", "is for ADMIN");
    }

}


