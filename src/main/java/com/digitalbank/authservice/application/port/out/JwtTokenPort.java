package com.digitalbank.authservice.application.port.out;

import com.digitalbank.authservice.domain.model.Session;

public interface JwtTokenPort {

    String issue(String subject, Session session);

    JwtClaims verify(String token);
}
