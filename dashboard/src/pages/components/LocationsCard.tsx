import { Table, TableBody, TableRow, TableCell } from "@/components/ui/table";
import { Button } from "@/components/ui/button";
import { Edit3, Trash2 } from "lucide-react";
import { Location } from "@/api/locationService";

type Props = {
  allLocations: Location[];
  isLoading: boolean;
  openEdit: (loc: Location) => void;
  confirmDelete: (id: string) => void;
};

export default function LocationsCard({
  allLocations,
  isLoading,
  openEdit,
  confirmDelete,
}: Props) {
  return (
    <Table>
      <TableBody>
        {allLocations.length === 0 && (
          <TableRow>
            <TableCell
              colSpan={3}
              className="p-4 text-sm text-muted-foreground"
            >
              {isLoading ? "Loading locations..." : "No locations found."}
            </TableCell>
          </TableRow>
        )}

        {allLocations.map((location) => (
          <TableRow key={location.id}>
            <TableCell>
              <div>
                <h3 className="text-base font-medium">{location.name}</h3>
                <p className="text-xs text-muted-foreground mt-1">
                  {location.address || "-"}
                </p>
              </div>
            </TableCell>
            <TableCell className="text-right">
              <div className="flex items-center gap-2 justify-end">
                <Button
                  variant="outline"
                  size="sm"
                  onClick={() => openEdit(location)}
                  aria-label={`Edit ${location.name}`}
                >
                  <Edit3 className="h-4 w-4" />
                </Button>

                <Button
                  variant="ghost"
                  size="sm"
                  onClick={() => confirmDelete(location.id)}
                  aria-label={`Delete ${location.name}`}
                >
                  <Trash2 className="h-4 w-4" />
                </Button>
              </div>
            </TableCell>
          </TableRow>
        ))}
      </TableBody>
    </Table>
  );
}
