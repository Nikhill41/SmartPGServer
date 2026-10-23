# SMART PG MANAGEMENT SYSTEM - COMPLETE PROJECT HANDOFF

**Project Name**: Smart PG Management System  
**Framework**: Spring Boot 3.2.0  
**Language**: Java 17  
**Database**: PostgreSQL  
**Cache**: Redis  
**Current Date**: February 14, 2026

---

## 1. EXECUTIVE SUMMARY

This is a **production-grade Spring Boot REST API** for managing Paying Guest (PG) accommodations with integrated **IoT smart meter management**. The system enables:

- **Owners**: Manage multiple properties, rooms, and tenants
- **Tenants**: Search rooms, book accommodations, view meter readings
- **Smart Meters**: Real-time electricity monitoring via AWS IoT Core + DynamoDB historical data

**Key Innovation**: Integration with AWS IoT Device Shadows for live meter data and remote relay control.

---

## 2. ARCHITECTURE OVERVIEW

```
┌─────────────────────────────────────────────────────────────────┐
│                        Mobile/Web Frontend                      │
└─────────────────────────────────────────────────────────────────┘
                              ↓ (HTTP/REST)
┌─────────────────────────────────────────────────────────────────┐
│                   Spring Boot REST API (8080)                   │
│                                                                 │
│  Controllers → Services → Repositories → Database              │
│                              ↓                                  │
│                      JWT Authentication                        │
│                 (Username/Password + OTP Flow)                 │
└─────────────────────────────────────────────────────────────────┘
         ↓                           ↓                    ↓
    PostgreSQL              Redis Cache            AWS Services
   (Persistent DB)      (Query Results)          (IoT + DynamoDB)
                                                      + S3 Images
```

---

## 3. CORE DOMAINS & BUSINESS LOGIC

### 3.1 AUTHENTICATION FLOW

```
REGISTRATION PATH:
1. User POST /api/auth/user/register
   ├─ Email uniqueness check
   ├─ Password hashing (BCrypt)
   ├─ Create UNVERIFIED user record
   └─ Send OTP email (SendGrid)

2. User POST /api/auth/verify-otp
   ├─ OTP verification (max 5 attempts, 10 min expiry)
   ├─ Mark user as verified (isVerified = true)
   └─ Generate JWT token (24-hour expiry)
   └─ Auto-login response

LOGIN PATH:
1. User POST /api/auth/login (ONLY verified users)
   ├─ Credential check
   ├─ Generate JWT token
   └─ Return AuthResponse
```

**Key Files**:
- `AuthService.java` - Registration, OTP verification, login logic
- `JwtTokenProvider.java` - Token generation and validation
- `OtpService.java` - OTP generation, verification, expiry checks
- `EmailService.java` - SendGrid integration
- `SecurityConfig.java` - JWT filter and route protection

---

### 3.2 PG & ROOM MANAGEMENT

```
OWNER OPERATIONS:
1. POST /api/owner/pg
   ├─ Create new property
   ├─ Max 5 images per PG
   └─ Upload to S3

2. POST /api/owner/pg/{pgId}/room
   ├─ Create room in PG
   ├─ Set capacity, rent, type (SINGLE/DOUBLE/SHARED)
   ├─ Attach amenities
   ├─ Max 10 images per room
   └─ Upload to S3

3. GET /api/owner/pgs
   └─ List all owner's properties

DATA VALIDATION:
- Room number unique per PG
- Capacity must be > 0
- Rent must be > 0
- Images: Max 10 per room, 5 per PG
```

**Key Files**:
- `OwnerService.java` - PG CRUD operations
- `RoomService.java` - Room CRUD, amenity management
- `OwnerController.java` - Routes and request handling
- `S3Service.java` - Image upload, presigned URLs

---

### 3.3 BOOKING WORKFLOW

```
BOOKING FLOW:
1. User POST /api/public/booking/room/{roomId}
   ├─ Check room availability (currentOccupancy < capacity)
   ├─ Validate gender compatibility (User.gender vs PG.genderPreference)
   ├─ Prevent duplicate pending bookings
   ├─ Create BookingRequest (status = PENDING)
   └─ Email owner about new request

2. Owner POST /api/owner/booking/{bookingId}/accept
   ├─ Verify ownership
   ├─ Update room occupancy (+1)
   ├─ Assign user to room (User.room = roomId)
   ├─ Update booking status = ACCEPTED
   ├─ Set moveInDate
   └─ Send confirmation email to tenant

3. Owner POST /api/owner/booking/{bookingId}/reject
   ├─ Update booking status = REJECTED
   ├─ Record rejection reason
   └─ Send rejection email to tenant

ENTITIES AND RELATIONSHIPS:
BookingRequest
├─ id (PK)
├─ user_id (FK to User)
├─ room_id (FK to Room)
├─ status (PENDING/ACCEPTED/REJECTED/COMPLETED)
├─ rejection_reason
└─ timestamps (requestDate, responseDate, createdAt, updatedAt)

User
├─ room_id (FK - which room is assigned now)
├─ isVerified (email verified via OTP)
└─ moveInDate
```

**Key Files**:
- `BookingService.java` - Complete booking workflow
- `BookingController.java` (in OwnerController) - Owner actions
- `UserController.java` - Tenant booking view

---

### 3.4 SMART METER MANAGEMENT

```
METER PAIRING:
Owner POST /api/owner/meters/pair
{
  "meterId": "METER_001",      // Unique identifier
  "roomId": 123                 // Room to pair with
}
├─ Verify owner owns room
├─ Create Meter entity
├─ Set awsThingName = meterId
├─ Status = ACTIVE
├─ Store pairedAt timestamp
└─ AWS IoT: Meter will send shadow updates autonomously

LIVE READINGS (IAM-accessible):
GET /api/meters/{meterId}/live
├─ Get AWS IoT Device Shadow
├─ Check if meter online (last reading < 60 seconds)
├─ Return: voltage, current, power, energy, frequency, powerFactor, relayStatus
├─ Timestamp of last reading

HISTORICAL READINGS:
GET /api/meters/{meterId}/readings?start=1708000000000&end=1708100000000
├─ Query DynamoDB meter_readings table
├─ Filter by timestamp range (Unix milliseconds)
├─ Return: list of readings grouped by hour/day
├─ Include summary (min/max/avg values)

RELAY CONTROL (Owner only):
POST /api/owner/meters/{meterId}/relay
{
  "command": "ON" or "OFF"
}
├─ Publish MQTT command to device
├─ Wait for confirmation (timeout 2 seconds, 3 retries)
├─ Update last_command timestamp
└─ Return command status

ACCESS CONTROL:
- Owner can view ALL their meters
- Owner can control relay
- Tenant can view ONLY their room's meter
- Unauthorized access → 403 Forbidden
```

**Meter Entity Structure**:
```
Meter
├─ id (PK)
├─ meterId (unique, e.g., "METER_001")
├─ awsThingName (= meterId)
├─ room_id (FK, unique)
├─ owner_id (indexed)
├─ status (ACTIVE/INACTIVE/UNPAIRED)
├─ pairedAt (timestamp)
└─ unpairedAt (when meter was unlinked)
```

**AWS Integration**:
- **Device Shadow**: AWS IoT stores key-value data for each meter
  - Reported state: Current readings (voltage, current, power, etc.)
  - Desired state: Commands (relay ON/OFF)
- **DynamoDB Table**: `meter_readings`
  - Partition Key: meterId
  - Sort Key: timestamp (Unix ms)
  - Attributes: voltage, current, power, energy, frequency, powerFactor, relayStatus

**Key Files**:
- `MeterService.java` - Main meter operations (pairing, relay, reads)
- `MeterAuthorizationService.java` - Access control logic
- `OwnerMeterController.java` - Owner meter routes
- `MeterController.java` - Public meter read routes
- `TenantMeterController.java` - Tenant convenience routes
- `AwsIotShadowService.java` - AWS IoT Device Shadow API calls
- `DynamoDbMeterReadingService.java` - DynamoDB querying

---

### 3.5 BILLING SYSTEM

```
BILLING PROCESS FLOWCHART:

OWNER SETUP:
1. Owner POST /api/owner/billing/config (Create billing configuration)
   ├─ Set UPI ID, Bank details for payment
   ├─ Configure electricity rate (per unit)
   ├─ Set billing mode (AUTO/MANUAL)
   ├─ Configure auto-generation day (1-28)
   ├─ GST settings and percentage
   ├─ Late fees, security deposit, pro-ration mode
   └─ Store in PGBillingConfig

2. Owner POST /api/owner/billing/charges (Create standard charges)
   ├─ Define additional charges (Maintenance, WiFi, etc.)
   ├─ Set amount per charge
   ├─ Mark as active/inactive (can be toggled)
   └─ Optionally apply to all new tenants

BILL GENERATION:
3a. MANUAL: Owner POST /api/owner/billing/bills/generate/pg/{pgId}
    ├─ Specify tenant and billing period
    ├─ System calculates all charges
    ├─ BillGenerationService with:
    │  ├─ Room rent calculation (full/half/daily based on config)
    │  ├─ Electricity calculation (TIME_WEIGHTED or EQUAL split)
    │  ├─ Standard charges application
    │  ├─ GST calculation if enabled
    │  └─ Security deposit deduction if applicable
    └─ Bill created with status = PENDING

3b. AUTO: Scheduled job runs on configured day
    ├─ Triggered by AutoBillingSchedulerService
    ├─ Generates bills for all active tenants in PG
    ├─ Uses same calculation as manual
    └─ Bulk operation with success/failure tracking

BILL DETAILS:
Bill Entity Contains:
├─ Bill Number (unique format: PG-ROOM-YYYY-MM-ID)
├─ Tenant/Room/PG/Owner IDs
├─ Billing Period (start/end dates)
├─ Break-down:
│  ├─ Room Rent (base rent or pro-rated)
│  ├─ Electricity Charge (calculated from meter usage)
│  ├─ Standard Charges (WiFi, maintenance, etc.)
│  ├─ Other Charges
│  ├─ Discount (if manual)
│  ├─ Late Fee (if overdue past grace period)
│  ├─ Additional Deductions
│  ├─ Subtotal
│  ├─ GST Amount (if enabled)
│  └─ Total Amount
├─ Security Deposit tracking
├─ Bill Type (MONTHLY/SECURITY_DEPOSIT/FINAL_SETTLEMENT)
├─ Bill Status (PENDING/PAID/OVERDUE/CANCELLED/SETTLED)
├─ Generated Date, Due Date
└─ Payment verification tracking

ELECTRICITY CALCULATION:
ElectricityCalculationService:
├─ TIME_WEIGHTED split:
│  ├─ Get total meter readings for room in period
│  ├─ Get all active tenant stays in period
│  ├─ Calculate weighted share per tenant (days stayed / total days)
│  └─ Assign electricity cost proportionally
├─ EQUAL split:
│  ├─ Count active tenants in period
│  ├─ Divide total consumption equally
│  └─ Each tenant pays equal share
└─ Conversion: Units × Rate per Unit = Electricity Charge

BILL EDITING (Before Payment):
Owner POST /api/owner/billing/bills/{billId}/edit
├─ Allowed edits (bill not PAID):
│  ├─ Discount amount
│  ├─ Additional deductions
│  ├─ Room rent (if marked editable)
│  └─ Other charges
├─ Cannot edit:
│  ├─ Rent if already electricity calculated
│  ├─ Electricity charge (use separate adjustment)
│  └─ Paid bills
└─ Email sent to tenant about changes

PAYMENT VERIFICATION:
1. Tenant POST /api/tenant/billing/payments/{billId}/upload
   ├─ Upload payment proof (screenshot/receipt)
   ├─ Provide payment method (UPI/BANK_TRANSFER/CASH/CHEQUE/OTHER)
   ├─ Transaction ID and date (optional)
   └─ Payment proof stored in S3 with PaymentProofHistory

2. Owner Review POST /api/owner/billing/payments/{paymentProofId}/(approve|reject)
   ├─ APPROVE:
   │  ├─ Verify proof details
   │  ├─ Mark as VERIFIED
   │  ├─ Update bill status to PAID
   │  ├─ Send payment confirmation email
   │  └─ Record verification timestamp
   ├─ REJECT:
   │  ├─ Provide rejection reason
   │  ├─ Update payment proof status to REJECTED
   │  ├─ Bill remains PENDING
   │  ├─ Send rejection email
   │  └─ Tenant can re-upload (max 5 attempts)

UPI PAYMENT LINK:
GET /api/tenant/billing/bills/{billId}/upi-link
├─ Generate dynamic UPI payment link
├─ Format: upi://pay?pa=UPIID&pn=OwnerName&am=Amount&tn=BillNumber
├─ Present to tenant for quick payment via UPI apps
└─ Not tracked automatically (requires manual proof upload)

LATE FEES & OVERDUE:
OverdueBillService (Scheduled Job):
├─ Runs daily (cron configured)
├─ Find bills with:
│  ├─ Status = PENDING
│  ├─ Current date > due date + grace days
│  └─ Late fee enabled in config
├─ Apply late fee amount (one-time or recurring)
├─ Update bill status to OVERDUE
└─ Send overdue notification email

SECURITY DEPOSIT:
1. On Booking Accept (Checkout service):
   ├─ IF security deposit required:
   │  ├─ Create separate bill with type = SECURITY_DEPOSIT
   │  ├─ Amount from PGBillingConfig
   │  ├─ Due date = security deposit due days from move-in
   │  ├─ Bill number with "SD" prefix
   │  └─ Send email with security deposit bill

2. On Final Settlement:
   ├─ If security deposit paid:
   │  ├─ Apply as credit to final settlement
   │  ├─ Deduct from total amount due
   │  └─ Track in Bill.securityDepositUsed

CHECKOUT & FINAL SETTLEMENT:
1. Owner Initiates: POST /api/owner/billing/checkout/tenant/{tenantId}
   ├─ Update TenantStayRecord.checkoutInitiated = true
   ├─ Set intendedMoveOutDate
   ├─ Status changes to PENDING_SETTLEMENT

2. System Creates Final Settlement Bill:
   ├─ Type = FINAL_SETTLEMENT
   ├─ Calculate pro-rated rent for final days
   ├─ Include any partial month charges
   ├─ Apply security deposit as credit
   ├─ Include final electricity reading
   └─ Create with status = PENDING (must be paid)

3. Owner Completes: POST /api/owner/billing/checkout/complete/{billId}
   ├─ Verify final settlement bill is PAID
   ├─ Update TenantStayRecord:
   │  ├─ stayStatus = COMPLETED
   │  ├─ moveOutDate = today
   │  └─ checkoutCompletedAt = now
   ├─ Remove tenant from room (room.currentOccupancy -= 1)
   ├─ Update bill status to SETTLED
   └─ Send checkout complete email

TENANT BILLING DASHBOARD:
GET /api/tenant/billing/dashboard
├─ Stay information:
│  ├─ Move-in date
│  ├─ Days stayed so far
│  └─ Checkout status (if initiated)
├─ Bill Summary:
│  ├─ Pending bills count
│  ├─ Paid bills count
│  ├─ Overdue count
│  ├─ Total pending amount
│  ├─ Total paid amount
│  └─ Next due bill with due date
└─ Quick actions (view bills, upload proof, generate UPI link)

KEY FILES:
- BillingConfigService - Config CRUD, validation
- BillGenerationService - Bill creation, calculation
- BillCalculationService - Charge calculation logic
- ElectricityCalculationService - Meter-based electricity cost
- StandardChargeService - Standard charges management
- BillEditService - Edit existing bills (before payment)
- PaymentVerificationService - Proof upload & verification
- CheckoutService - Final settlement & room cleanup
- OverdueBillService - Late fees, overdue tracking
- AutoBillingSchedulerService - Scheduled bill generation
- UpiPaymentLinkService - UPI link generation
```

