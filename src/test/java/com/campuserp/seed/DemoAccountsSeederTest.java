package com.campuserp.seed;

import com.campuserp.user.UserRepository;
import org.junit.jupiter.api.Test;
import org.springframework.security.crypto.bcrypt.BCryptPasswordEncoder;

import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

class DemoAccountsSeederTest {

    @Test
    void firstStartWithoutPasswordsIsRefused() {
        UserRepository users = mock(UserRepository.class);
        when(users.count()).thenReturn(0L);
        var seeder = new DemoAccountsSeeder(users, new BCryptPasswordEncoder(), "", "staff-pw-1", "viewer-pw-1");

        assertThatThrownBy(() -> seeder.run(null))
                .isInstanceOf(IllegalStateException.class)
                .hasMessageContaining("ADMIN_PASSWORD");
        verify(users, never()).save(any());
    }

    @Test
    void passwordsAreNotNeededOnceUsersExist() {
        UserRepository users = mock(UserRepository.class);
        when(users.count()).thenReturn(3L);
        new DemoAccountsSeeder(users, new BCryptPasswordEncoder(), "", "", "").run(null);
        verify(users, never()).save(any());
    }
}
