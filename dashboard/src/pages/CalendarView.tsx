import { useCallback, useEffect, useMemo, useState } from "react";
import { Card, CardContent, CardHeader } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { ChevronLeft, ChevronRight, Plus, Search, Download } from "lucide-react";
import { useToast } from "@/hooks/use-toast";
import { Input } from "@/components/ui/input";
import * as XLSX from "xlsx";

import {
  fetchSlotsByDate,
  fetchAppointmentsByDate,
  Slot,
  AppointmentSummary,
} from "@/api/appointmentService";
import { fetchUsersByRole, UserSummary } from "@/api/userService";
import {
  fetchAllLocations,
  Location as LocationType,
} from "@/api/locationService";
import { AppointmentPanel } from "@/components/Appointments/AppointmentPanel";
import AppointmentDialog from "@/components/Appointments/AppointmentDialog";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
import { fetchAllPatients } from "@/api/patientService";
import SlotGrid from "./components/CardsGrid";

function toISODate(d: Date) {
  return d.toISOString().slice(0, 10); // YYYY-MM-DD
}
function yyyyMMddFromIso(isoDate: string) {
  return isoDate.replace(/-/g, "");
}
function normalizeAppointmentDate(d?: string) {
  if (!d) return "";
  if (d.includes("-")) return d;
  if (d.length === 8) return `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6)}`;
  return d;
}
function formatTimeLabel(t: string) {
  try {
    const iso = `${new Date().toISOString().slice(0, 10)}T${t}`;
    const dt = new Date(iso);
    return dt.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
  } catch {
    return t;
  }
}

