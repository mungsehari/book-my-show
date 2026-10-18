package com.bookmyshow;

import org.springframework.boot.SpringApplication;
import org.springframework.boot.autoconfigure.SpringBootApplication;

/** Starts the app. All startup setup is loaded by {@link com.bookmyshow.bootstrap.Bootstrap}. */
@SpringBootApplication
public class BookMyShowApplication {

    public static void main(String[] args) {
        SpringApplication.run(BookMyShowApplication.class, args);
    }
}
