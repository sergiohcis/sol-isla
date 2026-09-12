package com.hosannasolutions.solisla.common.storage;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.awt.image.BufferedImage;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import javax.imageio.ImageIO;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.io.TempDir;
import org.springframework.mock.web.MockMultipartFile;

/** No Spring context needed — plain unit test of the file-signature validation added for Phase 8
 *  hardening (design doc §51: never trust the client-declared content-type). */
class LocalFileStorageServiceTest {

    @TempDir
    private java.nio.file.Path tempDir;

    private LocalFileStorageService service() {
        return new LocalFileStorageService(tempDir.toString());
    }

    @Test
    void acceptsARealPng() throws IOException {
        BufferedImage image = new BufferedImage(2, 2, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        ImageIO.write(image, "png", out);
        MockMultipartFile file = new MockMultipartFile("file", "photo.png", "image/png", out.toByteArray());

        assertDoesNotThrow(() -> service().store("products/test", file));
    }

    @Test
    void acceptsAWebpByItsRiffContainerSignature() {
        // ImageIO has no WebP decoder in a stock JDK, so store() validates the RIFF/WEBP
        // container header rather than decoding pixels — this only needs a correct header,
        // not a fully valid codec payload.
        byte[] bytes = new byte[20];
        System.arraycopy("RIFF".getBytes(), 0, bytes, 0, 4);
        System.arraycopy("WEBP".getBytes(), 0, bytes, 8, 4);
        MockMultipartFile file = new MockMultipartFile("file", "photo.webp", "image/webp", bytes);

        assertDoesNotThrow(() -> service().store("products/test", file));
    }

    @Test
    void rejectsATextFileSpoofingAnImageContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.jpg", "image/jpeg", "this is not an image".getBytes());

        assertThrows(FileStorageException.class, () -> service().store("products/test", file));
    }

    @Test
    void rejectsAWebpWithoutTheRiffSignature() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "fake.webp", "image/webp", "this is not a webp".getBytes());

        assertThrows(FileStorageException.class, () -> service().store("products/test", file));
    }

    @Test
    void rejectsAnUnsupportedContentType() {
        MockMultipartFile file = new MockMultipartFile(
                "file", "doc.pdf", "application/pdf", "%PDF-1.4".getBytes());

        assertThrows(FileStorageException.class, () -> service().store("products/test", file));
    }
}
