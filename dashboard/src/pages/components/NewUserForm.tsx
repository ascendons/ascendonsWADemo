import React, { FormEvent, useEffect, useState } from "react";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import {
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
} from "@/components/ui/select";
import { Checkbox } from "@/components/ui/checkbox";
import { Button } from "@/components/ui/button";
import { createUser, clearUserCaches } from "@/api/userService";
import { fetchAllLocations, Location } from "@/api/locationService";
import { useToast } from "@/hooks/use-toast";

type Props = {
  onCreated?: () => void;
  refreshSignal?: number;
};

export default function NewUserForm({ onCreated, refreshSignal }: Props) {
  const { toast } = useToast();

  const [name, setName] = useState("");
  const [email, setEmail] = useState("");
  const [password, setPassword] = useState("");
  const [userRole, setUserRole] = useState<
    "ADMIN" | "DOCTOR" | "RECEPTIONIST" | ""
  >("");
  const [selectedLocations, setSelectedLocations] = useState<string[]>([]);
  const [allLocations, setAllLocations] = useState<Location[]>([]);
  const [specialization, setSpecialization] = useState<string>("ARTHRITIS");
  const [gender, setGender] = useState<string>();
  const [loadingLocations, setLoadingLocations] = useState(false);
  const [submitting, setSubmitting] = useState(false);
  const [error, setError] = useState<string | null>(null);

  React.useEffect(() => {
    let mounted = true;
    (async () => {
      setLoadingLocations(true);
      try {
        const locs = await fetchAllLocations();
        if (mounted) setAllLocations(locs ?? []);
      } catch (err: any) {
        console.error("Load locations failed", err);
        toast({
          title: "Locations load failed",
          description: err?.message ?? String(err),
          variant: "destructive",
        });
      } finally {
        if (mounted) setLoadingLocations(false);
      }
    })();
    return () => {
      mounted = false;
    };
  }, [toast]);

  useEffect(() => {
    if (refreshSignal) {
    }
  }, [refreshSignal]);

  const toggleLocation = (locId: string, checked?: boolean) => {
    setSelectedLocations((prev) =>
      typeof checked === "boolean"
        ? checked
          ? [...prev, locId]
          : prev.filter((x) => x !== locId)
        : prev.includes(locId)
          ? prev.filter((x) => x !== locId)
          : [...prev, locId],
    );
  };

  const validate = () => {
    if (!name.trim()) return "Name is required.";
    if (!email.trim()) return "Email is required.";
    if (!/^\S+@\S+\.\S+$/.test(email)) return "Invalid email address.";
    if (!password.trim()) return "Password is required.";
    if (!userRole) return "Please select a role.";
    if (userRole === "DOCTOR" && selectedLocations.length === 0)
      return "Select at least one location for a doctor.";
    return null;
  };

  const handleSubmit = async (e: FormEvent) => {
    e.preventDefault();
    setError(null);

    const v = validate();
    if (v) {
      setError(v);
      return;
    }

    setSubmitting(true);
    try {
      const payload: any = {
        name: name.trim(),
        email: email.trim(),
        password,
        role: userRole,
      };
      if (userRole === "DOCTOR") {
        ((payload.gender = gender),
          (payload.specialization = specialization),
          (payload.locationIds = selectedLocations));
      }
      await createUser(payload);

      clearUserCaches();

      toast({ title: "Created", description: `${name} created.` });

      setName("");
      setEmail("");
      setPassword("");
      setUserRole("");
      setSelectedLocations([]);
      setSpecialization("");
      setGender("");
      setError(null);

      onCreated?.();
    } catch (err: any) {
      console.error("Create user failed", err);
      const msg =
        err?.response?.data?.message ?? err?.message ?? "Failed to create user";
      setError(msg);
      toast({
        title: "Create failed",
        description: msg,
        variant: "destructive",
      });
    } finally {
      setSubmitting(false);
    }
  };

  const disabled = submitting;

  return (
    <form onSubmit={handleSubmit} className="space-y-6">
      <div className="space-y-2">
        <Label htmlFor="new-name">Name</Label>
        <Input
          id="new-name"
          value={name}
          onChange={(e) => setName(e.target.value)}
          placeholder="e.g., Dr. John Doe"
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="new-email">Email</Label>
        <Input
          id="new-email"
          value={email}
          onChange={(e) => setEmail(e.target.value)}
          placeholder="user@example.com"
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="new-password">Password</Label>
        <Input
          id="new-password"
          type="password"
          value={password}
          onChange={(e) => setPassword(e.target.value)}
          placeholder="Strong password"
        />
      </div>

      <div className="space-y-2">
        <Label htmlFor="new-role">Role</Label>
        <Select value={userRole} onValueChange={(v) => setUserRole(v as any)}>
          <SelectTrigger id="new-role">
            <SelectValue placeholder="Select a role" />
          </SelectTrigger>
          <SelectContent>
            <SelectItem value="ADMIN">Admin</SelectItem>
            <SelectItem value="DOCTOR">Doctor</SelectItem>
            <SelectItem value="RECEPTIONIST">Receptionist</SelectItem>
          </SelectContent>
        </Select>
      </div>

      {userRole === "DOCTOR" && (
        <>
          <div className="">
            <Label className="font-semibold">Specialization</Label>
            <Select
              value={specialization}
              onValueChange={(v) => setSpecialization(v as any)}
            >
              <SelectTrigger id="new-role">
                <SelectValue placeholder="Select specialization" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="ARTHRITIS">Arthritis</SelectItem>
                <SelectItem value="NEUROLOGY">Neurology</SelectItem>
                <SelectItem value="ENT">Ear, Nose, and Throat (ENT)</SelectItem>
                <SelectItem value="DENTISTRY">Dentistry</SelectItem>
                <SelectItem value="PULMONOLOGY">Pulmonology</SelectItem>
                <SelectItem value="GASTROENTEROLOGY">
                  Gastroenterology
                </SelectItem>
                <SelectItem value="NEUROLOGY">Neurology</SelectItem>
                <SelectItem value="ORTHOPEDICS">Orthopedics</SelectItem>
                <SelectItem value="CARDIOLOGY">Cardiology</SelectItem>
                <SelectItem value="DERMATOLOGY">Dermatology</SelectItem>
                <SelectItem value="PEDIATRICS">Pediatrics</SelectItem>
                <SelectItem value="GENERAL">General Medicine</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="">
            <Label className="font-semibold">Gender</Label>
            <Select value={gender} onValueChange={(v) => setGender(v as any)}>
              <SelectTrigger id="new-role">
                <SelectValue placeholder="Select Gender" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value="MALE">Male</SelectItem>
                <SelectItem value="FEMALE">Female</SelectItem>
                <SelectItem value="OTHER">Other</SelectItem>
              </SelectContent>
            </Select>
          </div>
          <div className="space-y-4 rounded-md border p-4">
            <Label className="font-semibold">Assign locations</Label>
            {loadingLocations ? (
              <div className="text-sm text-muted-foreground">
                Loading locations...
              </div>
            ) : allLocations.length === 0 ? (
              <div className="text-sm text-muted-foreground">
                No locations available
              </div>
            ) : (
              <div className="grid grid-cols-2 gap-2">
                {allLocations.map((loc) => (
                  <label
                    key={loc.id}
                    className="inline-flex items-center gap-2"
                  >
                    <Checkbox
                      id={`loc-${loc.id}`}
                      checked={selectedLocations.includes(loc.id)}
                      onCheckedChange={(ch) =>
                        toggleLocation(loc.id, ch as boolean)
                      }
                    />
                    <span className="text-sm cursor-pointer">{loc.name}</span>
                  </label>
                ))}
              </div>
            )}
          </div>
        </>
      )}

      {error && <div className="text-sm text-red-600">{error}</div>}

      <div className="flex gap-2">
        <Button type="submit" disabled={disabled}>
          {submitting ? "Creating..." : "Create User"}
        </Button>
      </div>
    </form>
  );
}
