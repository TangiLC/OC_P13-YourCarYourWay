package com.ycyw.poc_chat.controller;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.security.authentication.AuthenticationManager;
import org.springframework.security.authentication.BadCredentialsException;
import org.springframework.security.authentication.UsernamePasswordAuthenticationToken;
import org.springframework.security.core.Authentication;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RestController;

import com.ycyw.poc_chat.dto.JwtResponse;
import com.ycyw.poc_chat.dto.LoginRequest;
import com.ycyw.poc_chat.dto.RegisterRequest;
import com.ycyw.poc_chat.model.Role;
import com.ycyw.poc_chat.model.UserCredential;
import com.ycyw.poc_chat.repository.UserCredentialRepository;
import com.ycyw.poc_chat.security.JwtTokenProvider;
import com.ycyw.poc_chat.security.UserPrincipal;

import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import jakarta.validation.Valid;

/**
 * Contrôleur pour gérer l'inscription et la connexion JWT.
 */
@RestController
@RequestMapping("/auth")
public class AuthController {

  private final AuthenticationManager authManager;
  private final JwtTokenProvider tokenProvider;
  private final PasswordEncoder passwordEncoder;
  private final UserCredentialRepository credentialRepo;

  public AuthController(
    AuthenticationManager authManager,
    JwtTokenProvider tokenProvider,
    PasswordEncoder passwordEncoder,
    UserCredentialRepository credentialRepo
  ) {
    this.authManager = authManager;
    this.tokenProvider = tokenProvider;
    this.passwordEncoder = passwordEncoder;
    this.credentialRepo = credentialRepo;
  }

  /**
   * Inscrit un nouvel utilisateur.
   *
   * @param req DTO contenant email, mot de passe et rôle
   * @return 200 si succès, 400 si email déjà existant
   */
  @Operation(
    summary = "Register",
    description = "Crée un nouvel utilisateur avec rôle"
  )
  @ApiResponse(responseCode = "200", description = "Inscription réussie")
  @ApiResponse(responseCode = "400", description = "Email déjà utilisé")
  @PostMapping("/register")
  public ResponseEntity<String> register(
    @Valid @RequestBody RegisterRequest req
  ) {
    if (credentialRepo.existsByEmail(req.getEmail())) {
      return ResponseEntity.badRequest().body("Email déjà utilisé");
    }

    Role roleEnum;
    try {
      roleEnum = Role.valueOf(req.getRole().toUpperCase());
    } catch (IllegalArgumentException e) {
      return ResponseEntity
        .badRequest()
        .body("Rôle invalide. Doit être USER, AGENT ou ADMIN.");
    }

    UserCredential uc = UserCredential
      .builder()
      .email(req.getEmail())
      .password(passwordEncoder.encode(req.getPassword()))
      .role(roleEnum)
      .build();
    credentialRepo.save(uc);

    return ResponseEntity.ok("Inscription réussie");
  }

  /**
   * Authentifie un utilisateur et renvoie un JWT.
   *
   * @param req DTO contenant email et mot de passe
   * @return 200 + JwtResponse si succès, 401 sinon
   */
  @Operation(
    summary = "Login",
    description = "Authentifie et retourne un token JWT"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Connexion réussie, JWT généré"
  )
  @ApiResponse(
    responseCode = "401",
    description = "Email ou mot de passe incorrect"
  )
  @PostMapping("/login")
  public ResponseEntity<?> login(@Valid @RequestBody LoginRequest req) {
    try {
      Authentication authentication = authManager.authenticate(
        new UsernamePasswordAuthenticationToken(
          req.getEmail(),
          req.getPassword()
        )
      );
      UserPrincipal principal = (UserPrincipal) authentication.getPrincipal();

      String authority = principal
        .getAuthorities()
        .stream()
        .findFirst()
        .orElseThrow()
        .getAuthority();

      String roleName = authority.replace("ROLE_", "");

      Role roleEnum = Role.valueOf(roleName);
      String token = tokenProvider.generateToken(principal.getId(), roleEnum);

      JwtResponse resp = new JwtResponse(
        token,
        principal.getId(),
        principal.getUsername(),
        roleEnum.name()
      );
      return ResponseEntity.ok(resp);
    } catch (BadCredentialsException ex) {
      return ResponseEntity
        .status(HttpStatus.UNAUTHORIZED)
        .body("Email ou mot de passe incorrect");
    }
  }
}
