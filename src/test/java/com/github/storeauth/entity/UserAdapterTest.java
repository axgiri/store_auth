package com.github.storeauth.entity;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import java.util.UUID;

import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;

class UserAdapterTest {

    @Nested
    @DisplayName("fromUser")
    class FromUser {

        @Test
        @DisplayName("maps all fields correctly from User entity")
        void mapsAllFields() {
            var key = UUID.randomUUID();
            var user = User.builder()
                    .idempotencyKey(key)
                    .email("u@test.com")
                    .password("encoded")
                    .roleEnum(Role.ADMIN)
                    .isActive(true)
                    .isNotBlocked(true)
                    .build();

            UserAdapter adapter = UserAdapter.fromUser(user);

            assertThat(adapter.getUsername()).isEqualTo(key.toString());
            assertThat(adapter.getPassword()).isEqualTo("encoded");
            assertThat(adapter.isEnabled()).isTrue();
            assertThat(adapter.isAccountNonLocked()).isTrue();
            assertThat(adapter.getAuthorities())
                    .extracting("authority")
                    .containsExactly("ROLE_ADMIN");
        }

        @Test
        @DisplayName("returns empty authorities when role is null")
        void emptyAuthorities_whenRoleNull() {
            var user = User.builder()
                    .idempotencyKey(UUID.randomUUID())
                    .roleEnum(null)
                    .build();

            UserAdapter adapter = UserAdapter.fromUser(user);

            assertThat(adapter.getAuthorities()).isEmpty();
        }

        @Test
        @DisplayName("throws IllegalArgumentException for null user")
        void throws_whenUserNull() {
            assertThatThrownBy(() -> UserAdapter.fromUser(null))
                    .isInstanceOf(IllegalArgumentException.class);
        }
    }
}
