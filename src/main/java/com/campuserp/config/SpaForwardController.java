package com.campuserp.config;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * In the single-jar build the React app lives in static/index.html and does its
 * own routing. Refreshing a page like /faculty/3, or opening a link to it, asks
 * the server for that path, so send those to index.html and let React take over.
 *
 * Only paths with no dot in the last segment are matched, so real files
 * (/assets/index-abc.js, /favicon.ico) are still served as files, and /api/**
 * is left alone so unknown API calls still get a JSON 404.
 */
@Controller
public class SpaForwardController {

    // The app's pages are one or two levels deep: /faculty, /faculty/3, /training/2
    @GetMapping({
            "/{page:^(?!api$)[^.]*$}",
            "/{page:^(?!api$)[^.]*$}/{id:[^.]*}"
    })
    public String forward() {
        return "forward:/index.html";
    }
}
