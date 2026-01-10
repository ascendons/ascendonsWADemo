import { useEffect, useCallback, useState } from "react";
import { Card, CardHeader, CardTitle, CardContent } from "@/components/ui/card";
import { Button } from "@/components/ui/button";
import { updatePassword, type UserSummary } from "@/api/userService";
import { toast } from "sonner";

type Props = {
  user: UserSummary | null;
  role?: string | null;
  onClose: () => void;
  saving?: boolean;
};

export default function EditUserModal({
  user,
  role,
  onClose,
  saving = false,
}: Props) {
  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [error, setError] = useState<string | null>(null);

  useEffect(() => {
    if (user) {
      setName(user.name || "");
      setEmail(user.email || "");
      setPassword("");
      setError(null);
    } else {
      setName("");
      setEmail("");
      setPassword("");
      setError(null);
    }
  }, [user]);

  const onKey = useCallback(
    (e: KeyboardEvent) => {
      if (e.key === "Escape") onClose();
    },
    [onClose],
  );

  useEffect(() => {
    if (user) document.addEventListener("keydown", onKey);
    return () => document.removeEventListener("keydown", onKey);
  }, [user, onKey]);

  if (!user) return null;

  const validate = () => {
    if (!name.trim()) return (setError("Name is required."), false);
    if (!email.trim()) return (setError("Email is required."), false);
    if (!/^\S+@\S+\.\S+$/.test(email))
      return (setError("Invalid email format."), false);
    setError(null);
    return true;
  };

  const handleSave = async () => {
    if (!user?.id) return;
    if (!validate()) return;
    try {
      await updatePassword(user.email, "", password, true);
      toast.success("Password updated");
      onClose();
    } catch (err) {
      setError(err instanceof Error ? err.message : "Failed to save");
    }
  };

  const title = role ? `Edit ${role}` : "Edit user";

  return (
    <div
      role="dialog"
      aria-modal="true"
      className="fixed inset-0 z-50 flex items-center justify-center p-4"
    >
      {/* backdrop */}
      <div
        className="absolute inset-0 bg-black/40"
        onClick={() => !saving && onClose()}
        aria-hidden
      />

      <Card className="relative z-10 w-full max-w-md mx-4">
        <CardHeader>
          <CardTitle>{title}</CardTitle>
        </CardHeader>

        <CardContent>
          <div className="space-y-4">
            <div>
              <label className="block text-sm font-medium mb-1">Name</label>
              <input
                value={name}
                onChange={(e) => setName(e.target.value)}
                className="w-full rounded-md border px-3 py-2"
                placeholder="Full name"
                aria-label="Name"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">Email</label>
              <input
                type="email"
                value={email}
                onChange={(e) => setEmail(e.target.value)}
                className="w-full rounded-md border px-3 py-2"
                placeholder="email@example.com"
                aria-label="Email"
              />
            </div>

            <div>
              <label className="block text-sm font-medium mb-1">Password</label>
              <input
                type="password"
                value={password}
                onChange={(e) => setPassword(e.target.value)}
                className="w-full rounded-md border px-3 py-2"
                placeholder="Leave blank to keep current password"
                aria-label="Password"
              />
            </div>

            {error && (
              <p className="text-sm text-red-600" role="alert">
                {error}
              </p>
            )}

            <div className="flex justify-end gap-2">
              <Button variant="ghost" onClick={onClose} disabled={saving}>
                Cancel
              </Button>
              <Button onClick={handleSave} disabled={saving}>
                {saving ? "Saving..." : "Save"}
              </Button>
            </div>
          </div>
        </CardContent>
      </Card>
    </div>
  );
}
