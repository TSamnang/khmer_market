package com.example.khmer_market.payment;

public class PaymentResult {

    private final boolean success;
    private final String transactionId;
    private final String qrPayload;
    private final String qrImageBase64;
    private final String instructions;

    public PaymentResult(boolean success, String transactionId, String qrPayload, String qrImageBase64, String instructions) {
        this.success = success;
        this.transactionId = transactionId;
        this.qrPayload = qrPayload;
        this.qrImageBase64 = qrImageBase64;
        this.instructions = instructions;
    }

    public static PaymentResult success(String transactionId, String instructions) {
        return new PaymentResult(true, transactionId, null, null, instructions);
    }

    public static PaymentResult qr(String transactionId, String qrPayload, String qrImageBase64, String instructions) {
        return new PaymentResult(true, transactionId, qrPayload, qrImageBase64, instructions);
    }

    public boolean isSuccess() { return success; }
    public String getTransactionId() { return transactionId; }
    public String getQrPayload() { return qrPayload; }
    public String getQrImageBase64() { return qrImageBase64; }
    public String getInstructions() { return instructions; }
}
