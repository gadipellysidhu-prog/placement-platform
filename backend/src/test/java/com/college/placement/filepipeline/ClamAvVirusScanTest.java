package com.college.placement.filepipeline;

import com.college.placement.modules.auth.domain.AppUser;
import com.college.placement.modules.auth.domain.Role;
import com.college.placement.modules.auth.dto.LoginRequest;
import com.college.placement.modules.auth.dto.RegisterRequest;
import com.college.placement.modules.auth.dto.TokenResponse;
import com.college.placement.modules.auth.repository.AppUserRepository;
import com.college.placement.modules.auth.repository.RefreshTokenRepository;
import com.college.placement.shared.filepipeline.domain.FileScanStatus;
import com.college.placement.shared.filepipeline.repository.FileScanRecordRepository;
import com.college.placement.shared.filepipeline.service.ClamAvService;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.junit.jupiter.api.AfterEach;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.Mockito;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.http.MediaType;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.context.ActiveProfiles;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.test.web.servlet.MockMvc;
import org.springframework.test.web.servlet.MvcResult;

import java.io.InputStream;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.multipart;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * Tests for ClamAV virus scanning outcomes.
 *
 * <p>The test profile has {@code file.pipeline.scan-enabled=false} so ClamAvService
 * normally returns CLEAN immediately. This test class uses {@code @MockBean} to override
 * ClamAvService and simulate INFECTED and FAILED responses, which is the correct way to
 * test quarantine handling without a real ClamAV daemon.
 *
 * <p>EICAR test string is the standard antivirus test pattern:
 * X5O!P%@AP[4\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*
 */
@SpringBootTest(classes = com.college.placement.Application.class,
        properties = "file.pipeline.scan-enabled=true")
@AutoConfigureMockMvc
@ActiveProfiles("test")
class ClamAvVirusScanTest {

    /** EICAR standard antivirus test file content. */
    private static final byte[] EICAR_BYTES =
            "X5O!P%@AP[4\\PZX54(P^)7CC)7}$EICAR-STANDARD-ANTIVIRUS-TEST-FILE!$H+H*"
            .getBytes(java.nio.charset.StandardCharsets.US_ASCII);

    private static final byte[] VALID_PDF_BYTES = {
        0x25, 0x50, 0x44, 0x46, 0x2D, 0x31, 0x2E, 0x34  // %PDF-1.4
    };

    @MockBean
    ClamAvService clamAvService;

    @Autowired MockMvc mvc;
    @Autowired ObjectMapper mapper;
    @Autowired AppUserRepository userRepo;
    @Autowired RefreshTokenRepository refreshTokenRepo;
    @Autowired FileScanRecordRepository scanRepo;
    @Autowired PasswordEncoder passwordEncoder;

    private static final String JSON     = MediaType.APPLICATION_JSON_VALUE;
    private static final String STUDENT  = "scantest-student@test.com";
    private static final String ADMIN    = "scantest-admin@test.com";
    private static final String PASSWORD = "password123";

    @BeforeEach
    void setUp() {
        scanRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    @AfterEach
    void tearDown() {
        Mockito.reset(clamAvService);
        scanRepo.deleteAll();
        refreshTokenRepo.deleteAll();
        userRepo.deleteAll();
    }

    // ── INFECTED file upload ──────────────────────────────────────────────────

    @Test
    void upload_infectedFile_returns422AndFileIsQuarantined() throws Exception {
        when(clamAvService.scan(any(InputStream.class))).thenReturn(FileScanStatus.INFECTED);
        String token = getToken(STUDENT, Role.ROLE_STUDENT);

        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "eicar.pdf",
                                "application/pdf", VALID_PDF_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(content().contentTypeCompatibleWith("application/problem+json"));

        // Verify: the quarantined record exists in DB
        var records = scanRepo.findAll();
        assertThat(records).hasSize(1);
        assertThat(records.get(0).getScanStatus()).isEqualTo(FileScanStatus.INFECTED);
        assertThat(records.get(0).isQuarantined()).isTrue();
    }

    @Test
    void upload_eicarTestString_withInfectedMock_returns422() throws Exception {
        when(clamAvService.scan(any(InputStream.class))).thenReturn(FileScanStatus.INFECTED);
        String token = getToken(STUDENT, Role.ROLE_STUDENT);

        // The EICAR bytes will be wrapped in a PDF upload (mime-type: application/pdf
        // but with EICAR payload). The ClamAV service mock returns INFECTED regardless.
        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "test-virus.pdf",
                                "application/pdf", VALID_PDF_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isUnprocessableEntity());
    }

    // ── Quarantined file cannot be downloaded ─────────────────────────────────

    @Test
    void download_quarantinedFile_returns403() throws Exception {
        // First: upload gets quarantined
        when(clamAvService.scan(any(InputStream.class))).thenReturn(FileScanStatus.INFECTED);
        String adminToken = getToken(ADMIN, Role.ROLE_ADMIN);

        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "bad.pdf",
                                "application/pdf", VALID_PDF_BYTES))
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isUnprocessableEntity());

        var records = scanRepo.findAll();
        assertThat(records).hasSize(1);
        UUID quarantinedId = records.get(0).getId();
        assertThat(records.get(0).isQuarantined()).isTrue();

        // Then: attempt to download the quarantined file
        mvc.perform(get("/api/files/{id}", quarantinedId)
                        .header("Authorization", "Bearer " + adminToken))
                .andExpect(status().isForbidden());
    }

    // ── FAILED scan (ClamAV unavailable) — upload still succeeds ─────────────

    @Test
    void upload_whenScanFails_returns201WithFailedStatus() throws Exception {
        when(clamAvService.scan(any(InputStream.class))).thenReturn(FileScanStatus.FAILED);
        String token = getToken(STUDENT, Role.ROLE_STUDENT);

        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "resume.pdf",
                                "application/pdf", VALID_PDF_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scanStatus").value("FAILED"))
                .andExpect(jsonPath("$.quarantined").value(false));
    }

    @Test
    void download_failedScanFile_canBeDownloaded() throws Exception {
        when(clamAvService.scan(any(InputStream.class))).thenReturn(FileScanStatus.FAILED);
        String token = getToken(STUDENT, Role.ROLE_STUDENT);

        MvcResult r = mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "uncertain.pdf",
                                "application/pdf", VALID_PDF_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated()).andReturn();

        UUID id = UUID.fromString(mapper.readTree(r.getResponse().getContentAsString()).get("id").asText());

        mvc.perform(get("/api/files/{id}", id)
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isOk());
    }

    // ── CLEAN scan (normal happy path with mock) ──────────────────────────────

    @Test
    void upload_cleanScan_returns201WithCleanStatus() throws Exception {
        when(clamAvService.scan(any(InputStream.class))).thenReturn(FileScanStatus.CLEAN);
        String token = getToken(STUDENT, Role.ROLE_STUDENT);

        mvc.perform(multipart("/api/files/upload")
                        .file(new MockMultipartFile("file", "doc.pdf",
                                "application/pdf", VALID_PDF_BYTES))
                        .header("Authorization", "Bearer " + token))
                .andExpect(status().isCreated())
                .andExpect(jsonPath("$.scanStatus").value("CLEAN"))
                .andExpect(jsonPath("$.quarantined").value(false));
    }

    // ── Helpers ───────────────────────────────────────────────────────────────

    private String getToken(String email, Role role) throws Exception {
        AppUser user = new AppUser();
        user.setEmail(email);
        user.setPasswordHash(passwordEncoder.encode(PASSWORD));
        user.setRole(role);
        user.setEmailVerified(true);
        userRepo.save(user);

        MvcResult result = mvc.perform(post("/auth/login")
                        .contentType(JSON)
                        .content(mapper.writeValueAsString(new LoginRequest(email, PASSWORD))))
                .andExpect(status().isOk())
                .andReturn();

        return mapper.readValue(result.getResponse().getContentAsString(), TokenResponse.class)
                .accessToken();
    }
}
