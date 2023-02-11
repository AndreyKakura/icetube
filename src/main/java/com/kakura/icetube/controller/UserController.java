package com.kakura.icetube.controller;

import com.auth0.jwt.JWT;
import com.auth0.jwt.JWTVerifier;
import com.auth0.jwt.algorithms.Algorithm;
import com.auth0.jwt.interfaces.DecodedJWT;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.kakura.icetube.dto.LoginDto;
import com.kakura.icetube.dto.RegistrationDto;
import com.kakura.icetube.dto.UserDto;
import com.kakura.icetube.exception.NotFoundException;
import com.kakura.icetube.model.Role;
import com.kakura.icetube.model.User;
import com.kakura.icetube.service.BlackListService;
import com.kakura.icetube.service.CustomUserDetailsService;
import com.kakura.icetube.service.UserService;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.util.MimeTypeUtils;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.io.IOException;
import java.net.URI;
import java.util.Date;
import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

import static org.springframework.http.HttpHeaders.AUTHORIZATION;
import static org.springframework.http.HttpStatus.FORBIDDEN;

@RestController
@RequestMapping("/api/v1/auth")
@RequiredArgsConstructor
@Log4j2
public class UserController {

    private final UserService userService;

    private final CustomUserDetailsService customUserDetailsService;

    private final AuthenticationManager authenticationManager;

    private final BlackListService blackListService;

    @Value("${jwt_secret}")
    private String SECRET_KEY;
    @Value("${access_token_expiration_millis}")
    private int ACCESS_TOKEN_EXPIRATION_TIME;
    @Value("${refresh_token_expiration_millis}")
    private int REFRESH_TOKEN_EXPIRATION_TIME;

    @PostMapping("/register")
    public ResponseEntity<UserDto> register(@ModelAttribute RegistrationDto registrationDto) {
        URI uri = URI.create(ServletUriComponentsBuilder.fromCurrentContextPath().path("/api/v1/auth/register").toUriString());
        return ResponseEntity.created(uri).body(userService.saveUser(registrationDto));
    }

    @PostMapping("/token")
    public ResponseEntity<?> getToken(@ModelAttribute LoginDto loginDto, HttpServletRequest request, HttpServletResponse response) {
        String username = loginDto.getUsername();
        String password = loginDto.getPassword();
        log.info("Username is: {}", username);
        log.info("Password is: {}", password);
        UsernamePasswordAuthenticationToken authenticationToken = new UsernamePasswordAuthenticationToken(username, password);

        authenticationManager.authenticate(authenticationToken);

        org.springframework.security.core.userdetails.User user =
                (org.springframework.security.core.userdetails.User) customUserDetailsService.loadUserByUsername(username);
        Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes());
        String accessToken = JWT.create()
                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                .withIssuer(request.getRequestURL().toString())
                .withClaim("roles", user.getAuthorities().stream().map(GrantedAuthority::getAuthority).collect(Collectors.toList()))
                .sign(algorithm);

        String refreshToken = JWT.create()
                .withSubject(user.getUsername())
                .withExpiresAt(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                .withIssuer(request.getRequestURL().toString())
                .sign(algorithm);

        Map<String, String> tokens = new HashMap<>();
        tokens.put("access_token", accessToken);
        tokens.put("refresh_token", refreshToken);
        return ResponseEntity.ok().body(tokens);
    }

    @GetMapping("/refresh")
    public void refreshToken(HttpServletRequest request, HttpServletResponse response) throws IOException {
        System.out.println(SecurityContextHolder.getContext().getAuthentication());
        String authorizationHeader = request.getHeader(AUTHORIZATION);
        if (authorizationHeader != null && authorizationHeader.startsWith("Bearer ")) {
            try {
                String oldRefreshToken = authorizationHeader.substring("Bearer ".length());
                blackListService.blackListJwt(oldRefreshToken);
                System.out.println(blackListService.blackListJwt(oldRefreshToken));
                Algorithm algorithm = Algorithm.HMAC256(SECRET_KEY.getBytes());
                JWTVerifier verifier = JWT.require(algorithm).build();
                DecodedJWT decodedJWT = verifier.verify(oldRefreshToken);
                String username = decodedJWT.getSubject();
                User user = userService.getUserByUsername(username)
                        .orElseThrow(() -> new NotFoundException("Cannot find user by username: " + username));

                String accessToken = JWT.create()
                        .withSubject(user.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + ACCESS_TOKEN_EXPIRATION_TIME))
                        .withIssuer(request.getRequestURL().toString())
                        .withClaim("roles", user.getRoles().stream().map(Role::getName).collect(Collectors.toList()))
                        .sign(algorithm);

                String refreshToken = JWT.create()
                        .withSubject(user.getUsername())
                        .withExpiresAt(new Date(System.currentTimeMillis() + REFRESH_TOKEN_EXPIRATION_TIME))
                        .withIssuer(request.getRequestURL().toString())
                        .sign(algorithm);

                Map<String, String> tokens = new HashMap<>();
                tokens.put("access_token", accessToken);
                tokens.put("refresh_token", refreshToken);
                response.setContentType(MediaType.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), tokens);
            } catch (Exception exception) {
                log.error(exception.getMessage());
                response.setHeader("error", exception.getMessage());
                response.setStatus(FORBIDDEN.value());
                //response.sendError(FORBIDDEN.value());
                Map<String, String> error = new HashMap<>();
                error.put("error_message", exception.getMessage());
                response.setContentType(MimeTypeUtils.APPLICATION_JSON_VALUE);
                new ObjectMapper().writeValue(response.getOutputStream(), error);
            }
        } else {
            throw new RuntimeException("Refresh token is missing");
        }
    }

    @GetMapping("/testuser")
    @PreAuthorize("hasRole('ROLE_USER')")
    public Map<String, String> testUser() {
        return Map.of("this", "is for USER");
    }

    @GetMapping("/testadmin")
    @PreAuthorize("hasRole('ROLE_ADMIN')")
    public Map<String, String> testAdmin() {
        return Map.of("this", "is for ADMIN");
    }

}


