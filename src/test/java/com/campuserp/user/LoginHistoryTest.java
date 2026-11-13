package com.campuserp.user;

import com.campuserp.ApiTest;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class LoginHistoryTest extends ApiTest {

    @Autowired
    UserRepository users;

    @Test
    void adminSeesEverySignInAndWhoIsActive() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .header("User-Agent", "TestBrowser").header("X-Forwarded-For", "203.0.113.7, 10.0.0.1")
                        .content("{\"username\":\"staff\",\"password\":\"not-the-password\"}"))
                .andExpect(status().isUnauthorized());
        String staff = staff();

        // the newest attempt comes first: staff's good sign-in, then the wrong password with its address
        getAs(admin(), "/api/users/logins").andExpect(status().isOk())
                .andExpect(jsonPath("$[1].username").value("staff"))
                .andExpect(jsonPath("$[1].outcome").value("SUCCESS"))
                .andExpect(jsonPath("$[2].outcome").value("WRONG_PASSWORD"))
                .andExpect(jsonPath("$[2].fullName").value("Office Staff"))
                .andExpect(jsonPath("$[2].ip").value("203.0.113.7"))
                .andExpect(jsonPath("$[2].userAgent").value("TestBrowser"));

        // signing in and using the app set the "last active" time
        User u = users.findByUsername("staff").orElseThrow();
        assertThat(u.getLastLoginAt()).isNotNull();
        assertThat(u.getLastSeenAt()).isNotNull();

        // only admins can see who signed in
        getAs(staff, "/api/users/logins").andExpect(status().isForbidden());
    }
}
