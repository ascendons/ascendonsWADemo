// @/components/AppointmentDialog.tsx
import React, { useEffect, useMemo, useRef, useState } from "react";
import {
  AppointmentSummary,
  createAppointment,
} from "@/api/appointmentService";
import { fetchUsersByRole, UserSummary } from "@/api/userService";
import { fetchAllLocations } from "@/api/locationService";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { fetchAllPatients, PatientDetails } from "@/api/patientService";

type Props = {
  doctorId?: string;
  locationId?: string;
  date?: string; // YYYY-MM-DD
  slot?: string; // "09:00" or "09:00 AM"
  onClose: () => void;
  onCreated?: (appointment: any) => void;
};

/** helpers */
function yyyyMMddFromIso(iso: string) {
  return iso.replace(/-/g, "");
}
function slotTo24(slot?: string) {
  if (!slot) return "";
  const s = slot.trim();
  if (/^\d{1,2}:\d{2}$/.test(s)) {
    const [h, m] = s.split(":").map((x) => x.padStart(2, "0"));
    return `${h}:${m}`;
  }
  const m = s.match(/^(\d{1,2}):(\d{2})\s*([AaPp][Mm])$/);
  if (m) {
    let hh = parseInt(m[1], 10);
    const mm = m[2];
    const ampm = m[3].toUpperCase();
    if (ampm === "AM" && hh === 12) hh = 0;
    if (ampm === "PM" && hh !== 12) hh += 12;
    return `${String(hh).padStart(2, "0")}:${mm}`;
  }
  try {
    const dt = new Date(`1970-01-01T${s}`);
    if (!isNaN(dt.getTime())) {
      const hh = String(dt.getHours()).padStart(2, "0");
      const mm = String(dt.getMinutes()).padStart(2, "0");
      return `${hh}:${mm}`;
    }
  } catch {}
  return s;
}

