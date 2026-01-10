import { api } from "./axiosClient";

export type PatientDetails = {
  id: string;
  name: string;
  patientId?: string;
  gender: "MALE" | "FEMALE" | "OTHER" | "UNDISCLOSED";
  age: number | null;
  notes?: string;
  guardianUserId?: string;
  createdAt?: string;
  phone?: string;
  email?: string;
};

type CacheShape = {
  patients?: PatientDetails[];
  ts?: number;
};

const cache: CacheShape = {};

const CACHE_TTL = 30 * 60 * 1000;

function isCacheValid(): boolean {
  if (!cache.patients || !cache.ts) return false;
  return Date.now() - cache.ts < CACHE_TTL;
}

function updateCache(patients: PatientDetails[]) {
  cache.patients = patients;
  cache.ts = Date.now();
}

export async function fetchAllPatients(
  refresh = false,
): Promise<PatientDetails[]> {
  if (!refresh && isCacheValid()) {
    return cache.patients!;
  }

  const response = await api.get<PatientDetails[]>("/api/patients");
  const data = response.data;
  updateCache(data);
  return data;
}

export async function fetchPatientDetails({
  refresh = false,
  patientId,
}: {
  refresh?: boolean;
  patientId: string;
}): Promise<PatientDetails> {
  if (!refresh && isCacheValid()) {
    const patient = cache.patients!.find((p) => p.id === patientId);
    if (patient) return patient;
  }

  const all = await fetchAllPatients(true);
  const found = all.find((p) => p.id === patientId);
  if (!found) throw new Error("Patient not found");
  return found;
}

export async function createPatient(
  payload: Partial<PatientDetails>,
): Promise<PatientDetails> {
  const { data } = await api.post<PatientDetails>("/api/patients", payload);

  if (cache.patients) {
    cache.patients = [data, ...cache.patients];
    cache.ts = Date.now();
  } else {
    updateCache([data]);
  }

  return data;
}

export async function updatePatientDetails(
  patientId: string,
  payload: Partial<PatientDetails>,
): Promise<PatientDetails> {
  const { data } = await api.put<PatientDetails>(
    `/api/patients/${patientId}`,
    payload,
  );

  if (cache.patients) {
    cache.patients = cache.patients.map((p) => (p.id === patientId ? data : p));
    cache.ts = Date.now();
  }

  return data;
}

export async function deletePatient(patientId: string): Promise<void> {
  await api.delete(`/api/patients/${patientId}`);

  if (cache.patients) {
    cache.patients = cache.patients.filter((p) => p.id !== patientId);
    cache.ts = Date.now();
  }
}

/**
 * Optional utility: force-clear the cache (handy for testing or admin actions).
 */
export function clearPatientsCache() {
  cache.patients = undefined;
  cache.ts = undefined;
}
