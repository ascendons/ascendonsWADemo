import { useEffect, useMemo, useState } from "react";
import { Button } from "@/components/ui/button";
import {
  Select,
  SelectTrigger,
  SelectValue,
  SelectContent,
  SelectItem,
} from "@/components/ui/select";
import { useToast } from "@/hooks/use-toast";
import {
  DayKey,
  DoctorSchedule,
  fetchDoctorAvailability,
  saveDoctorAvailability,
  mapBackendDaysToUI,
  mapUIDaysToBackend,
} from "@/api/availabilityService";

import { Location, fetchAllLocations } from "@/api/locationService";
/** UI day keys (short) */
const DAYS: { key: DayKey; label: string }[] = [
  { key: "M", label: "Mon" },
  { key: "T", label: "Tue" },
  { key: "W", label: "Wed" },
  { key: "Th", label: "Thu" },
  { key: "F", label: "Fri" },
  { key: "S", label: "Sat" },
  { key: "Su", label: "Sun" },
];

function minutesFromString(t: string) {
  const [h, m] = t.split(":").map(Number);
  return h * 60 + m;
}
function timeStringFromMinutes(mins: number) {
  const h = Math.floor(mins / 60);
  const m = mins % 60;
  return `${String(h).padStart(2, "0")}:${String(m).padStart(2, "0")}`;
}
function formatLabel(hhmm: string) {
  const [h, m] = hhmm.split(":").map(Number);
  const ampm = h >= 12 ? "PM" : "AM";
  const hour12 = ((h + 11) % 12) + 1;
  return `${hour12}:${String(m).padStart(2, "0")} ${ampm}`;
}
function buildTimeOptions() {
  const start = 5 * 60;
  const end = 21 * 60 + 30;
  const out: { value: string; label: string; mins: number }[] = [];
  for (let t = start; t <= end; t += 30) {
    const v = timeStringFromMinutes(t);
    out.push({ value: v, label: formatLabel(v), mins: t });
  }
  return out;
}
const TIME_OPTIONS = buildTimeOptions();

/** Utility: complement a set of DayKey values relative to DAYS */
function complementDays(selected: DayKey[] = []): DayKey[] {
  const set = new Set(selected);
  return DAYS.map((d) => d.key).filter((k) => !set.has(k));
}