/** Simple Autocomplete combobox (id/name items) */
function Autocomplete({
  items,
  value,
  onChange,
  placeholder,
  label,
  disabled,
}: {
  items: { id: string; name?: string }[];
  value?: string | undefined;
  onChange: (id: string | undefined) => void;
  placeholder?: string;
  label?: string;
  disabled?: boolean;
}) {
  const [open, setOpen] = useState(false);
  const [query, setQuery] = useState("");
  const ref = useRef<HTMLDivElement | null>(null);
  const inputRef = useRef<HTMLInputElement | null>(null);

  useEffect(() => {
    function onDoc(e: MouseEvent) {
      if (!ref.current) return;
      if (!ref.current.contains(e.target as Node)) setOpen(false);
    }
    document.addEventListener("mousedown", onDoc);
    return () => document.removeEventListener("mousedown", onDoc);
  }, []);

  // show current selected label in input when value changes
  const selectedLabel = useMemo(
    () => items.find((it) => it.id === value)?.name ?? "",
    [items, value],
  );

  useEffect(() => {
    // show selected label when parent value changes
    setQuery(selectedLabel);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [value]);

  const filtered = useMemo(() => {
    const q = query.trim().toLowerCase();
    if (!q) return items;
    return items.filter(
      (it) =>
        (it.name ?? it.id).toLowerCase().includes(q) ||
        it.id.toLowerCase().includes(q),
    );
  }, [items, query]);

  // pick first filtered (used for Enter)
  function pickFirst() {
    if (filtered.length === 0) {
      // clearing selection if query empty
      if (!query) onChange(undefined);
      return;
    }
    const it = filtered[0];
    onChange(it.id);
    setQuery(it.name ?? it.id);
    setOpen(false);
  }

  return (
    <div ref={ref} className="relative">
      {label && (
        <div className="text-xs text-muted-foreground mb-1">{label}</div>
      )}
      <div className="flex items-center">
        <input
          ref={inputRef}
          type="text"
          role="combobox"
          aria-expanded={open}
          aria-controls="listbox"
          aria-autocomplete="list"
          value={query}
          onFocus={() => setOpen(true)}
          onChange={(e) => {
            setQuery(e.target.value);
            setOpen(true);
            // If cleared input, clear selection in parent
            if (e.target.value === "") onChange(undefined);
          }}
          onKeyDown={(e) => {
            if (e.key === "Enter") {
              e.preventDefault();
              pickFirst();
            } else if (e.key === "Escape") {
              setOpen(false);
              inputRef.current?.blur();
            }
          }}
          placeholder={placeholder}
          disabled={disabled}
          className="w-full p-2 border rounded text-sm"
        />
        {/* removed clear button as requested */}
      </div>

      {open && filtered.length > 0 && (
        <ul
          id="listbox"
          role="listbox"
          className="absolute z-20 mt-1 w-full max-h-48 overflow-auto border rounded bg-white shadow"
        >
          {filtered.map((it) => (
            <li
              key={it.id}
              role="option"
              onMouseDown={(e) => {
                e.preventDefault();
                onChange(it.id);
                setQuery(it.name ?? it.id);
                setOpen(false);
              }}
              className="px-3 py-2 hover:bg-muted/50 cursor-pointer text-sm"
            >
              <div className="font-medium">{it.name ?? it.id}</div>
              <div className="text-xs text-muted-foreground">{it.id}</div>
            </li>
          ))}
        </ul>
      )}

      {open && filtered.length === 0 && (
        <div className="absolute z-20 mt-1 w-full border rounded bg-white shadow p-2 text-sm text-muted-foreground">
          No results
        </div>
      )}
    </div>
  );
}

export default function AppointmentDialog({
  doctorId: propDoctorId,
  locationId: propLocationId,
  date: propDate,
  slot: propSlot,
  onClose,
  onCreated,
}: Props) {
  const { toast } = useToast();

  const [doctors, setDoctors] = useState<UserSummary[]>([]);
  const [patients, setPatients] = useState<PatientDetails[]>([]);
  const [locations, setLocations] = useState<any[]>([]);

  const [doctorId, setDoctorId] = useState<string | undefined>();
  const [locationId, setLocationId] = useState<string | undefined>();
  const [patientId, setPatientId] = useState<string | undefined>();
  const [notes, setNotes] = useState<string>("");

  const [loading, setLoading] = useState(false);
  const [listsLoading, setListsLoading] = useState(false);

  useEffect(() => {
    let mounted = true;
    (async () => {
      setListsLoading(true);
      try {
        const [d, p, locs] = await Promise.all([
          fetchUsersByRole("DOCTOR"),
          fetchAllPatients(),
          fetchAllLocations(),
        ]);
        if (!mounted) return;
        setDoctors(d ?? []);
        setPatients(p ?? []);
        setLocations(locs ?? []);

        if (propDoctorId) {
          setDoctorId(propDoctorId);
        } else if (d && d.length > 0) {
          setDoctorId(d[0].id);
        }

        if (propLocationId) {
          setLocationId(propLocationId);
        } else if (locs && locs.length > 0) {
          setLocationId(locs[0].id);
        }
      } catch (err) {
        console.error(err);
        toast({
          title: "Failed to load lists",
          description: err instanceof Error ? err.message : "Unknown",
          variant: "destructive",
        });
      } finally {
        if (mounted) setListsLoading(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [propDoctorId, propLocationId, toast]);

  const doctorItems = useMemo(
    () => doctors.map((d) => ({ id: d.id, name: d.name })),
    [doctors],
  );
  const patientItems = useMemo(
    () => patients.map((p) => ({ id: p.id, name: p.name })),
    [patients],
  );
  const locationItems = useMemo(
    () => locations.map((l: any) => ({ id: l.id, name: l.name })),
    [locations],
  );

  function validate() {
    if (!doctorId) {
      toast({ title: "Select doctor", variant: "destructive" });
      return false;
    }
    if (!locationId) {
      toast({ title: "Select location", variant: "destructive" });
      return false;
    }
    if (!propDate) {
      toast({ title: "Missing date", variant: "destructive" });
      return false;
    }
    if (!propSlot) {
      toast({ title: "Missing slot", variant: "destructive" });
      return false;
    }
    if (!patientId) {
      toast({
        title: "Missing patient",
        description: "Select patient",
        variant: "destructive",
      });
      return false;
    }
    return true;
  }

  async function handleCreate() {
    if (!validate()) return;
    setLoading(true);
    const dateStr = propDate ? yyyyMMddFromIso(propDate) : "";
    try {
      const patient = patients.find((p) => p.id === patientId);
      console.log(patient);
      const payload: AppointmentSummary = {
        patientId: patient?.patientId,
        doctorId,
        locationId,
        date: dateStr,
        phone: patient?.phone,
        time: slotTo24(propSlot),
      };
      const created = await createAppointment(payload);
      toast({
        title: "Appointment created",
        description: "Booking successful.",
      });
      if (onCreated) onCreated(created);
      onClose();
    } catch (err) {
      console.error(err);
      toast({
        title: "Create failed",
        description: err instanceof Error ? err.message : "Unknown",
        variant: "destructive",
      });
    } finally {
      setLoading(false);
    }
  }

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
      onMouseDown={(e) => {
        if (e.target === e.currentTarget) onClose();
      }}
      role="dialog"
      aria-modal="true"
    >
      <div
        className="w-full max-w-lg bg-white rounded-lg shadow-lg p-6 mx-4"
        onMouseDown={(e) => e.stopPropagation()}
      >
        <div className="flex items-start justify-between mb-4">
          <div>
            <h2 className="text-lg font-semibold">New Appointment</h2>
            <p className="text-sm text-muted-foreground">
              <span className="font-medium">{propDate ?? "—"}</span> {" • "}
              <span className="font-mono text-sm">{propSlot ?? "—"}</span>
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

        <div className="space-y-3">
          <div className="grid grid-cols-1 sm:grid-cols-2 gap-3">
            <Autocomplete
              items={doctorItems}
              value={doctorId}
              onChange={(v) => setDoctorId(v)}
              label="Doctor"
              placeholder={listsLoading ? "Loading..." : "Search doctor"}
              disabled={listsLoading}
            />
            <Autocomplete
              items={locationItems}
              value={locationId}
              onChange={(v) => setLocationId(v)}
              label="Location"
              placeholder={listsLoading ? "Loading..." : "Search location"}
              disabled={listsLoading}
            />
          </div>

          <div>
            <Autocomplete
              items={patientItems}
              value={patientId}
              onChange={(v) => setPatientId(v)}
              label="Patient"
              placeholder={listsLoading ? "Loading..." : "Search patient"}
              disabled={listsLoading}
            />
          </div>

          {/* Notes: make it wider / full width */}
          <div>
            <label className="text-xs text-muted-foreground">
              Notes (optional)
            </label>
            <textarea
              value={notes}
              onChange={(e) => setNotes(e.target.value)}
              className="w-full mt-1 p-3 border rounded text-sm min-h-[96px]"
              placeholder="Optional notes"
            />
          </div>
        </div>

        <div className="mt-6 flex items-center justify-end gap-3">
          <Button
            variant="ghost"
            onClick={onClose}
            disabled={loading || listsLoading}
          >
            Cancel
          </Button>
          <Button onClick={handleCreate} disabled={loading || listsLoading}>
            {loading ? "Booking..." : "Create appointment"}
          </Button>
        </div>
      </div>
    </div>
  );
}
