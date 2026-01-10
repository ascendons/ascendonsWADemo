# Appointment Booking Flow - Visual Guide

## Patient Booking Flow (WhatsApp)

```
┌─────────────────────────────────────────────────────────────┐
│                    PATIENT OPENS WHATSAPP                   │
│                  Sends "Hi" to Divisha Number                │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SYSTEM CHECKS: Is Patient Registered?           │
└──────────────┬───────────────────────────────┬───────────────┘
               │                               │
        ┌──────▼──────┐                ┌──────▼──────┐
        │    YES      │                │     NO      │
        │ Registered  │                │  New Patient│
        └──────┬──────┘                └──────┬──────┘
               │                               │
               │                    ┌──────────▼──────────┐
               │                    │  REGISTRATION FLOW  │
               │                    │                     │
               │                    │ 1. Ask for Name     │
               │                    │ 2. Ask for Age      │
               │                    │ 3. Ask for Gender   │
               │                    │ 4. Create Patient   │
               │                    │ 5. Send Patient ID  │
               │                    └──────────┬──────────┘
               │                               │
               └───────────────┬───────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│              SEND APPOINTMENT BOOKING FLOW FORM              │
│         (Interactive WhatsApp Flow with dropdowns)            │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    STEP 1: SELECT DOCTOR                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Dropdown shows available doctors                     │   │
│  │ Patient selects: "Dr. Smith"                         │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                  STEP 2: SELECT LOCATION                     │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Dropdown shows locations where doctor works         │   │
│  │ Patient selects: "Main Clinic"                      │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    STEP 3: SELECT DATE                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Calendar shows available dates                      │   │
│  │ (Unavailable dates are grayed out)                  │   │
│  │ Patient selects: "January 15, 2025"                │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                  STEP 4: SELECT TIME SLOT                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Dropdown shows available time slots                 │   │
│  │ (Only shows slots that are not booked)             │   │
│  │ Patient selects: "2:00 PM"                        │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                  STEP 5: REVIEW & CONFIRM                    │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ Review Screen Shows:                                │   │
│  │ • Doctor: Dr. Smith                                │   │
│  │ • Location: Main Clinic                            │   │
│  │ • Date: January 15, 2025                           │   │
│  │ • Time: 2:00 PM                                    │   │
│  │                                                     │   │
│  │ [Go Back]  [Confirm Booking]                       │   │
│  └─────────────────────────────────────────────────────┘   │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SYSTEM CHECKS: Is Time Slot Available?          │
└──────────────┬───────────────────────────────┬───────────────┘
               │                               │
        ┌──────▼──────┐                ┌──────▼──────┐
        │   AVAILABLE │                │    FULL     │
        └──────┬──────┘                └──────┬──────┘
               │                               │
               │                    ┌──────────▼──────────┐
               │                    │  CREATE APPOINTMENT │
               │                    │  Status: WAITLISTED │
               │                    └──────────┬──────────┘
               │                               │
┌──────────────▼──────────┐        ┌──────────▼──────────┐
│  CREATE APPOINTMENT      │        │                      │
│  Status: CONFIRMED       │        │                      │
└──────────────┬───────────┘        └──────────┬──────────┘
               │                               │
               └───────────────┬───────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│              SEND CONFIRMATION MESSAGE                        │
│  ┌─────────────────────────────────────────────────────┐   │
│  │ ✅ Appointment Booked!                              │   │
│  │                                                     │   │
│  │ Booking ID: BID-1234567890                         │   │
│  │ Doctor: Dr. Smith                                  │   │
│  │ Location: Main Clinic                              │   │
│  │ Date: January 15, 2025                             │   │
│  │ Time: 2:00 PM                                      │   │
│  │ Status: CONFIRMED / WAITLISTED                    │   │
│  │                                                     │   │
│  │ Please save your Booking ID for reference.        │   │
│  └─────────────────────────────────────────────────────┘   │
└─────────────────────────────────────────────────────────────┘
```

## Reschedule Appointment Flow (WhatsApp)

