import { api } from "@/api/axiosClient";

export type Location = {
  id: string;
  name: string;
  code: string;
  address: string;
};

type CacheShape = {
  items?: Location[];
  ts?: number;
};

const cache: CacheShape = {};

// TTL: 30 minutes
const CACHE_TTL = 30 * 60 * 1000;

function isCacheValid() {
  return !!cache.items && !!cache.ts && Date.now() - cache.ts < CACHE_TTL;
}

function updateCache(items: Location[]) {
  cache.items = items;
  cache.ts = Date.now();
}

/**
 * Fetch all locations. Set `refresh=true` to bypass cache.
 */
export async function fetchAllLocations(refresh = false): Promise<Location[]> {
  if (!refresh && isCacheValid()) {
    return cache.items!;
  }

  const res = await api.get<Location[]>("/locations");
  const data = res.data;
  updateCache(data);
  return data;
}

export async function createLocation(
  payload: Partial<Location>,
): Promise<Location> {
  const { data } = await api.post<Location>("/locations", payload);
  const created = data;

  if (cache.items) {
    cache.items = [created, ...cache.items];
    cache.ts = Date.now();
  } else {
    updateCache([created]);
  }

  return created;
}


export async function updateLocation(
  id: string,
  payload: Partial<Location>,
): Promise<Location> {
  const res = await api.put<Location>(`/locations/${id}`, payload);
  const updated = res.data;

  if (cache.items) {
    cache.items = cache.items.map((l) => (l.id === id ? updated : l));
    cache.ts = Date.now();
  }

  return updated;
}

export async function deleteLocation(id: string): Promise<void> {
  await api.delete(`/locations/${id}`);

  if (cache.items) {
    cache.items = cache.items.filter((l) => l.id !== id);
    cache.ts = Date.now();
  }
}

/** Clear cache (manual) */
export function clearLocationsCache() {
  cache.items = undefined;
  cache.ts = undefined;
}
