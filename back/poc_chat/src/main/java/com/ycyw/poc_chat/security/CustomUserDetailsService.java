package com.ycyw.poc_chat.security;

import com.ycyw.poc_chat.model.UserCredential;
import com.ycyw.poc_chat.repository.UserCredentialRepository;
import java.util.List;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.security.core.authority.SimpleGrantedAuthority;
import org.springframework.security.core.userdetails.*;
import org.springframework.stereotype.Service;

/**
 * Charge l'utilisateur depuis la table user_credentials.
 */
@Service
public class CustomUserDetailsService implements UserDetailsService {

  @Autowired
  private UserCredentialRepository credentialRepo;

  /**
   * Charge un utilisateur par email.
   */
  @Override
  public UserDetails loadUserByUsername(String email)
    throws UsernameNotFoundException {
    UserCredential uc = credentialRepo
      .findByEmail(email)
      .orElseThrow(() ->
        new UsernameNotFoundException("Utilisateur non trouvé : " + email)
      );
    return buildPrincipal(uc);
  }

  /**
   * Charge un utilisateur par ID (pour le JWT filter).
   */
  public UserDetails loadUserById(Long id) {
    UserCredential uc = credentialRepo
      .findById(id)
      .orElseThrow(() ->
        new UsernameNotFoundException("Utilisateur non trouvé ID: " + id)
      );
    return buildPrincipal(uc);
  }

  private UserPrincipal buildPrincipal(UserCredential uc) {
    return new UserPrincipal(
      uc.getId(),
      uc.getEmail(),
      uc.getPassword(),
      List.of(new SimpleGrantedAuthority("ROLE_" + uc.getRole()))
    );
  }
}
