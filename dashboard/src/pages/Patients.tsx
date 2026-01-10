import { useEffect, useState } from "react";
import { Card, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { User, Phone, Mail, Plus, X, Trash, Edit } from "lucide-react";
import { Badge } from "@/components/ui/badge";
import { useAuth } from "@/context/AuthContext"; // optional: remove if you don't use auth
import { useToast } from "@/hooks/use-toast"; // optional: remove if not available
import {
  fetchAllPatients,
  createPatient,
  updatePatientDetails,
  deletePatient,
  PatientDetails,
} from "@/api/patientService";

type Patient = {
  id: string;
  name: string;
  gender?: "MALE" | "FEMALE" | "OTHER" | "UNDISCLOSED";
  age?: number | null;
  phone?: string;
  email?: string;
  lastVisit?: string;
  upcomingVisit?: string;
  status?: "active" | "new";
  notes?: string;
};

export default function Patients() {
  const { authToken } = (useAuth && useAuth()) ?? {};
  const { toast } = (useToast && useToast()) ?? { toast: undefined };

  const [patients, setPatients] = useState<Patient[]>([]);
  const [isOpen, setIsOpen] = useState(false);

  // form state (used for both create & edit)
  const [editingId, setEditingId] = useState<string | null>(null); // null => creating
  const [name, setName] = useState("");
  const [gender, setGender] = useState<Patient["gender"]>("MALE");
  const [age, setAge] = useState<number | "">("");
  const [phone, setPhone] = useState("");
  const [email, setEmail] = useState("");
  const [notes, setNotes] = useState("");

  const [errors, setErrors] = useState<Record<string, string>>({});
  const [loading, setLoading] = useState(false);
  const [saving, setSaving] = useState(false);
  const [deleting, setDeleting] = useState(false);

  // helper: extract message from response reliably (keeps your earlier helper)
  async function extractMessage(res: Response) {
    const text = await res.text().catch(() => "");
    if (!text) return `${res.status} ${res.statusText}`;
    try {
      const json = JSON.parse(text);
      return json?.message || json?.error || text;
    } catch {
      return text;
    }
  }

  // populate list
  useEffect(() => {
    fetchPatients();
  }, []);

  async function fetchPatients() {
    setLoading(true);
    try {
      const data = await fetchAllPatients(false);
      setPatients(
        data.map((d) => ({
          id: d.id,
          name: d.name,
          gender: d.gender,
          age: d.age ?? null,
          phone: d.phone,
          email: d.email,
          notes: d.notes,
        })),
      );
    } catch (err) {
      console.error(err);
      toast?.({
        title: "Load failed",
        description: err instanceof Error ? err.message : "Unknown error",
        variant: "destructive",
      } as any);
    } finally {
      setLoading(false);
    }
  }

  function resetForm() {
    setEditingId(null);
    setName("");
    setGender("MALE");
    setAge("");
    setPhone("");
    setEmail("");
    setNotes("");
    setErrors({});
  }

  function openCreateDialog() {
    resetForm();
    setIsOpen(true);
  }

  function openEditDialog(p: Patient) {
    setEditingId(p.id);
    setName(p.name ?? "");
    setGender(p.gender ?? "MALE");
    setAge(p.age ?? "");
    setPhone(p.phone ?? "");
    setEmail(p.email ?? "");
    setNotes(p.notes ?? "");
    setErrors({});
    setIsOpen(true);
  }

  function closeDialog() {
    setIsOpen(false);
    resetForm();
  }

  function validate() {
    const e: Record<string, string> = {};
    if (!name.trim()) e.name = "Name is required";
    if (!phone.trim()) e.phone = "Phone is required";
    if (
      age !== "" &&
      (Number.isNaN(Number(age)) || Number(age) < 0 || Number(age) > 120)
    )
      e.age = "Age must be a valid number between 0 and 120";
    setErrors(e);
    return Object.keys(e).length === 0;
  }

  async function handleSubmit(e?: React.FormEvent) {
    e?.preventDefault();
    if (!validate()) return;
    setSaving(true);

    const payload: Partial<PatientDetails> = {
      name: name.trim(),
      gender,
      age: age === "" ? null : Number(age),
      phone: phone.trim(),
      email: email.trim() || undefined,
      notes: notes.trim() || undefined,
    };

    try {
      if (editingId) {
        // update via service
        const updated = await updatePatientDetails(editingId, payload);
        // update local state
        setPatients((prev) =>
          prev.map((p) =>
            p.id === editingId ? { ...p, ...mapDetailsToPatient(updated) } : p,
          ),
        );

        toast?.({
          title: "Patient updated",
          description: `Updated ${updated.name}`,
        } as any);
      } else {
        // create via service
        const created = await createPatient(payload);
        setPatients((prev) => [mapDetailsToPatient(created), ...prev]);

        toast?.({
          title: "Patient created",
          description: `Created ${created.name}`,
        } as any);
      }

      closeDialog();
    } catch (err: any) {
      console.error(err);
      toast?.({
        title: "Save failed",
        description: err instanceof Error ? err.message : String(err),
        variant: "destructive",
      } as any);
    } finally {
      setSaving(false);
    }
  }

  // Delete patient
  async function handleDelete(id: string) {
    const ok = window.confirm("Are you sure you want to delete this patient?");
    if (!ok) return;

    setDeleting(true);
    // optimistic removal
    const prev = patients;
    setPatients((p) => p.filter((x) => x.id !== id));

    try {
      await deletePatient(id);

      toast?.({
        title: "Deleted",
        description: "Patient deleted",
      } as any);

      // if we deleted the currently editing patient, close dialog
      if (editingId === id) closeDialog();
    } catch (err: any) {
      console.error(err);
      // rollback
      setPatients(prev);
      toast?.({
        title: "Delete failed",
        description: err instanceof Error ? err.message : String(err),
        variant: "destructive",
      } as any);
    } finally {
      setDeleting(false);
    }
  }

  function mapDetailsToPatient(d: PatientDetails): Patient {
    return {
      id: d.id,
      name: d.name,
      gender: d.gender,
      age: d.age ?? null,
      phone: d.phone,
      email: d.email,
      notes: d.notes,
    };
  }

  return (
    <div className="space-y-6 animate-fade-in">
      {/* Header */}
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-3xl font-bold text-foreground">Patients</h1>
          <p className="text-muted-foreground">Patient records and profiles</p>
        </div>

        <div className="flex items-center gap-2">
          <Button
            className="bg-accent hover:bg-accent-hover"
            onClick={openCreateDialog}
          >
            <Plus className="mr-2 h-4 w-4" />
            New Patient
          </Button>
        </div>
      </div>

      {/* Patient List */}
      <div className="grid gap-4 md:grid-cols-2">
        {loading && (
          <div className="p-4 text-sm text-muted-foreground">
            Loading patients...
          </div>
        )}

        {!loading &&
          patients.map((patient) => (
            <Card
              key={patient.id}
              className="cursor-pointer transition-all hover:shadow-md hover:scale-[1.02]"
            >
              <CardContent className="p-6">
                <div className="flex items-start justify-between">
                  <div className="flex items-start gap-4">
                    <div className="flex h-12 w-12 items-center justify-center rounded-full bg-primary-light">
                      <User className="h-6 w-6 text-primary" />
                    </div>

                    <div className="space-y-3">
                      <div>
                        <div className="flex items-center gap-2">
                          <h3 className="font-semibold text-foreground">
                            {patient.name}
                          </h3>
                          {patient.status === "new" && (
                            <Badge className="bg-accent text-accent-foreground">
                              New
                            </Badge>
                          )}
                        </div>

                        <div className="mt-2 space-y-1 text-sm text-muted-foreground">
                          <div className="flex items-center gap-2">
                            <Phone className="h-3.5 w-3.5" />
                            <span>{patient.phone}</span>
                          </div>
                          {patient.email && (
                            <div className="flex items-center gap-2">
                              <Mail className="h-3.5 w-3.5" />
                              <span>{patient.email}</span>
                            </div>
                          )}
                        </div>
                      </div>

                      {patient.notes && (
                        <p className="text-sm text-muted-foreground mt-2">
                          Notes: {patient.notes}
                        </p>
                      )}
                    </div>
                  </div>

                  {/* small action buttons visible on card */}
                  <div className="flex flex-col items-end gap-2">
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        openEditDialog(patient);
                      }}
                    >
                      <Edit className="h-4 w-4" />
                    </Button>
                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={(e) => {
                        e.stopPropagation();
                        handleDelete(patient.id);
                      }}
                    >
                      <Trash className="h-4 w-4 text-rose-500" />
                    </Button>
                  </div>
                </div>
              </CardContent>
            </Card>
          ))}
      </div>

      {/* Modal / Dialog for Create & Edit */}
      {isOpen && (
        <div
          role="dialog"
          aria-modal="true"
          className="fixed inset-0 z-50 flex items-center justify-center p-4"
        >
          {/* overlay */}
          <div
            className="absolute inset-0 bg-black/40 backdrop-blur-sm"
            onClick={closeDialog}
            aria-hidden
          />

          <form
            onSubmit={handleSubmit}
            className="relative z-10 w-full max-w-xl rounded-2xl bg-popover p-6 shadow-lg"
          >
            <div className="flex items-center justify-between">
              <h2 className="text-xl font-semibold">
                {editingId ? "Edit Patient" : "New Patient"}
              </h2>
              <button
                type="button"
                aria-label="Close"
                onClick={closeDialog}
                className="rounded p-1 text-muted-foreground hover:bg-muted/50"
              >
                <X className="h-4 w-4" />
              </button>
            </div>

            <div className="mt-4 grid grid-cols-1 gap-3 sm:grid-cols-2">
              {/* Name */}
              <div>
                <label className="text-sm text-muted-foreground">Name *</label>
                <input
                  value={name}
                  onChange={(e) => setName(e.target.value)}
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm"
                  placeholder="Full name"
                />
                {errors.name && (
                  <p className="mt-1 text-xs text-rose-500">{errors.name}</p>
                )}
              </div>

              {/* Gender */}
              <div>
                <label className="text-sm text-muted-foreground">Gender</label>
                <select
                  value={gender}
                  onChange={(e) =>
                    setGender(e.target.value as Patient["gender"])
                  }
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm"
                >
                  <option>MALE</option>
                  <option>FEMALE</option>
                  <option>OTHER</option>
                  <option>UNDISCLOSED</option>
                </select>
              </div>

              {/* Age */}
              <div>
                <label className="text-sm text-muted-foreground">Age</label>
                <input
                  value={age === "" ? "" : String(age)}
                  onChange={(e) => {
                    const v = e.target.value;
                    if (v === "") setAge("");
                    else if (/^\d{1,3}$/.test(v)) setAge(Number(v));
                  }}
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm"
                  placeholder="e.g. 34"
                />
                {errors.age && (
                  <p className="mt-1 text-xs text-rose-500">{errors.age}</p>
                )}
              </div>

              {/* Phone */}
              <div>
                <label className="text-sm text-muted-foreground">Phone *</label>
                <input
                  value={phone}
                  onChange={(e) => setPhone(e.target.value)}
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm"
                  placeholder="+91 98xxxx"
                />
                {errors.phone && (
                  <p className="mt-1 text-xs text-rose-500">{errors.phone}</p>
                )}
              </div>

              {/* Email */}
              <div className="sm:col-span-2">
                <label className="text-sm text-muted-foreground">Email</label>
                <input
                  value={email}
                  onChange={(e) => setEmail(e.target.value)}
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm"
                  placeholder="email@example.com"
                />
              </div>

              {/* Notes */}
              <div className="sm:col-span-2">
                <label className="text-sm text-muted-foreground">Notes</label>
                <textarea
                  value={notes}
                  onChange={(e) => setNotes(e.target.value)}
                  className="mt-1 w-full rounded-md border px-3 py-2 text-sm"
                  rows={4}
                  placeholder="Any medical notes / remarks"
                />
              </div>
            </div>

            <div className="mt-6 flex items-center justify-end gap-2">
              <Button variant="ghost" type="button" onClick={closeDialog}>
                Cancel
              </Button>

              {editingId && (
                <Button
                  variant="destructive"
                  type="button"
                  onClick={async () => {
                    if (!editingId) return;
                    await handleDelete(editingId);
                  }}
                  disabled={deleting}
                >
                  {deleting ? "Deleting..." : "Delete"}
                </Button>
              )}

              <Button type="submit" disabled={saving}>
                {saving
                  ? editingId
                    ? "Saving..."
                    : "Creating..."
                  : editingId
                    ? "Save"
                    : "Create Patient"}
              </Button>
            </div>
          </form>
        </div>
      )}
    </div>
  );
}
