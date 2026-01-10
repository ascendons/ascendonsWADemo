// components/Appointments/AppointmentCard.tsx
import { Clock, User, Phone } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import { Badge } from "@/components/ui/badge";
import clsx from "clsx";

type KnownStatus =
  | "confirmed"
  | "waitlisted"
  | "cancelled"
  | "completed"
  | "unknown";

interface AppointmentCardProps {
  id?: string; // internal id (optional)
  bookingId?: string; // booking id from API (e.g. BID-...)
  patientId?: string | null; // patient id from API
  patientName?: string | null; // optional resolved name (preferred)
  patientPhone?: string | null; // may be null in api response
  time: string; // "11:30" or "09:00"
  status?: string | null; // "CONFIRMED", "waitlisted", etc
  doctorId?: string | null;
  provider?: string | null; // optional friendly provider name
  onClick?: () => void;
}

/* Visual config for statuses */
const statusConfig: Record<
  KnownStatus,
  { label: string; bg: string; badge: string }
> = {
  confirmed: {
    label: "Confirmed",
    bg: "bg-emerald-100 border-emerald-100",
    badge: "bg-emerald-200 text-emerald-800",
  },
  waitlisted: {
    label: "waitlisted",
    bg: "bg-amber-100 border-amber-100",
    badge: "bg-amber-200 text-amber-800",
  },
  cancelled: {
    label: "Cancelled",
    bg: "bg-rose-100 border-rose-100",
    badge: "bg-rose-200 text-rose-800",
  },
  completed: {
    label: "Completed",
    bg: "bg-blue-50 border-blue-100",
    badge: "bg-blue-200 text-blue-800",
  },
  walkin: {
    label: "Walk-in",
    bg: "bg-amber-100 border-amber-500",
    badge: "bg-amber-200 text-amber-700",
  },
  unknown: {
    label: "Unknown",
    bg: "bg-gray-50 border-gray-100",
    badge: "bg-gray-200 text-gray-800",
  },
};

/* Normalize status strings from API (safe) */
function normalizeStatus(s?: string | null): KnownStatus {
  if (!s) return "unknown";
  const v = s.toString().trim().toLowerCase();
  if (v === "booked" || v === "confirmed") return "confirmed";
  if (v === "waitlisted" || v === "pending" || v === "in_progress")
    return "waitlisted";
  if (v === "cancelled" || v === "canceled") return "cancelled";
  if (v === "completed" || v === "done") return "completed";
  if (v === "walkin" || v === "walk-in" || v === "walk_in") return "walkin";
  return Object.prototype.hasOwnProperty.call(statusConfig, v)
    ? (v as KnownStatus)
    : "unknown";
}

/* Turn "11:30" or "09:00" into locale 12-hour string "11:30 AM" */
function formatTimeDisplay(t: string) {
  if (!t) return t;
  // accept "HH:mm" or "HH:mm:ss"
  const safe = t.trim();
  // try to build a Date using today's date + time
  try {
    const iso = `${new Date().toISOString().slice(0, 10)}T${safe}`;
    const dt = new Date(iso);
    if (!isNaN(dt.getTime())) {
      return dt.toLocaleTimeString([], { hour: "2-digit", minute: "2-digit" });
    }
  } catch {
    // fallthrough
  }
  return safe;
}

export function AppointmentCard(props: AppointmentCardProps) {
  const {
    bookingId,
    patientId,
    patientName,
    patientPhone,
    time,
    status,
    doctorId,
    provider,
    onClick,
  } = props;

  const key = normalizeStatus(status);
  const { label, bg, badge } = statusConfig[key];

  const displayName = patientName ?? patientId ?? bookingId ?? "Unnamed";
  const displayProvider = provider ?? doctorId ?? "Provider";
  const timeLabel = formatTimeDisplay(time);

  return (
    <Card
      onClick={onClick}
      role={onClick ? "button" : undefined}
      tabIndex={onClick ? 0 : undefined}
      className={clsx(
        "cursor-pointer transition-all hover:shadow-md hover:scale-[1.01] border rounded-xl",
        bg,
      )}
      onKeyDown={(e) => {
        if (!onClick) return;
        if (e.key === "Enter" || e.key === " ") {
          e.preventDefault();
          onClick();
        }
      }}
    >
      <CardContent className="flex items-center gap-4 p-4">
        <div className="flex-1 space-y-2 min-w-0">
          <div className="flex items-center justify-between">
            <h4 className="font-semibold text-foreground truncate">
              {displayName}
            </h4>
            <Badge
              className={clsx(
                "text-xs font-medium px-2 py-0.5 rounded-full",
                badge,
              )}
            >
              {label}
            </Badge>
          </div>

          <div className="flex flex-wrap items-center gap-x-4 gap-y-1 text-sm text-muted-foreground">
            <div className="flex items-center gap-1.5">
              <Clock className="h-3.5 w-3.5" />
              <span>{timeLabel}</span>
            </div>

            <div className="flex items-center gap-1.5 min-w-0">
              <User className="h-3.5 w-3.5" />
              <span className="truncate">{displayProvider}</span>
            </div>

            {patientPhone ? (
              <div className="flex items-center gap-1.5">
                <Phone className="h-3.5 w-3.5" />
                <span>{patientPhone}</span>
              </div>
            ) : null}
          </div>
        </div>
      </CardContent>
    </Card>
  );
}

export default AppointmentCard;
