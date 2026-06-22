package com.college.placement.shared.filepipeline.service;

import com.college.placement.shared.filepipeline.config.FilePipelineProperties;
import com.college.placement.shared.filepipeline.domain.FileScanStatus;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.net.ConnectException;
import java.net.InetSocketAddress;
import java.net.Socket;
import java.net.SocketTimeoutException;
import java.nio.charset.StandardCharsets;

/**
 * Virus-scans file content using the ClamAV daemon INSTREAM protocol (RFC-like TCP protocol).
 * <p>
 * Protocol: connect → send {@code zINSTREAM\0} → stream file in length-prefixed chunks →
 * send 4-byte zero terminator → read single-line response.
 * <p>
 * When the scanner is unavailable the upload is allowed but recorded as {@code FAILED}
 * so an operator can re-scan. When scanning is disabled via configuration, files are
 * marked {@code CLEAN} immediately.
 */
@Slf4j
@Service
public class ClamAvService {

    // Use the 'n' (newline-terminated) prefix so ClamAV sends a newline-terminated response,
    // which BufferedReader.readLine() can parse. The 'z' (null-terminated) prefix causes
    // ClamAV to respond with \0 instead of \n, making readLine() accumulate the null byte
    // and fail the endsWith("OK") check on every scan.
    private static final byte[] INSTREAM_COMMAND =
            "nINSTREAM\n".getBytes(StandardCharsets.US_ASCII);
    private static final int CHUNK_SIZE = 2048;
    private static final byte[] ZERO_TERMINATOR = {0, 0, 0, 0};

    private final FilePipelineProperties props;

    public ClamAvService(FilePipelineProperties props) {
        this.props = props;
    }

    public FileScanStatus scan(InputStream fileContent) {
        if (!props.isScanEnabled()) {
            log.debug("ClamAV scanning disabled; skipping scan and marking CLEAN");
            return FileScanStatus.CLEAN;
        }

        try (Socket socket = new Socket()) {
            socket.connect(
                    new InetSocketAddress(props.getClamAvHost(), props.getClamAvPort()),
                    props.getClamAvTimeoutMs());
            socket.setSoTimeout(props.getClamAvTimeoutMs());

            OutputStream out = socket.getOutputStream();
            out.write(INSTREAM_COMMAND);

            // Stream file content in fixed-size chunks with 4-byte big-endian length prefix.
            byte[] buffer = new byte[CHUNK_SIZE];
            int read;
            while ((read = fileContent.read(buffer)) > 0) {
                writeChunkHeader(out, read);
                out.write(buffer, 0, read);
            }

            // Signal end of stream.
            out.write(ZERO_TERMINATOR);
            out.flush();

            String response = new BufferedReader(
                    new InputStreamReader(socket.getInputStream(), StandardCharsets.US_ASCII))
                    .readLine();

            return interpretResponse(response);

        } catch (ConnectException ex) {
            log.warn("ClamAV unavailable at {}:{} — file will be marked FAILED",
                    props.getClamAvHost(), props.getClamAvPort());
            return FileScanStatus.FAILED;
        } catch (SocketTimeoutException ex) {
            log.warn("ClamAV scan timed out after {}ms — file will be marked FAILED",
                    props.getClamAvTimeoutMs());
            return FileScanStatus.FAILED;
        } catch (IOException ex) {
            log.error("ClamAV scan error — file will be marked FAILED", ex);
            return FileScanStatus.FAILED;
        }
    }

    private FileScanStatus interpretResponse(String response) {
        if (response == null) {
            log.warn("ClamAV returned null response");
            return FileScanStatus.FAILED;
        }
        if (response.endsWith("OK")) {
            log.debug("ClamAV scan result: CLEAN");
            return FileScanStatus.CLEAN;
        }
        if (response.contains("FOUND")) {
            log.warn("ClamAV scan result: INFECTED — {}", response);
            return FileScanStatus.INFECTED;
        }
        log.error("ClamAV unexpected response: {}", response);
        return FileScanStatus.FAILED;
    }

    // Writes the 4-byte big-endian chunk length required by the INSTREAM protocol.
    private void writeChunkHeader(OutputStream out, int length) throws IOException {
        out.write((length >>> 24) & 0xFF);
        out.write((length >>> 16) & 0xFF);
        out.write((length >>> 8)  & 0xFF);
        out.write(length          & 0xFF);
    }
}
