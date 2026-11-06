package com.campuserp;

import com.jayway.jsonpath.JsonPath;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.ResultActions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.put;

/** Shared helpers: call the API as admin / staff / viewer. */
public abstract class ApiTest {

    @Autowired
    protected MockMvc mvc;

    @Autowired
    protected Fixtures fx;

    private String admin;
    private String staff;
    private String viewer;

    protected String admin() throws Exception {
        if (admin == null) admin = TestLogins.admin(mvc);
        return admin;
    }

    protected String staff() throws Exception {
        if (staff == null) staff = TestLogins.staff(mvc);
        return staff;
    }

    protected String viewer() throws Exception {
        if (viewer == null) viewer = TestLogins.viewer(mvc);
        return viewer;
    }

    protected ResultActions getAs(String token, String url) throws Exception {
        return mvc.perform(get(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    protected ResultActions postAs(String token, String url, String json) throws Exception {
        return mvc.perform(post(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions putAs(String token, String url, String json) throws Exception {
        return mvc.perform(put(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token)
                .contentType(MediaType.APPLICATION_JSON).content(json));
    }

    protected ResultActions deleteAs(String token, String url) throws Exception {
        return mvc.perform(delete(url).header(HttpHeaders.AUTHORIZATION, "Bearer " + token));
    }

    protected static Long id(ResultActions r, String path) throws Exception {
        Number n = JsonPath.read(r.andReturn().getResponse().getContentAsString(), path);
        return n.longValue();
    }
}
