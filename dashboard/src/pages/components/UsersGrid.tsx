import DoctorsCard from "../components/DoctorsCard";
import ReceptionistsCard from "../components/ReceptionistsCard";

type Props = {
  confirmDeleteUser: (id: string) => void;
  refreshSignal?: number;
};

export default function UsersGrid({ confirmDeleteUser, refreshSignal }: Props) {
  return (
    <div className="grid grid-cols-1 md:grid-cols-2 gap-6">
      <DoctorsCard
        confirmDelete={confirmDeleteUser}
        refreshSignal={refreshSignal}
      />
      <ReceptionistsCard
        confirmDelete={confirmDeleteUser}
        refreshSignal={refreshSignal}
      />
    </div>
  );
}
