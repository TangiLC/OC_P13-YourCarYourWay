package com.ycyw.poc_chat.controller;

import com.ycyw.poc_chat.dto.UserProfileRequest;
import com.ycyw.poc_chat.dto.UserProfileResponse;
import com.ycyw.poc_chat.mapper.UserProfileMapper;
import com.ycyw.poc_chat.model.UserProfile;
import com.ycyw.poc_chat.repository.UserProfileRepository;
import com.ycyw.poc_chat.security.UserPrincipal;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.Parameter;
import io.swagger.v3.oas.annotations.media.Content;
import io.swagger.v3.oas.annotations.media.Schema;
import io.swagger.v3.oas.annotations.responses.ApiResponse;
import io.swagger.v3.oas.annotations.responses.ApiResponses;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

/**
 * REST Controller for managing user profiles.
 */
@RestController
@RequestMapping("/api/profile")
@RequiredArgsConstructor
@Tag(name = "UserProfile", description = "Endpoints pour le profil utilisateur")
public class UserProfileController {

  private final UserProfileRepository userProfileRepository;
  private final UserProfileMapper userProfileMapper;

  /**
   * Récupère son propre profil.
   */
  @Operation(
    summary = "Get own profile",
    description = "Récupère le profil de l'utilisateur connecté"
  )
  @ApiResponse(
    responseCode = "200",
    description = "Profil de l'utilisateur connecté",
    content = @Content(
      schema = @Schema(implementation = UserProfileResponse.class)
    )
  )
  @GetMapping("/me")
  public ResponseEntity<UserProfileResponse> getMyProfile(
    @AuthenticationPrincipal UserPrincipal principal
  ) {
    UserProfile profile = userProfileRepository.findByUserCredentialId(
      principal.getId()
    );
    if (profile == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(userProfileMapper.toResponse(profile));
  }

  /**
   * Récupère un profil par ID (réservé aux AGENT / ADMIN).
   */
  @Operation(
    summary = "Get user profile by ID",
    description = "Accès réservé aux rôles AGENT et ADMIN"
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "Profil trouvé",
        content = @Content(
          schema = @Schema(implementation = UserProfileResponse.class)
        )
      ),
      @ApiResponse(responseCode = "404", description = "Profil non trouvé"),
    }
  )
  @PreAuthorize("hasAnyRole('AGENT', 'ADMIN')")
  @GetMapping("/{userId}")
  public ResponseEntity<UserProfileResponse> getProfileByUserId(
    @Parameter(
      description = "ID utilisateur (depuis user_credentials)"
    ) @PathVariable Long userId
  ) {
    UserProfile profile = userProfileRepository.findByUserCredentialId(userId);
    if (profile == null) {
      return ResponseEntity.notFound().build();
    }
    return ResponseEntity.ok(userProfileMapper.toResponse(profile));
  }

  /**
   * Mise à jour du profil utilisateur.
   */
  @Operation(
    summary = "Update user profile",
    description = "Mise à jour des informations utilisateur selon id."
  )
  @ApiResponses(
    {
      @ApiResponse(
        responseCode = "200",
        description = "Profil mis à jour",
        content = @Content(
          schema = @Schema(implementation = UserProfileResponse.class)
        )
      ),
      @ApiResponse(responseCode = "404", description = "Profil non trouvé"),
      @ApiResponse(responseCode = "403", description = "Privilèges incorrects"),
      @ApiResponse(responseCode = "400", description = "Entrée invalide"),
    }
  )
  @PutMapping("/{userId}")
  public ResponseEntity<UserProfileResponse> updateProfile(
    @Parameter(
      description = "ID utilisateur (depuis user_credentials)"
    ) @PathVariable Long userId,
    @Parameter(
      description = "Données du profil à jour",
      required = true
    ) @Valid @RequestBody UserProfileRequest request,
    @AuthenticationPrincipal UserPrincipal principal
  ) {
    UserProfile existing = userProfileRepository.findByUserCredentialId(userId);
    if (existing == null) {
      return ResponseEntity.notFound().build();
    }

    String userRole = principal
      .getAuthorities()
      .stream()
      .findFirst()
      .map(a -> a.getAuthority().replace("ROLE_", ""))
      .orElse("");

    boolean isOwner = principal.getId().equals(userId);
    boolean isPrivileged = userRole.equals("ADMIN") || userRole.equals("AGENT");

    if (!isOwner && !isPrivileged) {
      return ResponseEntity.status(403).build(); // Forbidden
    }

    existing.setFirstName(request.getFirstName());
    existing.setLastName(request.getLastName());
    existing.setCompany(request.getCompany());
    existing.setType(request.getType());

    UserProfile updated = userProfileRepository.save(existing);
    return ResponseEntity.ok(userProfileMapper.toResponse(updated));
  }
}
