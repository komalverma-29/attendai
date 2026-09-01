package com.attendai.school.school.entity;

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

/**
 * Root entity of the AttendAI School module.
 *
 * <p>A school is the top-level tenant boundary within {@code attendai-school}.
 * Every school-specific entity (teacher, student, class, section, etc.) holds
 * a {@code school_id} FK referencing this table.
 *
 * <p>{@code code} is uppercase, 4–10 characters, and immutable after creation.
 * It is auto-generated from the school name if not explicitly provided.
 */
@Getter
@Setter
@Builder
@NoArgsConstructor
@AllArgsConstructor
@Entity
@Table(name = "school_schools")
public class School extends SoftDeletableEntity {

    @Column(name = "name", nullable = false, unique = true, length = 255)
    private String name;

    /**
     * Unique short code. Uppercase, 4–10 alphanumeric characters.
     * Immutable after creation — {@code updatable = false}.
     */
    @Column(name = "code", nullable = false, unique = true, length = 10, updatable = false)
    private String code;

    @Enumerated(EnumType.STRING)
    @Column(name = "type", nullable = false, length = 20)
    private SchoolType type;

    @Enumerated(EnumType.STRING)
    @Column(name = "status", nullable = false, length = 20)
    @Builder.Default
    private SchoolStatus status = SchoolStatus.ACTIVE;

    @Column(name = "description", columnDefinition = "TEXT")
    private String description;

    @Column(name = "address_line1", nullable = false, length = 255)
    private String addressLine1;

    @Column(name = "address_line2", length = 255)
    private String addressLine2;

    @Column(name = "city", nullable = false, length = 100)
    private String city;

    @Column(name = "state_or_province", nullable = false, length = 100)
    private String stateOrProvince;

    @Column(name = "postal_code", length = 20)
    private String postalCode;

    /** ISO 3166-1 alpha-2 country code, e.g. "IN". */
    @Column(name = "country", nullable = false, length = 2)
    private String country;

    @Column(name = "phone", length = 30)
    private String phone;

    @Column(name = "email", length = 255)
    private String email;

    @Column(name = "website", length = 500)
    private String website;

    /** Optional FK → files(id) in core-file. */
    @Column(name = "logo_file_id")
    private Long logoFileId;
}
