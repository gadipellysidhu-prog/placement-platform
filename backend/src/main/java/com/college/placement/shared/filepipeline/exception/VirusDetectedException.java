package com.college.placement.shared.filepipeline.exception;

import org.springframework.http.HttpStatus;
import org.springframework.web.server.ResponseStatusException;

public class VirusDetectedException extends ResponseStatusException {

    public VirusDetectedException(String filename) {
        super(HttpStatus.UNPROCESSABLE_ENTITY,
              "Malicious content detected in file '%s'. Upload rejected and file quarantined."
                  .formatted(filename));
    }
}
