package com.pgmanagement.service;



import com.pgmanagement.constants.BillingConstants;
import com.pgmanagement.entity.PG;
import com.pgmanagement.entity.Room;
import com.pgmanagement.enums.BillType;
import com.pgmanagement.repository.BillRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.LocalDate;

@Service
@RequiredArgsConstructor
@Slf4j
public class BillNumberGeneratorService {

    private final BillRepository billRepository;

    /**
     * Generate unique bill number
     * Format: PREFIX-ROOM-YYYY-MM-ID
     * Examples:
     * - MB-101-2026-02-1 (Monthly Bill)
     * - SD-101-2026-02-1 (Security Deposit)
     * - FS-101-2026-02-1 (Final Settlement)
     */
    public String generateBillNumber(PG pg, Room room, BillType billType, LocalDate date) {
        String prefix = getBillTypePrefix(billType);
        String pgPrefix = sanitizeName(pg.getName());
        String roomNumber = room.getRoomNumber();
        int year = date.getYear();
        int month = date.getMonthValue();

        // Generate base bill number
        String baseBillNumber = String.format("%s-%s-%s-%d-%02d",
                prefix, pgPrefix, roomNumber, year, month);

        // Find next available ID
        int sequence = 1;
        String billNumber;

        do {
            billNumber = String.format("%s-%d", baseBillNumber, sequence);
            sequence++;
        } while (billRepository.existsByBillNumber(billNumber));

        log.info("Generated bill number: {}", billNumber);
        return billNumber;
    }

    private String getBillTypePrefix(BillType billType) {
        return switch (billType) {
            case MONTHLY -> BillingConstants.MONTHLY_BILL_PREFIX;
            case SECURITY_DEPOSIT -> BillingConstants.SECURITY_DEPOSIT_PREFIX;
            case FINAL_SETTLEMENT -> BillingConstants.FINAL_SETTLEMENT_PREFIX;
        };
    }

    private String sanitizeName(String name) {
        // Remove special characters, take first 3 letters, uppercase
        return name.replaceAll("[^a-zA-Z]", "")
                .substring(0, Math.min(3, name.length()))
                .toUpperCase();
    }
}

