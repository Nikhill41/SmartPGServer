package com.pgmanagement.service;

import com.pgmanagement.enums.OtpPurpose;
import com.pgmanagement.exception.EmailSendException;
import com.pgmanagement.repository.OwnerRepository;
import com.pgmanagement.repository.RoomRepository;
import com.pgmanagement.repository.UserRepository;
import com.resend.Resend;
import com.resend.core.exception.ResendException;
import com.resend.services.emails.model.CreateEmailOptions;
import com.resend.services.emails.model.CreateEmailResponse;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.stereotype.Service;

import java.io.IOException;

@Service
@RequiredArgsConstructor
@Slf4j
public class EmailService {

    @Value("${sendgrid.api.key}")
    private String resendApiKey;

    @Value("${sendgrid.from.email}")
    private String fromEmail;

    @Value("${sendgrid.from.name}")
    private String fromName;

    private final OwnerRepository ownerRepository;
    private final UserRepository userRepository;     // ✅ NEW
    private final RoomRepository roomRepository;     // ✅ NEW



    // ==================== CORE SEND METHOD ====================

//    private void sendEmail(String toEmail, String subject, String htmlContent) {
//        try {
//            Email from = new Email(fromEmail, fromName);
//            Email to = new Email(toEmail);
//            Content content = new Content("text/html", htmlContent);
//            Mail mail = new Mail(from, subject, to, content);
//
//            SendGrid sg = new SendGrid(sendGridApiKey);
//            Request request = new Request();
//
//            request.setMethod(Method.POST);
//            request.setEndpoint("mail/send");
//            request.setBody(mail.build());
//
//            Response response = sg.api(request);
//
//            if (response.getStatusCode() >= 200 && response.getStatusCode() < 300) {
//                log.info("Email sent successfully to: {}", toEmail);
//            } else {
//                log.error("Failed to send email. Status: {}, Body: {}",
//                        response.getStatusCode(), response.getBody());
//                throw new EmailSendException("Failed to send email: " + response.getBody());
//            }
//
//        } catch (IOException e) {
//            log.error("Error sending email to: {}", toEmail, e);
//            throw new EmailSendException("Failed to send email", e);
//        }
//    }

    // ==================== CORE SEND METHOD ====================

    private void sendEmail(String toEmail, String subject, String htmlContent) {
        try {
            Resend resend=new Resend(resendApiKey);
            CreateEmailOptions params= CreateEmailOptions.builder()
                    .from(fromName + " <" + fromEmail + ">")
                    .to(toEmail)
                    .subject(subject)
                    .html(htmlContent)
                    .build();

            CreateEmailResponse data=resend.emails().send(params);
            log.info("Email sent successfully to: {} with subject: {}", toEmail, subject);

        } catch (ResendException e) {
            log.error("Failed to send email to {}", toEmail, e);
            throw new RuntimeException(e);
        }
    }

    // ==================== BILL FIELD RESOLVERS ====================
    // Bill only stores IDs (tenantId, roomId, pgId) — these helpers resolve the display values.

    private String resolveTenantEmail(com.pgmanagement.entity.Bill bill) {
        return userRepository.findById(bill.getTenantId())
                .map(com.pgmanagement.entity.User::getEmail)
                .orElse(null);
    }

    private String resolveTenantName(com.pgmanagement.entity.Bill bill) {
        return userRepository.findById(bill.getTenantId())
                .map(com.pgmanagement.entity.User::getName)
                .orElse("Tenant");
    }

    private String resolveRoomNumber(com.pgmanagement.entity.Bill bill) {
        return roomRepository.findById(bill.getRoomId())
                .map(com.pgmanagement.entity.Room::getRoomNumber)
                .orElse("-");
    }

    /**
     * Resolves PG name via the Room entity's pg relationship.
     * If Room uses a lazy-loaded Pg, ensure this is called within a transaction
     * (it will be, since EmailService is called from transactional services).
     */
    private String resolvePgName(com.pgmanagement.entity.Bill bill) {
        return roomRepository.findById(bill.getRoomId())
                .map(room -> {
                    try {
                        return room.getPg() != null ? room.getPg().getName() : "-";
                    } catch (Exception e) {
                        return "-";
                    }
                })
                .orElse("-");
    }

    // ==================== OTP EMAILS ====================