export default function DoctorAvailability({ doctorId }: { doctorId: string }) {
  const { toast } = useToast();

  const [doctorSchedule, setDoctorSchedule] = useState<DoctorSchedule | null>(
    null,
  );
  const [loading, setLoading] = useState<boolean>(false);
  const [saving, setSaving] = useState<boolean>(false);

  // selectedDays now represent AVAILABLE days in the UI
  const [selectedDays, setSelectedDays] = useState<DayKey[]>([]);
  const [globalRange, setGlobalRange] = useState<{ from: string; to: string }>({
    from: "09:00",
    to: "17:30",
  });
  const [unavailableDates, setUnavailableDates] = useState<string[]>([]);
  const [newDate, setNewDate] = useState<string>("");
  const [locations, setLocations] = useState<Location[]>([]);

  // Fetch schedule when doctorId changes
  useEffect(() => {
    let mounted = true;
    async function fetchData() {
      if (!doctorId) return;
      setLoading(true);
      try {
        const data = await fetchDoctorAvailability(doctorId);
        if (!mounted) return;
        setDoctorSchedule(data ?? null);
        const locs = await fetchAllLocations();
        setLocations(locs);
        console.log("locations", locs);
        if (data) {
          // backend provides unavailableDaysOfWeek -> convert to UI short keys
          const backendUnavailable = data.unavailableDaysOfWeek ?? [];
          const uiUnavailable = mapBackendDaysToUI(backendUnavailable);

          // Compute available days = complement of unavailable
          const uiAvailable = complementDays(uiUnavailable);
          setSelectedDays(uiAvailable);

          if (data.startTime && data.endTime) {
            setGlobalRange({ from: data.startTime, to: data.endTime });
          } else if (
            data.availableOnlyDates &&
            data.availableOnlyDates.length > 0
          ) {
            setGlobalRange({ from: "09:00", to: "17:30" });
          }

          setUnavailableDates((data.unavailableDates ?? []).slice().sort());
        } else {
          setSelectedDays([]);
          setGlobalRange({ from: "09:00", to: "17:30" });
          setUnavailableDates([]);
        }
      } catch (err) {
        console.error("fetchDoctorAvailability error", err);
        toast({
          title: "Failed to load availability",
          description: err instanceof Error ? err.message : "Unknown error",
          variant: "destructive",
        });
      } finally {
        if (mounted) setLoading(false);
      }
    }

    fetchData();
    return () => {
      mounted = false;
    };
  }, [doctorId, toast]);

  const toggleDay = (d: DayKey) =>
    setSelectedDays((prev) =>
      prev.includes(d) ? prev.filter((x) => x !== d) : [...prev, d].sort(),
    );

  const endOptions = useMemo(() => {
    const fromMins = minutesFromString(globalRange.from);
    return TIME_OPTIONS.filter((o) => o.mins > fromMins);
  }, [globalRange.from]);

  const addUnavailable = () => {
    if (!newDate) return;
    if (!unavailableDates.includes(newDate)) {
      setUnavailableDates((p) => [...p, newDate].sort());
    }
    setNewDate("");
  };
  const removeUnavailable = (d: string) =>
    setUnavailableDates((p) => p.filter((x) => x !== d));

  const hasErrors = useMemo(() => {
    if (
      minutesFromString(globalRange.to) <= minutesFromString(globalRange.from)
    )
      return true;
    return false;
  }, [globalRange]);

  const handleSave = async () => {
    if (!doctorId) {
      toast({
        title: "Missing doctor",
        description: "Doctor ID is required",
        variant: "destructive",
      });
      return;
    }
    if (hasErrors) {
      toast({
        title: "Fix errors",
        description: "Ensure time range is valid",
        variant: "destructive",
      });
      return;
    }

    setSaving(true);
    try {
      const uiAvailable = selectedDays;
      const uiUnavailable = complementDays(uiAvailable); // short keys
      const backendUnavailable = mapUIDaysToBackend(uiUnavailable); // backend full names

      const merged: DoctorSchedule = {
        id: doctorSchedule?.id ?? "",
        doctorId: doctorId,
        doctorName: doctorSchedule?.doctorName ?? "",
        locationId: locations[0].id,
        daysOfWeek: doctorSchedule?.daysOfWeek ?? [],
        startTime: globalRange.from,
        endTime: globalRange.to,
        unavailableDaysOfWeek: backendUnavailable,
        unavailableDates: unavailableDates.slice().sort(),
        availableOnlyDates: doctorSchedule?.availableOnlyDates ?? [],
        customDateSlots: doctorSchedule?.customDateSlots ?? [],
      };

      const saved = await saveDoctorAvailability(merged);

      // reflect server response: server returns unavailableDaysOfWeek -> compute available days
      setDoctorSchedule(saved);
      const savedUnavailableUI = mapBackendDaysToUI(
        saved.unavailableDaysOfWeek ?? [],
      );
      const savedAvailableUI = complementDays(savedUnavailableUI);
      setSelectedDays(savedAvailableUI);

      if (saved.startTime && saved.endTime) {
        setGlobalRange({ from: saved.startTime, to: saved.endTime });
      }
      setUnavailableDates((saved.unavailableDates ?? []).slice().sort());

      toast({
        title: "Saved",
        description: "Availability saved successfully.",
      });
    } catch (err) {
      console.error("saveDoctorAvailability error", err);
      toast({
        title: "Save failed",
        description: err instanceof Error ? err.message : "Unknown error",
        variant: "destructive",
      });
    } finally {
      setSaving(false);
    }
  };

  return (
    <div className="p-4 bg-white rounded-md shadow-sm max-w-3xl">
      <h3 className="text-lg font-semibold mb-3">Doctor availability</h3>

      {loading && (
        <div className="mb-3 text-sm text-muted-foreground">
          Loading schedule...
        </div>
      )}

      {/* 1. pick single time range for all days */}
      <div className="mb-4 border rounded p-3">
        <div className="mb-2 font-medium">Daily time range</div>

        <div className="flex gap-4 items-center flex-wrap">
          <div className="flex items-center gap-2">
            <label className="text-xs">From</label>
            <Select
              value={globalRange.from}
              onValueChange={(val) => {
                const currentTo = globalRange.to;
                const valid =
                  minutesFromString(currentTo) > minutesFromString(val);
                const nextTo = valid
                  ? currentTo
                  : (TIME_OPTIONS.find((o) => o.mins > minutesFromString(val))
                      ?.value ?? currentTo);
                setGlobalRange({ from: val, to: nextTo });
              }}
            >
              <SelectTrigger className="w-36">
                <SelectValue>{formatLabel(globalRange.from)}</SelectValue>
              </SelectTrigger>
              <SelectContent>
                {TIME_OPTIONS.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="flex items-center gap-2">
            <label className="text-xs">End</label>
            <Select
              value={globalRange.to}
              onValueChange={(val) =>
                setGlobalRange((p) => ({ ...p, to: val }))
              }
            >
              <SelectTrigger className="w-36">
                <SelectValue>{formatLabel(globalRange.to)}</SelectValue>
              </SelectTrigger>
              <SelectContent>
                {endOptions.map((opt) => (
                  <SelectItem key={opt.value} value={opt.value}>
                    {opt.label}
                  </SelectItem>
                ))}
              </SelectContent>
            </Select>
          </div>

          <div className="text-sm text-muted-foreground">
            {minutesFromString(globalRange.to) <=
            minutesFromString(globalRange.from) ? (
              <span className="text-red-600">End must be after From</span>
            ) : (
              <span>
                Range: <strong>{formatLabel(globalRange.from)}</strong> —{" "}
                <strong>{formatLabel(globalRange.to)}</strong>
              </span>
            )}
          </div>
        </div>
      </div>

      {/* 2. Available days (UI) */}
      <div className="mb-4">
        <label className="block mb-2 font-medium">Available days</label>
        <div className="flex gap-3 flex-wrap">
          {DAYS.map((d) => (
            <label
              key={d.key}
              className="inline-flex items-center gap-2 cursor-pointer"
            >
              <input
                type="checkbox"
                checked={selectedDays.includes(d.key)}
                onChange={() => toggleDay(d.key)}
                className="h-4 w-4"
              />
              <span className="text-sm">{d.label}</span>
            </label>
          ))}
        </div>

        <div className="mt-2 text-sm">
          {selectedDays.length === 0 ? (
            <span className="text-muted-foreground">
              No available days set.
            </span>
          ) : (
            <span>
              Available:{" "}
              <strong>
                {selectedDays
                  .map((k) => DAYS.find((d) => d.key === k)!.label)
                  .join(", ")}
              </strong>
            </span>
          )}
        </div>
      </div>

      {/* 3. Unavailable dates (specific calendar dates) */}
      <div className="mb-4">
        <label className="block mb-2 font-medium">Unavailable dates</label>
        <div className="flex gap-2 items-center">
          <input
            type="date"
            value={newDate}
            onChange={(e) => setNewDate(e.target.value)}
            className="border rounded px-2 py-1"
          />
          <Button variant="outline" onClick={addUnavailable}>
            Add
          </Button>
        </div>

        <div className="mt-2 flex gap-2 flex-wrap">
          {unavailableDates.length === 0 && (
            <div className="text-sm text-muted-foreground">
              No unavailable dates
            </div>
          )}
          {unavailableDates.map((d) => (
            <div
              key={d}
              className="px-2 py-1 border rounded inline-flex items-center gap-2"
            >
              <span className="text-sm">{d}</span>
              <button onClick={() => removeUnavailable(d)} className="text-xs">
                x
              </button>
            </div>
          ))}
        </div>
      </div>

      {/* actions */}
      <div className="flex gap-3 items-center">
        <Button onClick={handleSave} disabled={hasErrors || loading || saving}>
          {saving ? "Saving..." : "Save"}
        </Button>

        {hasErrors && (
          <div className="text-sm text-red-600 ml-3">
            Fix time range before saving.
          </div>
        )}
      </div>
    </div>
  );
}
