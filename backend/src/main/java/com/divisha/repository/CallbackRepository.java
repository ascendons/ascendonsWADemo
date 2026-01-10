package com.divisha.repository;

import com.divisha.model.CallbackRequest;
import java.util.List;
import org.springframework.data.mongodb.repository.MongoRepository;

public interface CallbackRepository extends MongoRepository<CallbackRequest, String> {
  List<CallbackRequest> findByStatus(String status);
}