**Billing Database Tables** (detailed in Section 4):
- `pg_billing_configs` - PG-specific billing settings
- `bills` - Main bill records
- `bill_charges` - Line items per bill
- `bill_versions` - Bill change history
- `standard_charges` - Recurring charges template
- `payment_proof_history` - Payment verification records
- `tenant_stay_records` - Track tenant occupancy periods

---

### 3.6 PUBLIC SEARCH

```
SEARCH PGs:
GET /api/public/pg/search?city=Mumbai&genderPreference=FEMALE&roomType=SINGLE&minRent=5000&maxRent=15000&amenities=WIFI,AC
├─ Advanced search with Redis caching (5 min TTL)
├─ Filter by:
│  ├─ city (string match)
│  ├─ genderPreference (enum: MALE/FEMALE/ALL)
│  ├─ roomType (enum: SINGLE/DOUBLE/SHARED)
│  ├─ rent range (BigDecimal)
│  └─ amenities (list of strings)
├─ Return only PGs with available rooms
├─ Images converted to presigned S3 URLs (24-hour validity)
└─ Response: List[PGResponse] with room details

GET /api/public/pg/{pgId}
├─ Get full PG details
├─ Show available rooms count
└─ Include owner contact info

GET /api/public/room/{roomId}
└─ Get room details (images, amenities, current occupancy)
```

**Key Files**:
- `PublicController.java` - Search endpoints
- `SearchService.java` - Advanced search logic
- `AdvancedPGSearchService.java` - Complex filtering

---

## 4. DATABASE SCHEMA

### Core Tables

```sql
-- Users/Tenants
CREATE TABLE users (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(15),
  gender VARCHAR(10),  -- MALE/FEMALE/OTHER
  role VARCHAR(20) NOT NULL DEFAULT 'USER',
  is_verified BOOLEAN DEFAULT false,  -- Email verified via OTP
  is_active BOOLEAN DEFAULT true,
  room_id BIGINT,  -- Current assigned room (null if no booking)
  move_in_date TIMESTAMP,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  FOREIGN KEY (room_id) REFERENCES rooms(id)
);

-- Owners
CREATE TABLE owners (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) NOT NULL,
  email VARCHAR(100) UNIQUE NOT NULL,
  password_hash VARCHAR(255) NOT NULL,
  phone VARCHAR(15),
  role VARCHAR(20) NOT NULL DEFAULT 'OWNER',
  is_verified BOOLEAN DEFAULT false,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP
);

-- PG Properties
CREATE TABLE pgs (
  id BIGSERIAL PRIMARY KEY,
  owner_id BIGINT NOT NULL,
  name VARCHAR(200) NOT NULL,
  address TEXT NOT NULL,
  city VARCHAR(100) NOT NULL,
  state VARCHAR(100) NOT NULL,
  pincode VARCHAR(10) NOT NULL,
  description TEXT,
  gender_preference VARCHAR(10),  -- MALE/FEMALE/ALL
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  FOREIGN KEY (owner_id) REFERENCES owners(id) ON DELETE CASCADE
);

-- PG Images
CREATE TABLE pg_images (
  pg_id BIGINT NOT NULL,
  image_url VARCHAR(500),
  FOREIGN KEY (pg_id) REFERENCES pgs(id) ON DELETE CASCADE
);

-- Rooms
CREATE TABLE rooms (
  id BIGSERIAL PRIMARY KEY,
  pg_id BIGINT NOT NULL,
  room_number VARCHAR(20) NOT NULL,
  type VARCHAR(20) NOT NULL,  -- SINGLE/DOUBLE/SHARED
  capacity INT NOT NULL,
  current_occupancy INT DEFAULT 0,
  rent NUMERIC(10,2) NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',  -- AVAILABLE/OCCUPIED/MAINTENANCE
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  UNIQUE(pg_id, room_number),
  FOREIGN KEY (pg_id) REFERENCES pgs(id) ON DELETE CASCADE
);

-- Room Images
CREATE TABLE room_images (
  room_id BIGINT NOT NULL,
  image_url VARCHAR(500),
  FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE
);

-- Amenities
CREATE TABLE amenities (
  id BIGSERIAL PRIMARY KEY,
  name VARCHAR(100) UNIQUE NOT NULL,
  icon VARCHAR(200),
  is_predefined BOOLEAN DEFAULT false,
  created_at TIMESTAMP NOT NULL
);

-- Room-Amenity Junction
CREATE TABLE room_amenities (
  room_id BIGINT NOT NULL,
  amenity_id BIGINT NOT NULL,
  FOREIGN KEY (room_id) REFERENCES rooms(id) ON DELETE CASCADE,
  FOREIGN KEY (amenity_id) REFERENCES amenities(id)
);

-- Booking Requests
CREATE TABLE booking_requests (
  id BIGSERIAL PRIMARY KEY,
  user_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  rejection_reason TEXT,
  request_date TIMESTAMP NOT NULL,
  response_date TIMESTAMP,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  FOREIGN KEY (user_id) REFERENCES users(id),
  FOREIGN KEY (room_id) REFERENCES rooms(id)
);

-- OTP Tokens
CREATE TABLE otp_tokens (
  id BIGSERIAL PRIMARY KEY,
  email VARCHAR(100) NOT NULL,
  code_hash VARCHAR(255) NOT NULL,  -- BCrypt hashed
  purpose VARCHAR(30) NOT NULL,  -- OWNER_REGISTRATION/USER_REGISTRATION/RESET_PASSWORD/PASSWORD_SETUP
  attempts_left INT DEFAULT 5,
  expires_at TIMESTAMP NOT NULL,
  verified BOOLEAN DEFAULT false,
  created_at TIMESTAMP NOT NULL
);

-- Smart Meters
CREATE TABLE meters (
  id BIGSERIAL PRIMARY KEY,
  meter_id VARCHAR(50) UNIQUE NOT NULL,
  aws_thing_name VARCHAR(50) UNIQUE NOT NULL,
  room_id BIGINT UNIQUE NOT NULL,
  owner_id BIGINT NOT NULL,
  status VARCHAR(20) NOT NULL DEFAULT 'UNPAIRED',
  paired_at TIMESTAMP,
  unpaired_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  FOREIGN KEY (room_id) REFERENCES rooms(id),
  FOREIGN KEY (owner_id) REFERENCES owners(id)
);

-- Billing Configuration (PG-specific)
CREATE TABLE pg_billing_configs (
  id BIGSERIAL PRIMARY KEY,
  pg_id BIGINT NOT NULL UNIQUE,
  owner_id BIGINT NOT NULL,
  
  -- Payment Accounts
  upi_id VARCHAR(100),
  upi_name VARCHAR(100),
  bank_account_number VARCHAR(20),
  bank_ifsc_code VARCHAR(11),
  bank_account_name VARCHAR(100),
  bank_name VARCHAR(100),
  bank_branch VARCHAR(100),
  
  -- GST Configuration
  gst_enabled BOOLEAN NOT NULL DEFAULT false,
  gst_number VARCHAR(15),
  gst_percentage NUMERIC(5,2) DEFAULT 0.00,
  
  -- Billing Mode
  billing_mode VARCHAR(20) NOT NULL DEFAULT 'MANUAL',  -- AUTO/MANUAL
  auto_generation_day INT NOT NULL DEFAULT 1,  -- Day of month (1-28)
  due_days_offset INT NOT NULL DEFAULT 7,
  
  -- Electricity
  electricity_rate_per_unit NUMERIC(10,2) DEFAULT 8.50,
  include_electricity BOOLEAN NOT NULL DEFAULT true,
  electricity_split_method VARCHAR(30) DEFAULT 'TIME_WEIGHTED',  -- TIME_WEIGHTED/EQUAL
  
  -- Late Fees
  late_fee_enabled BOOLEAN NOT NULL DEFAULT false,
  late_fee_amount NUMERIC(10,2) DEFAULT 0.00,
  late_fee_days INT DEFAULT 3,
  
  -- Pro-rated Rent
  pro_rated_rent_mode VARCHAR(20) DEFAULT 'DAILY',  -- FULL_MONTH/HALF_MONTH/DAILY
  
  -- Security Deposit
  security_deposit_amount NUMERIC(10,2) DEFAULT 10000.00,
  security_deposit_due_days INT DEFAULT 7,
  
  -- Standard Charges
  auto_apply_standard_charges BOOLEAN NOT NULL DEFAULT true,
  pro_rate_standard_charges BOOLEAN NOT NULL DEFAULT false,
  
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  FOREIGN KEY (pg_id) REFERENCES pgs(id),
  FOREIGN KEY (owner_id) REFERENCES owners(id)
);

-- Tenant Stay Records (Track occupancy)
CREATE TABLE tenant_stay_records (
  id BIGSERIAL PRIMARY KEY,
  tenant_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  booking_id BIGINT,
  move_in_date DATE NOT NULL,
  move_out_date DATE,
  stay_status VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',  -- ACTIVE/PENDING_SETTLEMENT/COMPLETED
  days_stayed INT,
  checkout_initiated BOOLEAN DEFAULT false,
  intended_move_out_date DATE,
  checkout_completed_at TIMESTAMP,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  FOREIGN KEY (tenant_id) REFERENCES users(id),
  FOREIGN KEY (room_id) REFERENCES rooms(id),
  FOREIGN KEY (booking_id) REFERENCES booking_requests(id)
);

-- Bills (Main billing records)
CREATE TABLE bills (
  id BIGSERIAL PRIMARY KEY,
  bill_number VARCHAR(50) NOT NULL UNIQUE,
  tenant_id BIGINT NOT NULL,
  room_id BIGINT NOT NULL,
  pg_id BIGINT NOT NULL,
  owner_id BIGINT NOT NULL,
  
  -- Billing Period
  billing_period_start DATE NOT NULL,
  billing_period_end DATE NOT NULL,
  
  -- Charges Breakdown
  room_rent NUMERIC(10,2) DEFAULT 0.00,
  electricity_charge NUMERIC(10,2) DEFAULT 0.00,
  other_charges NUMERIC(10,2) DEFAULT 0.00,
  discount NUMERIC(10,2) DEFAULT 0.00,
  late_fee NUMERIC(10,2) DEFAULT 0.00,
  additional_deductions NUMERIC(10,2) DEFAULT 0.00,
  
  -- GST
  subtotal NUMERIC(10,2),
  gst_enabled BOOLEAN DEFAULT false,
  gst_percentage NUMERIC(5,2) DEFAULT 0.00,
  gst_amount NUMERIC(10,2) DEFAULT 0.00,
  
  -- Security Deposit
  security_deposit_used NUMERIC(10,2) DEFAULT 0.00,
  
  -- Total
  total_amount NUMERIC(10,2) NOT NULL,
  
  -- Electricity Details
  total_room_units NUMERIC(10,2),
  tenant_unit_share NUMERIC(10,2),
  
  -- Bill Status & Type
  bill_type VARCHAR(30) NOT NULL,  -- MONTHLY/SECURITY_DEPOSIT/FINAL_SETTLEMENT
  status VARCHAR(20) NOT NULL DEFAULT 'PENDING',  -- PENDING/PAID/OVERDUE/CANCELLED/SETTLED
  generation_type VARCHAR(20) NOT NULL DEFAULT 'MANUAL',  -- AUTO/MANUAL
  
  -- Dates
  generated_date TIMESTAMP NOT NULL,
  due_date DATE,
  
  -- Payment Info
  payment_proof_status VARCHAR(20),  -- UPLOADED/VERIFIED/REJECTED
  paid_at TIMESTAMP,
  rejection_attempts INT DEFAULT 0,
  
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  FOREIGN KEY (tenant_id) REFERENCES users(id),
  FOREIGN KEY (room_id) REFERENCES rooms(id),
  FOREIGN KEY (pg_id) REFERENCES pgs(id),
  FOREIGN KEY (owner_id) REFERENCES owners(id)
);

-- Bill Charges (Line items per bill)
CREATE TABLE bill_charges (
  id BIGSERIAL PRIMARY KEY,
  bill_id BIGINT NOT NULL,
  charge_name VARCHAR(100) NOT NULL,
  amount NUMERIC(10,2) NOT NULL,
  is_pro_rated BOOLEAN DEFAULT false,
  FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE
);

-- Bill Versions (Change history)
CREATE TABLE bill_versions (
  id BIGSERIAL PRIMARY KEY,
  bill_id BIGINT NOT NULL,
  version_number INT NOT NULL,
  changes TEXT,
  changed_by BIGINT,
  changed_at TIMESTAMP NOT NULL,
  FOREIGN KEY (bill_id) REFERENCES bills(id) ON DELETE CASCADE,
  FOREIGN KEY (changed_by) REFERENCES owners(id)
);

-- Standard Charges (Recurring additional charges)
CREATE TABLE standard_charges (
  id BIGSERIAL PRIMARY KEY,
  owner_id BIGINT NOT NULL,
  pg_id BIGINT NOT NULL,
  charge_name VARCHAR(100) NOT NULL,
  amount NUMERIC(10,2) NOT NULL,
  is_active BOOLEAN NOT NULL DEFAULT true,
  is_default BOOLEAN NOT NULL DEFAULT true,
  is_deleted BOOLEAN NOT NULL DEFAULT false,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  FOREIGN KEY (owner_id) REFERENCES owners(id),
  FOREIGN KEY (pg_id) REFERENCES pgs(id) ON DELETE CASCADE
);

-- Payment Proof History (Payment verification records)
CREATE TABLE payment_proof_history (
  id BIGSERIAL PRIMARY KEY,
  bill_id BIGINT NOT NULL,
  proof_url VARCHAR(500) NOT NULL,
  uploaded_at TIMESTAMP NOT NULL,
  status VARCHAR(20),  -- UPLOADED/VERIFIED/REJECTED
  rejection_reason TEXT,
  verified_at TIMESTAMP,
  verified_by BIGINT,
  attempt_number INT DEFAULT 1,
  payment_method VARCHAR(50),  -- UPI/BANK_TRANSFER/CASH/CHEQUE/OTHER
  transaction_id VARCHAR(100),
  transaction_date DATE,
  notes TEXT,
  created_at TIMESTAMP NOT NULL,
  updated_at TIMESTAMP,
  FOREIGN KEY (bill_id) REFERENCES bills(id),
  FOREIGN KEY (verified_by) REFERENCES owners(id)
);
```

