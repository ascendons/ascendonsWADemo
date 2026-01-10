import { Card, CardContent, CardHeader, CardTitle } from "@/components/ui/card";
import LocationsCard from "../components/LocationsCard";
import {
  fetchAllLocations,
  type Location as LocationType,
  createLocation,
} from "@/api/locationService";
import React, { useEffect, useState } from "react";
import { Button } from "@/components/ui/button";

type Props = {
  isLoading: boolean;
  openEditLocation: (loc: LocationType) => void;
  confirmDeleteLocation: (id: string) => void;
  refreshSignal?: number;
  onAddLocation?: (loc: { name: string; address: string }) => Promise<LocationType | void> | LocationType | void;
};

export default function LocationsPanel({
  isLoading,
  openEditLocation,
  confirmDeleteLocation,
  refreshSignal,
  onAddLocation,
}: Props) {
  const [allLocations, setAllLocations] = useState<LocationType[]>([]);
  const [loading, setLoading] = useState(false);

  const [showAdd, setShowAdd] = useState(false);
  const [name, setName] = useState("");
  const [address, setAddress] = useState("");
  const [submitting, setSubmitting] = useState(false);

  useEffect(() => {
    let mounted = true;
    setLoading(true);
    fetchAllLocations(!!refreshSignal)
      .then((locs) => {
        if (mounted) setAllLocations(locs ?? []);
      })
      .catch(() => {
        if (mounted) setAllLocations([]);
      })
      .finally(() => {
        if (mounted) setLoading(false);
      });
    return () => {
      mounted = false;
    };
  }, [refreshSignal]);

  const openModal = () => {
    setName("");
    setAddress("");
    setShowAdd(true);
  };

  const closeModal = () => {
    if (submitting) return;
    setShowAdd(false);
  };

  const handleSubmit = async (e?: React.FormEvent) => {
    e?.preventDefault();
    setSubmitting(true);
    try {
      const payload = { name, address };
      const created = await createLocation(payload);
      setShowAdd(false);
      setName("");
      setAddress("");
    } catch (err) {
      console.error("Failed to add location", err);
    } finally {
      setSubmitting(false);
    }
  };

  return (
    <>
      <Card>
        <CardHeader className="flex items-center">
          <CardTitle className="p-2">Locations</CardTitle>
          <div className="ml-auto">
            <Button size="sm" onClick={openModal}>
              + Add location
            </Button>
          </div>
        </CardHeader>

        <CardContent>
          <LocationsCard
            allLocations={allLocations}
            isLoading={isLoading || loading}
            openEdit={openEditLocation}
            confirmDelete={confirmDeleteLocation}
          />
        </CardContent>
      </Card>

      {showAdd && (
        <div role="dialog" aria-modal="true" className="fixed inset-0 z-50 flex items-center justify-center px-4">
          <div className="fixed inset-0 bg-black/40" onClick={closeModal} />

          <form
            onSubmit={handleSubmit}
            className="relative z-10 w-full max-w-md rounded-lg bg-white p-6 shadow-lg dark:bg-slate-800"
          >
            <h3 className="mb-4 text-lg font-semibold">Add Location</h3>

            <label className="mb-2 block text-sm">Name</label>
            <input
              className="mb-3 w-full rounded border px-3 py-2 text-sm"
              value={name}
              onChange={(e) => setName(e.target.value)}
              placeholder="Location name"
              autoFocus
            />

            <label className="mb-2 block text-sm">Address</label>
            <textarea
              className="mb-3 h-24 w-full rounded border px-3 py-2 text-sm"
              value={address}
              onChange={(e) => setAddress(e.target.value)}
              placeholder="Full address"
            />

            <div className="flex gap-2">
              <Button type="submit" disabled={submitting}>
                {submitting ? "Adding…" : "Add"}
              </Button>
              <Button variant="ghost" type="button" onClick={closeModal} disabled={submitting}>
                Cancel
              </Button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
