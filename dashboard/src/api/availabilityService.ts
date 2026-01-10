import { api } from "@/api/axiosClient";

/**
 * Backend weekday keys (as returned by the API)
 * Example values: "MONDAY", "TUESDAY", ...
 */
export type BackendDayKey =
  | "MONDAY"
  | "TUESDAY"
  | "WEDNESDAY"
  | "THURSDAY"
  | "FRIDAY"
  | "SATURDAY"
  | "SUNDAY";

/** UI-friendly short day keys used across the app */
export type DayKey = "M" | "T" | "W" | "Th" | "F" | "S" | "Su";

/** Custom slot for a particular date */
export type CustomDateSlot = {
  date: string; // YYYY-MM-DD
  startTime: string; // HH:mm
  endTime: string; // HH:mm
  slotDurationMinutes: number;
};

/** Doctor schedule shape as returned by the backend (and expected by save) */
export type DoctorSchedule = {
  id: string;
  doctorId: string;
  doctorName: string;
  locationId: string;

  // Full weekday names per backend
  daysOfWeek: BackendDayKey[];

  // Daily time range for the above days (HH:mm)
  startTime: string;
  endTime: string;

  // Weekly unavailable days (full names)
  unavailableDaysOfWeek: BackendDayKey[];

  // Specific unavailable calendar dates (YYYY-MM-DD)
  unavailableDates: string[];

  // Dates when doctor is available only (exceptions)
  availableOnlyDates: string[];

  // Array of custom slot definitions for particular dates
  customDateSlots: CustomDateSlot[];
};

/* ----- mapping helpers ----- */

const _uiToBackend: Record<DayKey, BackendDayKey> = {
  M: "MONDAY",
  T: "TUESDAY",
  W: "WEDNESDAY",
  Th: "THURSDAY",
  F: "FRIDAY",
  S: "SATURDAY",
  Su: "SUNDAY",
};

const _backendToUI: Record<BackendDayKey, DayKey> = {
  MONDAY: "M",
  TUESDAY: "T",
  WEDNESDAY: "W",
  THURSDAY: "Th",
  FRIDAY: "F",
  SATURDAY: "S",
  SUNDAY: "Su",
};

/** Convert a single UI DayKey -> BackendDayKey */
export function uiToBackend(d: DayKey): BackendDayKey {
  return _uiToBackend[d];
}

/** Convert a single BackendDayKey -> DayKey */
export function backendToUI(d: BackendDayKey): DayKey {
  return _backendToUI[d];
}

/** Convert an array of UI DayKey -> BackendDayKey[] */
export function mapUIDaysToBackend(
  arr: DayKey[] | undefined | null,
): BackendDayKey[] {
  return (arr ?? []).map(uiToBackend);
}

/** Convert an array of BackendDayKey -> DayKey[] */
export function mapBackendDaysToUI(
  arr: BackendDayKey[] | undefined | null,
): DayKey[] {
  return (arr ?? []).map(backendToUI);
}

export async function fetchDoctorAvailability(
  doctorId: string,
): Promise<DoctorSchedule> {
  const { data } = await api.get<DoctorSchedule>(
    `/api/doctor/schedule?doctorId=${encodeURIComponent(doctorId)}`,
  );
  return data;
}

/**
 * Save/update doctor schedule.
 * Endpoint: PUT /api/doctor/schedule
 * Body: DoctorSchedule (matching backend shape)
 * Returns: DoctorSchedule (updated record)
 */
export async function saveDoctorAvailability(
  schedule: DoctorSchedule,
): Promise<DoctorSchedule> {
  const { data } = await api.put<DoctorSchedule>(
    `/api/doctor/schedule`,
    schedule,
  );
  return data;
}
