package com.college.placement.filepipeline;

import com.college.placement.shared.filepipeline.config.FilePipelineProperties;
import com.college.placement.shared.filepipeline.exception.FileStorageException;
import com.college.placement.shared.filepipeline.service.FileStorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.core.io.Resource;
import org.springframework.http.HttpStatus;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.nio.file.Path;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

class FileStorageServiceTest {

    @TempDir
    Path tempDir;

    private FileStorageService service;

    @BeforeEach
    void setUp() throws Exception {
        FilePipelineProperties props = new FilePipelineProperties();
        props.setUploadDir(tempDir.toString());
        service = new FileStorageService(props);
        service.init(); // invoke @PostConstruct manually in unit test
    }

    // ── Store ─────────────────────────────────────────────────────────────────

    @Test
    void store_validFile_returnsStorageKey() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "test.pdf", "application/pdf", new byte[]{1, 2, 3});
        String key = service.store(file);
        assertThat(key).isNotBlank().hasSize(36); // UUID length
    }

    @Test
    void store_validFile_createsPhysicalFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "resume.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44});
        String key = service.store(file);
        assertThat(tempDir.resolve(key)).exists();
    }

    // ── Load ──────────────────────────────────────────────────────────────────

    @Test
    void load_existingFile_returnsReadableResource() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", new byte[]{0x25, 0x50, 0x44, 0x46});
        String key = service.store(file);
        Resource resource = service.load(key);
        assertThat(resource.exists()).isTrue();
        assertThat(resource.isReadable()).isTrue();
    }

    @Test
    void load_nonExistentKey_throws404() {
        assertThatThrownBy(() -> service.load("non-existent-uuid"))
                .isInstanceOf(ResponseStatusException.class)
                .satisfies(ex -> assertThat(((ResponseStatusException) ex).getStatusCode())
                        .isEqualTo(HttpStatus.NOT_FOUND));
    }

    @Test
    void load_pathTraversalAttempt_throwsFileStorageException() {
        assertThatThrownBy(() -> service.load("../../etc/passwd"))
                .isInstanceOf(FileStorageException.class);
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_existingFile_removesPhysicalFile() throws IOException {
        MockMultipartFile file = new MockMultipartFile(
                "file", "old.pdf", "application/pdf", new byte[]{1});
        String key = service.store(file);
        assertThat(tempDir.resolve(key)).exists();

        service.delete(key);
        assertThat(tempDir.resolve(key)).doesNotExist();
    }

    @Test
    void delete_nonExistentKey_doesNotThrow() {
        // deleteIfExists must succeed silently for missing files.
        org.assertj.core.api.Assertions.assertThatNoException()
                .isThrownBy(() -> service.delete("does-not-exist"));
    }
}