```
┌─────────────────────────────────────────────────────────────┐
│              PATIENT SENDS "Hi" TO WHATSAPP                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SYSTEM SHOWS MAIN MENU                          │
│  [BOOK APPOINTMENT]  [CANCEL/RESCHEDULE]  [WALK-IN]         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│        PATIENT SELECTS "CANCEL/RESCHEDULE"                   │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SYSTEM SHOWS SUB-MENU                           │
│              [CANCEL]  [RESCHEDULE]                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│        PATIENT SELECTS "RESCHEDULE"                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     SYSTEM FETCHES UPCOMING APPOINTMENTS                     │
│     Shows list of appointments with Booking IDs              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     PATIENT TAPS ON APPOINTMENT TO RESCHEDULE                │
│     (System stores Booking ID)                               │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     SYSTEM CANCELS OLD APPOINTMENT                            │
│     (Status → CANCELLED)                                     │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     SYSTEM SENDS APPOINTMENT BOOKING FLOW                    │
│     (Same flow as new booking)                               │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     PATIENT SELECTS NEW:                                     │
│     Doctor → Location → Date → Time                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     SYSTEM CREATES NEW APPOINTMENT                            │
│     Status: CONFIRMED (if slot available)                    │
│     Status: WAITLISTED (if slot full)                       │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     PATIENT RECEIVES CONFIRMATION                            │
│     New Booking ID, Date, Time, Doctor                       │
└─────────────────────────────────────────────────────────────┘
```

## Cancel Appointment Flow (WhatsApp)

```
┌─────────────────────────────────────────────────────────────┐
│              PATIENT SENDS "Hi" TO WHATSAPP                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SYSTEM SHOWS MAIN MENU                          │
│  [BOOK APPOINTMENT]  [CANCEL/RESCHEDULE]  [WALK-IN]         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│        PATIENT SELECTS "CANCEL/RESCHEDULE"                   │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SYSTEM SHOWS SUB-MENU                           │
│              [CANCEL]  [RESCHEDULE]                          │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│        PATIENT SELECTS "CANCEL"                              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     SYSTEM FETCHES UPCOMING APPOINTMENTS                     │
│     Shows list of appointments with Booking IDs              │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     PATIENT TAPS ON APPOINTMENT TO CANCEL                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     SYSTEM UPDATES APPOINTMENT STATUS                         │
│     Status → CANCELLED                                       │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     PATIENT RECEIVES CANCELLATION CONFIRMATION               │
│     Booking ID, Date, Time, Doctor                           │
│     Message: "Appointment Cancelled Successfully"            │
└─────────────────────────────────────────────────────────────┘
```

## Walk-In / Emergency Flow (WhatsApp)

```
┌─────────────────────────────────────────────────────────────┐
│              PATIENT SENDS "Hi" TO WHATSAPP                  │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SYSTEM SHOWS MAIN MENU                          │
│  [BOOK APPOINTMENT]  [CANCEL/RESCHEDULE]  [WALK-IN]         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│        PATIENT SELECTS "WALK-IN/EMERGENCY"                   │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     SYSTEM CHECKS: Is Patient Registered?                    │
└──────────────┬───────────────────────────────┬───────────────┘
               │                               │
        ┌──────▼──────┐                ┌──────▼──────┐
        │    YES      │                │     NO     │
        │ Registered  │                │ New Patient│
        └──────┬──────┘                └──────┬──────┘
               │                               │
               │                    ┌──────────▼──────────┐
               │                    │  REGISTRATION FLOW  │
               │                    │  Name, Age, Gender │
               │                    └──────────┬──────────┘
               │                               │
               └───────────────┬───────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│              STEP 1: SELECT LOCATION                         │
│  System shows available locations                            │
│  Patient selects location                                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              STEP 2: SELECT DOCTOR                            │
│  System shows doctors at selected location                   │
│  Patient selects doctor                                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              STEP 3: SELECT DATE                             │
│  System shows available dates (usually today/tomorrow)      │
│  Patient selects date                                        │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SYSTEM CREATES WALK-IN APPOINTMENT              │
│  Status: WALK_IN                                             │
│  Time: null (no specific time slot)                          │
│  Booking ID: Generated                                       │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              PATIENT RECEIVES CONFIRMATION                   │
│  Name, Doctor, Location, Date                                │
│  Booking ID, Patient ID                                      │
│  Note: No specific time - seen when available               │
└─────────────────────────────────────────────────────────────┘
```

## Waitlist Management Flow

