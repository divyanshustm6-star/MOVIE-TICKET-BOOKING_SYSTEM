package com.movie.moviebooking.config;

import io.swagger.v3.oas.models.OpenAPI;
import io.swagger.v3.oas.models.info.Info;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
public class OpenApiConfig {
    @Bean
    public OpenAPI movieBookingOpenApi() {
        return new OpenAPI()
                .info(new Info()
                        .title("Movie Booking API")
                        .version("1.0.0")
                        .description("REST APIs for movies, users, bookings, theaters, shows, and payments."));
    }
}
