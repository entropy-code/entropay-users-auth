package com.entropyteam.entropay.users.auth.repositories;

import java.util.Optional;
import java.util.UUID;
import com.entropyteam.entropay.users.auth.common.BaseRepository;
import com.entropyteam.entropay.users.auth.models.User;

public interface UserRepository extends BaseRepository<User, UUID> {

    Optional<User> findByUsername(String username);
}
