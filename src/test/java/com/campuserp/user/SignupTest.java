package com.campuserp.user;

import com.campuserp.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.assertj.core.api.Assertions.assertThat;
import static org.hamcrest.Matchers.containsString;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class SignupTest extends ApiTest {

    @Autowired
    UserRepository users;

    ResultActions signup(String json) throws Exception {
        return mvc.perform(post("/api/auth/signup").contentType(MediaType.APPLICATION_JSON).content(json));
    }

    ResultActions login(String username, String password) throws Exception {
        return mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)));
    }

    @Test
    void newAccountIsAViewerAndWaitsForApproval() throws Exception {
        signup("{\"name\":\" Asha Rao \",\"email\":\"Asha@Example.edu\",\"password\":\"asha-pw-1\"}")
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.message", containsString("approve")));

        User u = users.findFirstByEmailIgnoreCase("asha@example.edu").orElseThrow();
        assertThat(u.getRole()).isEqualTo(Role.VIEWER);
        assertThat(u.isEnabled()).isFalse();
        assertThat(u.getFullName()).isEqualTo("Asha Rao");
        assertThat(u.getUsername()).isEqualTo("asha@example.edu");

        // right password: told to wait; wrong password: the usual message, so nothing leaks
        login("asha@example.edu", "asha-pw-1")
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message", containsString("approve")));
        login("asha@example.edu", "wrong-pw").andExpect(status().isUnauthorized());

        // the admin approves it on the Users page; then the email logs in, in any case
        putAs(admin(), "/api/users/" + u.getId(), "{\"fullName\":\"Asha Rao\",\"role\":\"VIEWER\",\"enabled\":true}")
                .andExpect(status().isOk());
        String token = JsonPath.read(login("ASHA@example.edu", "asha-pw-1").andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString(), "$.token");

        // a viewer reads, but can't change things or reach admin pages
        getAs(token, "/api/students").andExpect(status().isOk());
        postAs(token, "/api/events", "{}").andExpect(status().isForbidden());
        getAs(token, "/api/users").andExpect(status().isForbidden());
    }

    @Test
    void anyFieldOtherThanNameEmailPasswordIsRefused() throws Exception {
        signup("{\"name\":\"Mallory\",\"email\":\"m@example.edu\",\"password\":\"mallory-1\",\"role\":\"ADMIN\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message").value("Only name, email and password can be sent. Not allowed: role"));
        signup("{\"name\":\"Mallory\",\"email\":\"m@example.edu\",\"password\":\"mallory-1\",\"enabled\":true,\"username\":\"admin2\"}")
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.message", containsString("enabled, username")));
        assertThat(users.existsByEmailIgnoreCase("m@example.edu")).isFalse();
    }

    @Test
    void badOrMissingValuesAreRefused() throws Exception {
        signup("{\"name\":\"No Password\",\"email\":\"np@example.edu\"}").andExpect(status().isBadRequest());
        signup("{\"name\":\"Bad\",\"email\":\"not-an-email\",\"password\":\"secret-1\"}").andExpect(status().isBadRequest());
        signup("{\"name\":\"Short\",\"email\":\"s@example.edu\",\"password\":\"123\"}").andExpect(status().isBadRequest());
        signup("{\"name\":[\"x\"],\"email\":\"a@example.edu\",\"password\":\"secret-1\"}").andExpect(status().isBadRequest());

        signup("{\"name\":\"First\",\"email\":\"dup@example.edu\",\"password\":\"secret-1\"}").andExpect(status().isCreated());
        signup("{\"name\":\"Second\",\"email\":\"DUP@example.edu\",\"password\":\"secret-2\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("An account with this email already exists"));
    }

    @Test
    void adminRightsComeFromTheDatabaseOnEveryRequest() throws Exception {
        // a second admin, logged in
        postAs(admin(), "/api/users",
                "{\"username\":\"boss2\",\"password\":\"boss2-pw\",\"fullName\":\"Boss Two\",\"role\":\"ADMIN\"}")
                .andExpect(status().isCreated());
        User boss = users.findByUsername("boss2").orElseThrow();
        String token = JsonPath.read(login("boss2", "boss2-pw").andReturn().getResponse().getContentAsString(), "$.token");
        getAs(token, "/api/users").andExpect(status().isOk());

        // demoted to staff: the same token stops working for admin pages at once
        putAs(admin(), "/api/users/" + boss.getId(), "{\"fullName\":\"Boss Two\",\"role\":\"STAFF\",\"enabled\":true}")
                .andExpect(status().isOk());
        getAs(token, "/api/users").andExpect(status().isForbidden());
        postAs(token, "/api/expenses/1/approve", "{}").andExpect(status().isForbidden());
    }
}
