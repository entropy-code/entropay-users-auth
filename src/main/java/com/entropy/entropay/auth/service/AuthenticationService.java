package com.entropy.entropay.auth.service;

public interface AuthenticationService {
    void authenticate(String username, String password) throws Exception;
}
