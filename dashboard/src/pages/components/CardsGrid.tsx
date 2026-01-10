// @/components/SlotGrid.tsx
import { Badge } from "@/components/ui/badge";
import { AppointmentSummary } from "@/api/appointmentService";

type SlotGridProps = {
  timesToRender: string[];
  slots: string[];
  appointmentsByTime: Map<string, AppointmentSummary[]>;
  doctorsList: { id: string; name?: string }[];
  locationsList: { id: string; name?: string }[];
  patientNameById: Map<string, string>;
  selectedDoctor?: string;
  selectedLocation?: string;
  onTimeClick: (time: string) => void;
  onAppointmentClick: (a: AppointmentSummary) => void;
  formatTimeLabel: (t: string) => string;
};

function normalizeStatus(value: any): string {
  if (value == null) return "";
  if (Array.isArray(value)) {
    return value.map(String).join(",").toLowerCase().trim();
  }
  return String(value).toLowerCase().trim();
}

function appointmentIsConfirmed(a: AppointmentSummary): boolean {
  const s = normalizeStatus(a.status);
  if (!s) return false;
  const pieces = s.split(/[,|]/).map((p) => p.trim());
  return pieces.includes("confirmed");
}

function getStatusLabel(a: AppointmentSummary) {
  return appointmentIsConfirmed(a) ? "confirmed" : String(a.status ?? "").toLowerCase();
}

export default function SlotGrid({
  timesToRender,
  slots,
  appointmentsByTime,
  doctorsList,
  locationsList,
  patientNameById,
  selectedDoctor,
  selectedLocation,
  onTimeClick,
  onAppointmentClick,
  formatTimeLabel,
}: SlotGridProps) {
  return (
    <div className="grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4 gap-4 py-2">
      {timesToRender.map((time) => {
        // Collect the raw list (may be undefined)
        const rawAppts = appointmentsByTime.get(time) ?? [];

        // Filter to only confirmed appointments (for this view)
        const confirmedAppts = rawAppts.filter(appointmentIsConfirmed);

        const isBooked = confirmedAppts.length > 0;
        // status for styling — if booked it's confirmed
        const status = isBooked ? "confirmed" : "";

        const getPatientLabel = (a: AppointmentSummary) =>
          patientNameById.get(a.patientId ?? "") ??
          a.patientId ??
          a.bookingId ??
          "Booked";

        const cardClass = isBooked
          ? "border rounded-lg p-3 bg-emerald-50 shadow-sm"
          : slots.includes(time)
          ? "border rounded-lg p-3 bg-white shadow-sm"
          : "border rounded-lg p-3 bg-white shadow-sm";

        return (
          <div
            key={time}
            role="button"
            tabIndex={0}
            onKeyDown={(e) => {
              if (e.key === "Enter" || e.key === " ") onTimeClick(time);
            }}
            onClick={() => onTimeClick(time)}
            className={`cursor-pointer ${cardClass}`}
          >
            <div className="flex items-center justify-between mb-2">
              <div className="text-sm font-medium">{formatTimeLabel(time)}</div>
              <div className="text-xs text-muted-foreground">{time}</div>
            </div>

            <div className="space-y-2">
              {slots.includes(time) ? (
                <>
                    <div className="text-xs">
                      <div className="font-small">
                        {selectedDoctor
                          ? doctorsList.find((d) => d.id === selectedDoctor)?.name ?? selectedDoctor
                          : "Selected doctor"}
                      </div>
                    </div>

                  {isBooked ? (
                    <div className="space-y-1">
                      {confirmedAppts.map((a) => {
                        const key = a.id ?? a.bookingId ?? `${time}-${Math.random().toString(36).slice(2, 8)}`;
                        const statusLabel = getStatusLabel(a);
                        return (
                          <div
                            key={key}
                            className="p-2 rounded-md border flex items-center justify-between"
                            onClick={(e) => {
                              e.stopPropagation();
                              onAppointmentClick(a);
                            }}
                            role="button"
                            tabIndex={0}
                            onKeyDown={(e) => {
                              if (e.key === "Enter" || e.key === " ") {
                                e.stopPropagation();
                                onAppointmentClick(a);
                              }
                            }}
                          >
                            <div className="text-sm">
                              <div className="font-medium">{getPatientLabel(a)}</div>
                              <div className="text-xs text-muted-foreground">
                                {a.bookingId ?? a.phone ?? a.doctorId ?? ""}
                              </div>
                            </div>
                            <Badge className={statusLabel === "confirmed" ? "bg-emerald-100 text-emerald-800" : "bg-rose-100 text-rose-600"}>
                              {statusLabel || "booked"}
                            </Badge>
                          </div>
                        );
                      })}
                    </div>
                  ) : (
                    <div className="flex items-center justify-between">
                      <div className="text-sm text-muted-foreground">Free</div>
                      <Badge className="bg-success-light text-success">available</Badge>
                    </div>
                  )}
                </>
              ) : isBooked ? (
                // slot not listed in available slots but has confirmed appointment(s)
                <div className="space-y-1">
                  {confirmedAppts.map((a) => {
                    const key = a.id ?? a.bookingId ?? `${time}-${Math.random().toString(36).slice(2, 8)}`;
                    const statusLabel = getStatusLabel(a);
                    return (
                      <div
                        key={key}
                        className="p-2 rounded-md border flex items-center justify-between"
                        onClick={(e) => {
                          e.stopPropagation();
                          onAppointmentClick(a);
                        }}
                        role="button"
                        tabIndex={0}
                      >
                        <div className="text-sm">
                          <div className="font-medium">{getPatientLabel(a)}</div>
                          <div className="text-xs text-muted-foreground">
                            {a.bookingId ?? a.phone ?? a.doctorId ?? ""}
                          </div>
                        </div>
                        <Badge className={statusLabel === "confirmed" ? "bg-emerald-100 text-emerald-800" : "bg-rose-100 text-rose-600"}>
                          {statusLabel || "booked"}
                        </Badge>
                      </div>
                    );
                  })}
                </div>
              ) : (
                <div className="text-sm text-muted-foreground">No slot</div>
              )}
            </div>
          </div>
        );
      })}
    </div>
  );
}
