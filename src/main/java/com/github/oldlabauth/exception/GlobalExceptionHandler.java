package com.github.oldlabauth.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import java.time.Instant;

@RestControllerAdvice
public class GlobalExceptionHandler {

    @ExceptionHandler(UserAlreadyExistsException.class)
    public ResponseEntity<ExceptionApi> handleUserAlreadyExists(UserAlreadyExistsException ex) {
        return ResponseEntity
                .status(HttpStatus.CONFLICT)
                .body(new ExceptionApi(Instant.now(), "USER_ALREADY_EXISTS", ex.getMessage()));
    }

    @ExceptionHandler(UserNotFoundException.class)
    public ResponseEntity<ExceptionApi> handleUserNotFound(UserNotFoundException ex) {
        return ResponseEntity
                .status(HttpStatus.NOT_FOUND)
                .body(new ExceptionApi(Instant.now(), "USER_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(InvalidOtpException.class)
    public ResponseEntity<ExceptionApi> handleInvalidOtp(InvalidOtpException ex) {
        return ResponseEntity
                .status(HttpStatus.BAD_REQUEST)
                .body(new ExceptionApi(Instant.now(), "INVALID_OTP", ex.getMessage()));
    }

    @ExceptionHandler(NoSuchAlgorithmException.class)
    public ResponseEntity<ExceptionApi> handleNoSuchAlgorithm(NoSuchAlgorithmException ex) {
        return ResponseEntity
                .status(HttpStatus.INTERNAL_SERVER_ERROR)
                .body(new ExceptionApi(Instant.now(), "ALGORITHM_NOT_FOUND", ex.getMessage()));
    }

    @ExceptionHandler(AccountNotActivatedException.class)
    public ResponseEntity<ExceptionApi> handleAccountNotActivated(AccountNotActivatedException ex) {
        return ResponseEntity
                .status(HttpStatus.UNAUTHORIZED)
                .body(new ExceptionApi(Instant.now(), "ACCOUNT_NOT_ACTIVATED", ex.getMessage()));
    }

    @ExceptionHandler(AccountBlockedException.class)
    public ResponseEntity<ExceptionApi> handleAccountBlocked(AccountBlockedException ex) {
        return ResponseEntity
                .status(HttpStatus.FORBIDDEN)
                .body(new ExceptionApi(Instant.now(), "ACCOUNT_BLOCKED", ex.getMessage()));
    }
}
