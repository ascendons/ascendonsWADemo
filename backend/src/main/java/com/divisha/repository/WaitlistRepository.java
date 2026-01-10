package com.divisha.repository;

import com.divisha.model.WaitlistEntry;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface WaitlistRepository extends MongoRepository<WaitlistEntry, String> {
  List<WaitlistEntry> findByStatus(String status);
}
