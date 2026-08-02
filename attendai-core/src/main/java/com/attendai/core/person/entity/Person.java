package com.attendai.core.person.entity;

import com.attendai.core.common.entity.SoftDeletableEntity;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.Table;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.time.LocalDate;

/**
 * Domain-agnostic person entity. Represents a real human being.
 *
 * <p>Person is a foundational entity. All business modules (school, college,
 * enterprise) link their domain entities to a person via a FK on persons.id.
 * Core never references those domain entities — the FK always points inward.
 *
 * <p>PII fields (email, phone, identityDocNumber) must NEVER be logged.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "persons")
public class Person extends SoftDeletableEntity {

    @Column(name = "first_name", nullable = false, length = 100)
    private String firstName;

    @Column(name = "middle_name", length = 100)
    private String middleName;

    @Column(name = "last_name", nullable = false, length = 100)
    private String lastName;

    @Enumerated(EnumType.STRING)
    @Column(name = "gender", length = 20)
    private Gender gender;

    @Column(name = "date_of_birth")
    private LocalDate dateOfBirth;

    /** Optional. Unique among non-deleted persons. PII — never log. */
    @Column(name = "email", length = 255)
    private String email;

    /** Optional. PII — never log. */
    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "address_line1", length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", length = 100)
    private String city;

    @Column(name = "state_or_province", length = 100)
    private String stateOrProvince;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /** ISO 3166-1 alpha-2 country code. */
    @Column(name = "country", length = 2)
    private String country;

    @Enumerated(EnumType.STRING)
    @Column(name = "identity_doc_type", length = 30)
    private IdentityDocType identityDocType;

    /** Stored as plain text in V1. PII — never log. */
    @Column(name = "identity_doc_number", length = 100)
    private String identityDocNumber;

    /** Optional FK → files(id). Managed by core-file. */
    @Column(name = "profile_photo_file_id")
    private Long profilePhotoFileId;
}