    public void sendOtpEmail(String toEmail, String otpCode, OtpPurpose purpose) {
        String subject;
        String htmlContent;

        switch (purpose) {
            case OWNER_REGISTRATION:
                subject = "Verify Your Owner Account - PG Management";
                htmlContent = buildOwnerRegistrationOtpEmail(otpCode);
                break;
            case USER_REGISTRATION:
                subject = "Verify Your Account - PG Management";
                htmlContent = buildUserRegistrationOtpEmail(otpCode);
                break;
            case RESET_PASSWORD:
                subject = "Reset Your Password - PG Management";
                htmlContent = buildPasswordResetOtpEmail(otpCode);
                break;
            case PASSWORD_SETUP:
                subject = "Set Up Your Password - PG Management";
                htmlContent = buildPasswordSetupOtpEmail(otpCode);
                break;
            default:
                subject = "Your OTP Code - PG Management";
                htmlContent = buildGenericOtpEmail(otpCode);
        }

        sendEmail(toEmail, subject, htmlContent);
    }

    // ==================== BOOKING EMAILS ====================

    public void sendBookingConfirmationEmail(String toEmail, String userName,
                                             String pgName, String roomNumber, String rent) {
        String subject = "Booking Confirmed - " + pgName;
        String htmlContent = buildBookingConfirmationEmail(userName, pgName, roomNumber, rent);
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendBookingRejectionEmail(String toEmail, String userName,
                                          String pgName, String roomNumber, String reason) {
        String subject = "Booking Request Update - " + pgName;
        String htmlContent = buildBookingRejectionEmail(userName, pgName, roomNumber, reason);
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendNewBookingNotificationToOwner(String toEmail, String ownerName,
                                                  String userName, String pgName, String roomNumber) {
        String subject = "New Booking Request - " + pgName;
        String htmlContent = buildNewBookingNotificationEmail(ownerName, userName, pgName, roomNumber);
        sendEmail(toEmail, subject, htmlContent);
    }

    public void sendTenantRemovalNotification(String toEmail, String tenantName, String pgName,
                                              String roomNumber, String removalReason, String stayDuration) {
        String subject = "Room Assignment Removed - " + pgName;
        String htmlContent = String.format("""
            <html>
            <body style="font-family: Arial, sans-serif; line-height: 1.6; color: #333;">
                <div style="max-width: 600px; margin: 0 auto; padding: 20px; border: 1px solid #ddd; border-radius: 10px;">
                    <h2 style="color: #d9534f;">Room Assignment Removed</h2>
                    <p>Dear %s,</p>
                    <p>Your room assignment at <strong>%s, Room %s</strong> has been removed by the owner.</p>
                    <div style="background-color: #f8f9fa; padding: 15px; border-left: 4px solid #d9534f; margin: 20px 0;">
                        <strong>Reason:</strong> %s
                    </div>
                    <div style="margin: 20px 0;">
                        <p><strong>Stay Duration:</strong> %s</p>
                    </div>
                    <p>If you have any questions or concerns, please contact the PG owner directly.</p>
                    <p>Thank you for staying with us.</p>
                    <hr style="border: none; border-top: 1px solid #ddd; margin: 20px 0;">
                    <p style="font-size: 12px; color: #666;">
                        This is an automated email from PG Management System.<br>
                        Please do not reply to this email.
                    </p>
                </div>
            </body>
            </html>
            """, tenantName, pgName, roomNumber, removalReason, stayDuration);
        sendEmail(toEmail, subject, htmlContent);
    }

    // ==================== BILLING EMAILS ====================

    /**
     * Send security deposit bill to tenant.
     * Tenant entity is passed directly — no resolver needed.
     */
    public void sendSecurityDepositBill(com.pgmanagement.entity.User tenant,
                                        com.pgmanagement.entity.Bill bill,
                                        com.pgmanagement.entity.PGBillingConfig config) {
        String subject = "Security Deposit Payment Required - " + bill.getBillNumber();

        String upiLink = config.getUpiId() != null
                ? String.format("upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s",
                config.getUpiId(),
                config.getUpiName() != null ? config.getUpiName() : "PG Owner",
                bill.getTotalAmount(),
                bill.getBillNumber())
                : null;

        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; background: #007bff; color: white; padding: 20px; border-radius: 8px; }
                    .amount-box { background: #f8f9fa; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0; }
                    .amount { font-size: 32px; font-weight: bold; color: #007bff; }
                    .payment-info { background: #e7f3ff; padding: 15px; border-radius: 8px; margin: 20px 0; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 30px; }
                    .button { background: #28a745; color: white; padding: 12px 30px; text-decoration: none; border-radius: 5px; display: inline-block; margin: 10px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <div class="header">
                        <h2>💰 Security Deposit Payment</h2>
                    </div>
                    <p>Dear %s,</p>
                    <p>Your booking has been confirmed! Please pay the security deposit to complete your check-in process.</p>
                    <div class="amount-box">
                        <p style="margin: 0; color: #666;">Security Deposit Amount</p>
                        <div class="amount">₹%s</div>
                        <p style="margin: 10px 0 0 0; color: #666;">Bill Number: <strong>%s</strong></p>
                    </div>
                    <div class="payment-info">
                        <h3 style="margin-top: 0;">📅 Payment Details</h3>
                        <p><strong>Due Date:</strong> %s</p>
                        <p><strong>Status:</strong> Pending</p>
                    </div>
                    %s
                    <p style="font-size: 12px; color: #666; margin-top: 20px;">
                        <strong>Note:</strong> This security deposit will be refunded at the time of checkout after deducting any applicable charges.
                    </p>
                    <div class="footer">
                        <p>© 2026 PG Management System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """,
                tenant.getName(),
                bill.getTotalAmount(),
                bill.getBillNumber(),
                bill.getDueDate(),
                upiLink != null
                        ? String.format("<p style=\"text-align: center;\"><a href=\"%s\" class=\"button\">Pay via UPI</a></p>", upiLink)
                        : "<p style=\"text-align: center; color: #666;\">Please contact owner for payment details.</p>"
        );

        sendEmail(tenant.getEmail(), subject, htmlContent);
    }

    /**
     * Send payment proof uploaded notification to owner.
     * Owner email resolved via ownerRepository using bill.getOwnerId().
     */
    public void sendPaymentProofUploadedNotification(com.pgmanagement.entity.Bill bill) {
        log.info("Sending payment proof uploaded notification for bill: {}", bill.getBillNumber());

        com.pgmanagement.entity.Owner owner = ownerRepository.findById(bill.getOwnerId()).orElse(null);
        if (owner == null || owner.getEmail() == null) {
            log.warn("Owner not found or has no email for ownerId: {} — skipping email", bill.getOwnerId());
            return;
        }

        String tenantName = resolveTenantName(bill);
        String roomNumber = resolveRoomNumber(bill);
        String pgName     = resolvePgName(bill);

        String subject = "🔔 Payment Proof Received - " + bill.getBillNumber();
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                <div style="max-width:600px; margin:0 auto; background:white; padding:30px; border-radius:10px;">
                    <h2 style="color: #007bff;">🔔 Payment Proof Uploaded</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>A tenant has submitted payment proof for the following bill. Please review and approve or reject it.</p>
                    <div style="background:#e7f3ff; padding:15px; border-radius:8px; margin:20px 0;">
                        <p style="margin:0;"><strong>Bill Number:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Tenant:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Room:</strong> %s | <strong>PG:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Amount:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Billing Period:</strong> %s to %s</p>
                    </div>
                    <p style="color:#856404; background:#fff3cd; padding:10px; border-radius:5px;">
                        ⏳ Please log in to your dashboard to verify within 24 hours.
                    </p>
                    <hr style="border:none; border-top:1px solid #eee; margin:20px 0;">
                    <p style="font-size:12px; color:#666;">© 2026 PG Management System. Do not reply to this email.</p>
                </div>
            </body>
            </html>
            """,
                owner.getName(),
                bill.getBillNumber(),
                tenantName,
                roomNumber,
                pgName,
                bill.getTotalAmount(),
                bill.getBillingPeriodStart(),
                bill.getBillingPeriodEnd()
        );

        sendEmail(owner.getEmail(), subject, htmlContent);
    }

    /**
     * Send payment confirmation to tenant.
     * Tenant email resolved via userRepository using bill.getTenantId().
     */
    public void sendPaymentConfirmation(com.pgmanagement.entity.Bill bill) {
        log.info("Sending payment confirmation for bill: {}", bill.getBillNumber());

        String tenantEmail = resolveTenantEmail(bill);
        if (tenantEmail == null) {
            log.warn("Tenant email not found for tenantId: {} — skipping email", bill.getTenantId());
            return;
        }

        String tenantName = resolveTenantName(bill);
        String roomNumber = resolveRoomNumber(bill);
        String pgName     = resolvePgName(bill);

        String subject = "✅ Payment Confirmed - " + bill.getBillNumber();
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                <div style="max-width:600px; margin:0 auto; background:white; padding:30px; border-radius:10px;">
                    <div style="background:#28a745; color:white; padding:20px; border-radius:8px; text-align:center;">
                        <h2>✅ Payment Confirmed!</h2>
                    </div>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your payment has been <strong>verified and confirmed</strong> by the owner.</p>
                    <div style="background:#d4edda; padding:15px; border-radius:8px; margin:20px 0; color:#155724;">
                        <p style="margin:0;"><strong>Bill Number:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Amount Paid:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Billing Period:</strong> %s to %s</p>
                        <p style="margin:8px 0 0;"><strong>Room:</strong> %s | <strong>PG:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Status:</strong> PAID ✅</p>
                    </div>
                    <p style="font-size:13px; color:#555;">
                        Please keep this email as your payment receipt for future reference.
                    </p>
                    <hr style="border:none; border-top:1px solid #eee; margin:20px 0;">
                    <p style="font-size:12px; color:#666;">© 2026 PG Management System. Do not reply to this email.</p>
                </div>
            </body>
            </html>
            """,
                tenantName,
                bill.getBillNumber(),
                bill.getTotalAmount(),
                bill.getBillingPeriodStart(),
                bill.getBillingPeriodEnd(),
                roomNumber,
                pgName
        );

        sendEmail(tenantEmail, subject, htmlContent);
    }

    /**
     * Send payment rejection notification to tenant.
     */
    public void sendPaymentRejection(com.pgmanagement.entity.Bill bill, String reason) {
        log.info("Sending payment rejection for bill: {} with reason: {}", bill.getBillNumber(), reason);

        String tenantEmail = resolveTenantEmail(bill);
        if (tenantEmail == null) {
            log.warn("Tenant email not found for tenantId: {} — skipping email", bill.getTenantId());
            return;
        }

        String tenantName = resolveTenantName(bill);

        // rejectionAttempts maps to DB column 'rejectionattempts' in the Bill entity.
        // If Bill.java doesn't have this field yet, add:
        //   @Column(name = "rejectionattempts")
        //   private Integer rejectionAttempts = 0;
        //int attempts = bill.getRejectionAttempts() != null ? bill.getRejectionAttempts() : 0;

        String subject = "⚠️ Payment Proof Rejected - " + bill.getBillNumber();
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                <div style="max-width:600px; margin:0 auto; background:white; padding:30px; border-radius:10px;">
                    <h2 style="color:#dc3545;">⚠️ Payment Proof Rejected</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your payment proof for bill <strong>%s</strong> has been rejected by the owner.</p>
                    <div style="background:#f8d7da; padding:15px; border-radius:8px; margin:20px 0; color:#721c24;">
                        <p style="margin:0;"><strong>Rejection Reason:</strong></p>
                        <p style="margin:8px 0 0;">%s</p>
                    </div>
                    <div style="background:#f8f9fa; padding:15px; border-radius:8px; margin:20px 0;">
                        <p style="margin:0;"><strong>Bill Number:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Amount Due:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Due Date:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Rejection Attempts Used:</strong> %s / 5</p>
                    </div>
                    <p style="color:#856404; background:#fff3cd; padding:10px; border-radius:5px;">
                        📎 Please upload the correct payment proof through the app to clear your dues.
                    </p>
                    <hr style="border:none; border-top:1px solid #eee; margin:20px 0;">
                    <p style="font-size:12px; color:#666;">© 2026 PG Management System. Do not reply to this email.</p>
                </div>
            </body>
            </html>
            """,
                tenantName,
                bill.getBillNumber(),
                reason != null ? reason : "No reason provided",
                bill.getBillNumber(),
                bill.getTotalAmount(),
                bill.getDueDate()
                //attempts
        );

        sendEmail(tenantEmail, subject, htmlContent);
    }

    /**
     * Send overdue bill reminder to tenant.
     */
    public void sendOverdueReminder(com.pgmanagement.entity.Bill bill) {
        log.info("Sending overdue reminder for bill: {}", bill.getBillNumber());

        String tenantEmail = resolveTenantEmail(bill);
        if (tenantEmail == null) {
            log.warn("Tenant email not found for tenantId: {} — skipping email", bill.getTenantId());
            return;
        }

        String tenantName = resolveTenantName(bill);

        String subject = "🔴 Overdue Payment Reminder - " + bill.getBillNumber();
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                <div style="max-width:600px; margin:0 auto; background:white; padding:30px; border-radius:10px;">
                    <div style="background:#dc3545; color:white; padding:20px; border-radius:8px; text-align:center;">
                        <h2>🔴 Overdue Payment Reminder</h2>
                    </div>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your bill is <strong>overdue</strong>. Please make the payment immediately to avoid further charges.</p>
                    <div style="background:#f8d7da; padding:15px; border-radius:8px; margin:20px 0; color:#721c24;">
                        <p style="margin:0;"><strong>Bill Number:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Total Amount Due:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Original Due Date:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Late Fee Applied:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Billing Period:</strong> %s to %s</p>
                    </div>
                    <p style="color:#dc3545;"><strong>⚠️ Please pay immediately to avoid further penalties.</strong></p>
                    <p>Open the PG Management app to pay now or contact your PG owner.</p>
                    <hr style="border:none; border-top:1px solid #eee; margin:20px 0;">
                    <p style="font-size:12px; color:#666;">© 2026 PG Management System. Do not reply to this email.</p>
                </div>
            </body>
            </html>
            """,
                tenantName,
                bill.getBillNumber(),
                bill.getTotalAmount(),
                bill.getDueDate(),
                bill.getLateFee(),
                bill.getBillingPeriodStart(),
                bill.getBillingPeriodEnd()
        );

        sendEmail(tenantEmail, subject, htmlContent);
    }

    /**
     * Send late fee applied notification to tenant.
     */
    public void sendLateFeeApplied(com.pgmanagement.entity.Bill bill) {
        log.info("Sending late fee applied notification for bill: {}", bill.getBillNumber());

        String tenantEmail = resolveTenantEmail(bill);
        if (tenantEmail == null) {
            log.warn("Tenant email not found for tenantId: {} — skipping email", bill.getTenantId());
            return;
        }

        String tenantName = resolveTenantName(bill);

        String subject = "⏰ Late Fee Applied - " + bill.getBillNumber();
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                <div style="max-width:600px; margin:0 auto; background:white; padding:30px; border-radius:10px;">
                    <h2 style="color:#856404;">⏰ Late Fee Applied to Your Bill</h2>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>A late fee has been added to your bill because payment was not received by the due date.</p>
                    <div style="background:#fff3cd; padding:15px; border-radius:8px; margin:20px 0; color:#856404;">
                        <p style="margin:0;"><strong>Bill Number:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Late Fee Added:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Updated Total Amount:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Billing Period:</strong> %s to %s</p>
                        <p style="margin:8px 0 0;"><strong>Bill Status:</strong> OVERDUE</p>
                    </div>
                    <p>Please pay immediately through the app to clear your dues and avoid further charges.</p>
                    <hr style="border:none; border-top:1px solid #eee; margin:20px 0;">
                    <p style="font-size:12px; color:#666;">© 2026 PG Management System. Do not reply to this email.</p>
                </div>
            </body>
            </html>
            """,
                tenantName,
                bill.getBillNumber(),
                bill.getLateFee(),
                bill.getTotalAmount(),
                bill.getBillingPeriodStart(),
                bill.getBillingPeriodEnd()
        );

        sendEmail(tenantEmail, subject, htmlContent);
    }

    /**
     * Send late fee waived notification to tenant.
     */
    public void sendLateFeeWaived(com.pgmanagement.entity.Bill bill, String reason) {
        log.info("Sending late fee waived notification for bill: {}", bill.getBillNumber());

        String tenantEmail = resolveTenantEmail(bill);
        if (tenantEmail == null) {
            log.warn("Tenant email not found for tenantId: {} — skipping email", bill.getTenantId());
            return;
        }

        String tenantName = resolveTenantName(bill);

        String subject = "🎉 Late Fee Waived - " + bill.getBillNumber();
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                <div style="max-width:600px; margin:0 auto; background:white; padding:30px; border-radius:10px;">
                    <div style="background:#28a745; color:white; padding:20px; border-radius:8px; text-align:center;">
                        <h2>🎉 Late Fee Waived!</h2>
                    </div>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Good news! The late fee on your bill <strong>%s</strong> has been waived by the owner.</p>
                    <div style="background:#d4edda; padding:15px; border-radius:8px; margin:20px 0; color:#155724;">
                        <p style="margin:0;"><strong>Waiver Reason:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Updated Total Amount:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Billing Period:</strong> %s to %s</p>
                    </div>
                    <p>Please ensure timely payment of the remaining amount by the due date.</p>
                    <hr style="border:none; border-top:1px solid #eee; margin:20px 0;">
                    <p style="font-size:12px; color:#666;">© 2026 PG Management System. Do not reply to this email.</p>
                </div>
            </body>
            </html>
            """,
                tenantName,
                bill.getBillNumber(),
                reason != null ? reason : "Owner discretion",
                bill.getTotalAmount(),
                bill.getBillingPeriodStart(),
                bill.getBillingPeriodEnd()
        );

        sendEmail(tenantEmail, subject, htmlContent);
    }

    /**
     * Send checkout initiated notification to tenant.
     * Tenant User entity passed directly — no resolver needed.
     */
    public void sendCheckoutRequest(com.pgmanagement.entity.User tenant,
                                    com.pgmanagement.entity.TenantStayRecord stay,
                                    com.pgmanagement.dto.request.InitiateCheckoutRequest request) {
        log.info("Sending checkout initiated notification for tenant: {}", tenant.getId());

        if (tenant.getEmail() == null) {
            log.warn("Tenant email is null for tenantId: {} — skipping email", tenant.getId());
            return;
        }

        String subject = "📦 Checkout Process Initiated";
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                <div style="max-width:600px; margin:0 auto; background:white; padding:30px; border-radius:10px;">
                    <div style="background:#6c757d; color:white; padding:20px; border-radius:8px; text-align:center;">
                        <h2>📦 Checkout Process Initiated</h2>
                    </div>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your checkout process has been initiated by the owner. Please ensure all pending bills are cleared before your move-out date.</p>
                    <div style="background:#e7f3ff; padding:15px; border-radius:8px; margin:20px 0;">
                        <p style="margin:0;"><strong>Your Move-in Date:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Intended Move-out Date:</strong> %s</p>
                    </div>
                    <div style="background:#fff3cd; padding:12px; border-radius:5px; color:#856404; margin:20px 0;">
                        <strong>⚠️ What happens next:</strong><br>
                        A final settlement bill will be generated. Please pay it to complete the checkout.
                        Your security deposit (if any) will be adjusted against dues.
                    </div>
                    <p>If you have any concerns, please contact your PG owner directly.</p>
                    <hr style="border:none; border-top:1px solid #eee; margin:20px 0;">
                    <p style="font-size:12px; color:#666;">© 2026 PG Management System. Do not reply to this email.</p>
                </div>
            </body>
            </html>
            """,
                tenant.getName(),
                stay.getMoveInDate(),
                request.getMoveOutDate()
        );

        sendEmail(tenant.getEmail(), subject, htmlContent);
    }

    /**
     * Send final settlement bill to tenant.
     * Tenant email resolved via userRepository using bill.getTenantId().
     */
    public void sendFinalSettlementBill(com.pgmanagement.entity.Bill bill,
                                        com.pgmanagement.entity.PGBillingConfig config) {
        log.info("Sending final settlement bill: {}", bill.getBillNumber());

        String tenantEmail = resolveTenantEmail(bill);
        if (tenantEmail == null) {
            log.warn("Tenant email not found for tenantId: {} — skipping email", bill.getTenantId());
            return;
        }

        String tenantName = resolveTenantName(bill);

        String upiSection;
        if (config.getUpiId() != null && !config.getUpiId().isBlank()) {
            String upiLink = String.format("upi://pay?pa=%s&pn=%s&am=%s&cu=INR&tn=%s",
                    config.getUpiId(),
                    config.getUpiName() != null ? config.getUpiName() : "PG Owner",
                    bill.getTotalAmount(),
                    bill.getBillNumber());
            upiSection = String.format("""
                <div style="text-align:center; margin:20px 0;">
                    <a href="%s" style="background:#28a745; color:white; padding:12px 30px;
                       text-decoration:none; border-radius:5px; display:inline-block;">
                       💳 Pay Now via UPI
                    </a>
                    <p style="font-size:12px; color:#666; margin-top:8px;">UPI ID: %s</p>
                </div>
                """, upiLink, config.getUpiId());
        } else {
            upiSection = "<p style='color:#666; text-align:center;'>Contact your owner for payment details.</p>";
        }

        String subject = "📋 Final Settlement Bill - " + bill.getBillNumber();
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                <div style="max-width:600px; margin:0 auto; background:white; padding:30px; border-radius:10px;">
                    <div style="background:#6c757d; color:white; padding:20px; border-radius:8px; text-align:center;">
                        <h2>📋 Final Settlement Bill</h2>
                    </div>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your final settlement bill has been generated. Please review and clear all dues to complete the checkout process.</p>
                    <div style="background:#f8f9fa; padding:15px; border-radius:8px; margin:20px 0;">
                        <p style="margin:0;"><strong>Bill Number:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Billing Period:</strong> %s to %s</p>
                        <p style="margin:8px 0 0;"><strong>Room Rent (Pro-rated):</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Electricity Charges:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Other Charges:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Security Deposit Adjusted:</strong> - ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Discount:</strong> - ₹%s</p>
                        <p style="margin:8px 0 0; font-size:16px; color:#007bff; border-top:1px solid #ddd; padding-top:10px;">
                            <strong>Total Amount Due: ₹%s</strong>
                        </p>
                    </div>
                    <p style="background:#fff3cd; padding:10px; border-radius:5px; color:#856404;">
                        📅 <strong>Due Date:</strong> %s — Checkout completes once this bill is settled.
                    </p>
                    %s
                    <hr style="border:none; border-top:1px solid #eee; margin:20px 0;">
                    <p style="font-size:12px; color:#666;">© 2026 PG Management System. Do not reply to this email.</p>
                </div>
            </body>
            </html>
            """,
                tenantName,
                bill.getBillNumber(),
                bill.getBillingPeriodStart(), bill.getBillingPeriodEnd(),
                bill.getRoomRent(),
                bill.getElectricityCharge(),
                bill.getOtherCharges(),
                bill.getSecurityDepositUsed(),
                bill.getDiscount(),
                bill.getTotalAmount(),
                bill.getDueDate(),
                upiSection
        );

        sendEmail(tenantEmail, subject, htmlContent);
    }

    /**
     * Send checkout completion confirmation to tenant.
     */
    public void sendCheckoutConfirmation(com.pgmanagement.entity.Bill bill,
                                         com.pgmanagement.dto.request.SettlementCompleteRequest request) {
        log.info("Sending checkout confirmation for bill: {}", bill.getBillNumber());

        String tenantEmail = resolveTenantEmail(bill);
        if (tenantEmail == null) {
            log.warn("Tenant email not found for tenantId: {} — skipping email", bill.getTenantId());
            return;
        }

        String tenantName = resolveTenantName(bill);
        String roomNumber = resolveRoomNumber(bill);
        String pgName     = resolvePgName(bill);

        String subject = "✅ Checkout Complete - Thank You for Staying!";
        String htmlContent = String.format("""
            <!DOCTYPE html>
            <html>
            <body style="font-family: Arial, sans-serif; background: #f4f4f4; padding: 20px;">
                <div style="max-width:600px; margin:0 auto; background:white; padding:30px; border-radius:10px;">
                    <div style="background:#28a745; color:white; padding:20px; border-radius:8px; text-align:center;">
                        <h2>✅ Checkout Successfully Completed!</h2>
                    </div>
                    <p>Dear <strong>%s</strong>,</p>
                    <p>Your checkout has been <strong>successfully completed</strong>. Thank you for your stay with us!</p>
                    <div style="background:#d4edda; padding:15px; border-radius:8px; margin:20px 0; color:#155724;">
                        <p style="margin:0;"><strong>Settlement Bill:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Final Amount Settled:</strong> ₹%s</p>
                        <p style="margin:8px 0 0;"><strong>Room:</strong> %s | <strong>PG:</strong> %s</p>
                        <p style="margin:8px 0 0;"><strong>Status:</strong> SETTLED ✅</p>
                    </div>
                    <p>We hope you had a comfortable stay. You are always welcome back!</p>
                    <p style="font-size:13px; color:#555;">
                        Please keep this email as your checkout confirmation for reference.
                    </p>
                    <hr style="border:none; border-top:1px solid #eee; margin:20px 0;">
                    <p style="font-size:12px; color:#666;">© 2026 PG Management System. Do not reply to this email.</p>
                </div>
            </body>
            </html>
            """,
                tenantName,
                bill.getBillNumber(),
                bill.getTotalAmount(),
                roomNumber,
                pgName
        );

        sendEmail(tenantEmail, subject, htmlContent);
    }



    // ==================== OTP & BOOKING TEMPLATES (unchanged) ====================

    private String buildOwnerRegistrationOtpEmail(String otpCode) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; color: #333; }
                    .otp-box { background: #f8f9fa; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #007bff; letter-spacing: 5px; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2 class="header">🏠 Welcome to PG Management!</h2>
                    <p>Thank you for registering as a <strong>PG Owner</strong>.</p>
                    <p>Please use the following OTP to verify your email address:</p>
                    <div class="otp-box">
                        <div class="otp-code">%s</div>
                    </div>
                    <p><strong>This OTP is valid for 10 minutes.</strong></p>
                    <p>If you didn't request this, please ignore this email.</p>
                    <div class="footer">
                        <p>© 2026 PG Management System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, otpCode);
    }

    private String buildUserRegistrationOtpEmail(String otpCode) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; color: #333; }
                    .otp-box { background: #f8f9fa; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #28a745; letter-spacing: 5px; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2 class="header">🎉 Welcome to PG Management!</h2>
                    <p>Thank you for registering with us.</p>
                    <p>Please use the following OTP to verify your email address:</p>
                    <div class="otp-box">
                        <div class="otp-code">%s</div>
                    </div>
                    <p><strong>This OTP is valid for 10 minutes.</strong></p>
                    <p>If you didn't create an account, please ignore this email.</p>
                    <div class="footer">
                        <p>© 2026 PG Management System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, otpCode);
    }

    private String buildPasswordResetOtpEmail(String otpCode) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; color: #333; }
                    .otp-box { background: #fff3cd; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #856404; letter-spacing: 5px; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2 class="header">🔐 Password Reset Request</h2>
                    <p>We received a request to reset your password.</p>
                    <p>Please use the following OTP to reset your password:</p>
                    <div class="otp-box">
                        <div class="otp-code">%s</div>
                    </div>
                    <p><strong>This OTP is valid for 10 minutes.</strong></p>
                    <p>If you didn't request this, please ignore this email and your password will remain unchanged.</p>
                    <div class="footer">
                        <p>© 2026 PG Management System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, otpCode);
    }

    private String buildPasswordSetupOtpEmail(String otpCode) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    .header { text-align: center; color: #333; }
                    .otp-box { background: #d1ecf1; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #0c5460; letter-spacing: 5px; }
                    .footer { text-align: center; color: #666; font-size: 12px; margin-top: 30px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2 class="header">🔑 Set Up Your Password</h2>
                    <p>Your account has been created by a PG owner.</p>
                    <p>Please use the following OTP to set up your password:</p>
                    <div class="otp-box">
                        <div class="otp-code">%s</div>
                    </div>
                    <p><strong>This OTP is valid for 10 minutes.</strong></p>
                    <div class="footer">
                        <p>© 2026 PG Management System. All rights reserved.</p>
                    </div>
                </div>
            </body>
            </html>
            """, otpCode);
    }

    private String buildGenericOtpEmail(String otpCode) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    .otp-box { background: #f8f9fa; padding: 20px; text-align: center; border-radius: 8px; margin: 20px 0; }
                    .otp-code { font-size: 32px; font-weight: bold; color: #333; letter-spacing: 5px; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Your OTP Code</h2>
                    <div class="otp-box">
                        <div class="otp-code">%s</div>
                    </div>
                    <p><strong>This OTP is valid for 10 minutes.</strong></p>
                </div>
            </body>
            </html>
            """, otpCode);
    }

    private String buildBookingConfirmationEmail(String userName, String pgName,
                                                 String roomNumber, String rent) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    .success { background: #d4edda; padding: 15px; border-radius: 5px; color: #155724; margin: 20px 0; }
                    .details { background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>🎉 Booking Confirmed!</h2>
                    <div class="success">
                        <strong>Congratulations, %s!</strong><br>
                        Your booking has been accepted.
                    </div>
                    <div class="details">
                        <p><strong>PG Name:</strong> %s</p>
                        <p><strong>Room Number:</strong> %s</p>
                        <p><strong>Monthly Rent:</strong> ₹%s</p>
                    </div>
                    <p>The PG owner will contact you soon with further details.</p>
                </div>
            </body>
            </html>
            """, userName, pgName, roomNumber, rent);
    }

    private String buildBookingRejectionEmail(String userName, String pgName,
                                              String roomNumber, String reason) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    .info { background: #fff3cd; padding: 15px; border-radius: 5px; color: #856404; margin: 20px 0; }
                    .reason { background: #f8f9fa; padding: 15px; border-radius: 5px; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>Booking Request Update</h2>
                    <p>Dear %s,</p>
                    <div class="info">
                        Your booking request for <strong>Room %s</strong> at <strong>%s</strong> has been declined.
                    </div>
                    <div class="reason">
                        <strong>Reason:</strong><br>
                        %s
                    </div>
                    <p>You can explore other available rooms on our platform.</p>
                </div>
            </body>
            </html>
            """, userName, roomNumber, pgName, reason);
    }

    private String buildNewBookingNotificationEmail(String ownerName, String userName,
                                                    String pgName, String roomNumber) {
        return String.format("""
            <!DOCTYPE html>
            <html>
            <head>
                <style>
                    body { font-family: Arial, sans-serif; background-color: #f4f4f4; padding: 20px; }
                    .container { max-width: 600px; margin: 0 auto; background: white; padding: 30px; border-radius: 10px; }
                    .alert { background: #d1ecf1; padding: 15px; border-radius: 5px; color: #0c5460; margin: 20px 0; }
                </style>
            </head>
            <body>
                <div class="container">
                    <h2>🔔 New Booking Request</h2>
                    <p>Dear %s,</p>
                    <div class="alert">
                        You have received a new booking request!
                    </div>
                    <p><strong>Tenant Name:</strong> %s</p>
                    <p><strong>PG:</strong> %s</p>
                    <p><strong>Room:</strong> %s</p>
                    <p>Please log in to your dashboard to review and respond to this request.</p>
                </div>
            </body>
            </html>
            """, ownerName, userName, pgName, roomNumber);
    }
}
