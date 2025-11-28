package spring.memewikibe.api.controller;

import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

/**
 * Web controller for serving Thymeleaf-based HTML pages.
 * <p>
 * This controller handles requests for static web pages, primarily serving
 * the landing page that provides navigation to various features of the Meme Wiki application.
 */
@Controller
public class WebController {

    /**
     * Serves the main landing page.
     * <p>
     * Returns the index template which displays a welcome page with links to
     * key features including the meme list, quiz functionality, and API documentation.
     *
     * @return the name of the Thymeleaf template to render ("index")
     */
    @GetMapping("/")
    public String index() {
        return "index";
    }
}