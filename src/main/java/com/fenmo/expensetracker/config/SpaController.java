package com.fenmo.expensetracker.config;

import org.springframework.core.io.FileSystemResource;
import org.springframework.core.io.Resource;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.web.bind.annotation.GetMapping;

import java.nio.file.Files;
import java.nio.file.Path;

@Controller
public class SpaController {
    @GetMapping(value = {"/", "/**/{path:[^.]*}"}, produces = MediaType.TEXT_HTML_VALUE)
    public ResponseEntity<Resource> index() {
        Path indexPath = Path.of(System.getProperty("user.dir"), "dist", "index.html");
        if (Files.exists(indexPath)) {
            return ResponseEntity.ok(new FileSystemResource(indexPath));
        }
        return ResponseEntity.notFound().build();
    }
}
