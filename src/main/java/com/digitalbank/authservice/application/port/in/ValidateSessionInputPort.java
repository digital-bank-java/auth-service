package com.digitalbank.authservice.application.port.in;

public interface ValidateSessionInputPort {

    SessionValidationResult validate(ValidateSessionCommand command);
}
