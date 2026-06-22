package com.college.placement.shared.filepipeline.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FileStorageException extends ResponseStatusException {

    public FileStorageException(String reason) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason);
    }

    public FileStorageException(String reason, Throwable cause) {
        super(HttpStatus.INTERNAL_SERVER_ERROR, reason, cause);
    }
}