---

## 5. AUTHENTICATION & AUTHORIZATION

### JWT Token Structure
```
Header:
{
  "alg": "HS256",
  "typ": "JWT"
}

Payload:
{
  "sub": "user@example.com",
  "userId": 123,
  "role": "USER",
  "iat": 1708000000,
  "exp": 1708086400   // 24 hours later
}

Signature: HMAC-SHA256(base64(header) + "." + base64(payload), secret)
```

### Security Rules
- **Unverified emails** cannot login (even with correct password)
- **OTP codes** are BCrypt hashed in database (never stored plaintext)
- **Password reset** requires OTP verification first
- **All endpoints** require JWT in Authorization header (except `/api/auth/**` and `/api/public/**`)
- **Owner endpoints** check `@PreAuthorize("hasRole('OWNER')")`
- **User endpoints** check `@PreAuthorize("hasRole('USER')")`
- **Meter access** verified by `MeterAuthorizationService`

### Authorization Checks (Key Logic)
```
Meter Operations:
├─ Can View Meter?
│  ├─ Owner of meter → YES
│  └─ Tenant with active booking in room → YES (read-only)
├─ Can Control Relay?
│  └─ Owner of meter → YES only
└─ Can Unpair Meter?
   └─ Owner of meter → YES only

Room Operations:
├─ Can Create Room?
│  └─ Owner of PG → YES
└─ Can Delete Room?
   └─ Owner of PG → YES

Booking Operations:
├─ Can Accept Booking?
│  └─ Owner of room's PG → YES
└─ Can Cancel Booking?
   └─ Creator of booking (User) → YES (if PENDING)
```

---

## 6. CONFIGURATION & ENVIRONMENT VARIABLES

### application.yml (Dev)
```yaml
spring:
  application:
    name: pg-management-system
  
  datasource:
    url: jdbc:postgresql://localhost:5432/smart_rooms_db
    username: postgres
    password: ******
    driver-class-name: org.postgresql.Driver
  
  jpa:
    hibernate:
      ddl-auto: update  # Use 'validate' in production
    show-sql: true
    properties:
      hibernate:
        format_sql: true
        dialect: org.hibernate.dialect.PostgreSQLDialect
  
  data:
    redis:
      host: localhost
      port: 6379
      timeout: 60000
  
  servlet:
    multipart:
      max-file-size: 10MB
      max-request-size: 10MB

jwt:
  secret: 8bfe293749db45ac998e12eaf45cd9837aa43ff23845123bb8834a5ec792ab74
  expiration-ms: 86400000  # 24 hours

sendgrid:
  api:
    key: ${SENDGRID_API_KEY}
  from:
    email: no-reply@smartmeeter.online
    name: PG Management System

aws:
  region: ap-south-1
  access-key: ${AWS_ACCESS_KEY}
  secret-key: ${AWS_SECRET_KEY}
  s3:
    bucket-name: pg-management-images
    region: ap-south-1
  iot:
    endpoint: https://your-iot-endpoint.iot.ap-south-1.amazonaws.com
  dynamodb:
    table-name: meter_readings

meter:
  online-threshold-seconds: 60
  relay-command-timeout-ms: 2000
  relay-command-retry-count: 3
  relay-command-retry-delay-ms: 500

server:
  port: 8080
```

### application-prod.yml
```yaml
spring:
  datasource:
    url: ${DATABASE_URL}
    username: ${DATABASE_USERNAME}
    password: ${DATABASE_PASSWORD}
  jpa:
    show-sql: false
    hibernate:
      ddl-auto: validate  # NEVER auto-update in production
```

---

## 7. API ENDPOINT REFERENCE

### Authentication Endpoints

```
POST /api/auth/owner/register
Content-Type: application/json
{
  "name": "John Doe",
  "email": "owner@example.com",
  "password": "SecurePass@123",
  "phone": "9876543210"
}
Response: 201 Created
{
  "success": true,
  "message": "Account created successfully! Please check your email for OTP."
}

POST /api/auth/user/register
Content-Type: application/json
{
  "name": "Alice Smith",
  "email": "tenant@example.com",
  "password": "SecurePass@123",
  "phone": "9876543210",
  "gender": "FEMALE"
}
Response: 201 Created

POST /api/auth/verify-otp
{
  "email": "tenant@example.com",
  "otp": "123456",
  "purpose": "USER_REGISTRATION"
}
Response: 200 OK
{
  "success": true,
  "message": "Email verified successfully! You are now logged in.",
  "data": {
    "token": "eyJhbGc...",
    "type": "Bearer",
    "userId": 5,
    "name": "Alice Smith",
    "email": "tenant@example.com",
    "role": "USER"
  }
}

POST /api/auth/resend-otp
{
  "email": "tenant@example.com"
}
Response: 200 OK

POST /api/auth/login
{
  "email": "tenant@example.com",
  "password": "SecurePass@123"
}
Response: 200 OK
{
  "success": true,
  "message": "Login successful",
  "data": {
    "token": "eyJhbGc...",
    "userId": 5,
    "role": "USER"
  }
}
```

### Owner PG Management

```
POST /api/owner/pg
Authorization: Bearer {token}
{
  "name": "Elite PG Mumbai",
  "address": "123 Main Street, Bandra",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400050",
  "description": "Premium PG with AC rooms",
  "genderPreference": "FEMALE",
  "imageUrls": ["https://s3.../img1.jpg"]
}
Response: 201 Created
{
  "success": true,
  "data": {
    "pgId": 1,
    "name": "Elite PG Mumbai",
    "city": "Mumbai",
    "totalRooms": 0,
    "availableRooms": 0
  }
}

GET /api/owner/pgs
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "data": [
    {
      "pgId": 1,
      "name": "Elite PG Mumbai",
      "totalRooms": 5,
      "availableRooms": 2
    }
  ]
}

PUT /api/owner/pg/{pgId}
Authorization: Bearer {token}
{
  "name": "Updated PG Name",
  "city": "Mumbai"
}
Response: 200 OK

DELETE /api/owner/pg/{pgId}
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "message": "PG deleted successfully"
}
```

### Owner Room Management

```
POST /api/owner/pg/{pgId}/room
Authorization: Bearer {token}
{
  "roomNumber": "101",
  "type": "SINGLE",
  "capacity": 1,
  "rent": 8000,
  "imageUrls": ["https://s3.../room1.jpg"],
  "amenityIds": [1, 2, 3]
}
Response: 201 Created
{
  "success": true,
  "data": {
    "roomId": 10,
    "roomNumber": "101",
    "type": "SINGLE",
    "capacity": 1,
    "rent": 8000,
    "status": "AVAILABLE"
  }
}

PUT /api/owner/pg/{pgId}/room/{roomId}
Authorization: Bearer {token}
{
  "rent": 9000,
  "capacity": 1
}
Response: 200 OK

DELETE /api/owner/pg/{pgId}/room/{roomId}
Authorization: Bearer {token}
Response: 200 OK
```

### Owner Booking Management

```
GET /api/owner/booking/requests?status=PENDING
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "data": [
    {
      "bookingId": 5,
      "userName": "Alice Smith",
      "userEmail": "alice@example.com",
      "roomNumber": "101",
      "pgName": "Elite PG",
      "requestDate": "2026-02-14T10:30:00",
      "status": "PENDING"
    }
  ]
}

POST /api/owner/booking/{bookingId}/accept
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "message": "Booking accepted. Tenant has been assigned to room."
}

POST /api/owner/booking/{bookingId}/reject
Authorization: Bearer {token}
{
  "rejectionReason": "Room dimensions too small for tenant requirement"
}
Response: 200 OK
```

### Tenant Booking Operations

```
GET /api/user/profile
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "data": {
    "userId": 5,
    "name": "Alice Smith",
    "email": "alice@example.com",
    "phone": "9876543210",
    "gender": "FEMALE",
    "hasActiveBooking": true
  }
}

GET /api/user/has-active-booking
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "data": true
}

GET /api/user/bookings
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "data": [
    {
      "bookingId": 5,
      "pgName": "Elite PG",
      "roomNumber": "101",
      "rent": 8000,
      "status": "ACCEPTED",
      "moveInDate": "2026-02-15T00:00:00"
    }
  ]
}

POST /api/user/booking/request
Authorization: Bearer {token}
{
  "roomId": 10
}
Response: 201 Created
{
  "success": true,
  "data": {
    "bookingId": 5,
    "status": "PENDING"
  }
}

DELETE /api/user/booking/{bookingId}
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "message": "Booking canceled successfully"
}
```

### Public Search & Browse

```
GET /api/public/pg/search?city=Mumbai&genderPreference=FEMALE&minRent=5000&maxRent=15000
Response: 200 OK
{
  "success": true,
  "data": [
    {
      "pgId": 1,
      "name": "Elite PG Mumbai",
      "address": "123 Main Street",
      "city": "Mumbai",
      "genderPreference": "FEMALE",
      "imageUrls": ["https://s3-presigned-url..."],
      "totalRooms": 5,
      "availableRooms": 2
    }
  ]
}

GET /api/public/pg/{pgId}
Response: 200 OK
{
  "success": true,
  "data": { ... PG details ... }
}

GET /api/public/room/{roomId}
Response: 200 OK
{
  "success": true,
  "data": {
    "roomId": 10,
    "roomNumber": "101",
    "type": "SINGLE",
    "capacity": 1,
    "currentOccupancy": 0,
    "availableSlots": 1,
    "rent": 8000,
    "imageUrls": ["https://s3-presigned..."],
    "amenities": ["WiFi", "AC", "Hot Water"]
  }
}
```

### Smart Meter Operations

```
POST /api/owner/meters/pair
Authorization: Bearer {token}
{
  "meterId": "METER_001",
  "roomId": 10
}
Response: 201 Created
{
  "success": true,
  "data": {
    "meterId": "METER_001",
    "awsThingName": "METER_001",
    "roomId": 10,
    "status": "ACTIVE",
    "pairedAt": "2026-02-14T12:00:00",
    "message": "Meter successfully paired to room"
  }
}

POST /api/owner/meters/{meterId}/unpair
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "data": {
    "meterId": "METER_001",
    "status": "UNPAIRED",
    "unpairedAt": "2026-02-14T12:05:00"
  }
}

GET /api/owner/meters
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "data": [
    {
      "meterId": "METER_001",
      "roomNumber": "101",
      "pgName": "Elite PG",
      "status": "ACTIVE",
      "pairedAt": "2026-02-14T12:00:00"
    }
  ]
}

GET /api/meters/room/{roomId}/live
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "data": {
    "meterId": "METER_001",
    "isOnline": true,
    "lastReadingTime": "2026-02-14T14:30:45Z",
    "timeSinceLastReading": "2 seconds ago",
    "readings": {
      "voltage": 230.5,
      "current": 2.3,
      "power": 530.15,
      "energy": 1250.75,
      "frequency": 50.0,
      "powerFactor": 0.95,
      "relayStatus": "ON"
    }
  }
}

GET /api/meters/{meterId}/live
Authorization: Bearer {token}
Response: 200 OK (same as above)

GET /api/meters/{meterId}/readings?start=1708000000000&end=1708100000000
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "data": {
    "meterId": "METER_001",
    "summary": {
      "totalReadings": 1440,
      "minPower": 100,
      "maxPower": 5000,
      "avgPower": 2500,
      "energyConsumed": 120.5
    },
    "readings": [
      {
        "timestamp": "2026-02-14T00:00:00Z",
        "power": 2500,
        "energy": 2500.5
      }
    ]
  }
}

POST /api/owner/meters/{meterId}/relay
Authorization: Bearer {token}
{
  "command": "ON"
}
Response: 200 OK
{
  "success": true,
  "data": {
    "meterId": "METER_001",
    "command": "ON",
    "status": "SUCCESS",
    "relayStatus": "ON",
    "timestamp": "2026-02-14T14:31:00Z"
  }
}

GET /api/tenant/my-room/meter/live
Authorization: Bearer {token}
Response: 200 OK (same as /api/meters/room/{roomId}/live)

GET /api/tenant/my-room/meter/readings?start=...&end=...
Authorization: Bearer {token}
Response: 200 OK (historical readings for tenant's room)
```

