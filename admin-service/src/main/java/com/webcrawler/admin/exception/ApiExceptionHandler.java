package com.webcrawler.admin.exception;

import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.bind.annotation.RestControllerAdvice;

import org.springframework.data.redis.RedisConnectionFailureException;

import java.util.Map;

@RestControllerAdvice
public class ApiExceptionHandler {

        @ExceptionHandler(MethodArgumentNotValidException.class)
        public ResponseEntity<Map<String, String>> handleValidation(
                        MethodArgumentNotValidException ex) {

                String message = ex.getBindingResult()
                                .getFieldErrors()
                                .stream()
                                .findFirst()
                                .map(error -> error.getDefaultMessage())
                                .orElse("Invalid request");

                return ResponseEntity
                                .badRequest()
                                .body(Map.of("error", message));
        }

        @ExceptionHandler(IllegalArgumentException.class)
        public ResponseEntity<Map<String, String>> handleBadRequest(
                        IllegalArgumentException ex) {

                return ResponseEntity
                                .badRequest()
                                .body(Map.of(
                                                "error",
                                                ex.getMessage() == null
                                                                ? "Invalid request"
                                                                : ex.getMessage()));
        }

        @ExceptionHandler(RedisConnectionFailureException.class)
        public ResponseEntity<Map<String, String>> handleRedisFailure(
                        RedisConnectionFailureException ex) {

                return ResponseEntity
                                .status(HttpStatus.SERVICE_UNAVAILABLE)
                                .body(Map.of(
                                                "error",
                                                "Redis is unavailable. Please try again later."));
        }
}