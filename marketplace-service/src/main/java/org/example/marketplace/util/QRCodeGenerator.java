package org.example.marketplace.util;

import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;

@Component
public class QRCodeGenerator {

    @Value("${app.public.trace.base-url:https://acm.vietfuture.vn}")
    private String baseUrl;

    public String generateTraceUrl(Long productId, String slug) {
        return baseUrl + "/trace/" + (slug != null ? slug : productId);
    }

    public byte[] generateQRImage(String content, int width) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix matrix = qrCodeWriter.encode(content, BarcodeFormat.QR_CODE, width, width);
            ByteArrayOutputStream out = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(matrix, "PNG", out);
            return out.toByteArray();
        } catch (Exception e) {
            throw new RuntimeException("Failed to generate QR code image", e);
        }
    }
}
