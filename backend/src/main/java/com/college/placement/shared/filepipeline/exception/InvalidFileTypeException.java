package com.college.placement.shared.filepipeline.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class InvalidFileTypeException extends ResponseStatusException {

    public InvalidFileTypeException(String contentType) {
        super(HttpStatus.UNSUPPORTED_MEDIA_TYPE,
              "File type '%s' is not permitted. Allowed types: application/pdf, image/png, image/jpeg."
                  .formatted(contentType));
    }
}
