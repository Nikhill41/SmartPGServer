package com.pgmanagement.service;



import com.pgmanagement.constants.BillingConstants;
import com.pgmanagement.entity.Bill;
import com.pgmanagement.entity.PGBillingConfig;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.io.UnsupportedEncodingException;
import java.net.URLEncoder;
import java.nio.charset.StandardCharsets;

@Service
@RequiredArgsConstructor
@Slf4j
public class UpiPaymentLinkService {

    /**
     * Generate UPI payment link
     * Format: upi://pay?pa=UPI_ID&pn=NAME&am=AMOUNT&cu=INR&tn=BILL_NUMBER
     */
    public String generateUpiPaymentLink(Bill bill, PGBillingConfig config) {
        if (config.getUpiId() == null || config.getUpiId().isBlank()) {
            log.warn("UPI ID not configured for PG ID: {}", config.getPgId());
            return null;
        }

        try {
            String upiId = config.getUpiId();
            String merchantName = config.getUpiName() != null ? config.getUpiName() : "PG Owner";
            String amount = bill.getTotalAmount().toString();
            String transactionNote = bill.getBillNumber();

            String upiLink = String.format(
                    BillingConstants.UPI_PAYMENT_LINK_FORMAT,
                    upiId,
                    URLEncoder.encode(merchantName, StandardCharsets.UTF_8.toString()),
                    amount,
                    URLEncoder.encode(transactionNote, StandardCharsets.UTF_8.toString())
            );

            log.info("Generated UPI payment link for bill: {}", bill.getBillNumber());
            return upiLink;

        } catch (UnsupportedEncodingException e) {
            log.error("Failed to generate UPI payment link for bill: {}", bill.getBillNumber(), e);
            return null;
        }
    }
}

