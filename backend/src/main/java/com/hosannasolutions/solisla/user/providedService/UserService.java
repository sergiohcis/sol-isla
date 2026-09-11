package com.hosannasolutions.solisla.user.providedService;

import com.hosannasolutions.solisla.user.User;
import com.hosannasolutions.solisla.user.dto.CreateUserRequest;
import java.util.Optional;
import java.util.UUID;

public interface UserService {

    User createUser(CreateUserRequest request);

    Optional<User> findByEmail(String email);

    User getById(UUID userId);

    User disableUser(UUID userId);
}
