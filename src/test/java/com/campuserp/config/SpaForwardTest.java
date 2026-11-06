package com.campuserp.config;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.test.web.servlet.MockMvc;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.forwardedUrl;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Refreshing a React page in the single-jar build should load the app, not a 404. */
@SpringBootTest
@AutoConfigureMockMvc
class SpaForwardTest {

    @Autowired
    MockMvc mvc;

    @Test
    void appPagesGoToIndexHtml() throws Exception {
        for (String page : new String[] {"/dashboard", "/faculty", "/faculty/3", "/training/2", "/login"}) {
            mvc.perform(get(page)).andExpect(forwardedUrl("/index.html"));
        }
    }

    @Test
    void apiAndFilesAreLeftAlone() throws Exception {
        mvc.perform(get("/api/health")).andExpect(status().isOk()).andExpect(jsonPath("$.status").value("ok"));
        mvc.perform(get("/api/nope")).andExpect(status().isUnauthorized());
        mvc.perform(get("/assets/index-abc.js")).andExpect(forwardedUrl(null));
        mvc.perform(get("/favicon.ico")).andExpect(forwardedUrl(null));
    }
}
