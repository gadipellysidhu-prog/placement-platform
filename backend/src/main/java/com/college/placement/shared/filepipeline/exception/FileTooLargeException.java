package com.college.placement.shared.filepipeline.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class FileTooLargeException extends ResponseStatusException {

    public FileTooLargeException(long actualBytes, long maxBytes) {
        super(HttpStatus.PAYLOAD_TOO_LARGE,
              "File size %d bytes exceeds the maximum allowed size of %d bytes."
                  .formatted(actualBytes, maxBytes));
    }
}
