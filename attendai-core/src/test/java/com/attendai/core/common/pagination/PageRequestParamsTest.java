package com.attendai.core.common.pagination;

import com.attendai.core.common.constants.AttendAIConstants;
import org.junit.jupiter.api.Test;
import org.springframework.data.domain.Pageable;
import org.springframework.data.domain.Sort;

import static org.assertj.core.api.Assertions.assertThat;

class PageRequestParamsTest {

    @Test
    void defaultValues_shouldBeAppliedWhenNothingSet() {
        PageRequestParams params = new PageRequestParams();

        assertThat(params.getPage()).isZero();
        assertThat(params.getSize()).isEqualTo(AttendAIConstants.DEFAULT_PAGE_SIZE);
        assertThat(params.getSortBy()).isEqualTo("createdAt");
        assertThat(params.getSortDirection()).isEqualTo("ASC");
    }

    @Test
    void toPageable_shouldReturnCorrectPageable() {
        PageRequestParams params = new PageRequestParams();
        params.setPage(2);
        params.setSize(50);
        params.setSortBy("name");
        params.setSortDirection("DESC");

        Pageable pageable = params.toPageable();

        assertThat(pageable.getPageNumber()).isEqualTo(2);
        assertThat(pageable.getPageSize()).isEqualTo(50);
        assertThat(pageable.getSort().getOrderFor("name")).isNotNull();
        assertThat(pageable.getSort().getOrderFor("name").getDirection())
                .isEqualTo(Sort.Direction.DESC);
    }

    @Test
    void toPageable_shouldDefaultToAsc_whenInvalidDirectionProvided() {
        PageRequestParams params = new PageRequestParams();
        params.setSortDirection("INVALID");

        Pageable pageable = params.toPageable();

        assertThat(pageable.getSort().getOrderFor("createdAt").getDirection())
                .isEqualTo(Sort.Direction.ASC);
    }
}
