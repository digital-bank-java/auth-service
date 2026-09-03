package com.digitalbank.authservice.configuration;

import org.springframework.boot.context.properties.ConfigurationProperties;

@ConfigurationProperties(prefix = "auth.identity.fixture")
public record FixtureIdentityProperties(String username, String passwordHash) {}
