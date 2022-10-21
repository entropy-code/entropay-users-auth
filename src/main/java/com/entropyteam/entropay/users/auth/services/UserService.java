package com.entropyteam.entropay.users.auth.services;

import java.util.Optional;
import java.util.UUID;
import org.springframework.stereotype.Service;
import com.entropyteam.entropay.users.auth.common.BaseRepository;
import com.entropyteam.entropay.users.auth.common.BaseService;
import com.entropyteam.entropay.users.auth.dtos.UserDto;
import com.entropyteam.entropay.users.auth.models.User;
import com.entropyteam.entropay.users.auth.repositories.UserRepository;

import lombok.AllArgsConstructor;

@Service
@AllArgsConstructor
public class UserService extends BaseService<User, UserDto, UUID> {

    private final UserRepository userRepository;
    public Optional<UserDto> getUserByUsername(String username) {
        return userRepository.findByUsername(username).map(UserDto::new);
    }

    @Override
    protected BaseRepository<User, UUID> getRepository() {
        return userRepository;
    }

    @Override
    protected UserDto toDTO(User entity) {
        return new UserDto(entity);
    }

    @Override
    protected User toEntity(UserDto entity) {
        return null;
    }
}
