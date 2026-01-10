import React from "react";
import { Navigate } from "react-router-dom";
import { useAuth } from "../context/AuthContext";

interface RequireAuthProps {
  children: React.ReactNode;
  allowedRoles?: string[]; // e.g. ["Admin"], or ["Admin","Doctor"]
}

const RequireAuth: React.FC<RequireAuthProps> = ({
  children,
  allowedRoles,
}) => {
  const { isAuthenticated, role } = useAuth();

  if (!isAuthenticated) return <Navigate to="/login" replace />;

  if (
    allowedRoles &&
    allowedRoles.length > 0 &&
    !allowedRoles.includes(role ?? "")
  ) {
    return <Navigate to="/" replace />; // or render a 403 component
  }

  return <>{children}</>;
};

export default RequireAuth;