export default function CalendarView() {
  const { toast } = useToast();

  const [selectedDate, setSelectedDate] = useState<string>(
    toISODate(new Date()),
  );
  const [slots, setSlots] = useState<string[]>([]); // normalized to strings (HH:mm)
  const [appointments, setAppointments] = useState<AppointmentSummary[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState<string | null>(null);

  const [doctors, setDoctors] = useState<UserSummary[]>([]);
  const doctorsList = useMemo(
    () => doctors.map((d) => ({ id: d.id, name: d.name })),
    [doctors],
  );

  const [locations, setLocations] = useState<LocationType[]>([]);
  const locationsList = useMemo(
    () => locations.map((l) => ({ id: l.id, name: l.name })),
    [locations],
  );

  const [patientsRaw, setPatientsRaw] = useState<any[]>([]);

  const patientNameById = useMemo(() => {
    const m = new Map<string, string>();
    for (const p of patientsRaw) {
      const name = p.name ?? p.fullName ?? p.displayName ?? "";
      const possibleIds = [
        p.id,
        p._id,
        p.userId,
        p.patientId,
        p.email,
        p.phone,
      ].filter(Boolean);
      for (const id of possibleIds) {
        m.set(String(id), name || String(id));
      }
    }
    return m;
  }, [patientsRaw]);

  const [selectedDoctor, setSelectedDoctor] = useState<string>("");
  const [selectedLocation, setSelectedLocation] = useState<string>("");
  const [searchPatient, setSearchPatient] = useState<string>("");

  const [selectedAppointment, setSelectedAppointment] =
    useState<AppointmentSummary | null>(null);

  const [newAppointmentParams, setNewAppointmentParams] = useState<{
    doctorId?: string;
    locationId?: string;
    date?: string;
    slot?: string;
  } | null>(null);

  useEffect(() => {
    let mounted = true;
    (async () => {
      try {
        const [docs, locs, pats] = await Promise.all([
          fetchUsersByRole("DOCTOR"),
          fetchAllLocations(),
          fetchAllPatients(),
        ]);
        if (!mounted) return;
        setDoctors(docs ?? []);
        setLocations(locs ?? []);
        setPatientsRaw(pats ?? []);
      } catch (err) {
        console.error("Failed to load lists", err);
        toast({
          title: "Load failed",
          description:
            err instanceof Error ? err.message : "Failed to load lists",
          variant: "destructive",
        });
      }
    })();
    return () => {
      mounted = false;
    };
  }, [toast]);

  useEffect(() => {
    if (doctors.length > 0 && !selectedDoctor) setSelectedDoctor(doctors[0].id);
    if (locations.length > 0 && !selectedLocation)
      setSelectedLocation(locations[0].id);
  }, [doctors, locations]);

  const load = async (
    dateIso: string,
    doctorId: string,
    locationId: string,
  ) => {
    setLoading(true);
    setError(null);
    try {
      if (doctorId === "" || locationId === "") return;
      const [rawSlots, a] = await Promise.all([
        fetchSlotsByDate(dateIso, doctorId ?? "", locationId ?? ""),
        fetchAppointmentsByDate(dateIso),
      ]);

      const normalizedSlots: string[] = (rawSlots ?? [])
        .map((s: any) => (typeof s === "string" ? s : (s?.time ?? "")))
        .filter(Boolean);

      const unique = Array.from(new Set(normalizedSlots)).sort((x, y) =>
        x.localeCompare(y),
      );
      setSlots(unique);
      setAppointments(a ?? []);
    } catch (err) {
      const msg = err instanceof Error ? err.message : "Failed to load data";
      setError(msg);
      toast({
        title: "Load failed",
        description: msg,
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  };

  // Background refresh function - silent, no loading state, no toasts
  const backgroundRefresh = useCallback(async () => {
    try {
      // Refresh appointments for current date
      const appointments = await fetchAppointmentsByDate(selectedDate);
      setAppointments(appointments ?? []);

      // Refresh patients
      const patients = await fetchAllPatients(true); // Force refresh to bypass cache
      setPatientsRaw(patients ?? []);
    } catch (err) {
      // Silently fail in background - don't disturb user
      console.error("Background refresh failed", err);
    }
  }, [selectedDate]);

  useEffect(() => {
    let mounted = true;
    if (doctors.length === 0 || locations.length === 0) return;
    load(selectedDate, selectedDoctor || "", selectedLocation || "");
    return () => {
      mounted = false;
    };
  }, [
    selectedDate,
    selectedDoctor,
    selectedLocation,
    doctors.length,
    locations.length,
    toast,
  ]);

  // Background polling every 30 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      backgroundRefresh();
    }, 30000); // 30 seconds

    return () => {
      clearInterval(interval);
    };
  }, [backgroundRefresh]);

  const refresh = () => {
    load(selectedDate, selectedDoctor || "", selectedLocation || "");
  };

  const appointmentsByTime = useMemo(() => {
    const map = new Map<string, AppointmentSummary[]>();
    const targetIso = selectedDate; // "YYYY-MM-DD"
    for (const a of appointments) {
      if (!a.time) continue;

      const aptIso = normalizeAppointmentDate(a.date);

      if (aptIso && aptIso !== targetIso) continue;

      if (selectedDoctor && a.doctorId && a.doctorId !== selectedDoctor)
        continue;

      const list = map.get(a.time) ?? [];
      list.push(a);
      map.set(a.time, list);
    }
    return map;
  }, [appointments, selectedDate, selectedDoctor, selectedLocation]);

  const timesToRender = useMemo(() => {
    // If slots API returns data, use those
    if (slots && slots.length > 0) {
      return slots;
    }
    const appointmentTimes = Array.from(appointmentsByTime.keys());
    if (appointmentTimes.length > 0) {
      return appointmentTimes.sort((x, y) => x.localeCompare(y));
    }
    return [];
  }, [slots, appointmentsByTime]);

  function goPrevDay() {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() - 1);
    setSelectedDate(toISODate(d));
  }
  function goNextDay() {
    const d = new Date(selectedDate);
    d.setDate(d.getDate() + 1);
    setSelectedDate(toISODate(d));
  }

  const filteredAppointments = useMemo(() => {
    const term = searchPatient.trim().toLowerCase();
    if (!term) return appointments;
    return appointments.filter(
      (a) =>
        (a.patientId ?? "").toLowerCase().includes(term) ||
        (a.bookingId ?? "").toLowerCase().includes(term) ||
        (a.phone ?? "").toLowerCase().includes(term),
    );
  }, [appointments, searchPatient]);

  const bookedCount = useMemo(() => {
    let c = 0;
    for (const [, arr] of appointmentsByTime) c += arr.length;
    return c;
  }, [appointmentsByTime]);

  function handleTimeCardClick(time: string) {
    const appts = appointmentsByTime.get(time) ?? [];
    const confirmedAppt = appts.find(
      (a) => String(a.status ?? "").toLowerCase() === "confirmed"
    );

    if (confirmedAppt) {
      // If a confirmed appointment exists
      setSelectedAppointment(confirmedAppt);
      setNewAppointmentParams(null);
    } else {
      // Otherwise, treat as new booking slot
      setNewAppointmentParams({
        doctorId: selectedDoctor || undefined,
        locationId: selectedLocation || undefined,
        date: selectedDate,
        slot: time,
      });
      setSelectedAppointment(null);
    }
  }


  function handleAppointmentClick(appt: AppointmentSummary) {
    setSelectedAppointment(appt);
    setNewAppointmentParams(null);
  }

  function resolvePatientName(a: AppointmentSummary) {
    if ((a as any).patientName) return (a as any).patientName;
    if (a.patientId && patientNameById.has(a.patientId))
      return patientNameById.get(a.patientId);
    if (a.patientId && patientNameById.has(String(a.patientId)))
      return patientNameById.get(String(a.patientId));
    return undefined;
  }

