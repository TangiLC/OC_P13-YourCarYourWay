package com.ycyw.poc_chat.security;

import java.util.Collection;

import org.springframework.security.core.GrantedAuthority;
import org.springframework.security.core.userdetails.UserDetails;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

/**
 * Représentation de l'utilisateur pour Spring Security,
 */
@Getter
@RequiredArgsConstructor
public class UserPrincipal implements UserDetails {

  private final Long id;
  private final String username; //email
  private final String password;
  private final Collection<? extends GrantedAuthority> authorities;

  private final boolean accountNonExpired = true;
  private final boolean accountNonLocked = true;
  private final boolean credentialsNonExpired = true;
  private final boolean enabled = true;
}
