package com.digitalbank.authservice.adapter.in.web;

import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.Size;

record LoginRequest(
        @NotBlank @Size(max = 254) String username,
        @NotBlank @Size(min = 12, max = 128) String password) {}
