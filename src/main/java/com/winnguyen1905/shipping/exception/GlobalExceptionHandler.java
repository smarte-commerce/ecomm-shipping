package com.winnguyen1905.shipping.exception;

import org.springframework.http.HttpHeaders;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.http.converter.HttpMessageNotReadableException;
import org.springframework.validation.FieldError;
import org.springframework.web.bind.MethodArgumentNotValidException;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;
import org.springframework.web.context.request.WebRequest;
import org.springframework.web.servlet.mvc.method.annotation.ResponseEntityExceptionHandler;

import java.util.HashMap;
import java.util.Map;
import java.util.stream.Collectors;

@ControllerAdvice
public class GlobalExceptionHandler extends ResponseEntityExceptionHandler {

    @ExceptionHandler(BaseException.class)
    public ResponseEntity<ErrorVm> handleBaseException(BaseException ex) {
        ErrorVm errorVm = new ErrorVm(
                String.valueOf(ex.getCode() != null ? ex.getCode() : HttpStatus.BAD_REQUEST.value()),
                ex.getMessage(),
                ex.getError() != null ? ex.getError().toString() : "An error occurred");
        return new ResponseEntity<>(errorVm, HttpStatus.valueOf(Integer.parseInt(errorVm.statusCode())));
    }

    @ExceptionHandler(ResourceNotFoundException.class)
    public ResponseEntity<ErrorVm> handleResourceNotFoundException(ResourceNotFoundException ex) {
        ErrorVm errorVm = new ErrorVm(
                String.valueOf(HttpStatus.NOT_FOUND.value()),
                "Resource not found",
                ex.getMessage());
        return new ResponseEntity<>(errorVm, HttpStatus.NOT_FOUND);
    }

    @ExceptionHandler(ResourceAlreadyExistsException.class)
    public ResponseEntity<ErrorVm> handleResourceAlreadyExistsException(ResourceAlreadyExistsException ex) {
        ErrorVm errorVm = new ErrorVm(
                String.valueOf(HttpStatus.CONFLICT.value()),
                "Resource already exists",
                ex.getMessage());
        return new ResponseEntity<>(errorVm, HttpStatus.CONFLICT);
    }

    @ExceptionHandler(BusinessLogicException.class)
    public ResponseEntity<ErrorVm> handleBusinessLogicException(BusinessLogicException ex) {
        ErrorVm errorVm = new ErrorVm(
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "Business logic error",
                ex.getMessage());
        return new ResponseEntity<>(errorVm, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(S3FileException.class)
    public ResponseEntity<ErrorVm> handleS3FileException(S3FileException ex) {
        ErrorVm errorVm = new ErrorVm(
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "File operation error",
                ex.getMessage());
        return new ResponseEntity<>(errorVm, HttpStatus.INTERNAL_SERVER_ERROR);
    }

    // @Override
    protected ResponseEntity<Object> handleMethodArgumentNotValid(
            MethodArgumentNotValidException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {

        Map<String, String> fieldErrors = ex.getBindingResult()
                .getFieldErrors()
                .stream()
                .collect(Collectors.toMap(
                        FieldError::getField,
                        fieldError -> fieldError.getDefaultMessage() != null ? fieldError.getDefaultMessage() : ""));

        ErrorVm errorVm = new ErrorVm(
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "Validation failed",
                "Invalid request content",
                fieldErrors.values().stream().toList());

        return new ResponseEntity<>(errorVm, HttpStatus.BAD_REQUEST);
    }

    // @Override
    protected ResponseEntity<Object> handleHttpMessageNotReadable(
            HttpMessageNotReadableException ex,
            HttpHeaders headers,
            HttpStatus status,
            WebRequest request) {

        ErrorVm errorVm = new ErrorVm(
                String.valueOf(HttpStatus.BAD_REQUEST.value()),
                "Malformed JSON request",
                ex.getMostSpecificCause().getMessage());
        return new ResponseEntity<>(errorVm, HttpStatus.BAD_REQUEST);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<ErrorVm> handleAllUncaughtException(Exception ex) {
        ErrorVm errorVm = new ErrorVm(
                String.valueOf(HttpStatus.INTERNAL_SERVER_ERROR.value()),
                "An unexpected error occurred",
                ex.getMessage());
        return new ResponseEntity<>(errorVm, HttpStatus.INTERNAL_SERVER_ERROR);
    }
}
