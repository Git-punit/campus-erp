package com.campuserp.user;

import com.campuserp.ApiTest;
import com.jayway.jsonpath.JsonPath;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.webmvc.test.autoconfigure.AutoConfigureMockMvc;
import org.springframework.http.HttpHeaders;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.web.servlet.ResultActions;
import org.springframework.transaction.annotation.Transactional;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
@Transactional
class ProfileTest extends ApiTest {

    // the first 8 bytes of every PNG file, enough for the type check
    static final byte[] PNG = {(byte) 0x89, 'P', 'N', 'G', 0x0D, 0x0A, 0x1A, 0x0A, 1, 2, 3};

    ResultActions uploadPhoto(String token, byte[] data) throws Exception {
        return mvc.perform(multipart("/api/auth/photo")
                .file(new MockMultipartFile("file", "me.png", "image/png", data))
                .header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    @Test
    void anyoneCanEditTheirOwnDetails() throws Exception {
        putAs(viewer(), "/api/auth/me", """
                {"fullName":"  Reena Viewer ","email":"reena@example.edu","phone":"+91 98765 43210",
                 "designation":"Accounts Officer, Finance"}""")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.fullName").value("Reena Viewer"))
                .andExpect(jsonPath("$.email").value("reena@example.edu"))
                .andExpect(jsonPath("$.phone").value("+91 98765 43210"))
                .andExpect(jsonPath("$.designation").value("Accounts Officer, Finance"))
                .andExpect(jsonPath("$.role").value("VIEWER"));
        getAs(viewer(), "/api/auth/me").andExpect(jsonPath("$.email").value("reena@example.edu"));

        // blank optional fields are cleared; bad values are refused
        putAs(viewer(), "/api/auth/me", "{\"fullName\":\"Reena\",\"email\":\"\",\"phone\":\"\"}")
                .andExpect(jsonPath("$.email").doesNotExist());
        putAs(viewer(), "/api/auth/me", "{\"fullName\":\"Reena\",\"email\":\"not-an-email\"}")
                .andExpect(status().isBadRequest());
        putAs(viewer(), "/api/auth/me", "{\"fullName\":\"Reena\",\"phone\":\"call me\"}")
                .andExpect(status().isBadRequest());
        putAs(viewer(), "/api/auth/me", "{\"fullName\":\" \"}").andExpect(status().isBadRequest());
    }

    @Test
    void usernameChangeNeedsThePasswordAndAFreeName() throws Exception {
        String token = staff();
        postAs(token, "/api/auth/username", "{\"newUsername\":\"office.staff\",\"currentPassword\":\"wrong\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Your current password is wrong"));
        postAs(token, "/api/auth/username", "{\"newUsername\":\"admin\",\"currentPassword\":\"staff-test-pw\"}")
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Username 'admin' is already taken"));
        postAs(token, "/api/auth/username", "{\"newUsername\":\"has space\",\"currentPassword\":\"staff-test-pw\"}")
                .andExpect(status().isBadRequest());

        String body = postAs(token, "/api/auth/username",
                "{\"newUsername\":\"office.staff\",\"currentPassword\":\"staff-test-pw\"}")
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.user.username").value("office.staff"))
                .andReturn().getResponse().getContentAsString();
        String fresh = JsonPath.read(body, "$.token");

        getAs(token, "/api/auth/me").andExpect(status().isUnauthorized());
        getAs(fresh, "/api/auth/me").andExpect(jsonPath("$.username").value("office.staff"));
        postAs(null, "/api/auth/login", "{\"username\":\"office.staff\",\"password\":\"staff-test-pw\"}")
                .andExpect(status().isOk());
    }

    @Test
    void photoCanBeUploadedShownAndRemoved() throws Exception {
        String token = viewer();
        uploadPhoto(token, PNG)
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.hasPhoto").value(true));
        mvc.perform(get("/api/auth/photo").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(header().string("Content-Type", "image/png"))
                .andExpect(content().bytes(PNG));

        // a PDF or a renamed text file is not a photo
        uploadPhoto(token, "%PDF-1.4".getBytes()).andExpect(status().isConflict());
        uploadPhoto(token, "<svg onload=alert(1)>".getBytes())
                .andExpect(status().isConflict())
                .andExpect(jsonPath("$.message").value("Only JPG and PNG photos can be used"));

        deleteAs(token, "/api/auth/photo").andExpect(status().isOk()).andExpect(jsonPath("$.hasPhoto").value(false));
        mvc.perform(get("/api/auth/photo").header(HttpHeaders.AUTHORIZATION, "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void adminCanDeleteAUserWhoHasAPhoto() throws Exception {
        String body = postAs(admin(), "/api/users",
                "{\"username\":\"temp\",\"password\":\"temp-pw-1\",\"fullName\":\"Temp\",\"role\":\"VIEWER\"}")
                .andExpect(status().isCreated()).andReturn().getResponse().getContentAsString();
        Integer id = JsonPath.read(body, "$.id");
        String temp = JsonPath.read(postAs(null, "/api/auth/login", "{\"username\":\"temp\",\"password\":\"temp-pw-1\"}")
                .andReturn().getResponse().getContentAsString(), "$.token");
        uploadPhoto(temp, PNG).andExpect(status().isOk());

        deleteAs(admin(), "/api/users/" + id).andExpect(status().isNoContent());
    }
}
