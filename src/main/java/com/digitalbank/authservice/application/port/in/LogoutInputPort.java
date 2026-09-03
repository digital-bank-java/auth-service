package com.digitalbank.authservice.application.port.in;

public interface LogoutInputPort {

    void logout(LogoutCommand command);
}
