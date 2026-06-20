package com.college.placement.shared.filepipeline.domain;

import com.college.placement.shared.audit.Auditable;
import jakarta.persistence.*;
import jakarta.validation.constraints.NotBlank;
import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Size;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

import java.util.UUID;

@Entity
@Table(
    name = "file_scan_records",
    uniqueConstraints = @UniqueConstraint(name = "uq_file_scan_records_storage_key", columnNames = "storage_key"),
    indexes = {
        @Index(name = "idx_file_scan_records_scan_status", columnList = "scan_status"),
        @Index(name = "idx_file_scan_records_sha256",      columnList = "sha256_hash")
    }
)
@Getter
@Setter
@NoArgsConstructor
public class FileScanRecord extends Auditable {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    @NotBlank
    @Size(max = 500)
    @Column(nullable = false, length = 500)
    private String filename;

    @NotBlank
    @Size(max = 500)
    @Column(name = "storage_key", nullable = false, unique = true, length = 500)
    private String storageKey;

    @Size(max = 255)
    @Column(name = "content_type", length = 255)
    private String contentType;

    @Column(name = "size_bytes")
    private Long sizeBytes;

    @Size(max = 64)
    @Column(name = "sha256_hash", length = 64)
    private String sha256Hash;

    @NotNull
    @Enumerated(EnumType.STRING)
    @Column(name = "scan_status", nullable = false, length = 50)
    private FileScanStatus scanStatus = FileScanStatus.PENDING;

    @Column(nullable = false)
    private boolean quarantined = false;

    @Size(max = 255)
    @Column(name = "uploaded_by", length = 255)
    private String uploadedBy;

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FileScanRecord f)) return false;
        return id != null && id.equals(f.id);
    }

    @Override
    public int hashCode() {
        return id == null ? 0 : id.hashCode();
    }
}
