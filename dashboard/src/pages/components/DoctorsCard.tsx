import { useEffect, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { fetchUsersByRole, UserSummary } from "@/api/userService";
import { Edit3, Settings, Trash2 } from "lucide-react";
import EditUserModal from "./EditUserModal";
import DoctorAvailability, {
  DoctorAvailabilityPayload,
} from "./DoctorAvailability";

export interface Doctor extends UserSummary {
  locationNames?: string[];
}

export default function DoctorsCard({
  confirmDelete,
  refreshSignal,
}: {
  confirmDelete: (id: string) => void;
  refreshSignal?: number;
}) {
  const { toast } = useToast();

  const [doctors, setDoctors] = useState<Doctor[]>([]);
  const [loading, setLoading] = useState(false);

  const [editing, setEditing] = useState<Doctor | null>(null);
  const [saving, setSaving] = useState(false);

  const [settingsDoctor, setSettingsDoctor] = useState<Doctor | null>(null);
  const [savingAvailability, setSavingAvailability] = useState(false);
  const [availabilityInitial, setAvailabilityInitial] = useState<
    Partial<DoctorAvailabilityPayload> | undefined
  >(undefined);

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      setLoading(true);
      try {
        const data = await fetchUsersByRole("DOCTOR");
        if (mounted) setDoctors(data as Doctor[]);
      } catch (err) {
        console.error("load doctors error", err);
        toast({
          title: "Error",
          description:
            err instanceof Error ? err.message : "Failed to load doctors",
          variant: "destructive",
        });
      } finally {
        if (mounted) setLoading(false);
      }
    };

    load();
    return () => {
      mounted = false;
    };
  }, [toast, refreshSignal]);

  const openEdit = (d: Doctor) => setEditing(d);
  const closeEdit = () => setEditing(null);

  const openSettingsModal = (doc: Doctor) => {
    setAvailabilityInitial(undefined);
    setSettingsDoctor(doc);
  };

  const closeSettingsModal = () => {
    setSettingsDoctor(null);
    setSavingAvailability(false);
    setAvailabilityInitial(undefined);
  };

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>Doctors</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="text-sm text-muted-foreground">Loading...</div>
          ) : doctors.length === 0 ? (
            <div className="text-sm text-muted-foreground">
              No doctors found.
            </div>
          ) : (
            <div className="space-y-2">
              {doctors.map((doc) => (
                <div
                  key={doc.id}
                  className="flex items-center justify-between border rounded-md p-2"
                >
                  <div>
                    <p className="font-medium">{doc.name}</p>
                    <p className="text-sm text-muted-foreground">{doc.email}</p>
                    {doc.locationNames && (
                      <p className="text-xs text-muted-foreground">
                        {doc.locationNames.join(", ")}
                      </p>
                    )}
                  </div>

                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => openEdit(doc)}
                    >
                      <Edit3 className="h-4 w-4" />
                    </Button>

                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => openSettingsModal(doc)}
                    >
                      <Settings className="h-4 w-4" />
                    </Button>

                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => confirmDelete(doc.id)}
                    >
                      <Trash2 className="h-4 w-4" />
                    </Button>
                  </div>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      {/* Edit user modal (doctor) */}
      <EditUserModal
        user={editing}
        role="Doctor"
        onClose={closeEdit}
        saving={saving}
      />

      {/* Settings modal: DoctorAvailability */}
      {settingsDoctor && (
        <div className="fixed inset-0 z-50 flex items-center justify-center p-4">
          <div
            className="absolute inset-0 bg-black/40"
            onClick={() => !savingAvailability && closeSettingsModal()}
          />
          <div className="relative z-10 w-full max-w-3xl bg-white rounded-md shadow-lg overflow-hidden">
            <div className="flex items-center justify-between border-b px-4 py-3">
              <div>
                <div className="text-lg font-semibold">
                  Availability — {settingsDoctor.name}
                </div>
                <div className="text-sm text-muted-foreground">
                  {settingsDoctor.email}
                </div>
              </div>
            </div>

            <div className="p-4">
              <DoctorAvailability doctorId={settingsDoctor.id} />
            </div>
          </div>
        </div>
      )}
    </>
  );
}
