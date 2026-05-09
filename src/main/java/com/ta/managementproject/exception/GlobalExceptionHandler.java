package com.ta.managementproject.exception;

import com.ta.managementproject.service.UtilService;
import org.springframework.http.HttpStatus;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.ControllerAdvice;
import org.springframework.web.bind.annotation.ExceptionHandler;


@ControllerAdvice
public class GlobalExceptionHandler {

    private final UtilService utilService;

    public GlobalExceptionHandler(UtilService utilService) {
        this.utilService = utilService;
    }

    @ExceptionHandler(NotFoundException.class)
    public ResponseEntity<?> handleNotFound(NotFoundException e) {
        return utilService.buildResponse(HttpStatus.NOT_FOUND, e.getMessage(), null);
    }

    @ExceptionHandler(ForbiddenException.class)
    public ResponseEntity<?> handleForbidden(ForbiddenException e) {
        return utilService.buildResponse(HttpStatus.FORBIDDEN, e.getMessage(), null);
    }

    @ExceptionHandler(BadRequestException.class)
    public ResponseEntity<?> handleBadRequest(BadRequestException e) {
        return utilService.buildResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    @ExceptionHandler(UnprocessableContentException.class)
    public ResponseEntity<?> handleUnprocessableContent(UnprocessableContentException e) {
        return utilService.buildResponse(HttpStatus.BAD_REQUEST, e.getMessage(), null);
    }

    @ExceptionHandler(ConflictException.class)
    public ResponseEntity<?> handleConflict(ConflictException e) {
        return utilService.buildResponse(HttpStatus.CONFLICT, e.getMessage(), null);
    }

    @ExceptionHandler(Exception.class)
    public ResponseEntity<?> handleGeneral(Exception e) {
        return utilService.buildResponse(HttpStatus.INTERNAL_SERVER_ERROR, e.getMessage(), null);
    }
}