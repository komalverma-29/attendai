package com.attendai.core.common.pagination;

import lombok.Builder;
import lombok.Getter;
import org.springframework.data.domain.Page;

/**
 * Pagination metadata extracted from a Spring Data {@link Page} for inclusion
 * in paginated API responses.
 *
 * <p>This is a standalone DTO used when you need to carry pagination metadata
 * separately from the data list. For the combined envelope, use
 * {@link com.attendai.core.common.response.PageResponse} instead.
 */
@Getter
@Builder
public class PageMetadata {

    /** Current page number (0-indexed). */
    private final int page;

    /** Number of elements per page requested. */
    private final int size;

    /** Total number of elements across all pages. */
    private final long totalElements;

    /** Total number of pages. */
    private final int totalPages;

    /** Whether this is the first page. */
    private final boolean first;

    /** Whether this is the last page. */
    private final boolean last;

    /**
     * Constructs {@link PageMetadata} from a Spring Data {@link Page}.
     *
     * @param page the page result from a repository query
     * @param <T>  the element type (unused, needed for generic resolution)
     * @return populated {@link PageMetadata}
     */
    public static <T> PageMetadata from(Page<T> page) {
        return PageMetadata.builder()
                .page(page.getNumber())
                .size(page.getSize())
                .totalElements(page.getTotalElements())
                .totalPages(page.getTotalPages())
                .first(page.isFirst())
                .last(page.isLast())
                .build();
    }
}
