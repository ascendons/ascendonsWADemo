import { useEffect, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { useToast } from "@/hooks/use-toast";
import { fetchUsersByRole, updateUser, UserSummary } from "@/api/userService";
import { Edit3, Trash2 } from "lucide-react";
import EditUserModal from "./EditUserModal";

export interface Receptionist extends UserSummary {}

export default function ReceptionistsCard({
  confirmDelete,
  refreshSignal,
}: {
  confirmDelete: (id: string) => void;
  refreshSignal?: number;
}) {
  const { toast } = useToast();

  const [receptionists, setReceptionists] = useState<Receptionist[]>([]);
  const [loading, setLoading] = useState(false);

  const [editing, setEditing] = useState<Receptionist | null>(null);
  const [saving, setSaving] = useState(false);

  useEffect(() => {
    let mounted = true;
    const load = async () => {
      setLoading(true);
      try {
        const data = await fetchUsersByRole("RECEPTIONIST");
        if (mounted) setReceptionists(data as Receptionist[]);
      } catch (err) {
        console.error("load receptionists error", err);
        toast({
          title: "Error",
          description:
            err instanceof Error ? err.message : "Failed to load receptionists",
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

  const openEdit = (r: Receptionist) => setEditing(r);
  const closeEdit = () => setEditing(null);

  const handleSave = async (payload: {
    id: string;
    name: string;
    email: string;
    password?: string;
  }) => {
    setSaving(true);
    // optimistic update
    const prev = receptionists;
    setReceptionists((list) =>
      list.map((x) =>
        x.id === payload.id
          ? { ...x, name: payload.name, email: payload.email }
          : x,
      ),
    );

    try {
      await updateUser(payload.id, {
        name: payload.name,
        email: payload.email,
        ...(payload.password ? { password: payload.password } : {}),
      });

      toast({ title: "Saved", description: "Receptionist updated." });

      try {
        const fresh = await fetchUsersByRole("RECEPTIONIST");
        setReceptionists(fresh as Receptionist[]);
      } catch {}

      closeEdit();
    } catch (err) {
      setReceptionists(prev);
      const message = err instanceof Error ? err.message : "Update failed";
      toast({
        title: "Update failed",
        description: message,
        variant: "destructive",
      });
      throw err;
    } finally {
      setSaving(false);
    }
  };

  return (
    <>
      <Card>
        <CardHeader>
          <CardTitle>Receptionists</CardTitle>
        </CardHeader>
        <CardContent>
          {loading ? (
            <div className="text-sm text-muted-foreground">Loading...</div>
          ) : receptionists.length === 0 ? (
            <div className="text-sm text-muted-foreground">
              No receptionists found.
            </div>
          ) : (
            <div className="space-y-2">
              {receptionists.map((rec) => (
                <div
                  key={rec.id}
                  className="flex items-center justify-between border rounded-md p-2"
                >
                  <div>
                    <p className="font-medium">{rec.name}</p>
                    <p className="text-sm text-muted-foreground">{rec.email}</p>
                  </div>
                  <div className="flex gap-2">
                    <Button
                      variant="outline"
                      size="sm"
                      onClick={() => openEdit(rec)}
                    >
                      <Edit3 className="h-4 w-4" />
                    </Button>

                    <Button
                      variant="ghost"
                      size="sm"
                      onClick={() => confirmDelete(rec.id)}
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
      <EditUserModal
        user={editing}
        role="RECEPTIONIST"
        onClose={closeEdit}
        saving={saving}
      />
    </>
  );
}