### S3 Image Operations

```
GET /api/s3/presigned-url?folder=pg-images&fileName=photo.jpg&contentType=image/jpeg
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "data": {
    "presignedUrl": "https://s3.amazonaws.com/pg-management-images/...",
    "fileKey": "pg-images/photo_uuid.jpg",
    "expirySeconds": 900
  }
}

POST /api/s3/upload
Authorization: Bearer {token}
Content-Type: multipart/form-data
file: @photo.jpg
folder: room-images
Response: 200 OK
{
  "success": true,
  "data": {
    "fileUrl": "https://s3.../room-images/photo_uuid.jpg",
    "fileName": "photo_uuid.jpg"
  }
}

DELETE /api/s3/delete?fileUrl=https://s3.../image.jpg
Authorization: Bearer {token}
Response: 200 OK
{
  "success": true,
  "message": "File deleted successfully"
}
```

---

## 8. COMPREHENSIVE API DOCUMENTATION WITH REQUEST/RESPONSE EXAMPLES

### API Base Configuration

```json
{
  "servers": [
    {
      "url": "http://localhost:8080",
      "description": "Development Server"
    },
    {
      "url": "https://api.smartmeeter.online",
      "description": "Production Server"
    }
  ],
  "authentication": "Bearer <JWT_TOKEN>",
  "jwt_expiry": "24 hours",
  "content_type": "application/json"
}
```

---

### SECTION 1: AUTHENTICATION APIs (Public - No Token Required)

#### 1.1 Owner Registration Endpoint

**POST** `/api/auth/owner/register`

**Request Body:**
```json
{
  "name": "John Doe",
  "email": "owner@smartmeeter.online",
  "password": "SecurePass@123",
  "phone": "9876543210"
}
```

**Request Validation Rules:**
- `name`: String, 2-100 characters (required)
- `email`: Valid email format (required, must be unique)
- `password`: 8-50 characters, must contain uppercase, lowercase, digit, special char (required)
- `phone`: Exactly 10 digits (required)

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Account created successfully! Check email for OTP",
  "data": {
    "id": 1,
    "email": "owner@smartmeeter.online",
    "name": "John Doe",
    "phone": "9876543210",
    "isVerified": false,
    "createdAt": "2026-02-16T10:30:00Z"
  }
}
```

**Response (400 Bad Request):**
```json
{
  "timestamp": "2026-02-16T10:30:00Z",
  "status": 400,
  "error": "Validation Failed",
  "message": "Email already exists",
  "path": "/api/auth/owner/register",
  "details": [
    {
      "field": "email",
      "message": "Email already exists"
    }
  ]
}
```

---

#### 1.2 Tenant/User Registration Endpoint

**POST** `/api/auth/user/register`

**Request Body:**
```json
{
  "name": "Alice Smith",
  "email": "alice@example.com",
  "password": "SecurePass@123",
  "phone": "9876543210",
  "gender": "FEMALE"
}
```

**Request Validation Rules:**
- `name`: String, 2-100 characters (required)
- `email`: Valid email format (required, must be unique)
- `password`: 8-50 characters with complexity (required)
- `phone`: Exactly 10 digits (required)
- `gender`: MALE / FEMALE / OTHER (required)

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Registration successful! OTP sent to email",
  "data": {
    "id": 5,
    "email": "alice@example.com",
    "name": "Alice Smith",
    "phone": "9876543210",
    "gender": "FEMALE",
    "isVerified": false,
    "createdAt": "2026-02-16T10:35:00Z"
  }
}
```

---

#### 1.3 OTP Verification & Login Endpoint

**POST** `/api/auth/verify-otp`

**Request Body:**
```json
{
  "email": "alice@example.com",
  "otp": "123456",
  "purpose": "USER_REGISTRATION"
}
```

**Request Validation Rules:**
- `email`: Valid email format (required)
- `otp`: Exactly 6 digits (required)
- `purpose`: USER_REGISTRATION / OWNER_REGISTRATION / RESET_PASSWORD / PASSWORD_SETUP (required)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "OTP verified successfully",
  "data": {
    "userId": 5,
    "email": "alice@example.com",
    "name": "Alice Smith",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": "86400000",
    "isVerified": true,
    "createdAt": "2026-02-16T10:35:00Z"
  }
}
```

**Response (400 Bad Request - Invalid OTP):**
```json
{
  "timestamp": "2026-02-16T10:30:00Z",
  "status": 400,
  "error": "Invalid OTP",
  "message": "OTP is invalid or has expired",
  "path": "/api/auth/verify-otp"
}
```

---

#### 1.4 Direct Login Endpoint

**POST** `/api/auth/login`

**Request Body:**
```json
{
  "email": "alice@example.com",
  "password": "SecurePass@123"
}
```

**Request Validation Rules:**
- `email`: Valid email format (required)
- `password`: String (required)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Login successful",
  "data": {
    "userId": 5,
    "email": "alice@example.com",
    "name": "Alice Smith",
    "userType": "TENANT",
    "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
    "expiresIn": "86400000",
    "isVerified": true
  }
}
```

**Response (401 Unauthorized):**
```json
{
  "timestamp": "2026-02-16T10:30:00Z",
  "status": 401,
  "error": "Authentication Failed",
  "message": "Invalid email or password",
  "path": "/api/auth/login"
}
```

---

### SECTION 2: TENANT/USER APIs (Require JWT Token)

#### 2.1 User Profile Management

**GET** `/api/user/profile`

**Headers:**
```
Authorization: Bearer eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile retrieved successfully",
  "data": {
    "id": 5,
    "name": "Alice Smith",
    "email": "alice@example.com",
    "phone": "9876543210",
    "gender": "FEMALE",
    "isVerified": true,
    "createdAt": "2026-02-16T10:35:00Z",
    "updatedAt": "2026-02-16T10:35:00Z"
  }
}
```

---

**PUT** `/api/user/profile`

**Request Body:**
```json
{
  "name": "Alice Updated",
  "phone": "9876543211",
  "gender": "FEMALE"
}
```

**Request Validation Rules:**
- `name`: String, 2-100 characters (optional)
- `phone`: Exactly 10 digits (optional)
- `gender`: MALE / FEMALE / OTHER (optional)

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Profile updated successfully",
  "data": {
    "id": 5,
    "name": "Alice Updated",
    "email": "alice@example.com",
    "phone": "9876543211",
    "gender": "FEMALE",
    "updatedAt": "2026-02-16T11:45:00Z"
  }
}
```

---

#### 2.2 Booking Management

**GET** `/api/user/has-active-booking`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Booking status retrieved",
  "data": {
    "hasActiveBooking": true,
    "bookingId": 12,
    "roomId": 10,
    "pgId": 1,
    "status": "APPROVED",
    "moveInDate": "2026-02-20",
    "moveOutDate": null
  }
}
```

---

**GET** `/api/user/bookings`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Bookings retrieved successfully",
  "data": [
    {
      "id": 12,
      "roomId": 10,
      "pgId": 1,
      "pgName": "Premium PG Mumbai",
      "roomNumber": "101",
      "status": "APPROVED",
      "requestDate": "2026-02-14T10:00:00Z",
      "responseDate": "2026-02-14T15:30:00Z",
      "moveInDate": "2026-02-20",
      "moveOutDate": null
    },
    {
      "id": 8,
      "roomId": 5,
      "pgId": 2,
      "pgName": "Standard PG Delhi",
      "roomNumber": "203",
      "status": "REJECTED",
      "requestDate": "2026-02-10T10:00:00Z",
      "responseDate": "2026-02-11T09:00:00Z",
      "rejectionReason": "Room already booked"
    }
  ]
}
```

---

**POST** `/api/user/booking/request`

**Request Body:**
```json
{
  "roomId": 10
}
```

**Request Validation Rules:**
- `roomId`: Long >= 1 (required)

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Booking request created successfully",
  "data": {
    "id": 13,
    "roomId": 10,
    "pgId": 1,
    "userId": 5,
    "status": "PENDING",
    "requestDate": "2026-02-16T12:00:00Z",
    "responseDate": null
  }
}
```

---

**DELETE** `/api/user/booking/{bookingId}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Booking cancelled successfully"
}
```

---

#### 2.3 Tenant Room & Meter Operations

**GET** `/api/tenant/my-room/details`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Room details retrieved",
  "data": {
    "id": 10,
    "roomNumber": "101",
    "type": "SINGLE",
    "capacity": 1,
    "rent": 12000.00,
    "pgId": 1,
    "pgName": "Premium PG Mumbai",
    "meterId": "METER_001",
    "amenities": [
      {
        "id": 1,
        "name": "WiFi",
        "description": "High-speed internet"
      },
      {
        "id": 2,
        "name": "AC",
        "description": "Air conditioning"
      }
    ],
    "imageUrls": [
      "https://s3.../room_101_1.jpg",
      "https://s3.../room_101_2.jpg"
    ]
  }
}
```

---

**GET** `/api/tenant/my-room/meter/live`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Live meter readings retrieved",
  "data": {
    "meterId": "METER_001",
    "roomId": 10,
    "timestamp": "2026-02-16T12:45:30Z",
    "voltage": 230.5,
    "current": 2.3,
    "power": 530,
    "energy": 1250.75,
    "powerFactor": 0.95,
    "frequency": 50.0,
    "isOnline": true,
    "lastStatusUpdate": "2026-02-16T12:45:30Z"
  }
}
```

---

**GET** `/api/tenant/my-room/meter/readings`

**Query Parameters:**
- `start`: Unix timestamp in milliseconds (required)
- `end`: Unix timestamp in milliseconds (required)

**Example:** `GET /api/tenant/my-room/meter/readings?start=1708000000000&end=1708100000000`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Historical readings retrieved",
  "data": {
    "meterId": "METER_001",
    "roomId": 10,
    "startTime": "2026-02-01T00:00:00Z",
    "endTime": "2026-02-16T12:45:30Z",
    "totalReadings": 384,
    "readings": [
      {
        "timestamp": "2026-02-01T00:00:00Z",
        "voltage": 230.2,
        "current": 1.8,
        "power": 414,
        "energy": 1200.50,
        "powerFactor": 0.96
      },
      {
        "timestamp": "2026-02-01T01:00:00Z",
        "voltage": 230.1,
        "current": 1.5,
        "power": 345,
        "energy": 1201.85,
        "powerFactor": 0.96
      }
    ]
  }
}
```

---

### SECTION 3: OWNER APIs (Require JWT Token with Owner Role)

#### 3.1 PG & Room Management

**POST** `/api/owner/pg`

**Request Body:**
```json
{
  "name": "Premium PG Mumbai",
  "address": "123 Marine Drive, Bandra",
  "city": "Mumbai",
  "state": "Maharashtra",
  "pincode": "400050",
  "genderPreference": "FEMALE",
  "description": "Luxury PG near beach with all amenities",
  "imageUrls": [
    "https://s3.../pg_image_1.jpg",
    "https://s3.../pg_image_2.jpg"
  ]
}
```

**Request Validation Rules:**
- `name`: String, 3-200 characters (required)
- `address`: String (required)
- `city`: String, 2-100 characters (required)
- `state`: String (required)
- `pincode`: Exactly 6 digits (required)
- `genderPreference`: MALE / FEMALE / BOTH (required)
- `description`: String (optional)
- `imageUrls`: Max 5 URLs (optional)

**Response (201 Created):**
```json
{
  "success": true,
  "message": "PG created successfully",
  "data": {
    "id": 1,
    "name": "Premium PG Mumbai",
    "address": "123 Marine Drive, Bandra",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400050",
    "genderPreference": "FEMALE",
    "ownerId": 1,
    "description": "Luxury PG near beach with all amenities",
    "imageUrls": [
      "https://s3.../pg_image_1.jpg",
      "https://s3.../pg_image_2.jpg"
    ],
    "createdAt": "2026-02-16T10:00:00Z"
  }
}
```

---

**GET** `/api/owner/pgs`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PG list retrieved successfully",
  "data": [
    {
      "id": 1,
      "name": "Premium PG Mumbai",
      "address": "123 Marine Drive, Bandra",
      "city": "Mumbai",
      "state": "Maharashtra",
      "pincode": "400050",
      "genderPreference": "FEMALE",
      "totalRooms": 15,
      "occupiedRooms": 12,
      "availableRooms": 3,
      "createdAt": "2026-02-16T10:00:00Z"
    }
  ]
}
```

---

**POST** `/api/owner/pg/{pgId}/room`

**Request Body:**
```json
{
  "roomNumber": "101",
  "type": "SINGLE",
  "capacity": 1,
  "rent": 12000.00,
  "amenityIds": [1, 2, 3],
  "imageUrls": [
    "https://s3.../room_101_1.jpg",
    "https://s3.../room_101_2.jpg"
  ]
}
```

**Request Validation Rules:**
- `roomNumber`: String, 1-20 characters (required)
- `type`: SINGLE / DOUBLE / SHARED / DORMITORY (required)
- `capacity`: Integer, 1-20 (required)
- `rent`: Decimal, must be > 0 (required)
- `amenityIds`: Array of Long (optional)
- `imageUrls`: Max 10 URLs (optional)

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Room created successfully",
  "data": {
    "id": 10,
    "roomNumber": "101",
    "type": "SINGLE",
    "capacity": 1,
    "rent": 12000.00,
    "pgId": 1,
    "isAvailable": true,
    "amenities": [
      {
        "id": 1,
        "name": "WiFi"
      },
      {
        "id": 2,
        "name": "AC"
      }
    ],
    "imageUrls": [
      "https://s3.../room_101_1.jpg",
      "https://s3.../room_101_2.jpg"
    ],
    "createdAt": "2026-02-16T10:30:00Z"
  }
}
```

---

#### 3.2 Booking Request Management

**GET** `/api/owner/booking-requests/pg/{pgId}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Booking requests retrieved",
  "data": [
    {
      "id": 12,
      "userId": 5,
      "userName": "Alice Smith",
      "userEmail": "alice@example.com",
      "userPhone": "9876543210",
      "userGender": "FEMALE",
      "roomId": 10,
      "roomNumber": "101",
      "status": "PENDING",
      "requestDate": "2026-02-16T10:00:00Z",
      "responseDate": null
    }
  ]
}
```

---

**PUT** `/api/owner/booking-request/{requestId}/approve`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Booking request approved successfully",
  "data": {
    "id": 12,
    "status": "APPROVED",
    "responseDate": "2026-02-16T14:30:00Z"
  }
}
```

