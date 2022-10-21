package com.entropyteam.entropay.users.auth.common.exceptions;

import org.springframework.http.HttpStatus;
import org.springframework.web.bind.annotation.ResponseStatus;

@ResponseStatus(HttpStatus.BAD_REQUEST)
public class InvalidRequestParametersException extends RuntimeException {

    public InvalidRequestParametersException(String message) {
        super(message);
    }

}
