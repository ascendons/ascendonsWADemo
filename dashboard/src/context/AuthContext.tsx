import React, { createContext, useContext, useEffect, useState } from "react";
import { useNavigate } from "react-router-dom";
import {
  loginUser as apiLoginUser,
  fetchUserDetails,
  UserDetails,
  clearUserCaches,
} from "../api/userService";
import { api } from "../api/axiosClient";

type AuthContextType = {
  isAuthenticated: boolean;
  role: string | null;
  authToken: string | null;
  user: UserDetails | null;
  login: (email: string, password: string) => Promise<void>;
  logout: () => void;
  refreshUser: () => Promise<void>;
};

const AuthContext = createContext<AuthContextType | undefined>(undefined);

export const AuthProvider: React.FC<{ children: React.ReactNode }> = ({
  children,
}) => {
  const navigate = useNavigate();

  const [isAuthenticated, setIsAuthenticated] = useState(
    sessionStorage.getItem("isAuthenticated") === "true",
  );
  const [role, setRole] = useState<string | null>(
    sessionStorage.getItem("role"),
  );
  const [authToken, setAuthToken] = useState<string | null>(
    sessionStorage.getItem("token"),
  );
  const [user, setUser] = useState<UserDetails | null>(() => {
    const v = sessionStorage.getItem("user");
    return v ? (JSON.parse(v) as UserDetails) : null;
  });

  useEffect(() => {
    if (authToken) {
      api.defaults.headers.common["Authorization"] = `Bearer ${authToken}`;
    } else {
      sessionStorage.removeItem("token");
      delete api.defaults.headers.common["Authorization"];
    }
    sessionStorage.setItem("isAuthenticated", String(isAuthenticated));
  }, [authToken, isAuthenticated]);

  useEffect(() => {
    if (role) sessionStorage.setItem("role", role);
    else sessionStorage.removeItem("role");
  }, [role]);

  useEffect(() => {
    if (user) sessionStorage.setItem("user", JSON.stringify(user));
    else sessionStorage.removeItem("user");
  }, [user]);

  const login = async (email: string, password: string) => {
    const loginResp = await apiLoginUser(email, password);
    console.log("Login response:", loginResp);
    sessionStorage.setItem("token", loginResp.token);
    console.log("Token set in session storage:", loginResp.token);
    setAuthToken(loginResp.token);
    setIsAuthenticated(true);

    const userDetails = await fetchUserDetails({
      userId: loginResp.userId,
      refresh: true,
    });
    setUser(userDetails);
    setRole(userDetails.role ?? null);
    navigate("/", { replace: true });
  };

  const logout = () => {
    setIsAuthenticated(false);
    setRole(null);
    setAuthToken(null);
    setUser(null);
    sessionStorage.clear();
    clearUserCaches();
    navigate("/login", { replace: true });
  };

  const refreshUser = async () => {
    if (!user) return;
    const refreshed = await fetchUserDetails({
      userId: user.id,
      refresh: true,
    });
    setUser(refreshed);
    setRole(refreshed.role ?? null);
  };

  return (
    <AuthContext.Provider
      value={{
        isAuthenticated,
        role,
        authToken,
        user,
        login,
        logout,
        refreshUser,
      }}
    >
      {children}
    </AuthContext.Provider>
  );
};

export const useAuth = () => {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
};