---

**PUT** `/api/owner/booking-request/{requestId}/reject`

**Request Body:**
```json
{
  "rejectionReason": "Room already booked"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Booking request rejected successfully",
  "data": {
    "id": 12,
    "status": "REJECTED",
    "rejectionReason": "Room already booked",
    "responseDate": "2026-02-16T14:30:00Z"
  }
}
```

---

### SECTION 4: COMPREHENSIVE BILLING APIs

#### 4.1 Owner Billing Configuration

**POST** `/api/owner/billing/config`

**Request Body:**
```json
{
  "pgId": 1,
  "upiId": "owner@upi",
  "upiName": "Owner Name",
  "bankAccountNumber": "1234567890",
  "bankIfscCode": "SBIN0001234",
  "bankAccountName": "Owner Name",
  "bankName": "State Bank of India",
  "bankBranch": "Mumbai Main",
  "gstEnabled": true,
  "gstNumber": "27AABCT1234H1Z0",
  "gstPercentage": 18.00,
  "billingMode": "AUTO",
  "autoGenerationDay": 1,
  "dueDaysOffset": 7,
  "electricityRatePerUnit": 8.50,
  "includeElectricity": true,
  "electricitySplitMethod": "TIME_WEIGHTED",
  "lateFeeEnabled": true,
  "lateFeeAmount": 200.00,
  "lateFeeDays": 3,
  "proRatedRentMode": "DAILY",
  "securityDepositAmount": 10000.00,
  "securityDepositDueDays": 7,
  "autoApplyStandardCharges": true
}
```

**Request Validation Rules:**
- `pgId`: Long >= 1 (required)
- `upiId`: String (optional)
- `gstNumber`: Valid GST format (optional)
- `gstPercentage`: Decimal 0-100 (optional)
- `billingMode`: AUTO / MANUAL (required)
- `electricitySplitMethod`: TIME_WEIGHTED / EQUAL (optional)
- `lateFeeAmount`: Decimal >= 0 (optional)
- `proRatedRentMode`: FULL_MONTH / HALF_MONTH / DAILY (optional)

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Billing configuration created successfully",
  "data": {
    "id": 1,
    "pgId": 1,
    "pgName": "Premium PG Mumbai",
    "upiId": "owner@upi",
    "gstEnabled": true,
    "gstPercentage": 18.00,
    "billingMode": "AUTO",
    "electricityRatePerUnit": 8.50,
    "lateFeeEnabled": true,
    "lateFeeAmount": 200.00,
    "proRatedRentMode": "DAILY",
    "securityDepositAmount": 10000.00,
    "createdAt": "2026-02-16T10:00:00Z",
    "updatedAt": "2026-02-16T10:00:00Z"
  }
}
```

---

**GET** `/api/owner/billing/config/pg/{pgId}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Billing configuration retrieved",
  "data": {
    "id": 1,
    "pgId": 1,
    "pgName": "Premium PG Mumbai",
    "upiId": "owner@upi",
    "bankAccountNumber": "****67890",
    "gstEnabled": true,
    "gstNumber": "27AABCT1234H1Z0",
    "gstPercentage": 18.00,
    "billingMode": "AUTO",
    "autoGenerationDay": 1,
    "electricityRatePerUnit": 8.50,
    "electricitySplitMethod": "TIME_WEIGHTED",
    "lateFeeEnabled": true,
    "lateFeeAmount": 200.00,
    "lateFeeDays": 3,
    "proRatedRentMode": "DAILY",
    "securityDepositAmount": 10000.00,
    "securityDepositDueDays": 7,
    "autoApplyStandardCharges": true
  }
}
```

---

**PUT** `/api/owner/billing/config/pg/{pgId}`

**Request Body:**
```json
{
  "gstPercentage": 18.00,
  "electricityRatePerUnit": 9.00,
  "lateFeeAmount": 250.00,
  "lateFeeEnabled": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Billing configuration updated successfully",
  "data": {
    "id": 1,
    "pgId": 1,
    "gstPercentage": 18.00,
    "electricityRatePerUnit": 9.00,
    "lateFeeAmount": 250.00,
    "updatedAt": "2026-02-16T11:00:00Z"
  }
}
```

---

#### 4.2 Standard Charges Management

**POST** `/api/owner/billing/charges`

**Request Body:**
```json
{
  "pgId": 1,
  "chargeName": "WiFi Charge",
  "amount": 500.00,
  "isActive": true,
  "isDefault": true
}
```

**Request Validation Rules:**
- `pgId`: Long >= 1 (required)
- `chargeName`: String, 3-50 characters (required)
- `amount`: Decimal > 0 (required)
- `isActive`: Boolean (required)
- `isDefault`: Boolean (optional)

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Standard charge created successfully",
  "data": {
    "id": 15,
    "pgId": 1,
    "chargeName": "WiFi Charge",
    "amount": 500.00,
    "isActive": true,
    "isDefault": true,
    "createdAt": "2026-02-16T10:00:00Z"
  }
}
```

---

**GET** `/api/owner/billing/charges/pg/{pgId}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Standard charges retrieved",
  "data": [
    {
      "id": 15,
      "pgId": 1,
      "chargeName": "WiFi Charge",
      "amount": 500.00,
      "isActive": true,
      "isDefault": true,
      "createdAt": "2026-02-16T10:00:00Z"
    },
    {
      "id": 16,
      "pgId": 1,
      "chargeName": "Maintenance Charge",
      "amount": 300.00,
      "isActive": true,
      "isDefault": false,
      "createdAt": "2026-02-16T10:05:00Z"
    }
  ]
}
```

---

**PUT** `/api/owner/billing/charges/{chargeId}/pg/{pgId}`

**Request Body:**
```json
{
  "chargeName": "WiFi Charge",
  "amount": 600.00,
  "isActive": true
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Standard charge updated successfully",
  "data": {
    "id": 15,
    "pgId": 1,
    "chargeName": "WiFi Charge",
    "amount": 600.00,
    "isActive": true,
    "updatedAt": "2026-02-16T11:00:00Z"
  }
}
```

---

**PATCH** `/api/owner/billing/charges/{chargeId}/pg/{pgId}/toggle`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Charge status toggled successfully",
  "data": {
    "id": 15,
    "chargeName": "WiFi Charge",
    "isActive": false,
    "updatedAt": "2026-02-16T11:05:00Z"
  }
}
```

---

#### 4.3 Manual Bill Generation

**POST** `/api/owner/billing/bills/generate/pg/{pgId}`

**Request Body:**
```json
{
  "tenantId": 5,
  "billingPeriodStart": "2026-02-01",
  "billingPeriodEnd": "2026-02-28",
  "discount": 500.00,
  "otherCharges": 200.00,
  "additionalDeductions": 100.00
}
```

**Request Validation Rules:**
- `tenantId`: Long >= 1 (required)
- `billingPeriodStart`: Date in YYYY-MM-DD format (required)
- `billingPeriodEnd`: Date in YYYY-MM-DD format (required, must be after start)
- `discount`: Decimal >= 0 (optional)
- `otherCharges`: Decimal >= 0 (optional)
- `additionalDeductions`: Decimal >= 0 (optional)

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Bill generated successfully",
  "data": {
    "id": 25,
    "billNumber": "BILL-PG001-2026-02-001",
    "pgId": 1,
    "tenantId": 5,
    "tenantName": "Alice Smith",
    "roomId": 10,
    "billingPeriodStart": "2026-02-01",
    "billingPeriodEnd": "2026-02-28",
    "status": "PENDING",
    "billType": "MONTHLY",
    "roomRent": 12000.00,
    "electricityCharge": 450.50,
    "gstAmount": 2187.09,
    "discount": 500.00,
    "otherCharges": 200.00,
    "additionalDeductions": 100.00,
    "lateFee": 0.00,
    "securityDeposit": 0.00,
    "totalAmount": 14237.59,
    "charges": [
      {
        "chargeId": 1,
        "chargeName": "WiFi Charge",
        "amount": 500.00,
        "isProRated": false
      },
      {
        "chargeId": 2,
        "chargeName": "Maintenance Charge",
        "amount": 300.00,
        "isProRated": false
      }
    ],
    "createdAt": "2026-02-16T10:00:00Z"
  }
}
```

---

**POST** `/api/owner/billing/bills/generate/bulk/pg/{pgId}`

**Request Body:**
```json
{
  "billingPeriodStart": "2026-02-01",
  "billingPeriodEnd": "2026-02-28"
}
```

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Bulk bills generated successfully",
  "data": {
    "pgId": 1,
    "billingPeriodStart": "2026-02-01",
    "billingPeriodEnd": "2026-02-28",
    "totalTenantsProcessed": 12,
    "billsCreatedCount": 12,
    "billsFailedCount": 0,
    "generatedBills": [
      {
        "billId": 25,
        "billNumber": "BILL-PG001-2026-02-001",
        "tenantId": 5,
        "tenantName": "Alice Smith",
        "totalAmount": 14237.59,
        "status": "PENDING"
      }
    ],
    "createdAt": "2026-02-16T10:00:00Z"
  }
}
```

---

#### 4.4 Bill Viewing & Management

**GET** `/api/owner/billing/bills/pg/{pgId}`

**Query Parameters:**
- `page`: Integer >= 0 (optional, default: 0)
- `size`: Integer >= 1 (optional, default: 10)
- `status`: PENDING / PAID / OVERDUE / CANCELLED / SETTLED (optional)

**Example:** `GET /api/owner/billing/bills/pg/1?page=0&size=10&status=PENDING`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Bills retrieved successfully",
  "data": {
    "content": [
      {
        "id": 25,
        "billNumber": "BILL-PG001-2026-02-001",
        "pgId": 1,
        "tenantId": 5,
        "tenantName": "Alice Smith",
        "roomNumber": "101",
        "billingPeriodStart": "2026-02-01",
        "billingPeriodEnd": "2026-02-28",
        "status": "PENDING",
        "totalAmount": 14237.59,
        "paidAmount": 0.00,
        "dueDate": "2026-03-07",
        "createdAt": "2026-02-16T10:00:00Z"
      }
    ],
    "totalElements": 1,
    "totalPages": 1,
    "currentPage": 0,
    "pageSize": 10
  }
}
```

---

**GET** `/api/owner/billing/bills/{billId}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Bill details retrieved",
  "data": {
    "id": 25,
    "billNumber": "BILL-PG001-2026-02-001",
    "pgId": 1,
    "pgName": "Premium PG Mumbai",
    "tenantId": 5,
    "tenantName": "Alice Smith",
    "tenantEmail": "alice@example.com",
    "tenantPhone": "9876543210",
    "roomId": 10,
    "roomNumber": "101",
    "status": "PENDING",
    "billType": "MONTHLY",
    "billingPeriodStart": "2026-02-01",
    "billingPeriodEnd": "2026-02-28",
    "roomRent": 12000.00,
    "electricityCharge": 450.50,
    "standardCharges": 800.00,
    "discount": 500.00,
    "otherCharges": 200.00,
    "additionalDeductions": 100.00,
    "lateFee": 0.00,
    "securityDeposit": 0.00,
    "subtotal": 12850.50,
    "gstAmount": 2187.09,
    "totalAmount": 15037.59,
    "paidAmount": 0.00,
    "remainingAmount": 15037.59,
    "dueDate": "2026-03-07",
    "charges": [
      {
        "id": 1,
        "chargeName": "WiFi Charge",
        "amount": 500.00,
        "isProRated": false
      },
      {
        "id": 2,
        "chargeName": "Maintenance Charge",
        "amount": 300.00,
        "isProRated": false
      }
    ],
    "paymentProofs": [],
    "createdAt": "2026-02-16T10:00:00Z",
    "updatedAt": "2026-02-16T10:00:00Z"
  }
}
```

---

**POST** `/api/owner/billing/bills/{billId}/edit`

**Request Body:**
```json
{
  "discount": 1000.00,
  "additionalDeductions": 200.00,
  "otherCharges": 300.00,
  "editReason": "Tenant complaint about meter reading"
}
```

**Request Validation Rules:**
- `discount`: Decimal >= 0 (optional)
- `additionalDeductions`: Decimal >= 0 (optional)
- `otherCharges`: Decimal >= 0 (optional)
- `editReason`: String, max 500 characters (optional)
- **Note**: Only editable before payment

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Bill edited successfully",
  "data": {
    "id": 25,
    "billNumber": "BILL-PG001-2026-02-001",
    "discount": 1000.00,
    "additionalDeductions": 200.00,
    "otherCharges": 300.00,
    "totalAmount": 14537.59,
    "editReason": "Tenant complaint about meter reading",
    "lastEditedAt": "2026-02-16T11:00:00Z"
  }
}
```

---

**POST** `/api/owner/billing/bills/{billId}/cancel`

**Request Body:**
```json
{
  "cancellationReason": "Tenant moved out early"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Bill cancelled successfully",
  "data": {
    "id": 25,
    "billNumber": "BILL-PG001-2026-02-001",
    "status": "CANCELLED",
    "cancellationReason": "Tenant moved out early",
    "cancelledAt": "2026-02-16T11:00:00Z"
  }
}
```

---

#### 4.5 Payment Verification (Owner)

**GET** `/api/owner/billing/payments/pg/{pgId}`

**Query Parameters:**
- `status`: UPLOADED / VERIFIED / REJECTED (optional)

**Example:** `GET /api/owner/billing/payments/pg/1?status=UPLOADED`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Payment proofs retrieved",
  "data": [
    {
      "id": 5,
      "billId": 25,
      "billNumber": "BILL-PG001-2026-02-001",
      "tenantId": 5,
      "tenantName": "Alice Smith",
      "totalBillAmount": 15037.59,
      "paymentMethod": "UPI",
      "transactionId": "TXN123456789",
      "status": "UPLOADED",
      "proofUrl": "https://s3.../payment_proof_5.jpg",
      "uploadedAt": "2026-02-16T15:30:00Z",
      "attemptNumber": 1
    }
  ]
}
```

