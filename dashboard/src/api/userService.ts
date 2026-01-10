import { api } from "@/api/axiosClient";

export type UserSummary = {
  id: string;
  name: string;
  email?: string;
  locationNames?: string[];
};

type RoleCacheShape = {
  items?: UserSummary[];
  ts?: number;
};

export type UserDetails = {
  name: string;
  email: string;
  role: string;
  id: string;
  locationIds?: string[];
  doctorId?: string;
  patientIds?: string[];
  phone?: string;
};

export type LoginResponse = {
  token: string;
  message?: string;
  userId: string;
};

const cache: { user?: UserDetails } = {};

export async function loginUser(
  username: string,
  password: string,
): Promise<LoginResponse> {
  const { data } = await api.post<LoginResponse>("/api/auth/login", {
    username,
    password,
  });

  return data;
}

export async function updatePassword(
  username: string,
  password: string,
  newPassword: string,
  isAdmin = false,
): Promise<void> {
  await api.put("/api/auth/updatePassword", {
    username,
    password,
    newPassword,
    isAdmin,
  });
}
export async function fetchUserDetails({
  refresh = false,
  userId,
}: {
  refresh?: boolean;
  userId?: string;
}): Promise<UserDetails> {
  if (!refresh && cache.user) {
    return cache.user;
  }
  const { data } = await api.get<UserDetails>(`/api/auth/user?id=${userId}`);
  cache.user = data;
  return data;
}

const roleCaches: Record<string, RoleCacheShape> = {};

const CACHE_TTL = 30 * 60 * 1000;

function isRoleCacheValid(role: string) {
  const c = roleCaches[role];
  return !!c?.items && !!c?.ts && Date.now() - c.ts < CACHE_TTL;
}

function updateRoleCache(role: string, items: UserSummary[]) {
  roleCaches[role] = { items, ts: Date.now() };
}

export async function fetchUsersByRole(
  role: string,
  refresh = false,
): Promise<UserSummary[]> {
  if (!refresh && isRoleCacheValid(role)) {
    return roleCaches[role].items!;
  }

  const res = await api.get<UserSummary[]>("/api/auth/usersByRole", {
    params: { role },
  });
//   console.log("Fetched users by role from API:", role, res.data);
  const data = res.data ?? [];
  updateRoleCache(role, data);
//   console.log("Updated role cache:", roleCaches);
  return data;
}

/** Update user */
export async function updateUser(
  id: string,
  payload: Partial<UserSummary>,
): Promise<UserSummary> {
  const res = await api.put<UserSummary>(`/users/${id}`, payload);
  const updated = res.data;

  Object.keys(roleCaches).forEach((role) => {
    if (roleCaches[role].items) {
      roleCaches[role].items = roleCaches[role].items!.map((u) =>
        u.id === id ? updated : u,
      );
      roleCaches[role].ts = Date.now();
    }
  });

  return updated;
}

export async function deleteUser(id: string): Promise<void> {
  await api.delete(`api/auth/user?id=${id}`);

  Object.keys(roleCaches).forEach((role) => {
    if (roleCaches[role].items) {
      roleCaches[role].items = roleCaches[role].items!.filter(
        (u) => u.id !== id,
      );
      roleCaches[role].ts = Date.now();
    }
  });
}

export async function createUser(payload: {
  name: string;
  email: string;
  password: string;
  role: string;
  locationIds?: string[];
}) {
  const res = await api.post("/api/auth/register", payload);
  const data = res.data;
  return data;
}

export function clearUserCaches() {
  Object.keys(roleCaches).forEach((k) => delete roleCaches[k]);
}
