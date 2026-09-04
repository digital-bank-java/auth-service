package com.digitalbank.authservice.adapter.out.session;

import com.digitalbank.authservice.application.port.out.SessionRepository;
import com.digitalbank.authservice.application.port.out.SingleSessionPolicy;
import com.digitalbank.authservice.domain.model.Session;
import com.digitalbank.authservice.domain.model.SessionId;
import com.digitalbank.authservice.domain.model.SessionStatus;
import java.util.Objects;
import java.util.Optional;
import org.springframework.stereotype.Repository;
import org.springframework.transaction.annotation.Transactional;

@Repository
class PostgresSessionRepository implements SessionRepository {

    private final SpringDataSessionRepository repository;

    PostgresSessionRepository(SpringDataSessionRepository repository) {
        this.repository = repository;
    }

    @Override
    @Transactional
    public Session open(Session session, SingleSessionPolicy policy) {
        Objects.requireNonNull(session, "session must not be null");
        Objects.requireNonNull(policy, "policy must not be null");
        if (policy == SingleSessionPolicy.REVOKE_PREVIOUS) {
            repository.acquireUsernameLock(session.username());
            repository.revokeActiveByUsername(
                    session.username(), session.createdAt(), SessionStatus.ACTIVE, SessionStatus.REVOKED);
        }
        return SessionJpaMapper.toDomain(repository.saveAndFlush(SessionJpaMapper.toEntity(session)));
    }

    @Override
    @Transactional(readOnly = true)
    public Optional<Session> findById(SessionId sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        return repository.findById(sessionId.value()).map(SessionJpaMapper::toDomain);
    }

    @Override
    @Transactional
    public void revoke(SessionId sessionId) {
        Objects.requireNonNull(sessionId, "sessionId must not be null");
        repository.revokeIfActive(sessionId.value(), SessionStatus.ACTIVE, SessionStatus.REVOKED);
    }
}
