import { api } from "@/api/axiosClient";

export type AppointmentSummary = {
  id?: string;
  doctorId?: string;
  locationId?: string;
  patientId?: string;
  bookingId?: string;
  date: string;
  time: string;
  phone?: string;
  status?: string;
  createdTs?: number;
};

export type Slot = string;

export async function fetchSlotsByDate(
  date: string,
  doctorId?: string,
  locationId?: string,
): Promise<Slot[]> {
  const { data } = await api.get<string[]>("/appointments/slots", {
    params: { date, doctorId, locationId },
  });
  return data ?? [];
}

export async function fetchAppointmentsByDate(
  date: string,
): Promise<AppointmentSummary[]> {
  const { data } = await api.get<AppointmentSummary[]>("/appointments/byDate", {
    params: { date },
  });
  return data ?? [];
}

export async function createAppointment(
  appointment: AppointmentSummary,
): Promise<AppointmentSummary> {
  const { data } = await api.post<AppointmentSummary>(
    "/appointments/book",
    appointment,
  );
  return data;
}

export async function rescheduleAppointment(
  appointment: AppointmentSummary,
): Promise<AppointmentSummary> {
  const { data } = await api.put<AppointmentSummary>(
    "/appointments/reschedule",
    appointment,
  );
  return data;
}

export async function cancelAppointment(bookingId: string): Promise<void> {
//   console.log("[cancelAppointment] bookingId:", bookingId);
  const data = await api.put(`/appointments/cancel?bookingId=${bookingId}`);
}

export async function completeAppointment(
  id: string,
): Promise<void> {
//   console.log("[completeAppointment] bookingId:", id);
  const data = await api.put(`/appointments/complete?id=${id}`);
}
