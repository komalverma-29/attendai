package com.attendai.core.common.pagination;

import com.attendai.core.common.constants.AttendAIConstants;
import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import lombok.Getter;
import lombok.Setter;
import org.springframework.data.domain.PageRequest;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

/**
 * Standard query parameters for all paginated API endpoints.
 *
 * <p>Bind to controller method parameters using {@code @ModelAttribute} or as
 * individual {@code @RequestParam} fields. The {@link #toPageable()} method
 * converts these params into a Spring Data {@link Pageable}.
 *
 * <p>Defaults:
 * <ul>
 *   <li>{@code page} = 0 (first page)</li>
 *   <li>{@code size} = 20</li>
 *   <li>{@code sortBy} = "createdAt"</li>
 *   <li>{@code sortDirection} = "ASC"</li>
 * </ul>
 */
@Getter
@Setter
public class PageRequestParams {

    /** Page number, 0-indexed. */
    @Min(value = 0, message = "Page number must be 0 or greater")
    private int page = 0;

    /** Page size. Minimum 1, maximum 100. */
    @Min(value = 1, message = "Page size must be at least 1")
    @Max(value = AttendAIConstants.MAX_PAGE_SIZE, message = "Page size must not exceed " + AttendAIConstants.MAX_PAGE_SIZE)
    private int size = AttendAIConstants.DEFAULT_PAGE_SIZE;

    /** Field name to sort by. Defaults to "createdAt". */
    private String sortBy = "createdAt";

    /**
     * Sort direction: "ASC" or "DESC" (case-insensitive).
     * Defaults to "ASC".
     */
    private String sortDirection = AttendAIConstants.DEFAULT_SORT_DIRECTION;

    /**
     * Converts these parameters into a Spring Data {@link Pageable}.
     *
     * @return a {@link PageRequest} with the configured page, size, and sort
     */
    public Pageable toPageable() {
        Sort.Direction direction;
        try {
            direction = Sort.Direction.fromString(sortDirection);
        } catch (IllegalArgumentException e) {
            direction = Sort.Direction.ASC;
        }
        return PageRequest.of(page, size, Sort.by(direction, sortBy));
    }
}
