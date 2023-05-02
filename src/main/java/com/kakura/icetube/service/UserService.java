package com.kakura.icetube.service;

import com.kakura.icetube.dto.ChangePasswordDto;
import com.kakura.icetube.dto.RegistrationDto;
import com.kakura.icetube.dto.UserDto;
import com.kakura.icetube.dto.VideoDto;
import com.kakura.icetube.exception.BadRequestException;
import com.kakura.icetube.exception.NotFoundException;
import com.kakura.icetube.mapper.UserMapper;
import com.kakura.icetube.mapper.VideoMapper;
import com.kakura.icetube.model.Subscription;
import com.kakura.icetube.model.User;
import com.kakura.icetube.model.Video;
import com.kakura.icetube.repository.RoleRepository;
import com.kakura.icetube.repository.SubscriptionRepository;
import com.kakura.icetube.repository.UserRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.log4j.Log4j2;
import org.springframework.dao.DataIntegrityViolationException;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
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
    private final SubscriptionRepository subscriptionRepository;
    private final VideoMapper videoMapper;
    private final AuthenticationManager authenticationManager;

    public UserDto saveUser(RegistrationDto registrationDto) {
        User user = userMapper.toModel(registrationDto);
        user.setPassword(passwordEncoder.encode(registrationDto.getPassword()));
        user.setRoles(List.of(roleRepository.findByName("ROLE_USER")));
        return userMapper.toDto(userRepository.save(user));
    }

    public Optional<User> getUserByUsername(String username) {
        return userRepository.findByUsername(username);
    }

    User getCurrentUser() {

        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userRepository.findByUsername(username)
                .orElseThrow(() -> new NotFoundException("Cannot find user by username " + username));
    }

    public UserDto getCurrentUserDto() {
        return userMapper.toDto(getCurrentUser());
    }

    public boolean isLoggedIn() {
        String username = (String) SecurityContextHolder.getContext().getAuthentication().getPrincipal();

        return userRepository.existsUserByUsername(username);
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

    @Transactional
    public void subscribeToUser(Long id) {
        User subscriber = getCurrentUser();
        User subscribedTo = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cannot find user by id" + id));

        if (subscriber.equals(subscribedTo)) {
            throw new BadRequestException("Cannot subscribe to yourself");
        }

        Subscription subscription = Subscription.builder()
                .subscriber(subscriber)
                .subscribedTo(subscribedTo)
                .build();
        subscribedTo.incrementSubscribersCount();
        userRepository.save(subscribedTo);
        try {
            subscriptionRepository.save(subscription);
        } catch (DataIntegrityViolationException e) {
            throw new BadRequestException("Already subscribed to user with id " + id);
        }
    }

    @Transactional
    public void unsubscribeFromUser(Long id) {
        User subscriber = getCurrentUser();
        User subscribedTo = userRepository.findById(id)
                .orElseThrow(() -> new NotFoundException("Cannot find user by id" + id));

        subscribedTo.decrementSubscribersCount();
        userRepository.save(subscribedTo);

        Subscription subscription = subscriptionRepository.findBySubscriberAndSubscribedTo(subscriber, subscribedTo)
                .orElseThrow(() -> new NotFoundException("Subscription not found"));

        subscriptionRepository.delete(subscription);
    }

    public Boolean isSubscribedToAuthor(User author) {
        User currentUser = getCurrentUser();
        Optional<Subscription> optionalSubscription = subscriptionRepository.findBySubscriberAndSubscribedTo(currentUser, author);
        return optionalSubscription.isPresent();
    }

    public List<VideoDto> getLikedVideos() {
        User currentUser = getCurrentUser();
        return currentUser.getLikedVideos().stream().map(videoMapper::toDto).toList();
    }

    public List<UserDto> getSubscriptions() {
        User currentUser = getCurrentUser();
        return subscriptionRepository.findBySubscriber(currentUser).stream()
                .map(subscription -> userMapper.toDto(subscription.getSubscribedTo())).toList();
    }

    public UserDto getById(Long userId) {
        User user = userRepository.findById(userId)
                .orElseThrow(() -> new NotFoundException("Cannot find user by id " + userId));
        UserDto userDto = userMapper.toDto(user);
        if (isLoggedIn()) {
            userDto.setIsSubscribed(isSubscribedToAuthor(user));
        }
        return userDto;
    }

    public void changePassword(ChangePasswordDto changePasswordDto) {
        User currentUser = getCurrentUser();

        try {
            UsernamePasswordAuthenticationToken authenticationToken =
                    new UsernamePasswordAuthenticationToken(currentUser.getUsername(),
                            changePasswordDto.getOldPassword());

            authenticationManager.authenticate(authenticationToken);

            currentUser.setPassword(passwordEncoder.encode(changePasswordDto.getNewPassword()));
            userRepository.save(currentUser);
        } catch (Exception e) {
            throw new BadRequestException("Old password is not correct");
        }
    }
}