import {
  X,
  Calendar,
  Clock,
  Edit,
  Ban,
  PersonStanding,
  PhoneCall,
} from "lucide-react";
import { useRef, useEffect, useState } from "react";
import { Button } from "@/components/ui/button";
import { Badge } from "@/components/ui/badge";
import { Separator } from "@/components/ui/separator";
import {
  AppointmentSummary,
  cancelAppointment,
  completeAppointment,
} from "@/api/appointmentService";
import { fetchUsersByRole } from "@/api/userService";
import { fetchAllPatients, PatientDetails } from "@/api/patientService";
import { fetchAllLocations } from "@/api/locationService";
import RescheduleDialog from "./RescheduleDialog";
import { useToast } from "@/hooks/use-toast";

interface AppointmentPanelProps {
  appointment: AppointmentSummary;
  onClose: () => void;
  onClickOutside?: () => void;
}

export function AppointmentPanel({
  appointment,
  onClose,
  onClickOutside,
}: AppointmentPanelProps) {
  const panelRef = useRef<HTMLDivElement>(null);
  const { toast } = useToast();

  const [doctorName, setDoctorName] = useState<string>("");
  const [patient, setPatient] = useState<PatientDetails | undefined>(undefined);
  const [locationName, setLocationName] = useState<string>("");
  const [listsLoading, setListsLoading] = useState<boolean>(false);

  // show/hide reschedule dialog
  const [showReschedule, setShowReschedule] = useState<boolean>(false);
  const [showCancel, setShowCancel] = useState<boolean>(false);

  useEffect(() => {
    let mounted = true;
    (async () => {
      setListsLoading(true);
      try {
        const [dList, pList, locList] = await Promise.all([
          fetchUsersByRole("DOCTOR"),
          fetchAllPatients(),
          fetchAllLocations(),
        ]);

        if (!mounted) return;

        // find doctor once
        if (Array.isArray(dList) && appointment.doctorId) {
          const doc = dList.find((x) => x.id === appointment.doctorId);
          if (doc) setDoctorName((prev) => prev || doc.name || "");
        }

        // find patient once
        if (Array.isArray(pList) && appointment.patientId) {
          const pat = pList.find(
            (x: any) =>
              x?.patientId === appointment.patientId ||
              String(x?.userId) === String(appointment.patientId),
          );
          if (pat) setPatient(pat);
        }

        // find location once
        if (Array.isArray(locList) && appointment.locationId) {
          const loc = locList.find((x) => x.id === appointment.locationId);
          if (loc) setLocationName((prev) => prev || loc.name || "");
        }
      } catch (err) {
        // optional: show toast or console.error
        // console.error("AppointmentPanel list load failed", err);
      } finally {
        if (mounted) setListsLoading(false);
      }
    })();

    return () => {
      mounted = false;
    };
  }, [appointment.doctorId, appointment.patientId, appointment.locationId]);

  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (
        panelRef.current &&
        !panelRef.current.contains(event.target as Node)
      ) {
        onClickOutside ? onClickOutside() : onClose();
      }
    }

    document.addEventListener("mousedown", handleClickOutside);
    return () => document.removeEventListener("mousedown", handleClickOutside);
  }, [onClose, onClickOutside]);

  // Open the reschedule dialog
  function handleOpenReschedule() {
    setShowReschedule(true);
  }

  function cancel() {
    setShowCancel(true);
  }

  function refresh(payload?: {
    appointmentId?: string;
    date?: string;
    time?: string;
  }) {
    // optional toast (you can remove if you don't want it)
    toast({
      title: "Refreshing appointments...",
      description: payload?.date
        ? `${payload.date} ${payload.time ?? ""}`
        : undefined,
    });

    // dispatch event — parent components can listen to this and reload data
    try {
      const event = new CustomEvent("appointments:refresh", {
        detail: payload ?? {},
      });
      window.dispatchEvent(event);
    } catch (err) {
      // fallback: set a short timeout and call a no-op (keeps behavior safe)
      // (This catch is extremely defensive — CustomEvent is well-supported.)
      console.warn("Could not dispatch appointments:refresh event", err);
    }
  }
  async function handleRescheduled(payload: {
    appointmentId: string;
    date: string;
    time: string;
  }) {
    toast({
      title: "Appointment rescheduled",
      description: `${payload.date} • ${payload.time}`,
    });

    // close reschedule dialog and the panel (parent can refresh)
    setShowReschedule(false);
    refresh(payload);
    onClose();
  }
  async function cancelConfirm() {
    // prefer bookingId (server param) then fall back to id
    const bookingId = appointment.bookingId ?? appointment.id;
    // debug log — check console
//     console.log("[AppointmentPanel] cancelConfirm -> bookingId:", bookingId);

    if (!bookingId) {
      toast({
        title: "Missing booking id",
        description: "Cannot cancel: no bookingId available.",
      });
      return;
    }

    try {
      // show immediate feedback
      toast({
        title: "Cancelling appointment...",
        description: `Booking ${bookingId}`,
      });

      // await the API and log
      const start = Date.now();
      await cancelAppointment(String(bookingId));
//       console.log(
//         "[AppointmentPanel] cancelAppointment resolved in",
//         Date.now() - start,
//         "ms",
//       );

      // notify rest of app
      refresh({ appointmentId: String(bookingId) });

      // success toast
      toast({
        title: "Appointment cancelled",
        description: `Booking ${bookingId} has been cancelled.`,
      });

      // close dialogs / panel
      setShowCancel(false);
      onClose();
    } catch (err: any) {
      console.error("[AppointmentPanel] cancel failed:", err);
      toast({
        title: "Could not cancel appointment",
        description: err?.message ?? String(err),
      });
    }
  }
  async function checkIn(){
    const appointmentId = appointment.id;
    await completeAppointment(String(appointmentId));
    toast({
      title: "Appointment checked in",
      description: `Appointment ${appointmentId} has been checked in.`,
    });
    onClose();
  }

  return (
    <>
      <div className="fixed inset-0 z-50 flex justify-end bg-black/30 backdrop-blur-sm">
        <div
          ref={panelRef}
          className="h-full w-full max-w-md animate-slide-in-right border-l border-border bg-card shadow-xl"
        >
          <div className="flex h-full flex-col">
            {/* Header */}
            <div className="flex items-center justify-between border-b border-border p-6">
              <h2 className="text-xl font-semibold text-foreground">
                Appointment Details
              </h2>
              <Button variant="ghost" size="icon" onClick={onClose}>
                <X className="h-5 w-5" />
              </Button>
            </div>

            <div className="flex-1 overflow-y-auto p-6">
              <div className="space-y-6">
                {/* Patient / booking info */}
                <div>
                  <h3 className="mb-3 text-lg font-semibold text-foreground">
                    {appointment.bookingId ??
                      patient?.name ??
                      appointment.patientId ??
                      "Appointment"}
                  </h3>
                  <div className="space-y-2 text-sm">
                    <div className="flex items-center gap-2 text-muted-foreground">
                      <PersonStanding className="h-4 w-4" />
                      <span>{patient?.name ?? "—"}</span>
                      {listsLoading && (
                        <span className="ml-2 text-xs text-muted-foreground">
                          loading...
                        </span>
                      )}
                    </div>
                    <div className="flex items-center gap-2 text-muted-foreground">
                      <PhoneCall className="h-4 w-4" />
                      <span>{patient?.phone ?? appointment.phone ?? "—"}</span>
                    </div>
                  </div>
                </div>

                <Separator />

                <div className="flex items-center justify-between">
                  <span className="text-sm font-medium text-foreground">
                    {locationName || appointment.locationId || "—"}
                  </span>
                </div>

                <div className="space-y-3">
                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium text-muted-foreground">
                      Status
                    </span>
                    <Badge
                      className={
                        appointment.status &&
                        appointment.status.toLowerCase() === "booked"
                          ? "bg-success-light text-success"
                          : "bg-rose-100 text-rose-600"
                      }
                    >
                      {appointment.status ?? "unknown"}
                    </Badge>
                  </div>

                  <div className="flex items-center justify-between">
                    <span className="text-sm font-medium text-muted-foreground">
                      Doctor
                    </span>
                    <span className="text-sm font-medium text-foreground">
                      {doctorName || appointment.doctorId || "—"}
                    </span>
                  </div>

                  <div className="flex items-center gap-2 text-sm">
                    <Calendar className="h-4 w-4 text-muted-foreground" />
                    <span className="font-medium text-foreground">
                      {formatDateDisplay(appointment.date)}
                    </span>
                  </div>

                  <div className="flex items-center gap-2 text-sm">
                    <Clock className="h-4 w-4 text-muted-foreground" />
                    <span className="font-medium text-foreground">
                      {appointment.time}
                    </span>
                  </div>
                </div>

                <Separator />

                <div>
                  <h4 className="mb-2 text-sm font-semibold text-foreground">
                    Details
                  </h4>
                  <div className="text-sm text-muted-foreground space-y-1">
                    <div>Booking ID: {appointment.bookingId ?? "—"}</div>
                    <div>Patient ID: {appointment.patientId ?? "—"}</div>
                  </div>
                </div>
              </div>
            </div>

            <div className="border-t border-border p-6">
              <div className="flex flex-col gap-3">
                {appointment.status !== "CANCELLED" && (<Button className="w-full" onClick={checkIn}>
                  <Calendar className="mr-2 h-4 w-4" />
                  Check In
                </Button>)}
                <div className="flex gap-3">
                  <Button
                    variant="outline"
                    className="flex-1"
                    onClick={handleOpenReschedule}
                  >
                    <Edit className="mr-2 h-4 w-4" />
                    Reschedule
                  </Button>
                  {appointment.status !== "CANCELLED" && (<Button
                    variant="outline"
                    className="flex-1 text-destructive hover:text-destructive"
                    onClick={cancelConfirm}
                  >
                    <Ban className="mr-2 h-4 w-4" />
                    Cancel
                  </Button>)}
                </div>
              </div>
            </div>
          </div>
        </div>
      </div>

      {/* Reschedule dialog */}
      {showReschedule && (
        <RescheduleDialog
          appointment={appointment}
          open={showReschedule}
          onClose={() => {
            setShowReschedule(false);
          }}
          onRescheduled={handleRescheduled}
        />
      )}
      {/* Cancel dialog */}
      {showCancel && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="cancel-appointment-title"
          className="fixed inset-0 z-50 flex items-center justify-center px-4"
        >
          <div
            className="fixed inset-0 bg-black/40 backdrop-blur-sm"
            aria-hidden="true"
          />
          <div className="relative z-10 w-full max-w-lg rounded-lg bg-white p-6 shadow-lg">
            <h3
              id="cancel-appointment-title"
              className="text-lg font-semibold mb-2"
            >
              Cancel Appointment
            </h3>
            <p className="text-sm text-muted-foreground mb-4">
              Are you sure you want to cancel this appointment?
            </p>
            <div className="flex justify-end gap-3">
              <Button variant="outline" onClick={() => setShowCancel(false)}>
                No, Go Back
              </Button>
              <Button
                className="bg-destructive hover:bg-destructive/90 text-white"
                onClick={async () => {
                  console.log("[AppointmentPanel] cancelConfirm");
                  await cancelConfirm();
                }}
                type="button"
              >
                Yes, Cancel
              </Button>
            </div>
          </div>
        </div>
      )}
    </>
  );
}

function formatDateDisplay(d?: string) {
  if (!d) return "—";
  if (/^\d{8}$/.test(d)) {
    const y = d.slice(0, 4);
    const m = d.slice(4, 6);
    const day = d.slice(6, 8);
    try {
      return new Date(`${y}-${m}-${day}`).toLocaleDateString();
    } catch {
      return `${y}-${m}-${day}`;
    }
  }
  try {
    return new Date(d).toLocaleDateString();
  } catch {
    return d;
  }
}
