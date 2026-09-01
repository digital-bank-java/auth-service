package com.digitalbank.authservice.adapter.out.session;

import com.digitalbank.authservice.application.port.out.SessionRepository;
import com.digitalbank.authservice.application.port.out.SingleSessionPolicy;
import com.digitalbank.authservice.domain.model.Session;
import com.digitalbank.authservice.domain.model.SessionId;
import java.util.HashMap;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;

public class InMemorySessionRepository implements SessionRepository {

    private final Map<SessionId, Session> sessions = new HashMap<>();

    @Override
    public synchronized Session open(Session session, SingleSessionPolicy policy) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        if (policy == SingleSessionPolicy.REVOKE_PREVIOUS) {
            sessions.replaceAll((id, existing) -> {
                if (existing.username().equals(session.username()) && existing.isActiveAt(session.createdAt())) {
                    return existing.revoke();
                }
                return existing;
            });
        }
        sessions.put(session.id(), session);
        return session;
    }

    @Override
    public synchronized Optional<Session> findById(SessionId sessionId) {
        return Optional.ofNullable(sessions.get(sessionId));
    }

    @Override
    public synchronized void revoke(SessionId sessionId) {
        var session = sessions.get(sessionId);
        if (session != null) {
            sessions.put(sessionId, session.revoke());
        }
    }
}