---

**POST** `/api/owner/billing/payments/{paymentProofId}/approve`

**Request Body:**
```json
{
  "notes": "Payment verified via UPI transfer"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Payment approved successfully",
  "data": {
    "id": 5,
    "status": "VERIFIED",
    "notes": "Payment verified via UPI transfer",
    "verifiedAt": "2026-02-16T15:40:00Z",
    "billStatus": "PAID"
  }
}
```

---

**POST** `/api/owner/billing/payments/{paymentProofId}/reject`

**Request Body:**
```json
{
  "rejectionReason": "Transaction ID doesn't match",
  "instructions": "Please upload correct payment proof"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Payment rejected successfully",
  "data": {
    "id": 5,
    "status": "REJECTED",
    "rejectionReason": "Transaction ID doesn't match",
    "instructions": "Please upload correct payment proof",
    "rejectedAt": "2026-02-16T15:40:00Z",
    "canReupload": true
  }
}
```

---

#### 4.6 Checkout & Settlement

**POST** `/api/owner/billing/checkout`

**Request Body:**
```json
{
  "tenantId": 5,
  "roomId": 10,
  "pgId": 1,
  "moveOutDate": "2026-02-28",
  "securityDepositDeduction": 500.00,
  "deductionReason": "Damage to furniture"
}
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Checkout settlement completed",
  "data": {
    "id": 3,
    "tenantId": 5,
    "tenantName": "Alice Smith",
    "roomId": 10,
    "roomNumber": "101",
    "moveInDate": "2025-08-01",
    "moveOutDate": "2026-02-28",
    "stayDays": 182,
    "lastBillId": 25,
    "lastBillStatus": "PAID",
    "securityDepositAmount": 10000.00,
    "securityDepositDeduction": 500.00,
    "securityDepositRefund": 9500.00,
    "finalSettlementAmount": 9500.00,
    "refundStatus": "PROCESSED",
    "completedAt": "2026-02-28T10:00:00Z"
  }
}
```

---

### SECTION 5: TENANT/USER BILLING APIs

#### 5.1 Tenant Billing Dashboard

**GET** `/api/tenant/billing/dashboard`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Billing dashboard retrieved",
  "data": {
    "tenantId": 5,
    "tenantName": "Alice Smith",
    "roomId": 10,
    "roomNumber": "101",
    "pgId": 1,
    "pgName": "Premium PG Mumbai",
    "totalBillsGenerated": 12,
    "totalPaid": 168000.00,
    "totalPending": 15037.59,
    "totalOverdue": 5000.00,
    "pendingBills": 1,
    "overdueBills": 1,
    "lastBillDate": "2026-02-01",
    "nextBillDate": "2026-03-01",
    "billsSummary": {
      "pending": 1,
      "paid": 11,
      "overdue": 1,
      "cancelled": 0
    }
  }
}
```

---

#### 5.2 Tenant View Bills

**GET** `/api/tenant/billing/bills`

**Query Parameters:**
- `page`: Integer >= 0 (optional, default: 0)
- `size`: Integer >= 1 (optional, default: 10)

**Example:** `GET /api/tenant/billing/bills?page=0&size=10`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Bills retrieved successfully",
  "data": {
    "content": [
      {
        "id": 25,
        "billNumber": "BILL-PG001-2026-02-001",
        "billingPeriodStart": "2026-02-01",
        "billingPeriodEnd": "2026-02-28",
        "status": "PENDING",
        "totalAmount": 15037.59,
        "paidAmount": 0.00,
        "dueDate": "2026-03-07",
        "createdAt": "2026-02-16T10:00:00Z"
      }
    ],
    "totalElements": 12,
    "totalPages": 2,
    "currentPage": 0,
    "pageSize": 10
  }
}
```

---

**GET** `/api/tenant/billing/bills/{billId}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Bill details retrieved",
  "data": {
    "id": 25,
    "billNumber": "BILL-PG001-2026-02-001",
    "pgName": "Premium PG Mumbai",
    "roomNumber": "101",
    "status": "PENDING",
    "billingPeriodStart": "2026-02-01",
    "billingPeriodEnd": "2026-02-28",
    "roomRent": 12000.00,
    "electricityCharge": 450.50,
    "standardCharges": 800.00,
    "discount": 500.00,
    "otherCharges": 200.00,
    "additionalDeductions": 100.00,
    "subtotal": 12850.50,
    "gstAmount": 2187.09,
    "totalAmount": 15037.59,
    "paidAmount": 0.00,
    "remainingAmount": 15037.59,
    "dueDate": "2026-03-07",
    "charges": [
      {
        "chargeName": "WiFi Charge",
        "amount": 500.00
      },
      {
        "chargeName": "Maintenance Charge",
        "amount": 300.00
      }
    ],
    "paymentMethods": {
      "upi": "owner@upi",
      "bank": "State Bank of India - ****7890",
      "cheque": "Available"
    },
    "createdAt": "2026-02-16T10:00:00Z"
  }
}
```

---

#### 5.3 Tenant Payment Proof Upload

**POST** `/api/tenant/billing/payments/{billId}/upload`

**Form Data (Multipart):**
- `file`: Binary file (image/pdf) - max 10MB (required)
- `paymentMethod`: UPI / BANK_TRANSFER / CASH / CHEQUE / OTHER (required)
- `transactionId`: String, max 50 characters (required for UPI/Bank)
- `transactionDate`: Date in YYYY-MM-DD format (required)
- `notes`: String, max 500 characters (optional)

**Response (201 Created):**
```json
{
  "success": true,
  "message": "Payment proof uploaded successfully. Awaiting verification.",
  "data": {
    "id": 5,
    "billId": 25,
    "billNumber": "BILL-PG001-2026-02-001",
    "tenantId": 5,
    "paymentMethod": "UPI",
    "transactionId": "TXN123456",
    "status": "UPLOADED",
    "proofUrl": "https://s3.../payment_proof_5.jpg",
    "uploadedAt": "2026-02-16T15:30:00Z",
    "attemptNumber": 1,
    "message": "Proof uploaded. Owner will verify within 24 hours."
  }
}
```

---

**GET** `/api/tenant/billing/payments/{billId}/history`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Payment proof history retrieved",
  "data": {
    "billId": 25,
    "billNumber": "BILL-PG001-2026-02-001",
    "totalAttempts": 2,
    "history": [
      {
        "id": 5,
        "attemptNumber": 1,
        "paymentMethod": "UPI",
        "transactionId": "TXN123456",
        "status": "UPLOADED",
        "uploadedAt": "2026-02-16T15:30:00Z",
        "proofUrl": "https://s3.../payment_proof_5.jpg"
      },
      {
        "id": 6,
        "attemptNumber": 2,
        "paymentMethod": "UPI",
        "transactionId": "TXN789012",
        "status": "VERIFIED",
        "uploadedAt": "2026-02-16T16:00:00Z",
        "verifiedAt": "2026-02-16T16:15:00Z",
        "proofUrl": "https://s3.../payment_proof_6.jpg"
      }
    ]
  }
}
```

---

#### 5.4 UPI Payment Link Generation

**GET** `/api/tenant/billing/bills/{billId}/upi-link`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "UPI payment link generated",
  "data": {
    "billId": 25,
    "billNumber": "BILL-PG001-2026-02-001",
    "payeeUpiId": "owner@upi",
    "payeeName": "Owner Name",
    "amount": 15037.59,
    "description": "Bill BILL-PG001-2026-02-001 for Feb 2026",
    "upiLink": "upi://pay?pa=owner@upi&pn=Owner%20Name&am=15037.59&tn=Bill%20BILL-PG001-2026-02-001%20for%20Feb%202026&tr=BILL25",
    "qrCode": "data:image/png;base64,iVBORw0KGgoAAAANS...",
    "expiresAt": "2026-03-07T23:59:59Z"
  }
}
```

---

### SECTION 6: PUBLIC APIs (No Authentication Required)

#### 6.1 PG Search & Browsing

**GET** `/api/public/pg/search`

**Query Parameters:**
- `city`: String (optional)
- `genderPreference`: MALE / FEMALE / BOTH (optional)
- `roomType`: SINGLE / DOUBLE / SHARED / DORMITORY (optional)
- `minRent`: Integer >= 0 (optional)
- `maxRent`: Integer >= 0 (optional)
- `amenities`: Comma-separated list (optional, e.g., "WIFI,AC,TV")
- `page`: Integer >= 0 (optional, default: 0)
- `size`: Integer >= 1 (optional, default: 20)

**Example:**
```
GET /api/public/pg/search?city=Mumbai&genderPreference=FEMALE&roomType=SINGLE&minRent=5000&maxRent=15000&amenities=WIFI,AC&page=0&size=10
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PGs retrieved successfully",
  "data": {
    "content": [
      {
        "id": 1,
        "name": "Premium PG Mumbai",
        "address": "123 Marine Drive, Bandra",
        "city": "Mumbai",
        "state": "Maharashtra",
        "pincode": "400050",
        "genderPreference": "FEMALE",
        "description": "Luxury PG near beach with all amenities",
        "imageUrls": [
          "https://s3.../pg_image_1.jpg"
        ],
        "availableRooms": 3,
        "totalRooms": 15,
        "minRent": 10000.00,
        "maxRent": 15000.00
      }
    ],
    "totalElements": 25,
    "totalPages": 3,
    "currentPage": 0,
    "pageSize": 10
  }
}
```

---

**GET** `/api/public/pg/{pgId}`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "PG details retrieved",
  "data": {
    "id": 1,
    "name": "Premium PG Mumbai",
    "address": "123 Marine Drive, Bandra",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400050",
    "genderPreference": "FEMALE",
    "description": "Luxury PG near beach with all amenities",
    "ownerName": "John Doe",
    "ownerPhone": "9876543210",
    "ownerEmail": "owner@smartmeeter.online",
    "imageUrls": [
      "https://s3.../pg_image_1.jpg",
      "https://s3.../pg_image_2.jpg"
    ],
    "totalRooms": 15,
    "availableRooms": 3,
    "amenities": [
      {
        "id": 1,
        "name": "WiFi",
        "description": "High-speed internet"
      }
    ],
    "createdAt": "2026-02-16T10:00:00Z"
  }
}
```

---

**GET** `/api/public/pg/{pgId}/rooms`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Rooms retrieved successfully",
  "data": [
    {
      "id": 10,
      "roomNumber": "101",
      "type": "SINGLE",
      "capacity": 1,
      "rent": 12000.00,
      "isAvailable": true,
      "amenities": [
        {
          "id": 1,
          "name": "WiFi"
        }
      ],
      "imageUrls": [
        "https://s3.../room_101_1.jpg"
      ]
    }
  ]
}
```

---

### SECTION 7: METER APIs (Public with Optional Auth)

#### 7.1 Live Readings

**GET** `/api/meters/{meterId}/live`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Live meter data retrieved",
  "data": {
    "meterId": "METER_001",
    "timestamp": "2026-02-16T12:45:30Z",
    "voltage": 230.5,
    "current": 2.3,
    "power": 530,
    "energy": 1250.75,
    "powerFactor": 0.95,
    "frequency": 50.0,
    "isOnline": true,
    "lastStatusUpdate": "2026-02-16T12:45:30Z"
  }
}
```

---

**GET** `/api/meters/room/{roomId}/live`

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Live meter data retrieved",
  "data": {
    "roomId": 10,
    "meterId": "METER_001",
    "timestamp": "2026-02-16T12:45:30Z",
    "voltage": 230.5,
    "current": 2.3,
    "power": 530,
    "energy": 1250.75,
    "powerFactor": 0.95,
    "frequency": 50.0,
    "isOnline": true
  }
}
```

---

#### 7.2 Historical Readings

**GET** `/api/meters/{meterId}/readings`

**Query Parameters:**
- `start`: Unix timestamp in milliseconds (required)
- `end`: Unix timestamp in milliseconds (required)

**Example:**
```
GET /api/meters/METER_001/readings?start=1708000000000&end=1708100000000
```

**Response (200 OK):**
```json
{
  "success": true,
  "message": "Historical readings retrieved",
  "data": {
    "meterId": "METER_001",
    "startTime": "2026-02-01T00:00:00Z",
    "endTime": "2026-02-16T12:45:30Z",
    "totalReadings": 384,
    "readings": [
      {
        "timestamp": "2026-02-01T00:00:00Z",
        "voltage": 230.2,
        "current": 1.8,
        "power": 414,
        "energy": 1200.50,
        "powerFactor": 0.96,
        "frequency": 50.0
      }
    ]
  }
}
```

---

### SECTION 8: UNIVERSAL RESPONSE FORMAT & STATUS CODES

#### Standard HTTP Status Codes

**200 OK** - Successful GET, PUT, PATCH operations
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": { ... }
}
```

**201 Created** - Successful POST operations (resource created)
```json
{
  "success": true,
  "message": "Resource created successfully",
  "data": { ... }
}
```

**400 Bad Request** - Validation failed, invalid input
```json
{
  "timestamp": "2026-02-16T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already exists",
  "path": "/api/auth/user/register",
  "details": [
    {
      "field": "email",
      "message": "Email already exists"
    }
  ]
}
```

**401 Unauthorized** - Missing or invalid JWT token
```json
{
  "timestamp": "2026-02-16T12:00:00Z",
  "status": 401,
  "error": "Unauthorized",
  "message": "JWT token has expired or is invalid",
  "path": "/api/owner/pgs"
}
```

**403 Forbidden** - Insufficient permissions
```json
{
  "timestamp": "2026-02-16T12:00:00Z",
  "status": 403,
  "error": "Forbidden",
  "message": "You do not have permission to access this resource",
  "path": "/api/owner/pgs"
}
```

**404 Not Found** - Resource doesn't exist
```json
{
  "timestamp": "2026-02-16T12:00:00Z",
  "status": 404,
  "error": "Not Found",
  "message": "PG with ID 999 not found",
  "path": "/api/owner/pgs/999"
}
```

**500 Internal Server Error** - Server-side error
```json
{
  "timestamp": "2026-02-16T12:00:00Z",
  "status": 500,
  "error": "Internal Server Error",
  "message": "An unexpected error occurred. Please try again later.",
  "path": "/api/owner/pgs"
}
```

---

### Complete CURL Command Examples

#### AUTHENTICATION ENDPOINTS

```bash
# 1. Owner Registration
curl -X POST http://localhost:8080/api/auth/owner/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John Doe",
    "email": "owner@smartmeeter.online",
    "password": "SecurePass@123",
    "phone": "9876543210"
  }'
