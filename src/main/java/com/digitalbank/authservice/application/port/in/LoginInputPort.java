package com.digitalbank.authservice.application.port.in;

public interface LoginInputPort {

    LoginResult login(LoginCommand command);
}