function exportExcel() {
  // Helpers (use existing scope variables: appointments, selectedDate, resolvePatientName,
  // doctorsList, locationsList, timesToRender, appointmentsByTime, slots, patientNameById)
  const exportTime = new Date().toISOString();
  const doctorMap = new Map(doctorsList.map((d) => [d.id, d.name]));
  const locationMap = new Map(locationsList.map((l) => [l.id, l.name]));

  // ---------- Sheet 1: Appointments ----------
  const apptCols = [
    "date",
    "time",
    "status",
    "bookingId",
    "patientId",
    "patientName",
    "phone",
    "doctorId",
    "doctorName",
    "locationId",
    "locationName",
    "notes",
    "createdAt",
    "updatedAt",
  ];

  const apptRows = (appointments ?? []).map((a) => {
    const pid = a.patientId ?? "";
    const patientName = (a as any).patientName ?? resolvePatientName(a) ?? patientNameById.get(pid) ?? "";
    const doctorName =
      (a.doctorId && doctorMap.get(a.doctorId)) ||
      (selectedDoctor && doctorMap.get(selectedDoctor)) ||
      "";
    const locationName =
      (a.locationId && locationMap.get(a.locationId)) ||
      (selectedLocation && locationMap.get(selectedLocation)) ||
      "";

    return {
      date: normalizeAppointmentDate(a.date) || selectedDate,
      time: a.time ?? "",
      status: a.status ?? "",
      bookingId: a.bookingId ?? "",
      patientId: pid,
      patientName,
      phone: a.phone ?? "",
      doctorId: a.doctorId ?? selectedDoctor ?? "",
      doctorName,
      locationId: a.locationId ?? selectedLocation ?? "",
      locationName,
      notes: (a as any).notes ?? (a as any).reason ?? "",
      createdAt: (a as any).createdAt ?? "",
      updatedAt: (a as any).updatedAt ?? "",
    };
  });

  // ---------- Sheet 2: Slots ----------
  // We'll iterate timesToRender (prefer slots if available)
  const slotCols = [
    "date",
    "time",
    "slotDuration",
    "slotStatus", // free / booked / partially booked
    "bookedCount",
    "capacity",
    "bookingIds",
    "patientNames",
    "patientPhones",
    "doctorId",
    "doctorName",
    "locationId",
    "locationName",
    "firstBookingAt",
    "lastBookingAt",
  ];

  // default capacity per slot (change if your app supports different capacity)
  const DEFAULT_CAPACITY = 1;

  const slotRows = (timesToRender ?? []).map((time) => {
    const appts = appointmentsByTime.get(time) ?? [];
    const bookedCount = appts.length;
    const capacity = DEFAULT_CAPACITY;

    let slotStatus = "free";
    if (bookedCount === 0) slotStatus = "free";
    else if (bookedCount > 0 && bookedCount < capacity) slotStatus = "partially booked";
    else slotStatus = "booked";

    const bookingIds = appts.map((x) => x.bookingId ?? "").filter(Boolean);
    // preserve unique patient names / phones
    const patientNames = appts
      .map((x) => (x as any).patientName ?? resolvePatientName(x) ?? (x.patientId ?? ""))
      .filter(Boolean);
    const patientPhones = appts.map((x) => x.phone ?? "").filter(Boolean);

    // first/last booking times if available on appointment metadata (fallback to undefined)
    const allCreated = appts
      .map((x) => (x as any).createdAt)
      .filter(Boolean)
      .sort();
    const firstBookingAt = allCreated.length ? allCreated[0] : "";
    const lastBookingAt = allCreated.length ? allCreated[allCreated.length - 1] : "";

    return {
      date: selectedDate,
      time,
      slotDuration: (appts[0] && (appts[0] as any).duration) ?? "", // if you store duration on appt
      slotStatus,
      bookedCount,
      capacity,
      bookingIds: bookingIds.join(", "),
      patientNames: Array.from(new Set(patientNames)).join(", "),
      patientPhones: Array.from(new Set(patientPhones)).join(", "),
      doctorId: selectedDoctor ?? (appts[0] && appts[0].doctorId) ?? "",
      doctorName: (selectedDoctor && doctorMap.get(selectedDoctor)) ?? (appts[0] && doctorMap.get(appts[0].doctorId)) ?? "",
      locationId: selectedLocation ?? (appts[0] && (appts[0] as any).locationId) ?? "",
      locationName: (selectedLocation && locationMap.get(selectedLocation)) ?? (appts[0] && locationMap.get((appts[0] as any).locationId)) ?? "",
      firstBookingAt,
      lastBookingAt,
    };
  });

  // ---------- Build workbook ----------
  const wb = XLSX.utils.book_new();

  // Optional: add a top-row metadata sheet (or add export metadata as an extra row in first sheet)
  const metaRows = [
    { key: "exportedAt", value: exportTime },
    { key: "exportedForDate", value: selectedDate },
    { key: "selectedDoctor", value: selectedDoctor || "" },
    { key: "selectedLocation", value: selectedLocation || "" },
  ];
  const metaSheet = XLSX.utils.json_to_sheet(metaRows, { header: ["key", "value"] });
  XLSX.utils.book_append_sheet(wb, metaSheet, "ExportMetadata");

  // Appointments sheet
  const apptWs = XLSX.utils.json_to_sheet(apptRows, { header: apptCols });
  // Ensure header order by writing header row
  XLSX.utils.sheet_add_aoa(apptWs, [apptCols], { origin: "A1" });
  XLSX.utils.sheet_add_json(apptWs, apptRows, { origin: "A2", skipHeader: true });
  XLSX.utils.book_append_sheet(wb, apptWs, "Appointments");

  // Slots sheet
  const slotWs = XLSX.utils.json_to_sheet(slotRows, { header: slotCols });
  XLSX.utils.sheet_add_aoa(slotWs, [slotCols], { origin: "A1" });
  XLSX.utils.sheet_add_json(slotWs, slotRows, { origin: "A2", skipHeader: true });
  XLSX.utils.book_append_sheet(wb, slotWs, "Slots");

  // Write file
  const filename = `appointments-${selectedDate}.xlsx`;
  XLSX.writeFile(wb, filename);
}

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Calendar</h1>
          <p className="text-muted-foreground">Appointments and schedule</p>
        </div>
      </div>

      <Card>
        <CardHeader>
          <div className="w-full">
            <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 items-center">
              {/* 1) Date picker + prev/next */}
              <div className="flex items-center gap-3">
                <Button
                  variant="outline"
                  size="icon"
                  onClick={goPrevDay}
                  aria-label="Previous day"
                >
                  <ChevronLeft className="h-4 w-4" />
                </Button>

                <Input
                  type="date"
                  value={selectedDate}
                  onChange={(e) => setSelectedDate(e.target.value)}
                  className="h-9 rounded-md px-2 text-sm w-full max-w-[220px]"
                />

                <Button
                  variant="outline"
                  size="icon"
                  onClick={goNextDay}
                  aria-label="Next day"
                >
                  <ChevronRight className="h-4 w-4" />
                </Button>
              </div>

              {/* 2) Doctor select */}
              <div className="min-w-0">
                <Select
                  value={selectedDoctor}
                  onValueChange={(v) => setSelectedDoctor(v)}
                >
                  <SelectTrigger
                    title={
                      selectedDoctor
                        ? (doctorsList.find((d) => d.id === selectedDoctor)
                            ?.name ?? selectedDoctor)
                        : "Select doctor"
                    }
                    className="h-9 text-sm w-full truncate"
                  >
                    <SelectValue placeholder="Select doctor" />
                  </SelectTrigger>

                  <SelectContent className="max-w-full overflow-auto z-[60]">
                    {doctorsList.map((d) => (
                      <SelectItem key={d.id} value={d.id}>
                        {d.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 3) Location select */}
              <div className="min-w-0">
                <Select
                  value={selectedLocation}
                  onValueChange={(v) => setSelectedLocation(v)}
                >
                  <SelectTrigger
                    title={
                      selectedLocation
                        ? (locationsList.find((l) => l.id === selectedLocation)
                            ?.name ?? selectedLocation)
                        : "Select location"
                    }
                    className="h-9 text-sm w-full truncate"
                  >
                    <SelectValue placeholder="Select location" />
                  </SelectTrigger>

                  <SelectContent className="max-w-full overflow-auto z-[60]">
                    {locationsList.map((l) => (
                      <SelectItem key={l.id} value={l.id}>
                        {l.name}
                      </SelectItem>
                    ))}
                  </SelectContent>
                </Select>
              </div>

              {/* 4) Search
              <div className="min-w-0">
                <div className="relative">
                  <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
                  <Input
                    value={searchPatient}
                    onChange={(e) => setSearchPatient(e.target.value)}
                    placeholder="Search patient/booking/phone..."
                    className="pl-10 h-9 text-sm w-full"
                  />
                </div>
              </div>*/}
              <div>
                <Button
                  variant="outline"
                  className="w-full"
                  size="icon"
                  onClick={exportExcel}
                  aria-label="Refresh"
                >
                <Download className="h-4 w-4" />
                  Export Excel
                </Button>
              </div>
            </div>
          </div>
        </CardHeader>
      </Card>

      <Card>
        <CardContent className="p-6">
          {loading && (
            <div className="mb-4 text-sm text-muted-foreground">
              Loading slots...
            </div>
          )}
          {error && <div className="mb-4 text-sm text-rose-600">{error}</div>}

          <div className="mb-3 text-sm text-muted-foreground">
            Showing <strong>{timesToRender.length}</strong> time(s) •{" "}
            <strong>{bookedCount}</strong> booked for{" "}
            <strong>{new Date(selectedDate).toDateString()}</strong>
          </div>

          {/* Responsive grid: 1 column on xs, 2 on sm, 4 on lg+ */}
          {/* Slot grid */}
          <SlotGrid
            timesToRender={timesToRender}
            slots={slots}
            appointmentsByTime={appointmentsByTime}
            doctorsList={doctorsList}
            locationsList={locationsList}
            patientNameById={patientNameById}
            selectedDoctor={selectedDoctor}
            selectedLocation={selectedLocation}
            onTimeClick={handleTimeCardClick}
            onAppointmentClick={handleAppointmentClick}
            formatTimeLabel={formatTimeLabel}
          />
        </CardContent>
      </Card>

      {/* Appointment panel (booked appointment details) */}
      {selectedAppointment && (
        <AppointmentPanel
          appointment={selectedAppointment}
          onClose={() => {setSelectedAppointment(null);refresh();}}
          onClickOutside={() => setSelectedAppointment(null)}
        />
      )}

      {/* New appointment dialog for free slots */}
      {newAppointmentParams && (
        <AppointmentDialog
          doctorId={newAppointmentParams.doctorId}
          locationId={newAppointmentParams.locationId}
          date={newAppointmentParams.date}
          slot={newAppointmentParams.slot}
          onClose={() => {
            setNewAppointmentParams(null);
            refresh();
          }}
        />
      )}
    </div>
  );
}