```
┌─────────────────────────────────────────────────────────────┐
│        PATIENT TRIES TO BOOK APPOINTMENT                     │
│        Selects: Doctor → Location → Date → Time             │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     SYSTEM CHECKS: Is Time Slot Available?                  │
└──────────────┬───────────────────────────────┬───────────────┘
               │                               │
        ┌──────▼──────┐                ┌──────▼──────┐
        │   AVAILABLE │                │    FULL     │
        └──────┬──────┘                └──────┬──────┘
               │                               │
               │                    ┌──────────▼──────────┐
               │                    │  CREATE APPOINTMENT  │
               │                    │  Status: WAITLISTED │
               │                    └──────────┬──────────┘
               │                               │
┌──────────────▼──────────┐        ┌──────────▼──────────┐
│  CREATE APPOINTMENT      │        │                    │
│  Status: CONFIRMED       │        │                    │
└──────────────┬───────────┘        └──────────┬──────────┘
               │                               │
               └───────────────┬───────────────┘
                               │
                               ▼
┌─────────────────────────────────────────────────────────────┐
│     PATIENT RECEIVES CONFIRMATION                            │
│     Status: CONFIRMED or WAITLISTED                         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     ADMIN VIEWS WAITLISTED APPOINTMENTS                      │
│     Dashboard → Filter: "Waitlisted"                        │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│     WHEN SLOT OPENS (Due to Cancellation)                   │
│     Admin can:                                              │
│     1. Find waitlisted appointment                          │
│     2. Click appointment → Reschedule                       │
│     3. Assign to newly available slot                       │
│     4. Contact patient to notify                            │
└─────────────────────────────────────────────────────────────┘
```

## Admin Management Flow

```
┌─────────────────────────────────────────────────────────────┐
│                    ADMIN LOGS INTO PORTAL                    │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                        DASHBOARD VIEW                         │
│  • Today's appointments                                      │
│  • Quick statistics (Confirmed, Waitlisted, etc.)            │
│  • Search and filter options                                 │
└──────────────────────────┬───────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│   SETTINGS   │  │   PATIENTS   │  │ CALENDAR VIEW│
│              │  │              │  │              │
│ • Doctors    │  │ • View all   │  │ • Time slots │
│ • Schedules  │  │ • Add/Edit   │  │ • Daily view │
│ • Locations  │  │ • Delete     │  │ • Navigation │
│ • Users      │  │              │  │              │
└──────────────┘  └──────────────┘  └──────────────┘
```

## Doctor Schedule Setup Flow

```
┌─────────────────────────────────────────────────────────────┐
│              ADMIN GOES TO SETTINGS → DOCTOR                 │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SET DAILY TIME RANGE                            │
│  From: [9:00 AM ▼]  To: [5:00 PM ▼]                         │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              SELECT AVAILABLE DAYS                           │
│  ☑ Monday  ☑ Tuesday  ☑ Wednesday  ☑ Thursday  ☑ Friday     │
│  ☐ Saturday  ☐ Sunday                                        │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              ADD UNAVAILABLE DATES (Optional)                │
│  Date: [2025-12-25] [Add]                                    │
│  • December 25, 2025                                        │
└──────────────────────────┬───────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    CLICK "SAVE"                              │
│                                                              │
│  ✅ Schedule saved successfully!                             │
│  Doctor is now available for booking                        │
└─────────────────────────────────────────────────────────────┘
```

## Appointment Management Flow

```
┌─────────────────────────────────────────────────────────────┐
│              ADMIN VIEWS APPOINTMENT ON DASHBOARD            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│              CLICK ON APPOINTMENT CARD                       │
│  Opens side panel with full details                         │
└──────────────────────────┬───────────────────────────────────┘
                           │
        ┌──────────────────┼──────────────────┐
        │                  │                  │
        ▼                  ▼                  ▼
┌──────────────┐  ┌──────────────┐  ┌──────────────┐
│  RESCHEDULE  │  │    CANCEL    │  │  CHECK IN     │
│              │  │              │  │              │
│ 1. Select    │  │ 1. Confirm   │  │ 1. Click     │
│    new date  │  │    action    │  │    button    │
│ 2. Select    │  │ 2. Status →  │  │ 2. Status    │
│    new time  │  │    Cancelled │  │    updated   │
│ 3. Confirm   │  │ 3. Patient   │  │              │
│              │  │    notified  │  │              │
└──────────────┘  └──────────────┘  └──────────────┘
```

## Key System Components

