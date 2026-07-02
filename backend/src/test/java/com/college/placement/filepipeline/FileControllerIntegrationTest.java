package com.college.placement.filepipeline;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.shared.filepipeline.repository.FileScanRecordRepository;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(classes = com.college.placement.Application.class)
@AutoConfigureMockMvc
@ActiveProfiles("test")
class FileControllerIntegrationTest {

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepository;
    @Autowired RefreshTokenRepository refreshTokenRepository;
    @Autowired FileScanRecordRepository fileScanRecordRepository;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String STUDENT_EMAIL  = "filestudent@test.com";
    private static final String ADMIN_EMAIL    = "fileadmin@test.com";
    private static final String PASSWORD       = "password123";
    private static final String JSON           = MediaType.APPLICATION_JSON_VALUE;

    // Minimal valid PDF: starts with the %PDF magic bytes.
    private static final byte[] VALID_PDF_BYTES = {
        0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34  // "%PDF-1.4"
    };
    // Minimal valid PNG: .PNG magic header.
    private static final byte[] VALID_PNG_BYTES = {
        (byte)0x89, 0x50, 0x4E, 0x47, 0x0D, 0x0A, 0x1A, 0x0A
    };

    @BeforeEach
    void cleanDb() {
        fileScanRecordRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    @AfterEach
    void cleanDbAfter() {
        fileScanRecordRepository.deleteAll();
        refreshTokenRepository.deleteAll();
        userRepository.deleteAll();
    }

    // ── Upload — success ──────────────────────────────────────────────────────

    @Test
    void upload_validPdf_returns201WithMetadata() throws Exception {
        String token = getToken(STUDENT_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "resume.pdf",
                                "application/pdf", VALID_PDF_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.id").isNotEmpty())
                .andExpect(jsonPath("$.filename").value("resume.pdf"))
                .andExpect(jsonPath("$.contentType").value("application/pdf"))
                .andExpect(jsonPath("$.sha256Hash").isNotEmpty())
                .andExpect(jsonPath("$.scanStatus").value("CLEAN"))
                .andExpect(jsonPath("$.quarantined").value(false));
    }

    @Test
    void upload_validPng_returns201() throws Exception {
        String token = getToken(STUDENT_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "photo.png",
                                "image/png", VALID_PNG_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scanStatus").value("CLEAN"));
    }

    @Test
    void upload_adminRole_returns201() throws Exception {
        String token = getToken(ADMIN_EMAIL, Role.ROLE_ADMIN);

        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "doc.pdf",
                                "application/pdf", VALID_PDF_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated());
    }

    // ── Upload — rejected ─────────────────────────────────────────────────────

    @Test
    void upload_invalidMimeType_returns415() throws Exception {
        String token = getToken(STUDENT_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "malware.exe",
                                "application/x-msdownload", new byte[]{0x4D, 0x5A}))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnsupportedMediaType())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));
    }

    @Test
    void upload_pdfMimeButWrongMagicBytes_returns415() throws Exception {
        String token = getToken(STUDENT_EMAIL, Role.ROLE_STUDENT);

        // Sends application/pdf but with PNG magic bytes.
        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "trick.pdf",
                                "application/pdf", VALID_PNG_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnsupportedMediaType());
    }

    @Test
    void upload_unauthenticated_returns401() throws Exception {
        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "doc.pdf",
                                "application/pdf", VALID_PDF_BYTES)))
                .andExpect(status().isUnauthorized());
    }

    // ── Download ──────────────────────────────────────────────────────────────

    @Test
    void download_existingCleanFile_returns200WithBody() throws Exception {
        String token = getToken(STUDENT_EMAIL, Role.ROLE_STUDENT);
        UUID id = uploadAndGetId(token, VALID_PDF_BYTES, "application/pdf", "file.pdf");

        mvc.perform(get("/api/files/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk())
                .andExpect(content().contentType("application/pdf"));
    }

    @Test
    void download_nonExistentId_returns404() throws Exception {
        String token = getToken(STUDENT_EMAIL, Role.ROLE_STUDENT);

        mvc.perform(get("/api/files/{id}", UUID.randomUUID())
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isNotFound());
    }

    @Test
    void download_unauthenticated_returns401() throws Exception {
        mvc.perform(get("/api/files/{id}", UUID.randomUUID()))
                .andExpect(status().isUnauthorized());
    }

    // ── Delete ────────────────────────────────────────────────────────────────

    @Test
    void delete_asAdmin_returns204AndRemovesRecord() throws Exception {
        String adminToken = getToken(ADMIN_EMAIL, Role.ROLE_ADMIN);
        UUID id = uploadAndGetId(adminToken, VALID_PDF_BYTES, "application/pdf", "todelete.pdf");

        mvc.perform(delete("/api/files/{id}", id)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isNoContent());

        assertThat(fileScanRecordRepository.findById(id)).isEmpty();
    }

    @Test
    void delete_asStudent_returns403() throws Exception {
        String studentToken = getToken(STUDENT_EMAIL, Role.ROLE_STUDENT);
        UUID id = uploadAndGetId(studentToken, VALID_PDF_BYTES, "application/pdf", "doc.pdf");

        mvc.perform(delete("/api/files/{id}", id)
                        .header("Authorization", "Bearer " + studentToken))
                .andExpect(status().isForbidden());
    }

    // ── SHA-256 deduplication signal ──────────────────────────────────────────

    @Test
    void upload_sameFileTwice_producesIdenticalHash() throws Exception {
        String token = getToken(STUDENT_EMAIL, Role.ROLE_STUDENT);

        MvcResult r1 = mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "a.pdf", "application/pdf", VALID_PDF_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn();

        MvcResult r2 = mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "b.pdf", "application/pdf", VALID_PDF_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn();

        String hash1 = mapper.readTree(r1.getResponse().getContentAsString()).get("sha256Hash").asText();
        String hash2 = mapper.readTree(r2.getResponse().getContentAsString()).get("sha256Hash").asText();
        assertThat(hash1).isEqualTo(hash2);
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getToken(String email, Role role) throws Exception {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setEmailVerified(true);
        userRepository.save(user);

        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class)
                .accessToken();
    }

    private UUID uploadAndGetId(String token, byte[] content, String mimeType, String filename)
            throws Exception {
        MvcResult result = mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", filename, mimeType, content))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andReturn();

        return UUID.fromString(
                mapper.readTree(result.getResponse().getContentAsString())
                        .get("id").asText());
    }
}
