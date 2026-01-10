package com.divisha.repository;

import com.divisha.model.User;
import java.util.Optional;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface UserRepository extends MongoRepository<User, String> {

  Optional<User> findByPhone(String phone);

  Optional<User> findByEmail(String email);

  Boolean existsByEmail(String email);

  Boolean existsByPhone(String phone);
}
