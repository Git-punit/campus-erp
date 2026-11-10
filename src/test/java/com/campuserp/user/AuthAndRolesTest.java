package com.campuserp.user;

import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.transaction.annotation.Transactional;

import static com.campuserp.TestLogins.admin;
import static com.campuserp.TestLogins.bearer;
import static com.campuserp.TestLogins.staff;
import static com.campuserp.TestLogins.viewer;
import static org.hamcrest.Matchers.hasItems;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class AuthAndRolesTest {

    @Autowired
    MockMvc mvc;

    @Autowired
    UserRepository users;

    // --- login ---

    @Test
    void loginReturnsTokenAndUser() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"admin-test-pw\"}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.token").isString())
                .andExpect(jsonPath("$.user.role").value("ADMIN"))
                .andExpect(jsonPath("$.user.passwordHash").doesNotExist());
    }

    @Test
    void wrongPasswordIs401() throws Exception {
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"admin\",\"password\":\"nope\"}"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.message").value("Wrong username or password"));
    }

    @Test
    void badTokenIs401() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, "Bearer not-a-real-token"))
                .andExpect(status().isUnauthorized())
                .andExpect(jsonPath("$.status").value(401));
    }

    @Test
    void meReturnsLoggedInUser() throws Exception {
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(viewer(mvc))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.username").value("viewer"))
                .andExpect(jsonPath("$.role").value("VIEWER"));
    }

    @Test
    void disabledUserCannotLogIn() throws Exception {
        User staff = users.findByUsername("staff").orElseThrow();
        staff.setEnabled(false);
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"staff\",\"password\":\"staff-test-pw\"}"))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("This account isn't active. An admin has to approve or re-enable it."));
        // a wrong password still gets the usual answer, so it doesn't show the account exists
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"staff\",\"password\":\"guess\"}"))
                .andExpect(status().isUnauthorized());
    }

    // --- role rules ---

    @Test
    void onlyAdminCanSeeUsers() throws Exception {
        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(admin(mvc))))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$[*].username", hasItems("admin", "staff", "viewer")));
        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(staff(mvc))))
                .andExpect(status().isForbidden());
        mvc.perform(get("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(viewer(mvc))))
                .andExpect(status().isForbidden())
                .andExpect(jsonPath("$.message").value("You don't have permission to do that"));
    }

    @Test
    void viewerCannotWriteButStaffCan() throws Exception {
        // No module endpoints exist yet, so a made-up path shows the rule:
        // viewer is stopped by security (403), staff gets through to "no such endpoint" (404).
        mvc.perform(post("/api/anything").header(HttpHeaders.AUTHORIZATION, bearer(viewer(mvc))))
                .andExpect(status().isForbidden());
        mvc.perform(post("/api/anything").header(HttpHeaders.AUTHORIZATION, bearer(staff(mvc))))
                .andExpect(status().isNotFound());
        mvc.perform(get("/api/anything").header(HttpHeaders.AUTHORIZATION, bearer(viewer(mvc))))
                .andExpect(status().isNotFound());
    }

    // --- managing users ---

    @Test
    void adminCanCreateEditAndDeleteUser() throws Exception {
        String token = admin(mvc);
        String created = mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"clerk1\",\"password\":\"secret1\",\"fullName\":\"Clerk One\",\"role\":\"STAFF\"}"))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.role").value("STAFF"))
                .andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(created, "$.id");

        mvc.perform(put("/api/users/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Clerk 1\",\"role\":\"VIEWER\",\"enabled\":true}"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Clerk 1"))
                .andExpect(jsonPath("$.role").value("VIEWER"));

        mvc.perform(delete("/api/users/" + id).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
    }

    @Test
    void duplicateUsernameIs409() throws Exception {
        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(admin(mvc)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"staff\",\"password\":\"secret1\",\"fullName\":\"X\",\"role\":\"STAFF\"}"))
                .andExpect(status().isConflict());
    }

    @Test
    void invalidUserGivesFieldErrors() throws Exception {
        mvc.perform(post("/api/users").header(HttpHeaders.AUTHORIZATION, bearer(admin(mvc)))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"a b\",\"password\":\"123\",\"fullName\":\"\"}"))
                .andExpect(status().isBadRequest())
                .andExpect(jsonPath("$.fieldErrors.username").exists())
                .andExpect(jsonPath("$.fieldErrors.password").exists())
                .andExpect(jsonPath("$.fieldErrors.fullName").exists())
                .andExpect(jsonPath("$.fieldErrors.role").exists());
    }

    // --- last admin safeguard ---

    @Test
    void lastAdminCannotBeDeletedDemotedOrDisabled() throws Exception {
        String token = admin(mvc);
        Long adminId = users.findByUsername("admin").orElseThrow().getId();

        mvc.perform(delete("/api/users/" + adminId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Can't delete the last admin. Make another user admin first."));

        mvc.perform(put("/api/users/" + adminId).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Administrator\",\"role\":\"STAFF\",\"enabled\":true}"))
                .andExpect(status().isConflict());

        mvc.perform(put("/api/users/" + adminId).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Administrator\",\"role\":\"ADMIN\",\"enabled\":false}"))
                .andExpect(status().isConflict());
    }

    @Test
    void adminCanBeDeletedWhenAnotherAdminExists() throws Exception {
        String token = admin(mvc);
        Long staffId = users.findByUsername("staff").orElseThrow().getId();
        mvc.perform(put("/api/users/" + staffId).header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"fullName\":\"Office Staff\",\"role\":\"ADMIN\",\"enabled\":true}"))
                .andExpect(status().isOk());

        Long adminId = users.findByUsername("admin").orElseThrow().getId();
        mvc.perform(delete("/api/users/" + adminId).header(HttpHeaders.AUTHORIZATION, bearer(token)))
                .andExpect(status().isNoContent());
    }

    // --- change own password ---

    @Test
    void anyoneCanChangeTheirOwnPassword() throws Exception {
        String token = viewer(mvc);
        mvc.perform(post("/api/auth/password").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"wrong\",\"newPassword\":\"brand-new-pw\"}"))
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Your current password is wrong"));
        mvc.perform(post("/api/auth/password").header(HttpHeaders.AUTHORIZATION, bearer(token))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"viewer-test-pw\",\"newPassword\":\"brand-new-pw\"}"))
                .andExpect(status().isOk());

        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"viewer\",\"password\":\"viewer-test-pw\"}"))
                .andExpect(status().isUnauthorized());
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"viewer\",\"password\":\"brand-new-pw\"}"))
                .andExpect(status().isOk());
    }

    @Test
    void changingThePasswordLogsOutOldTokens() throws Exception {
        String old = viewer(mvc);
        String body = mvc.perform(post("/api/auth/password").header(HttpHeaders.AUTHORIZATION, bearer(old))
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"currentPassword\":\"viewer-test-pw\",\"newPassword\":\"another-pw-1\"}"))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String fresh = com.jayway.jsonpath.JsonPath.read(body, "$.token");

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(old)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(fresh)))
                .andExpect(status().isOk());
    }

    @Test
    void tooManyWrongPasswordsLockTheUsername() throws Exception {
        // a username no other test uses: the lock lives in memory for the whole test run
        String wrong = "{\"username\":\"locked-out-test\",\"password\":\"guess\"}";
        for (int i = 0; i < 5; i++) {
            mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrong))
                    .andExpect(status().isUnauthorized());
        }
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON).content(wrong))
                .andExpect(status().isTooManyRequests())
                .andExpect(jsonPath("$.message").value("Too many wrong passwords. Try again in 15 minutes."));
    }

    @Test
    void logOutOthersEndsEveryOtherSessionButKeepsThisOne() throws Exception {
        String laptop = staff(mvc);
        String phone = staff(mvc);
        String body = mvc.perform(post("/api/auth/logout-others").header(HttpHeaders.AUTHORIZATION, bearer(laptop)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        String fresh = com.jayway.jsonpath.JsonPath.read(body, "$.token");

        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(phone)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(laptop)))
                .andExpect(status().isUnauthorized());
        mvc.perform(get("/api/auth/me").header(HttpHeaders.AUTHORIZATION, bearer(fresh)))
                .andExpect(status().isOk());
        // the password still works for a new login
        mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"staff\",\"password\":\"staff-test-pw\"}"))
                .andExpect(status().isOk());
    }
}
