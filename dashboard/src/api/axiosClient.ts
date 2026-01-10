import axios from "axios";

export const api = axios.create({
  baseURL: "/", // relative URL; goes to the same origin
  headers: {
    "Content-Type": "application/json",
  },
  withCredentials: false,
});

api.interceptors.request.use(
  (config) => {
    const token = sessionStorage.getItem("token");
    if (token) {
      config.headers.Authorization = `Bearer ${token}`;
    }
    return config;
  },
  (error) => Promise.reject(error),
);

api.interceptors.response.use(
  (response) => response,
  (error) => {
    if (error.response?.status === 401) {
      console.warn("Unauthorized: clearing session");
      sessionStorage.clear();
    }
    return Promise.reject(error);
  },
);