```
┌─────────────────────────────────────────────────────────────┐
│                    WHATSAPP INTERFACE                       │
│  • Receives patient messages                                │
│  • Sends interactive flow forms                              │
│  • Handles booking confirmations                            │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    BACKEND SYSTEM                            │
│  • Processes bookings                                       │
│  • Manages schedules                                       │
│  • Stores patient data                                     │
│  • Checks availability                                      │
└──────────────────────────┬──────────────────────────────────┘
                           │
                           ▼
┌─────────────────────────────────────────────────────────────┐
│                    ADMIN PORTAL                              │
│  • Dashboard for viewing appointments                      │
│  • Settings for managing doctors/schedules                  │
│  • Patient management                                      │
│  • Calendar view                                            │
└─────────────────────────────────────────────────────────────┘
```

## Data Flow: Booking Creation

```
Patient Selection → System Checks → Database Query → Response
     │                  │                │              │
     │                  │                │              │
     ▼                  ▼                ▼              ▼
┌─────────┐      ┌──────────┐    ┌──────────┐   ┌──────────┐
│ Doctor  │ ────▶│ Is Doctor│───▶│ Query    │──▶│ Available│
│ Selected│      │ Available?│    │ Schedule │   │ Doctors   │
└─────────┘      └──────────┘    └──────────┘   └──────────┘
     │                  │                │              │
     │                  │                │              │
     ▼                  ▼                ▼              ▼
┌─────────┐      ┌──────────┐    ┌──────────┐   ┌──────────┐
│Location │ ────▶│ Doctor at│───▶│ Query    │──▶│ Available│
│Selected │      │ Location?│    │ Locations│   │ Locations│
└─────────┘      └──────────┘    └──────────┘   └──────────┘
     │                  │                │              │
     │                  │                │              │
     ▼                  ▼                ▼              ▼
┌─────────┐      ┌──────────┐    ┌──────────┐   ┌──────────┐
│  Date   │ ────▶│ Date in   │───▶│ Query     │──▶│ Available│
│Selected │      │ Schedule? │    │ Available │   │ Dates    │
└─────────┘      └──────────┘    └──────────┘   └──────────┘
     │                  │                │              │
     │                  │                │              │
     ▼                  ▼                ▼              ▼
┌─────────┐      ┌──────────┐    ┌──────────┐   ┌──────────┐
│  Time   │ ────▶│ Slot      │───▶│ Query     │──▶│ Available│
│Selected │      │ Available?│    │ Bookings  │   │ Time     │
└─────────┘      └──────────┘    └──────────┘   └──────────┘
```

---

## Summary of All Flows

### Patient-Facing Flows (WhatsApp)

1. **Regular Booking**: Hi → Register (if new) → Book Appointment → Select Doctor/Location/Date/Time → Confirm
2. **Reschedule**: Hi → Cancel/Reschedule → Reschedule → Select Appointment → New Booking Flow → Confirm
3. **Cancel**: Hi → Cancel/Reschedule → Cancel → Select Appointment → Cancelled
4. **Walk-In**: Hi → Walk-In/Emergency → Register (if new) → Select Location/Doctor/Date → Confirmed

### Admin-Facing Flows (Portal)

1. **View Appointments**: Dashboard → Select Date → View List → Filter by Status
2. **Reschedule**: Click Appointment → Reschedule → Select New Date/Time → Confirm
3. **Cancel**: Click Appointment → Cancel → Confirm
4. **Check In**: Click Appointment → Check In
5. **Manage Waitlist**: Filter "Waitlisted" → When slot opens → Reschedule to available slot
6. **Manage Walk-Ins**: Filter "Walk-in" → View walk-in appointments → Check in when patient arrives

### Appointment Status Flow

```
NEW BOOKING
    │
    ├─→ Slot Available? ──YES──→ CONFIRMED
    │
    └─→ Slot Available? ──NO───→ WAITLISTED
                                    │
                                    ├─→ Admin Reschedules ──→ CONFIRMED
                                    │
                                    └─→ Patient Reschedules ──→ CONFIRMED/WAITLISTED

CONFIRMED
    │
    ├─→ Patient Arrives ──→ CHECK IN ──→ COMPLETED
    │
    ├─→ Patient Cancels ──→ CANCELLED
    │
    └─→ Admin Cancels ──→ CANCELLED

WALK-IN
    │
    └─→ Patient Arrives ──→ CHECK IN ──→ COMPLETED
```

---

## Legend

- **Solid arrows (→)**: Process flow
- **Dashed boxes**: User actions
- **Rounded boxes**: System checks/decisions
- **Rectangular boxes**: Data storage/queries

---

*This diagram provides a visual representation of how the appointment booking system works. Refer to USER_GUIDE.md for detailed step-by-step instructions.*

