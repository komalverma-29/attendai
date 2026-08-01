package com.attendai.core.user.entity;

import org.junit.jupiter.api.Test;

import static org.assertj.core.api.Assertions.assertThat;

class UserStatusTest {

    @Test
    void canAuthenticate_shouldReturnTrue_onlyForActive() {
        assertThat(UserStatus.ACTIVE.canAuthenticate()).isTrue();
        assertThat(UserStatus.INACTIVE.canAuthenticate()).isFalse();
        assertThat(UserStatus.SUSPENDED.canAuthenticate()).isFalse();
        assertThat(UserStatus.LOCKED.canAuthenticate()).isFalse();
    }
}
