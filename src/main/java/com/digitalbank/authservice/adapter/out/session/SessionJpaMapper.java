package com.digitalbank.authservice.adapter.out.session;

import com.digitalbank.authservice.domain.model.Session;
import com.digitalbank.authservice.domain.model.SessionId;
import com.digitalbank.authservice.domain.model.SessionStatus;

final class SessionJpaMapper {

    private SessionJpaMapper() {}

    static SessionJpaEntity toEntity(Session session) {
        return new SessionJpaEntity(
                session.id().value(), session.username(), session.createdAt(), session.expiresAt(), session.status());
    }

    static Session toDomain(SessionJpaEntity entity) {
        var session = Session.start(
                new SessionId(entity.sessionId()), entity.username(), entity.createdAt(), entity.expiresAt());
        return entity.status() == SessionStatus.REVOKED ? session.revoke() : session;
    }
}
