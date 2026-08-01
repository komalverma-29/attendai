package com.attendai.core.common.response;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

import java.util.List;

/**
 * Standard paginated response envelope for all paginated AttendAI endpoints.
 *
 * <p>Response structure:
 * <pre>{@code
 * {
 *   "success": true,
 *   "data": [ ... ],
 *   "pagination": {
 *     "page": 0,
 *     "size": 20,
 *     "totalElements": 150,
 *     "totalPages": 8,
 *     "first": true,
 *     "last": false
 *   }
 * }
 * }</pre>
 *
 * Use {@link #of(Page)} to construct from a Spring Data {@link Page} object.
 *
 * @param <T> the element type of the data list
 */
@Getter
public class PageResponse<T> {

    private final boolean success = true;
    private final List<T> data;
    private final Pagination pagination;

    private PageResponse(List<T> data, Pagination pagination) {
        this.data = data;
        this.pagination = pagination;
    }

    /**
     * Constructs a {@link PageResponse} from a Spring Data {@link Page}.
     *
     * @param page the Spring Data page result
     * @param <T>  the element type
     * @return a populated {@link PageResponse}
     */
    public static <T> PageResponse<T> of(Page<T> page) {
        Pagination pagination = Pagination.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
        return new PageResponse<>(page.getContent(), pagination);
    }

    // -------------------------------------------------------------------------
    // Nested type
    // -------------------------------------------------------------------------

    /**
     * Pagination metadata included in every paginated response.
     */
    @Getter
    @Builder
    public static class Pagination {

        /** Current page number (0-indexed). */
        private final int page;

        /** Number of elements per page. */
        private final int size;

        /** Total number of elements across all pages. */
        private final long totalElements;

        /** Total number of pages. */
        private final int totalPages;

        /** Whether this is the first page. */
        private final boolean first;

        /** Whether this is the last page. */
        private final boolean last;
    }
}
