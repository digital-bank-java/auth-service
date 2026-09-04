package com.digitalbank.authservice.configuration;

import com.digitalbank.authservice.adapter.out.identity.FixtureCredentialStore;
import com.digitalbank.authservice.adapter.out.security.BCryptPasswordHasher;
import com.digitalbank.authservice.adapter.out.security.JjwtTokenAdapter;
import com.digitalbank.authservice.application.port.out.CredentialStore;
import com.digitalbank.authservice.application.port.out.JwtTokenPort;
import com.digitalbank.authservice.application.port.out.PasswordHasher;
import java.time.Clock;
import org.springframework.boot.context.properties.EnableConfigurationProperties;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;

@Configuration
@EnableConfigurationProperties({AuthSessionProperties.class, AuthJwtProperties.class, FixtureIdentityProperties.class})
public class AuthConfiguration {

    @Bean
    Clock authClock() {
        return Clock.systemUTC();
    }

    @Bean
    PasswordHasher passwordHasher() {
        return new BCryptPasswordHasher();
    }

    @Bean
    JwtTokenPort jwtTokenPort(AuthJwtProperties properties) {
        return new JjwtTokenAdapter(properties);
    }

    @Bean
    CredentialStore credentialStore(FixtureIdentityProperties properties) {
        return new FixtureCredentialStore(properties);
    }
}
