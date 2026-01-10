import React from "react";
import { Button } from "@/components/ui/button";

type Props = {
  deletingId: string | null;
  cancelDelete: () => void;
  handleDelete: () => Promise<void> | void;
  deleting: boolean;
};

export default function DeleteConfirm({
  deletingId,
  cancelDelete,
  handleDelete,
  deleting,
}: Props) {
  if (!deletingId) return null;

  return (
    <div
      className="fixed inset-0 z-40 flex items-center justify-center"
      role="alertdialog"
      aria-modal="true"
    >
      <div
        className="fixed inset-0 bg-black/40"
        onClick={cancelDelete}
        aria-hidden="true"
      />
      <div className="bg-white rounded-lg shadow p-6 z-10 w-full max-w-md">
        <h4 className="text-lg font-semibold mb-2">Confirm delete</h4>
        <p className="text-sm text-muted-foreground mb-4">
          Are you sure you want to delete this location? This action cannot be
          undone.
        </p>

        <div className="flex justify-end gap-2">
          <Button onClick={cancelDelete} variant={"outline"}>
            Cancel
          </Button>
          <Button onClick={handleDelete} disabled={deleting}>
            {deleting ? "Deleting..." : "Delete"}
          </Button>
        </div>
      </div>
    </div>
  );
}
