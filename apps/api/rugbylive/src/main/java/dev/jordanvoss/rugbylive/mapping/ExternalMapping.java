package dev.jordanvoss.rugbylive.mapping;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "external_mappings")
public class ExternalMapping {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String provider;
    private String entityType;
    private String internalId;
    private String externalId;
    private String externalName;
    @Column(name = "is_active")
    private boolean active = true;
    private String confidence = "provider";
}
