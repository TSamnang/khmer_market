package com.example.khmer_market.payment.khqr;

import com.example.khmer_market.domain.entity.Order;
import com.example.khmer_market.domain.enums.PaymentMethod;
import com.example.khmer_market.payment.PaymentProvider;
import com.example.khmer_market.payment.PaymentResult;
import com.google.zxing.BarcodeFormat;
import com.google.zxing.client.j2se.MatrixToImageWriter;
import com.google.zxing.common.BitMatrix;
import com.google.zxing.qrcode.QRCodeWriter;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.io.ByteArrayOutputStream;
import java.util.Base64;
import java.util.UUID;

@Component
public class KhqrPaymentProvider implements PaymentProvider {

    private static final Logger log = LoggerFactory.getLogger(KhqrPaymentProvider.class);

    @Override
    public PaymentMethod getMethod() {
        return PaymentMethod.KHQR;
    }

    @Override
    public PaymentResult initiatePayment(Order order) {
        String txId = "KHQR-" + UUID.randomUUID().toString().substring(0, 8).toUpperCase();

        // Standard Bakong KHQR Mock Payload (EMVCo representation)
        String qrPayload = String.format("https://bakong.nbc.org.kh/qr?acc=khmermarket@aba&cur=USD&amt=%.2f&orderId=%s&tx=%s",
                order.getTotalAmount(), order.getOrderNumber(), txId);

        String base64Image = generateQrCodeBase64(qrPayload, 260, 260);

        String instructions = "Scan the KHQR code using Bakong, ABA Mobile, or any Cambodian bank app to complete payment.";
        return PaymentResult.qr(txId, qrPayload, base64Image, instructions);
    }

    public String generateQrCodeBase64(String text, int width, int height) {
        try {
            QRCodeWriter qrCodeWriter = new QRCodeWriter();
            BitMatrix bitMatrix = qrCodeWriter.encode(text, BarcodeFormat.QR_CODE, width, height);
            ByteArrayOutputStream outputStream = new ByteArrayOutputStream();
            MatrixToImageWriter.writeToStream(bitMatrix, "PNG", outputStream);
            return "data:image/png;base64," + Base64.getEncoder().encodeToString(outputStream.toByteArray());
        } catch (Exception e) {
            log.error("Failed to generate ZXing QR code image", e);
            return null;
        }
    }
}