# Response: 201 Created - "Account created successfully! Check email for OTP"

# 2. User/Tenant Registration
curl -X POST http://localhost:8080/api/auth/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "Alice Smith",
    "email": "alice@example.com",
    "password": "SecurePass@123",
    "phone": "9876543210",
    "gender": "FEMALE"
  }'

# 3. Verify OTP (Get JWT Token)
curl -X POST http://localhost:8080/api/auth/verify-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "otp": "123456",
    "purpose": "USER_REGISTRATION"
  }'
# Response: 200 OK
# {
#   "success": true,
#   "data": {
#     "token": "eyJhbGciOiJIUzI1NiIsInR5cCI6IkpXVCJ9...",
#     "userId": 5,
#     "role": "USER",
#     "email": "alice@example.com"
#   }
# }

# 4. Resend OTP
curl -X POST http://localhost:8080/api/auth/resend-otp \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "purpose": "USER_REGISTRATION"
  }'

# 5. Login (For already verified users)
curl -X POST http://localhost:8080/api/auth/login \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "password": "SecurePass@123"
  }'

# 6. Forgot Password (Request OTP)
curl -X POST http://localhost:8080/api/auth/forgot-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com"
  }'

# 7. Reset Password (With OTP)
curl -X POST http://localhost:8080/api/auth/reset-password \
  -H "Content-Type: application/json" \
  -d '{
    "email": "alice@example.com",
    "otp": "654321",
    "newPassword": "NewSecure@123"
  }'
```

#### OWNER - PG MANAGEMENT

```bash
# 1. Create New PG
curl -X POST http://localhost:8080/api/owner/pg \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "name": "Elite Mumbai PG",
    "address": "123 Bandra West, Mumbai",
    "city": "Mumbai",
    "state": "Maharashtra",
    "pincode": "400050",
    "description": "Premium PG with WiFi and AC",
    "genderPreference": "FEMALE",
    "imageUrls": ["https://s3.../image1.jpg"]
  }'

# 2. Get All My PGs
curl -X GET http://localhost:8080/api/owner/pgs \
  -H "Authorization: Bearer eyJhbGc..."

# 3. Get PG by ID
curl -X GET http://localhost:8080/api/owner/pg/1 \
  -H "Authorization: Bearer eyJhbGc..."

# 4. Update PG
curl -X PUT http://localhost:8080/api/owner/pg/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "name": "Updated PG Name",
    "description": "Updated description",
    "genderPreference": "BOTH"
  }'

# 5. Delete PG
curl -X DELETE http://localhost:8080/api/owner/pg/1 \
  -H "Authorization: Bearer eyJhbGc..."
```

#### OWNER - ROOM MANAGEMENT

```bash
# 1. Create Room
curl -X POST http://localhost:8080/api/owner/room \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "pgId": 1,
    "roomNumber": "101",
    "type": "SINGLE",
    "capacity": 1,
    "rent": 8000,
    "amenityIds": [1, 2, 3],
    "imageUrls": ["https://s3.../room1.jpg"]
  }'

# 2. Get Rooms in PG
curl -X GET http://localhost:8080/api/owner/pg/1/rooms \
  -H "Authorization: Bearer eyJhbGc..."

# 3. Get Room Details
curl -X GET http://localhost:8080/api/owner/room/10 \
  -H "Authorization: Bearer eyJhbGc..."

# 4. Update Room
curl -X PUT http://localhost:8080/api/owner/room/10 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "rent": 9000,
    "capacity": 1
  }'

# 5. Delete Room
curl -X DELETE http://localhost:8080/api/owner/room/10 \
  -H "Authorization: Bearer eyJhbGc..."

# 6. Get Room Tenants
curl -X GET http://localhost:8080/api/owner/room/10/tenants \
  -H "Authorization: Bearer eyJhbGc..."

# 7. Remove Tenant from Room
curl -X POST http://localhost:8080/api/owner/room/10/remove-tenant \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "userId": 5,
    "removalReason": "Lease term completed"
  }'
```

#### OWNER - BOOKING MANAGEMENT

```bash
# 1. Get All Booking Requests
curl -X GET http://localhost:8080/api/owner/bookings \
  -H "Authorization: Bearer eyJhbGc..."

# 2. Get Pending Bookings Only
curl -X GET http://localhost:8080/api/owner/bookings?status=PENDING \
  -H "Authorization: Bearer eyJhbGc..."

# 3. Accept Booking
curl -X POST http://localhost:8080/api/owner/booking/5/accept \
  -H "Authorization: Bearer eyJhbGc..."

# 4. Reject Booking
curl -X POST http://localhost:8080/api/owner/booking/5/reject \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "rejectionReason": "Room dimensions not suitable for this tenant"
  }'

# 5. Get All Tenants in PG
curl -X GET http://localhost:8080/api/owner/pg/1/all-tenants \
  -H "Authorization: Bearer eyJhbGc..."

# 6. Get All Amenities
curl -X GET http://localhost:8080/api/owner/amenities \
  -H "Authorization: Bearer eyJhbGc..."
```

#### OWNER - SMART METER OPERATIONS

```bash
# 1. Pair Meter to Room
curl -X POST http://localhost:8080/api/owner/meters/pair \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "meterId": "METER_001",
    "roomId": 10
  }'

# 2. Get All My Meters
curl -X GET http://localhost:8080/api/owner/meters \
  -H "Authorization: Bearer eyJhbGc..."

# 3. Unpair Meter
curl -X POST http://localhost:8080/api/owner/meters/METER_001/unpair \
  -H "Authorization: Bearer eyJhbGc..."

# 4. Control Relay (Turn ON)
curl -X POST http://localhost:8080/api/owner/meters/METER_001/relay \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "command": "ON"
  }'

# 5. Control Relay (Turn OFF)
curl -X POST http://localhost:8080/api/owner/meters/METER_001/relay \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "command": "OFF"
  }'
```

#### OWNER - BILLING CONFIGURATION

```bash
# 1. Create Billing Configuration for PG
curl -X POST http://localhost:8080/api/owner/billing/config \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "pgId": 1,
    "upiId": "owner@upi",
    "upiName": "Owner Name",
    "bankAccountNumber": "1234567890",
    "bankIfscCode": "SBIN0001234",
    "bankAccountName": "Owner Name",
    "bankName": "State Bank of India",
    "bankBranch": "Mumbai Main",
    "gstEnabled": true,
    "gstNumber": "27AABCT1234H1Z0",
    "gstPercentage": 18.00,
    "billingMode": "AUTO",
    "autoGenerationDay": 1,
    "dueDaysOffset": 7,
    "electricityRatePerUnit": 8.50,
    "includeElectricity": true,
    "electricitySplitMethod": "TIME_WEIGHTED",
    "lateFeeEnabled": true,
    "lateFeeAmount": 200.00,
    "lateFeeDays": 3,
    "proRatedRentMode": "DAILY",
    "securityDepositAmount": 10000.00,
    "securityDepositDueDays": 7,
    "autoApplyStandardCharges": true
  }'

# 2. Get Billing Configuration for PG
curl -X GET http://localhost:8080/api/owner/billing/config/pg/1 \
  -H "Authorization: Bearer eyJhbGc..."

# 3. Update Billing Configuration
curl -X PUT http://localhost:8080/api/owner/billing/config/pg/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "gstPercentage": 18.00,
    "electricityRatePerUnit": 9.00,
    "lateFeeAmount": 250.00
  }'
```

#### OWNER - STANDARD CHARGES MANAGEMENT

```bash
# 1. Create Standard Charge
curl -X POST http://localhost:8080/api/owner/billing/charges \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "pgId": 1,
    "chargeName": "WiFi Charge",
    "amount": 500.00,
    "isActive": true,
    "isDefault": true
  }'

# 2. Get All Standard Charges for PG
curl -X GET http://localhost:8080/api/owner/billing/charges/pg/1 \
  -H "Authorization: Bearer eyJhbGc..."

# 3. Update Standard Charge
curl -X PUT http://localhost:8080/api/owner/billing/charges/5/pg/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "chargeName": "WiFi Charge",
    "amount": 600.00
  }'

# 4. Toggle Charge Active/Inactive
curl -X PATCH http://localhost:8080/api/owner/billing/charges/5/pg/1/toggle \
  -H "Authorization: Bearer eyJhbGc..."
```

#### OWNER - BILL MANAGEMENT

```bash
# 1. Generate Manual Bill
curl -X POST http://localhost:8080/api/owner/billing/bills/generate/pg/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "tenantId": 5,
    "billingPeriodStart": "2026-02-01",
    "billingPeriodEnd": "2026-02-28",
    "discount": 0.00,
    "otherCharges": 0.00,
    "additionalDeductions": 0.00
  }'

# 2. Generate Bulk Bills for PG
curl -X POST http://localhost:8080/api/owner/billing/bills/generate/bulk/pg/1 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "billingPeriodStart": "2026-02-01",
    "billingPeriodEnd": "2026-02-28"
  }'

# 3. Get All Bills for PG (paginated)
curl -X GET "http://localhost:8080/api/owner/billing/bills/pg/1?page=0&size=10&status=PENDING" \
  -H "Authorization: Bearer eyJhbGc..."

# 4. Get Specific Bill Details
curl -X GET http://localhost:8080/api/owner/billing/bills/1 \
  -H "Authorization: Bearer eyJhbGc..."

# 5. Edit Bill (Before Payment)
curl -X POST http://localhost:8080/api/owner/billing/bills/1/edit \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "discount": 500.00,
    "additionalDeductions": 100.00,
    "otherCharges": 200.00,
    "editReasonl": "Tenant complaint about meter reading"
  }'

# 6. Cancel Bill
curl -X POST http://localhost:8080/api/owner/billing/bills/1/cancel \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "cancellationReason": "Tenant moved out early"
  }'
```

#### OWNER - PAYMENT VERIFICATION

```bash
# 1. Get Payment Proof Submissions
curl -X GET "http://localhost:8080/api/owner/billing/payments/pg/1?status=UPLOADED" \
  -H "Authorization: Bearer eyJhbGc..."

# 2. Approve Payment Proof
curl -X POST http://localhost:8080/api/owner/billing/payments/15/approve \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "notes": "Payment verified via UPI transfer"
  }'

# 3. Reject Payment Proof
curl -X POST http://localhost:8080/api/owner/billing/payments/15/reject \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "rejectionReason": "Amount doesn't match bill amount"
  }'
```

#### OWNER - CHECKOUT & SETTLEMENT

```bash
# 1. Initiate Tenant Checkout
curl -X POST http://localhost:8080/api/owner/billing/checkout/tenant/5 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "intendedMoveOutDate": "2026-02-28",
    "checkoutReason": "Lease completion"
  }'

# 2. Complete Checkout (After Final Settlement Paid)
curl -X POST http://localhost:8080/api/owner/billing/checkout/complete/20 \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "completionNotes": "Final bill settled, room cleaned"
  }'
```

#### TENANT - BILLING DASHBOARD

```bash
# 1. View Billing Dashboard
curl -X GET http://localhost:8080/api/tenant/billing/dashboard \
  -H "Authorization: Bearer eyJhbGc..."
```

#### TENANT - BILL VIEWING

```bash
# 1. Get All My Bills
curl -X GET "http://localhost:8080/api/tenant/billing/bills?status=PENDING" \
  -H "Authorization: Bearer eyJhbGc..."

# 2. Get Bill Details
curl -X GET http://localhost:8080/api/tenant/billing/bills/1 \
  -H "Authorization: Bearer eyJhbGc..."

# 3. Generate UPI Payment Link
curl -X GET http://localhost:8080/api/tenant/billing/bills/1/upi-link \
  -H "Authorization: Bearer eyJhbGc..."
```

#### TENANT - PAYMENT PROOF UPLOAD

```bash
# 1. Upload Payment Proof
curl -X POST http://localhost:8080/api/tenant/billing/payments/1/upload \
  -H "Authorization: Bearer eyJhbGc..." \
  -F "file=@payment_screenshot.jpg" \
  -F "paymentMethod=UPI" \
  -F "transactionId=TXN123456" \
  -F "transactionDate=2026-02-15" \
  -F "notes=Payment done via Google Pay"

# 2. Get Payment Proof History
curl -X GET http://localhost:8080/api/tenant/billing/payments/1/history \
  -H "Authorization: Bearer eyJhbGc..."
```

#### TENANT - USER OPERATIONS

```bash
# 1. Get My Profile
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer eyJhbGc..."

# 2. Update My Profile
curl -X PUT http://localhost:8080/api/user/profile \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "name": "Alice Updated",
    "phone": "9876543211",
    "gender": "FEMALE"
  }'

# 3. Check If I Have Active Booking
curl -X GET http://localhost:8080/api/user/has-active-booking \
  -H "Authorization: Bearer eyJhbGc..."

# 4. Get My Bookings
curl -X GET http://localhost:8080/api/user/bookings \
  -H "Authorization: Bearer eyJhbGc..."

# 5. Request Booking for Room
curl -X POST http://localhost:8080/api/user/booking/request \
  -H "Content-Type: application/json" \
  -H "Authorization: Bearer eyJhbGc..." \
  -d '{
    "roomId": 10
  }'

# 6. Cancel Pending Booking
curl -X DELETE http://localhost:8080/api/user/booking/5 \
  -H "Authorization: Bearer eyJhbGc..."
```

#### TENANT - METER VIEWING

```bash
# 1. Get My Room's Live Meter Readings
curl -X GET http://localhost:8080/api/tenant/my-room/meter/live \
  -H "Authorization: Bearer eyJhbGc..."

