package com.digitalbank.authservice.application.port.out;

import com.digitalbank.authservice.domain.model.Session;
import com.digitalbank.authservice.domain.model.SessionId;
import java.util.Optional;

public interface SessionRepository {

    Session open(Session session, SingleSessionPolicy policy);

    Optional<Session> findById(SessionId sessionId);

    void revoke(SessionId sessionId);
}
