package com.campuserp;

import com.jayway.jsonpath.JsonPath;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Logs in as the seeded test accounts (passwords from src/test/resources/application.properties). */
public final class TestLogins {

    private TestLogins() {}

    public static String admin(MockMvc mvc) throws Exception {
        return token(mvc, "admin", "admin-test-pw");
    }

    public static String staff(MockMvc mvc) throws Exception {
        return token(mvc, "staff", "staff-test-pw");
    }

    public static String viewer(MockMvc mvc) throws Exception {
        return token(mvc, "viewer", "viewer-test-pw");
    }

    public static String token(MockMvc mvc, String username, String password) throws Exception {
        String body = mvc.perform(post("/api/auth/login")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{\"username\":\"%s\",\"password\":\"%s\"}".formatted(username, password)))
                .andExpect(status().isOk())
                .andReturn().getResponse().getContentAsString();
        return JsonPath.read(body, "$.token");
    }

    public static String bearer(String token) {
        return "Bearer " + token;
    }
}