# 2. Get My Room's Historical Readings
# startMs and endMs are Unix timestamps in milliseconds
curl -X GET "http://localhost:8080/api/tenant/my-room/meter/readings?start=1708000000000&end=1708100000000" \
  -H "Authorization: Bearer eyJhbGc..."
```

#### METER READ OPERATIONS (Owner & Tenant)

```bash
# 1. Get Live Readings by Meter ID
curl -X GET http://localhost:8080/api/meters/METER_001/live \
  -H "Authorization: Bearer eyJhbGc..."

# 2. Get Live Readings by Room ID
curl -X GET http://localhost:8080/api/meters/room/10/live \
  -H "Authorization: Bearer eyJhbGc..."

# 3. Get Historical Readings (Time Range)
curl -X GET "http://localhost:8080/api/meters/METER_001/readings?start=1708000000000&end=1708100000000" \
  -H "Authorization: Bearer eyJhbGc..."
```

#### PUBLIC - SEARCH & BROWSE

```bash
# 1. Search PGs with All Filters
curl -X GET "http://localhost:8080/api/public/pg/search?city=Mumbai&genderPreference=FEMALE&roomType=SINGLE&minRent=5000&maxRent=15000&amenities=WIFI,AC" \
  -H "Content-Type: application/json"

# 2. Search by City Only
curl -X GET "http://localhost:8080/api/public/pg/search?city=Mumbai"

# 3. Get PG Details (Public)
curl -X GET http://localhost:8080/api/public/pg/1

# 4. Get Rooms in PG (Public)
curl -X GET http://localhost:8080/api/public/pg/1/rooms

# 5. Get Room Details (Public)
curl -X GET http://localhost:8080/api/public/room/10

# 6. Search Available Rooms
curl -X GET "http://localhost:8080/api/public/rooms/available?city=Mumbai&minRent=5000&maxRent=15000"
```

#### S3 IMAGE OPERATIONS

```bash
# 1. Get Presigned Upload URL
curl -X GET "http://localhost:8080/api/s3/presigned-url?folder=pg-images&fileName=photo.jpg&contentType=image/jpeg" \
  -H "Authorization: Bearer eyJhbGc..."
# Response includes 15-minute valid presigned URL

# 2. Upload File to S3 (Server-side)
curl -X POST http://localhost:8080/api/s3/upload \
  -H "Authorization: Bearer eyJhbGc..." \
  -F "file=@/path/to/photo.jpg" \
  -F "folder=room-images"

# 3. Delete File from S3
curl -X DELETE "http://localhost:8080/api/s3/delete?fileUrl=https://s3.../image.jpg" \
  -H "Authorization: Bearer eyJhbGc..."
```

### Response Schema Examples

#### Success Response (with data)
```json
{
  "success": true,
  "message": "Operation completed successfully",
  "data": {
    "id": 1,
    "name": "Example",
    "createdAt": "2026-02-14T12:00:00Z"
  }
}
```

#### Success Response (without data)
```json
{
  "success": true,
  "message": "Operation completed successfully"
}
```

#### Error Response
```json
{
  "timestamp": "2026-02-14T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Email already exists or is invalid",
  "path": "/api/auth/user/register",
  "details": [
    {
      "field": "email",
      "message": "Email already exists"
    }
  ]
}
```

### Request Body DTOs (All Validation Rules)

#### Authentication Requests
```
OwnerRegistrationRequest:
  - name: 2-100 chars (required)
  - email: valid format (required, unique)
  - password: 8-50 chars, must contain uppercase, lowercase, digit, special char (required)
  - phone: exactly 10 digits (required)

UserRegistrationRequest:
  - name: 2-100 chars (required)
  - email: valid format (required, unique)
  - password: 8-50 chars, complex (required)
  - phone: exactly 10 digits (required)
  - gender: MALE/FEMALE/OTHER (required)

LoginRequest:
  - email: valid format (required)
  - password: string (required)

OtpVerificationRequest:
  - email: valid format (required)
  - otp: exactly 6 digits (required)
  - purpose: OWNER_REGISTRATION, USER_REGISTRATION, RESET_PASSWORD, PASSWORD_SETUP (required)
```

#### PG Requests
```
CreatePGRequest:
  - name: 3-200 chars (required)
  - address: string (required)
  - city: 2-100 chars (required)
  - state: string (required)
  - pincode: exactly 6 digits (required)
  - genderPreference: MALE/FEMALE/BOTH (required)
  - description: string (optional)
  - imageUrls: max 5 images, URL strings (optional)

UpdatePGRequest:
  - All fields optional
  - imageUrls: max 5 items
```

#### Room Requests
```
CreateRoomRequest:
  - pgId: long >= 1 (required)
  - roomNumber: 1-20 chars (required)
  - type: SINGLE/DOUBLE/SHARED/DORMITORY (required)
  - capacity: 1-20 (required)
  - rent: decimal, must be > 0 (required)
  - amenityIds: array of long (optional)
  - imageUrls: max 10 images (optional)

UpdateRoomRequest:
  - All fields optional
  - imageUrls: max 10 items
```

#### Booking Requests
```
CreateBookingRequest:
  - roomId: long >= 1 (required)

BookingActionRequest (for rejection):
  - rejectionReason: 10-500 chars, minimum 10 (required for rejection)

RemoveTenantRequest:
  - userId: long >= 1 (required)
  - removalReason: 0-500 chars (optional)
```

#### Meter Requests
```
PairMeterRequest:
  - meterId: format "METER_001" or similar (pattern: ^[A-Za-z]+_[A-Za-z0-9_]+$) (required)
  - roomId: long >= 1 (required)

RelayCommandRequest:
  - command: "ON" or "OFF" (required, pattern: ^(ON|OFF)$)
```

---

## 8. KEY SERVICE CLASSES & RESPONSIBILITIES

| Service | Responsibilities |
|---------|------------------|
| `AuthService` | User/Owner registration, OTP verification, JWT generation, password reset |
| `UserService` | User profile management, view PG details, check active bookings |
| `OwnerService` | PG CRUD, tenant management, property overview |
| `RoomService` | Room CRUD, amenity linking, image management |
| `BookingService` | Complete booking workflow, gender validation, duplicate prevention |
| `MeterService` | Pairing, unpairing, live reads, history, relay control |
| `MeterAuthorizationService` | Centralized permission checks for meter access |
| `SearchService` | PG search with caching, filtering by city/rent/amenities |
| `OtpService` | OTP generation, verification, expiry, attempt tracking |
| `EmailService` | SendGrid integration, OTP emails, booking notifications |
| `S3Service` | Image upload, presigned URLs, file deletion |
| `AwsIotShadowService` | AWS IoT Device Shadow API calls |
| `DynamoDbMeterReadingService` | DynamoDB querying for historical readings |

---

## 10. IMPORTANT BUSINESS RULES & CONSTRAINTS

### Booking Rules
- ✅ Only available rooms (status = AVAILABLE) can be booked
- ✅ Room must have space (currentOccupancy < capacity)
- ✅ Gender compatibility required (unless PG preference = ALL)
- ✅ No duplicate pending bookings for same user+room
- ✅ User can only have ONE active booking at a time
- ✅ **On accept**: User.room = room assigned, currentOccupancy += 1
- ✅ **On reject**: Booking marked as REJECTED, no room change

### Meter Rules
- ✅ Each room can have AT MOST ONE meter
- ✅ Each meter pairs to EXACTLY ONE room
- ✅ Meter ID must be globally unique
- ✅ awsThingName = meterId (always synced)
- ✅ Owner can only control their own meters' relays
- ✅ Tenants can only view their room's meter (no relay control)
- ✅ Meter considered ONLINE if last reading ≤ 60 seconds old
- ✅ Relay command timeout: 2 seconds, 3 retries, 500ms between retries

### Image Upload Rules
- ✅ Max 5 images per PG
- ✅ Max 10 images per Room
- ✅ Max 10MB per file
- ✅ Supported: JPEG, PNG, WebP
- ✅ Presigned upload URLs valid for 15 minutes
- ✅ Presigned download URLs valid for 24 hours
- ✅ All images stored in AWS S3 with UUID-based naming

### OTP Rules
- ✅ OTP valid for 10 minutes
- ✅ Max 5 verification attempts
- ✅ OTP codes are 6-digit numbers
- ✅ OTP stored as BCrypt-hashed values (never plaintext)
- ✅ Each email can have only 1 active OTP per purpose
- ✅ Resend OTP deletes previous OTP

### User Account Rules
- ✅ Email must be unique per role (owner vs user)
- ✅ Unverified accounts are marked `isVerified = false`
- ✅ Unverified accounts CANNOT login
- ✅ Unverified accounts can be re-registered (old record deleted)
- ✅ After OTP verification → `isVerified = true` → Auto-login
- ✅ Password minimum 8 characters, BCrypt hashed

---

## 11. ERROR HANDLING

### Standard Error Response Format
```json
{
  "timestamp": "2026-02-14T12:00:00Z",
  "status": 400,
  "error": "Bad Request",
  "message": "Validation failed",
  "path": "/api/user/booking/request",
  "details": [
    {
      "field": "roomId",
      "message": "roomId must not be null"
    }
  ]
}
```

### Common HTTP Status Codes
- `200 OK` - Success
- `201 Created` - Resource created
- `400 Bad Request` - Validation error
- `401 Unauthorized` - Invalid credentials or missing JWT
- `403 Forbidden` - Access denied (wrong role or no permission)
- `404 Not Found` - Resource doesn't exist
- `409 Conflict` - Duplicate email, meter already paired, etc.
- `500 Internal Server Error` - Server-side exception

### Custom Exceptions
- `EmailAlreadyExistsException` → 409
- `ResourceNotFoundException` → 404
- `InvalidCredentialsException` → 401
- `UnauthorizedException` → 401
- `ForbiddenException` → 403
- `MeterAlreadyPairedException` → 409
- `MeterNotFoundException` → 404
- `RoomFullException` → 400
- `GenderMismatchException` → 400
- `DuplicateBookingException` → 409
- `OtpExpiredException` → 400
- `ImageLimitExceededException` → 400

---

## 12. DEVELOPMENT GUIDELINES

### Adding a New Feature

1. **Create DTOs** (Request/Response) in `dto/request` and `dto/response`
2. **Create/Update Entity** in `entity/` with proper annotations
3. **Create Repository** extending JpaRepository (queries as needed)
4. **Create Service** with business logic and transactions
5. **Create Controller** with route mapping and validation
6. **Update SecurityConfig** if adding new routes
7. **Write Tests** in `test/`

### Code Patterns Used

```java
// Transaction management
@Transactional
public SomeResponse doSomething(SomeRequest request) {
  // Auto-rollback on exception
}

// Logging
log.info("User {} performing action", userId);
log.warn("Unusual activity detected");
log.error("Failed to process", exception);

// Authorization
authorizationService.verifyOwnership(entityId, ownerId);

// Error handling
throw new ForbiddenException("You don't have permission");
```

### Testing Endpoints

Use `curl` or REST client:
```bash
# Register
curl -X POST http://localhost:8080/api/auth/user/register \
  -H "Content-Type: application/json" \
  -d '{
    "name": "John",
    "email": "john@example.com",
    "password": "Pass@123",
    "phone": "9876543210",
    "gender": "MALE"
  }'

# With Authorization
curl -X GET http://localhost:8080/api/user/profile \
  -H "Authorization: Bearer eyJhbGc..."
```

---

## 13. RUNNING THE PROJECT

```bash
# Start PostgreSQL (Docker)
docker run -d --name postgres \
  -e POSTGRES_USER=postgres \
  -e POSTGRES_PASSWORD=Shivam@12345 \
  -e POSTGRES_DB=smart_rooms_db \
  -p 5432:5432 postgres:15

# Start Redis (Docker)
docker run -d --name redis -p 6379:6379 redis:latest

# Build project
mvn clean install

# Run application
mvn spring-boot:run

# Application should be available at http://localhost:8080
# Swagger UI at http://localhost:8080/swagger-ui.html
```

---

## 14. DEPLOYMENT CONFIGURATION

### Environment Variables (Production)
```bash
export DATABASE_URL=jdbc:postgresql://prod-db:5432/smart_rooms_db
export DATABASE_USERNAME=prod_user
export DATABASE_PASSWORD=secure_password_here
export JWT_SECRET=your_secret_key_here
export SENDGRID_API_KEY=your_sendgrid_key
export AWS_ACCESS_KEY=your_aws_access_key
export AWS_SECRET_KEY=your_aws_secret_key
export AWS_REGION=ap-south-1
export AWS_IOT_ENDPOINT=https://your-iot-endpoint.iot.ap-south-1.amazonaws.com
export AWS_S3_BUCKET=pg-management-images
export AWS_DYNAMO_TABLE=meter_readings
export REDIS_HOST=prod-redis
export REDIS_PORT=6379
```

### AWS Account Setup Required
1. **S3 Bucket**: `pg-management-images` with public read policy
2. **IoT Core**: 
   - Create IoT Policy for device operations
   - Create certificates for device authentication
3. **DynamoDB**: Table `meter_readings` with
   - Partition Key: `meterId` (String)
   - Sort Key: `timestamp` (Number)
   - On-demand billing or provisioned capacity

---

## 15. CONTACT & NEXT STEPS

**For Claude Opus 4.5 taking over this project:**

1. Start by understanding this document completely
2. Review the entity relationships in Section 4
3. Study the authentication flow in Section 3.1
4. Understand meter pairing and authorization in Section 3.4
5. Clone the repository and run locally
6. Test API endpoints using curl commands in Section 7
7. When modifying code, maintain existing patterns from Section 11
8. Always add proper logging and error handling
9. Run tests after any changes

**Key Files to Review First:**
- `SmartPgApplication.java` - Main entry point
- `SecurityConfig.java` - Security configuration
- `MeterService.java` - Core business logic (large and complex)
- `BookingService.java` - Booking workflow
- `AuthService.java` - Authentication flow

---

**Generated**: February 14, 2026  
**Project Status**: Production-Ready  
**Java Version**: 17  
**Spring Boot Version**: 3.2.0

