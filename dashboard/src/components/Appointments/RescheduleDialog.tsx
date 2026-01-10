// @/components/Appointments/RescheduleDialog.tsx
import React, { useEffect, useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { useToast } from "@/hooks/use-toast";
import {
  AppointmentSummary,
  fetchAppointmentsByDate,
  fetchSlotsByDate,
  rescheduleAppointment,
} from "@/api/appointmentService";

type Props = {
  appointment: AppointmentSummary;
  open?: boolean;
  onClose: () => void;
  /**
   * Called when the appointment was rescheduled successfully.
   * Receives the payload with appointmentId, date and time.
   *
   * If you want the component to call your backend directly, you can
   * pass a function here that performs the API call and returns a promise.
   */
  onRescheduled?: (payload: AppointmentSummary) => Promise<any> | void;
};

export default function RescheduleDialog({
  appointment,
  open = true,
  onClose,
  onRescheduled,
}: Props) {
  const { toast } = useToast();

  // normalize to ISO (YYYY-MM-DD)
  const normalizeToIso = (d?: string) => {
    if (!d) return "";
    if (d.includes("-")) return d.slice(0, 10);
    if (d.length === 8)
      return `${d.slice(0, 4)}-${d.slice(4, 6)}-${d.slice(6)}`;
    return d;
  };

  const initialDate = normalizeToIso(appointment.date);
  const [date, setDate] = useState<string>(
    initialDate || new Date().toISOString().slice(0, 10),
  );
  const [slots, setSlots] = useState<string[]>([]);
  const [slotsLoading, setSlotsLoading] = useState(false);
  const [slotsError, setSlotsError] = useState<string | null>(null);
  const [takenTimes, setTakenTimes] = useState<Set<string>>(new Set());
  const [takenBy, setTakenBy] = useState<Map<string, string>>(new Map());

  const [selectedSlot, setSelectedSlot] = useState<string | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setDate(initialDate || new Date().toISOString().slice(0, 10));
    setSelectedSlot(null);
    setSlots([]);
    setSlotsError(null);
    setTakenTimes(new Set());
    setTakenBy(new Map());
  }, [appointment, initialDate, open]);

  useEffect(() => {
    let mounted = true;

    async function loadSlotsAndTaken(dateStr: string) {
      setSlotsLoading(true);
      setSlotsError(null);
      try {
        const docId = appointment.doctorId ?? "";
        const locId = appointment.locationId ?? "";
        if (!docId || !locId) {
          if (mounted) {
            setSlots([]);
            setSlotsError("Missing doctor or location");
          }
          return;
        }

        const [rawSlots, apptsOnDate] = await Promise.all([
          fetchSlotsByDate(dateStr, docId, locId),
          fetchAppointmentsByDate(dateStr),
        ]);

        if (!mounted) return;

        const normalized = (rawSlots ?? [])
          .map((x: any) => (typeof x === "string" ? x : (x?.time ?? "")))
          .filter(Boolean);

        const uniq = Array.from(new Set(normalized)).sort((a, b) =>
          a.localeCompare(b),
        );
        setSlots(uniq);

        const taken = new Set<string>();
        const map = new Map<string, string>();
        for (const a of apptsOnDate ?? []) {
          if (!a.time) continue;
          if (String(a.doctorId) !== String(docId)) continue;
          if (String(a.locationId) !== String(locId)) continue;
          if (String(a.id) === String(appointment.id)) continue;
          if(a.status !== "CONFIRMED") continue;
          taken.add(a.time);
          map.set(a.time, a.bookingId ?? a.patientId ?? "booked");
        }

        setTakenTimes(taken);
        setTakenBy(map);

        if (uniq.includes(appointment.time)) {
          setSelectedSlot(appointment.time);
        } else {
          setSelectedSlot(null);
        }
      } catch (err) {
        if (!mounted) return;
        setSlots([]);
        setTakenTimes(new Set());
        setTakenBy(new Map());
        setSlotsError(
          err instanceof Error ? err.message : "Failed to load slots",
        );
      } finally {
        if (mounted) setSlotsLoading(false);
      }
    }

    loadSlotsAndTaken(date);

    return () => {
      mounted = false;
    };
  }, [
    date,
    appointment.doctorId,
    appointment.locationId,
    appointment.id,
    appointment.time,
  ]);

  const canSave =
    !!selectedSlot &&
    !saving &&
    (!takenTimes.has(selectedSlot) || selectedSlot === appointment.time);

  async function handleSave() {
    if (!selectedSlot) {
      toast({
        title: "Select slot",
        description: "Choose an available slot to reschedule.",
        variant: "destructive",
      });
      return;
    }
    if (takenTimes.has(selectedSlot) && selectedSlot !== appointment.time) {
      toast({
        title: "Slot taken",
        description: "Selected slot is already booked.",
        variant: "destructive",
      });
      return;
    }

    setSaving(true);

    try {
      // Create a copy of appointment and overwrite date/time
      const updatedAppointment: AppointmentSummary = {
        ...appointment,
        date: date, // YYYY-MM-DD (same shape as `date` in this component)
        time: selectedSlot, // HH:mm
      };

      // Call backend reschedule endpoint
      // Expectation: rescheduleAppointment returns the updated appointment or a truthy result
      const result = await rescheduleAppointment(updatedAppointment);

      // If backend returned an updated object, use it; otherwise fall back to our updatedAppointment
      const returned = result ?? updatedAppointment;

      // Notify consumer if provided
      if (onRescheduled) {
        const maybePromise = onRescheduled(returned);
        if (maybePromise instanceof Promise) await maybePromise;
      }

      toast({
        title: "Appointment rescheduled",
        description: `${date} • ${selectedSlot}`,
      });
      onClose();
    } catch (err) {
      console.error("reschedule error", err);
      toast({
        title: "Reschedule failed",
        description: err instanceof Error ? err.message : "Unknown",
        variant: "destructive",
      });
    } finally {
      setSaving(false);
    }
  }
  if (!open) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      role="dialog"
      aria-modal="true"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
    >
      <div
        className="w-full max-w-lg bg-white rounded-lg shadow-lg p-6 mx-4"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-lg font-semibold">Reschedule Appointment</h2>
            <p className="text-sm text-muted-foreground">
              Current:{" "}
              <span className="font-medium">
                {normalizeToIso(appointment.date)} • {appointment.time}
              </span>
            </p>
          </div>
          <button
            className="text-sm text-muted-foreground hover:text-foreground"
            onClick={onClose}
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        <div className="space-y-4">
          <div>
            <label className="text-xs text-muted-foreground">
              Select new date
            </label>
            <Input
              type="date"
              value={date}
              onChange={(e) => setDate(e.target.value)}
              className="mt-1"
            />
          </div>

          <div>
            <label className="text-xs text-muted-foreground">
              Available slots
            </label>
            <div className="mt-2">
              {slotsLoading && (
                <div className="text-sm text-muted-foreground">
                  Loading slots...
                </div>
              )}
              {!slotsLoading && slotsError && (
                <div className="text-sm text-rose-600">{slotsError}</div>
              )}
              {!slotsLoading && !slotsError && slots.length === 0 && (
                <div className="text-sm text-muted-foreground">
                  No slots available for this date.
                </div>
              )}

              {!slotsLoading && slots.length > 0 && (
                <div className="grid grid-cols-5 gap-2 mt-2">
                  {slots.map((s) => {
                    const isTaken = takenTimes.has(s);
                    const takenLabel = takenBy.get(s);
                    const selected = s === selectedSlot;
                    const disabled = isTaken && s !== appointment.time;

                    return (
                      <button
                        key={s}
                        type="button"
                        onClick={() => !disabled && setSelectedSlot(s)}
                        disabled={disabled}
                        aria-pressed={selected}
                        className={`px-2 py-2 rounded-md text-sm border text-left transition ${
                          selected
                            ? "border-emerald-600 bg-emerald-50"
                            : disabled
                              ? "opacity-60 cursor-not-allowed border-rose-100 bg-rose-50"
                              : "border-input bg-white hover:bg-muted/50"
                        }`}
                      >
                        <div className="font-mono text-xs">{s}</div>
                        <div className="text-[10px] text-muted-foreground">
                          {disabled ? `booked (${takenLabel})` : "slot"}
                        </div>
                      </button>
                    );
                  })}
                </div>
              )}
            </div>
          </div>
        </div>

        <div className="mt-6 flex items-center justify-end gap-3">
          <Button variant="ghost" onClick={onClose} disabled={saving}>
            Cancel
          </Button>
          <Button onClick={handleSave} disabled={!canSave}>
            {saving ? "Saving..." : "Reschedule"}
          </Button>
        </div>
      </div>
    </div>
  );
}
