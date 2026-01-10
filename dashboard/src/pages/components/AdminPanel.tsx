import React from "react";
import {
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardDescription,
} from "@/components/ui/card";
import NewUserForm from "./NewUserForm";
import UsersGrid from "./UsersGrid";
import LocationsPanel from "./LocationsPanel";
import type { Location as LocationType } from "@/api/locationService";
import type { UserSummary } from "@/api/userService";

type Props = {
  onUserSubmit?: (e: React.FormEvent) => Promise<void>;

  isLoading: boolean;
  doctors: UserSummary[];
  receptionists: UserSummary[];

  openEditLocation: (loc: LocationType) => void;
  confirmDeleteLocation: (id: string) => void;

  openEditDoctor: (d: UserSummary) => void;
  openEditReceptionist: (r: UserSummary) => void;
  confirmDeleteUser: (id: string) => void;
  refreshSignal: number;
};

export default function AdminPanel(props: Props) {
  const handleCreated = () => {
    if (props.onUserSubmit) {
      const evt = new Event("submit") as unknown as React.FormEvent;
      void props.onUserSubmit(evt);
    }
  };

  return (
    <section className="space-y-6">
      <Card>
        <CardHeader>
          <CardTitle>Create New User</CardTitle>
          <CardDescription>Create a new user account</CardDescription>
        </CardHeader>
        <CardContent>
          <NewUserForm
            onCreated={handleCreated}
            refreshSignal={props.refreshSignal}
          />
        </CardContent>
      </Card>

      <UsersGrid
        confirmDeleteUser={props.confirmDeleteUser}
        refreshSignal={props.refreshSignal}
      />

      <LocationsPanel
        isLoading={props.isLoading}
        openEditLocation={props.openEditLocation}
        confirmDeleteLocation={props.confirmDeleteLocation}
        refreshSignal={props.refreshSignal}
      />
    </section>
  );
}
