import React, { useEffect, useState } from "react";
import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import { useAuth } from "@/context/AuthContext";
import { useToast } from "@/hooks/use-toast";

import {
  clearLocationsCache,
  deleteLocation,
  fetchAllLocations,
  updateLocation,
  Location as LocationType,
} from "@/api/locationService";
import {
  clearUserCaches,
  deleteUser,
  fetchUsersByRole,
  UserSummary,
} from "@/api/userService";
import AdminPanel from "./components/AdminPanel";
import DeleteConfirm from "./components/DeleteConfirm";
import EditLocationModal from "./components/EditLocationModal";

import DoctorAvailability from "./components/DoctorAvailability";

export default function Settings() {
  const { role, user } = useAuth();
  const isAdmin = role === "ADMIN";
  const isDoctor = role === "DOCTOR";
  const { toast } = useToast();

  const [allLocations, setAllLocations] = useState<LocationType[]>([]);
  const [doctors, setDoctors] = useState<UserSummary[]>([]);
  const [receptionists, setReceptionists] = useState<UserSummary[]>([]);

  const [isLoading, setIsLoading] = useState(false);

  const [editingLocationId, setEditingLocationId] = useState<string | null>(
    null,
  );
  const [editForm, setEditForm] = useState<Partial<LocationType>>({});
  const [savingEdit, setSavingEdit] = useState(false);

  const [editingDoctor, setEditingDoctor] =
    useState<Partial<UserSummary> | null>(null);

  const [refreshSignal, setRefreshSignal] = useState<number>(0);

  const [editingReceptionist, setEditingReceptionist] =
    useState<Partial<UserSummary> | null>(null);

  const [deletingLocationId, setDeletingLocationId] = useState<string | null>(
    null,
  );
  const [deletingLocation, setDeletingLocation] = useState(false);

  const [deletingUserId, setDeletingUserId] = useState<string | null>(null);
  const [deletingUser, setDeletingUser] = useState(false);

  useEffect(() => {
    if (!isAdmin) return;
    loadAll(false);
  }, [isAdmin]);

  async function loadAll(refresh = false) {
    setIsLoading(true);
    try {
//       console.log("Loading data...", refresh);
      const [locs, docs, recs] = await Promise.all([
        fetchAllLocations(refresh),
        fetchUsersByRole("DOCTOR", refresh),
        fetchUsersByRole("RECEPTIONIST", refresh),
      ]);
      console.log("Data loaded.");
      setAllLocations(locs);
      setDoctors(docs);
      setReceptionists(recs);
      if (refresh) setRefreshSignal((s) => s + 1);
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unknown error";
      toast({
        title: "Load failed",
        description: message,
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  }

  const handleRefresh = async () => {
    console.log("Refreshing data...");
    setIsLoading(true);
    try {
      clearLocationsCache();
      clearUserCaches();
      await loadAll(true);
      toast({ title: "Refreshed", description: "Data refreshed from server." });
    } catch (err) {
      const message = err instanceof Error ? err.message : "Unknown error";
      toast({
        title: "Refresh failed",
        description: message,
        variant: "destructive",
      });
    } finally {
      setIsLoading(false);
    }
  };

  const handleUserSubmit = async () => {
    setIsLoading(true);
    console.log("Submitting new user:");
    await loadAll(true);
    setIsLoading(false);
  };

  const openEditLocation = (loc: LocationType) => {
    setEditingLocationId(loc.id);
    setEditForm({ ...loc });
  };
  const closeEditLocation = () => {
    setEditingLocationId(null);
    setEditForm({});
    setSavingEdit(false);
  };
  const handleEditChange = (key: keyof LocationType, value: string) =>
    setEditForm((p) => ({ ...p, [key]: value }));

  const handleSaveEdit = async (e?: React.FormEvent) => {
    if (e) e.preventDefault();
    if (!editingLocationId) return;
    setSavingEdit(true);
    const updatedPayload: Partial<LocationType> = {
      name: (editForm.name || "").trim(),
      address: editForm.address || "",
    };
    if (!updatedPayload.name) {
      toast({
        title: "Validation",
        description: "Name is required.",
        variant: "destructive",
      });
      setSavingEdit(false);
      return;
    }
    const prev = allLocations;
    setAllLocations((prevList) =>
      prevList.map((p) =>
        p.id === editingLocationId ? { ...p, ...updatedPayload } : p,
      ),
    );
    try {
      const updated = await updateLocation(editingLocationId, updatedPayload);
      setAllLocations((prevList) =>
        prevList.map((p) => (p.id === updated.id ? updated : p)),
      );
      toast({
        title: "Saved",
        description: `Location "${updated.name}" updated.`,
      });
      closeEditLocation();
    } catch (err) {
      setAllLocations(prev);
      const message = err instanceof Error ? err.message : "Unknown error";
      toast({
        title: "Update failed",
        description: message,
        variant: "destructive",
      });
      setSavingEdit(false);
    }
  };

  const openEditDoctor = (doc: UserSummary) => setEditingDoctor({ ...doc });

  const openEditReceptionist = (r: UserSummary) =>
    setEditingReceptionist({ ...r });

  const confirmDeleteLocation = (id: string) => setDeletingLocationId(id);
  const cancelDeleteLocation = () => setDeletingLocationId(null);

  const handleDeleteLocation = async () => {
    if (!deletingLocationId) return;
    setDeletingLocation(true);
    const prev = allLocations;
    setAllLocations((p) => p.filter((l) => l.id !== deletingLocationId));
    try {
      await deleteLocation(deletingLocationId);
      toast({ title: "Deleted", description: "Location deleted." });
      setDeletingLocationId(null);
    } catch (err) {
      setAllLocations(prev);
      const message = err instanceof Error ? err.message : "Unknown error";
      toast({
        title: "Delete failed",
        description: message,
        variant: "destructive",
      });
    } finally {
      setDeletingLocation(false);
    }
  };

  const confirmDeleteUser = (id: string) => setDeletingUserId(id);
  const cancelDeleteUser = () => setDeletingUserId(null);

  const handleDeleteUser = async () => {
    if (!deletingUserId) return;
    setDeletingUser(true);
    const prevDocs = doctors;
    const prevRecs = receptionists;
    setDoctors((d) => d.filter((x) => x.id !== deletingUserId));
    setReceptionists((r) => r.filter((x) => x.id !== deletingUserId));
    try {
      await deleteUser(deletingUserId);
      toast({ title: "Deleted", description: "User deleted." });
      setDeletingUserId(null);
    } catch (err) {
      setDoctors(prevDocs);
      setReceptionists(prevRecs);
      const message = err instanceof Error ? err.message : "Unknown error";
      toast({
        title: "Delete failed",
        description: message,
        variant: "destructive",
      });
    } finally {
      setDeletingUser(false);
    }
  };

  return (
    <div className="space-y-6 animate-fade-in">
      <div className="flex flex-col sm:flex-row items-start sm:items-center justify-between gap-4">
        <div>
          <h1 className="text-2xl sm:text-3xl font-bold text-foreground">
            Settings
          </h1>
        </div>

        <div className="flex items-center gap-2">
          <button
            onClick={handleRefresh}
            className="rounded-md border px-3 py-2 text-sm bg-white hover:bg-muted/50"
            disabled={isLoading}
          >
            {isLoading ? "Refreshing..." : "Refresh"}
          </button>
        </div>
      </div>

      {isAdmin && (
        <AdminPanel
          onUserSubmit={handleUserSubmit}
          isLoading={isLoading}
          doctors={doctors}
          receptionists={receptionists}
          openEditLocation={openEditLocation}
          confirmDeleteLocation={confirmDeleteLocation}
          openEditDoctor={openEditDoctor}
          openEditReceptionist={openEditReceptionist}
          confirmDeleteUser={confirmDeleteUser}
          refreshSignal={refreshSignal}
        />
      )}

      {isDoctor && (
        <div className="mt-6">
          <Card>
            <CardHeader>
              <CardTitle>{user.name}</CardTitle>
            </CardHeader>
            <CardContent>
              <DoctorAvailability doctorId={user.id} />
            </CardContent>
          </Card>
        </div>
      )}

      <EditLocationModal
        editingId={editingLocationId}
        editForm={editForm}
        handleEditChange={handleEditChange}
        handleSaveEdit={handleSaveEdit}
        closeEdit={closeEditLocation}
        savingEdit={savingEdit}
      />

      <DeleteConfirm
        deletingId={deletingLocationId}
        cancelDelete={cancelDeleteLocation}
        handleDelete={handleDeleteLocation}
        deleting={deletingLocation}
      />
      <DeleteConfirm
        deletingId={deletingUserId}
        cancelDelete={cancelDeleteUser}
        handleDelete={handleDeleteUser}
        deleting={deletingUser}
      />
    </div>
  );
}
