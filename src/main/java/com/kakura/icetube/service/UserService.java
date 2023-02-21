package com.kakura.icetube.service;

import com.kakura.icetube.dto.RegistrationDto;
import com.kakura.icetube.dto.UserDto;
import com.kakura.icetube.exception.NotFoundException;
import com.kakura.icetube.mapper.UserMapper;
import com.kakura.icetube.model.User;
import com.kakura.icetube.model.Video;
import com.kakura.icetube.repository.RoleRepository;
import com.kakura.icetube.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.Optional;

@Service
@RequiredArgsConstructor
@Transactional
@Log4j2
public class UserService {

    private final UserRepository userRepository;
    private final RoleRepository roleRepository;
    private final PasswordEncoder passwordEncoder;
    private final UserMapper userMapper;

    public UserDto saveUser(RegistrationDto registrationDto) {
        User user = userMapper.toModel(registrationDto);
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRoles(List.of(roleRepository.findByName("ROLE_USER")));
        return userMapper.toDto(userRepository.save(user));
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    public User getCurrentUser() {

        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Cannot find user by username " + username));
    }

    public void addToLikedVideos(Video video) {
        User currentUser = getCurrentUser();
        currentUser.addToLikedVideos(video);
        userRepository.save(currentUser);
    }

    public boolean ifLikedVideo(Long videoId) {
        return getCurrentUser().getLikedVideos().stream().anyMatch(likedVideo -> likedVideo.getId().equals(videoId));
    }

    public boolean ifDislikedVideo(Long videoId) {
        return getCurrentUser().getDislikedVideos().stream().anyMatch(likedVideo -> likedVideo.getId().equals(videoId));
    }

    public void removeFromLikedVideos(Video video) {
        User currentUser = getCurrentUser();
        currentUser.removeFromLikedVideos(video);
        userRepository.save(currentUser);
    }

    public void removeFromDislikedVideos(Video video) {
        User currentUser = getCurrentUser();
        currentUser.removeFromDislikedVideos(video);
        userRepository.save(currentUser);
    }

    public void addToDisLikedVideos(Video video) {
        User currentUser = getCurrentUser();
        currentUser.addToDislikedVideos(video);
        userRepository.save(currentUser);
    }

    public void addToWatchedVideos(Video videoFromDb) {
        User currentUser = getCurrentUser();
        currentUser.addToWatchedVideos(videoFromDb);
        userRepository.save(currentUser);
    }
}