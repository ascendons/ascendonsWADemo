import { FormEvent } from "react";
import { Button } from "@/components/ui/button";
import { Input } from "@/components/ui/input";
import { Label } from "@/components/ui/label";
import { Location } from "@/api/locationService";

type Props = {
  editingId: string | null;
  editForm: Partial<Location>;
  handleEditChange: (key: keyof Location, value: string) => void;
  handleSaveEdit: (e?: FormEvent) => Promise<void> | void;
  closeEdit: () => void;
  savingEdit: boolean;
};

export default function EditLocationModal({
  editingId,
  editForm,
  handleEditChange,
  handleSaveEdit,
  closeEdit,
  savingEdit,
}: Props) {
  if (!editingId) return null;

  return (
    <div
      className="fixed inset-0 z-50 flex items-center justify-center"
      role="dialog"
      aria-modal="true"
    >
      <div
        className="fixed inset-0 bg-black/40"
        onClick={closeEdit}
        aria-hidden="true"
      />

      <div className="bg-white rounded-lg shadow-lg z-10 w-full max-w-2xl p-6">
        <div className="flex justify-between items-center mb-4">
          <h3 className="text-lg font-medium">Edit Location</h3>
          <button
            onClick={closeEdit}
            className="text-sm px-2 py-1 rounded hover:bg-gray-100"
            aria-label="Close"
          >
            ✕
          </button>
        </div>

        <form onSubmit={handleSaveEdit} className="space-y-4">
          <div>
            <Label htmlFor="edit-name">Name</Label>
            <Input
              id="edit-name"
              value={editForm.name ?? ""}
              onChange={(e) => handleEditChange("name", e.target.value)}
              required
            />
          </div>

          <div>
            <Label htmlFor="edit-address">Address</Label>
            <Input
              id="edit-address"
              value={editForm.address ?? ""}
              onChange={(e) => handleEditChange("address", e.target.value)}
            />
          </div>

          <div className="flex justify-end gap-2 pt-2">
            <Button type="button" variant={"outline"} onClick={closeEdit}>
              Cancel
            </Button>
            <Button type="submit" disabled={savingEdit}>
              {savingEdit ? "Saving..." : "Save"}
            </Button>
          </div>
        </form>
      </div>
    </div>
  );
}
