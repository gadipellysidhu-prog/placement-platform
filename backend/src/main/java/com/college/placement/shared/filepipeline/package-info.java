/**
 * File-security pipeline: magic-byte detection, MIME whitelist, SHA-256 hashing, ClamAV
 * scanning, and S3 upload (SADD 6, MOP 17). Every uploaded file is treated as untrusted.
 */
package com.college.placement.shared.filepipeline;
