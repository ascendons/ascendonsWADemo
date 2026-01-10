// @/components/Dashboard/index.tsx
import { useCallback, useEffect, useMemo, useState } from "react";
import { format } from "date-fns";
import { Calendar as CalendarIcon, Users, XCircle, Search, RefreshCw } from "lucide-react";
import { KPICard } from "@/components/Dashboard/KPICard";
import { AppointmentCard } from "@/components/Appointments/AppointmentCard";
import { AppointmentPanel } from "@/components/Appointments/AppointmentPanel";
import { Input } from "@/components/ui/input";
import { Button } from "@/components/ui/button";
import { Calendar } from "@/components/ui/calendar";
import { Popover, PopoverContent, PopoverTrigger } from "@/components/ui/popover";
import { useToast } from "@/hooks/use-toast";
import {
  AppointmentSummary,
  fetchAppointmentsByDate,
} from "@/api/appointmentService";
import { fetchAllPatients } from "@/api/patientService";

/** helper: yyyy-mm-dd */
function toISODate(d: Date) {
  // Use local date formatting to avoid UTC off-by-one issues.
  return format(d, "yyyy-MM-dd");
}

/** normalize status from API to the UI keys used by AppointmentCard */
function normalizeStatus(s?: string | null) {
  if (!s) return "unknown";
  const v = s.toString().trim().toLowerCase();
  if (v === "booked" || v === "confirmed") return "confirmed";
  if (v === "waitlisted" || v === "pending" || v === "in_progress")
    return "waitlisted";
  if (v === "cancelled" || v === "canceled") return "cancelled";
  if (v === "completed" || v === "done") return "completed";
  if (v === "walkin" || v === "walk-in" || v === "walk_in") return "walkin";
  return "unknown";
}

type UIStatus =
  | "all"
  | "confirmed"
  | "waitlisted"
  | "cancelled"
  | "completed"
  | "walkin"
  | "unknown";

