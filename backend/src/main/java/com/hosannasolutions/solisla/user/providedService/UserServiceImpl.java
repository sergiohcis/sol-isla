package com.hosannasolutions.solisla.user.providedService;

import com.hosannasolutions.solisla.user.User;
import com.hosannasolutions.solisla.user.UserStatus;
import com.hosannasolutions.solisla.user.dto.CreateUserRequest;
import com.hosannasolutions.solisla.user.exception.DuplicateUserEmailException;
import com.hosannasolutions.solisla.user.exception.UserNotFoundException;
import com.hosannasolutions.solisla.user.repository.UserRepository;
import java.util.Locale;
import java.util.Optional;
import java.util.UUID;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Service;

@Service
public class UserServiceImpl implements UserService {

    private final UserRepository userRepository;
    private final PasswordEncoder passwordEncoder;

    public UserServiceImpl(UserRepository userRepository, PasswordEncoder passwordEncoder) {
        this.userRepository = userRepository;
        this.passwordEncoder = passwordEncoder;
    }

    @Override
    public User createUser(CreateUserRequest request) {
        String normalizedEmail = request.email().toLowerCase(Locale.ROOT);
        if (userRepository.findByEmail(normalizedEmail).isPresent()) {
            throw new DuplicateUserEmailException(normalizedEmail);
        }
        User user = new User(
                request.email(),
                passwordEncoder.encode(request.password()),
                request.firstName(),
                request.lastName(),
                request.role(),
                UserStatus.ACTIVE
        );
        return userRepository.save(user);
    }

    @Override
    public Optional<User> findByEmail(String email) {
        return userRepository.findByEmail(email.toLowerCase(Locale.ROOT));
    }

    @Override
    public User getById(UUID userId) {
        return userRepository.findById(userId).orElseThrow(() -> new UserNotFoundException(userId));
    }

    @Override
    public User disableUser(UUID userId) {
        User user = getById(userId);
        user.disable();
        return userRepository.save(user);
    }
}
