package project.controller;

import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.bind.annotation.RequestMapping;
@RestController
@RequestMapping("/api/test")
public class TestController {
    @GetMapping("/test")
    public String test() {
        return "Hello from Spring Boot!";
    }
    @GetMapping("/hello")
    public String protectedEndpoint() {
        return "If you see this, authentication works!";
    }
}