export default function Dashboard() {
  const { toast } = useToast();

  const [selectedDate, setSelectedDate] = useState<Date>(() => new Date());
  const [todayAppointments, setTodayAppointments] = useState<
    AppointmentSummary[]
  >([]);
  const [patients, setPatients] = useState<any[]>([]);
  const [loading, setLoading] = useState(false);
  const [selectedAppointment, setSelectedAppointment] =
    useState<AppointmentSummary | null>(null);
  const [searchTerm, setSearchTerm] = useState<string>("");
  const [activeFilter, setActiveFilter] = useState<UIStatus>("all");

  // patient lookup map
  const patientNameById = useMemo(() => {
    const m = new Map<string, string>();
    for (const p of patients) {
      const name = p.name ?? p.fullName ?? p.displayName ?? "";
      const possibleIds = [p.id, p._id, p.userId, p.patientId].filter(Boolean);
      for (const id of possibleIds) {
        m.set(String(id), name || String(id));
      }
    }
    return m;
  }, [patients]);

  // Extracted load function — can be called on mount or via refresh button
  const load = useCallback(async () => {
    setLoading(true);
    try {
      const day = toISODate(selectedDate);
      const [appts, pats] = await Promise.all([
        fetchAppointmentsByDate(day),
        fetchAllPatients(),
      ]);

      setTodayAppointments(appts ?? []);
      setPatients(pats ?? []);
    } catch (err) {
      console.error("Failed to load appointments/patients", err);
      toast({
        title: "Failed to load appointments",
        description: err instanceof Error ? err.message : "Unknown error",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  }, [selectedDate, toast]);

  // Background refresh function - silent, no loading state, no toasts
  const backgroundRefresh = useCallback(async () => {
    try {
      const day = toISODate(selectedDate);
      const [appts, pats] = await Promise.all([
        fetchAppointmentsByDate(day),
        fetchAllPatients(true), // Force refresh to bypass cache
      ]);

      setTodayAppointments(appts ?? []);
      setPatients(pats ?? []);
    } catch (err) {
      // Silently fail in background - don't disturb user
      console.error("Background refresh failed", err);
    }
  }, [selectedDate]);

  // Load on mount
  useEffect(() => {
    let mounted = true;
    (async () => {
      if (!mounted) return;
      await load();
    })();
    return () => {
      mounted = false;
    };
  }, [load]);

  // Background polling every 30 seconds
  useEffect(() => {
    const interval = setInterval(() => {
      backgroundRefresh();
    }, 30000); // 30 seconds

    return () => {
      clearInterval(interval);
    };
  }, [backgroundRefresh]);

  // If date changes, close any open appointment details (it may no longer exist).
  useEffect(() => {
    setSelectedAppointment(null);
  }, [selectedDate]);

  // build a normalized-status field on the fly where needed
  const appointmentsWithNormalizedStatus = useMemo(() => {
    return todayAppointments.map((a) => ({
      ...a,
      _uiStatus: normalizeStatus(a.status),
    }));
  }, [todayAppointments]);

  // counts (use normalized statuses)
  const confirmedCount = appointmentsWithNormalizedStatus.filter(
    (a) => a._uiStatus === "confirmed"
  ).length;
  const waitingCount = appointmentsWithNormalizedStatus.filter(
    (a) => a._uiStatus === "waitlisted"
  ).length;
  const cancelledCount = appointmentsWithNormalizedStatus.filter(
    (a) => a._uiStatus === "cancelled"
  ).length;
  const completedCount = appointmentsWithNormalizedStatus.filter(
    (a) => a._uiStatus === "completed"
  ).length;
  const walkInCount = appointmentsWithNormalizedStatus.filter(
    (a) => a._uiStatus === "walkin"
  ).length;


  // Filtering + search (search against patientName, bookingId, phone, doctorId)
  const filteredAppointments = useMemo(() => {
    const term = searchTerm.trim().toLowerCase();
    let base = appointmentsWithNormalizedStatus;

    if (term) {
      base = base.filter((a) => {
        const pid = (a.patientId ?? "").toString().toLowerCase();
        const booking = (a.bookingId ?? "").toString().toLowerCase();
        const phone = (a.phone ?? "").toString().toLowerCase();
        const doc = (a.doctorId ?? "").toString().toLowerCase();
        const resolvedName = (
          patientNameById.get(a.patientId ?? "") ?? ""
        ).toLowerCase();
        return (
          pid.includes(term) ||
          booking.includes(term) ||
          phone.includes(term) ||
          doc.includes(term) ||
          resolvedName.includes(term)
        );
      });
    }

    if (activeFilter !== "all") {
      base = base.filter((a) => a._uiStatus === activeFilter);
    }

    return base;
  }, [
    appointmentsWithNormalizedStatus,
    searchTerm,
    activeFilter,
    patientNameById,
  ]);

  const renderAppointmentList = () => {
    if (loading) {
      return (
        <div className="py-12 text-center text-muted-foreground">Loading...</div>
      );
    }

    if (filteredAppointments.length === 0) {
      return (
        <div className="py-12 text-center text-muted-foreground">
          {searchTerm
            ? `No appointments found for "${searchTerm}" ${
                activeFilter !== "all" ? `in ${activeFilter}` : ""
              }.`
            : `No ${activeFilter === "all" ? "" : activeFilter + " "}appointments yet today.`}
        </div>
      );
    }

    return (
      <div className="mt-6 space-y-4">
        {filteredAppointments.map((appointment) => (
          <AppointmentCard
            key={appointment.id}
            id={appointment.id}
            bookingId={appointment.bookingId}
            patientId={appointment.patientId ?? undefined}
            patientName={patientNameById.get(appointment.patientId ?? "")}
            patientPhone={appointment.phone ?? undefined}
            time={appointment.time}
            status={appointment._uiStatus}
            doctorId={appointment.doctorId ?? undefined}
            onClick={() => setSelectedAppointment(appointment)}
          />
        ))}
      </div>
    );
  };

  const baseBtn =
    "inline-flex items-center gap-2 rounded-md px-3 py-1.5 text-sm font-medium transition";
  const styles: Record<string, string> = {
    all: `${baseBtn} bg-transparent border border-input text-foreground hover:bg-muted/50`,
    confirmed: `${baseBtn} text-white bg-emerald-600 hover:bg-emerald-700`,
    waitlisted: `${baseBtn} text-amber-900 bg-amber-200 hover:bg-amber-300`,
    completed: `${baseBtn} text-white bg-blue-600 hover:bg-blue-700`,
    cancelled: `${baseBtn} text-white bg-rose-600 hover:bg-rose-700`,
    walkin: `${baseBtn} text-white bg-amber-600 hover:bg-amber-700`, // <- changed key and temporary color
  };

  const activeStyles: Record<string, string> = {
    all: "bg-muted text-foreground border-none",
    confirmed: "bg-emerald-600 text-white",
    waitlisted: "bg-amber-300 text-amber-900",
    completed: "bg-blue-600 text-white",
    cancelled: "bg-rose-600 text-white",
    walkin: "bg-amber-600 text-white", // <- changed key
  };

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-foreground">
            Dashboard
          </h1>
          <p className="text-sm sm:text-base text-muted-foreground">
            Welcome back! Here's the overview for {format(selectedDate, "PPP")}.
          </p>
        </div>

        {/* Date picker + Refresh button — top-right */}
        <div className="flex flex-wrap items-center gap-2">
          <Popover>
            <PopoverTrigger asChild>
              <Button
                type="button"
                variant="outline"
                className="justify-start text-left font-normal min-w-[220px]"
              >
                <CalendarIcon className="h-4 w-4" />
                {format(selectedDate, "PPP")}
              </Button>
            </PopoverTrigger>
            <PopoverContent className="w-auto p-0" align="end">
              <Calendar
                mode="single"
                selected={selectedDate}
                onSelect={(d) => d && setSelectedDate(d)}
                initialFocus
              />
            </PopoverContent>
          </Popover>

          <Button
            onClick={load}
            disabled={loading}
            type="button"
            className="inline-flex items-center gap-2"
          >
            <RefreshCw className="h-4 w-4" />
            {loading ? "Refreshing..." : "Refresh"}
          </Button>
        </div>
      </div>

      {/* KPIs */}
      <div className="grid gap-4 grid-cols-1 sm:grid-cols-2 lg:grid-cols-4">
        <KPICard
          title="Appointments"
          value={todayAppointments.length}
          icon={CalendarIcon}
        />
        <KPICard title="waitlisted Now" value={waitingCount} icon={Users} />
        <KPICard title="Confirmed" value={confirmedCount} icon={CalendarIcon} />
        <KPICard title="Cancellations" value={cancelledCount} icon={XCircle} />
      </div>

      <div className="flex w-full">
        <div className="w-full max-w-full">
          {/* Search Bar */}
          <div className="relative mb-3 lg:mb-6">
            <Search className="absolute left-3 top-1/2 h-4 w-4 -translate-y-1/2 text-muted-foreground" />
            <Input
              placeholder="Search patients, appointments..."
              className="pl-10 w-full"
              value={searchTerm}
              onChange={(e) => setSearchTerm(e.target.value)}
            />
          </div>

          {/* FILTER BUTTONS */}
          <div className="flex flex-wrap gap-2 items-center">
            <button
              type="button"
              aria-pressed={activeFilter === "all"}
              onClick={() => setActiveFilter("all")}
              className={`${styles.all} ${activeStyles.all} ${activeFilter === "all" ? activeStyles.all : ""}`}
            >
              All ({todayAppointments.length})
            </button>

            <button
              type="button"
              aria-pressed={activeFilter === "confirmed"}
              onClick={() => setActiveFilter("confirmed")}
              className={`${styles.confirmed} ${activeFilter === "confirmed" ? activeStyles.confirmed : "opacity-95"}`}
            >
              Confirmed ({confirmedCount})
            </button>

            <button
              type="button"
              aria-pressed={activeFilter === "waitlisted"}
              onClick={() => setActiveFilter("waitlisted")}
              className={`${styles.waitlisted} ${activeFilter === "waitlisted" ? activeStyles.waitlisted : ""}`}
            >
              Waitlisted ({waitingCount})
            </button>

            <button
              type="button"
              aria-pressed={activeFilter === "completed"}
              onClick={() => setActiveFilter("completed")}
              className={`${styles.completed} ${activeFilter === "completed" ? activeStyles.completed : ""}`}
            >
              Completed ({completedCount})
            </button>

            <button
              type="button"
              aria-pressed={activeFilter === "cancelled"}
              onClick={() => setActiveFilter("cancelled")}
              className={`${styles.cancelled} ${activeFilter === "cancelled" ? activeStyles.cancelled : ""}`}
            >
              Cancelled ({cancelledCount})
            </button>
           <button
             type="button"
             aria-pressed={activeFilter === "walkin"}
             onClick={() => setActiveFilter("walkin")}
             className={`${styles.walkin} ${activeFilter === "walkin" ? activeStyles.walkin : ""}`}
           >
             Walk-in ({walkInCount})
           </button>


          </div>

          {/* Appointment list */}
          <div className="mt-3">{renderAppointmentList()}</div>
        </div>
      </div>

      {/* Right-side panel on larger screens */}
      {selectedAppointment && (
        <AppointmentPanel
          appointment={selectedAppointment}
          onClose={() => {setSelectedAppointment(null);load();}}
          onClickOutside={() => setSelectedAppointment(null)}
        />
      )}
    </div>
  );
}
