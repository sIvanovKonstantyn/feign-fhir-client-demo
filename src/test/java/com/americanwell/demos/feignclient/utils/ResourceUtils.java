package com.americanwell.demos.feignclient.utils;

import org.springframework.core.io.ClassPathResource;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.stream.Collectors;

public class ResourceUtils {

    public static String readFromClasspath(String path) {
        try {
            return Files.lines(
                    Path.of(
                            new ClassPathResource(path).getFile().getPath()
                    )
            ).collect(Collectors.joining());
        } catch (IOException e) {
            throw new RuntimeException(e);
        }
    }
}
