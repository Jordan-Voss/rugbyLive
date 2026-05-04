package dev.jordanvoss.rugbylive.country;

import jakarta.persistence.*;
import lombok.Data;

@Data
@Entity
@Table(name = "countries")
public class Country {

    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    private String canonicalKey;
    private String name;
    private String code;
    private String flagUrl;
    private String flagSource;
}