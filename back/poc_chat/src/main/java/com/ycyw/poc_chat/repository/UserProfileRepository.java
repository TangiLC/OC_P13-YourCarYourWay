package com.ycyw.poc_chat.repository;

import com.ycyw.poc_chat.model.UserProfile;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

/**
 * Repository interface for accessing UserProfile entities.
 */
@Repository
public interface UserProfileRepository
  extends JpaRepository<UserProfile, Long> {
  UserProfile findByUserCredentialId(Long userId);

  /* futur dev : 1 credential to many profile
List<UserProfile> findAllByUserCredentialId(Long userId);
  */
}
