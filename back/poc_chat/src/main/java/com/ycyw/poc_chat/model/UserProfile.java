package com.ycyw.poc_chat.model;

import com.fasterxml.jackson.annotation.*;
import jakarta.persistence.*;
import java.util.HashSet;
import java.util.Set;
import lombok.*;

@Entity
@Table(name = "user_profiles")
@Data
@Builder
@NoArgsConstructor
@AllArgsConstructor
public class UserProfile {

  @Id
  @GeneratedValue(strategy = GenerationType.IDENTITY)
  private Long id;

  @OneToOne(fetch = FetchType.LAZY, optional = false)
  @JoinColumn(name = "user_id", nullable = false)
  @JsonIgnore
  private UserCredential userCredential;

  private String firstName;
  private String lastName;
  private String company;

  @Enumerated(EnumType.STRING)
  private ProfileType type;

  @ManyToMany(mappedBy = "participants")
  @JsonIgnoreProperties({ "participants" }) // Évite la boucle infinie
  @Builder.Default
  private Set<Dialog> dialogs = new HashSet<>();
}
