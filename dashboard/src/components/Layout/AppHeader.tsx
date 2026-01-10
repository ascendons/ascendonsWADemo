import React, { useState } from "react";
import { User, Key, Power } from "lucide-react";
import { Button } from "@/components/ui/button";
import {
  DropdownMenu,
  DropdownMenuContent,
  DropdownMenuItem,
  DropdownMenuSeparator,
  DropdownMenuTrigger,
} from "@/components/ui/dropdown-menu";
import { useNavigate } from "react-router-dom";
import { useAuth } from "../../context/AuthContext";
import { fetchUsersByRole, updatePassword } from "@/api/userService";
import { fetchAllLocations } from "@/api/locationService";

export function AppHeader() {
  const navigate = useNavigate();
  const { role, logout, user } = useAuth();

  const [showChangePwd, setShowChangePwd] = useState(false);
  const [curPass, setCurPass] = useState("");
  const [newPass, setNewPass] = useState("");
  const [confirmPass, setConfirmPass] = useState("");
  const [cpLoading, setCpLoading] = useState(false);
  const [cpError, setCpError] = useState<string | null>(null);
  const [cpSuccess, setCpSuccess] = useState<string | null>(null);

  const handleLogout = () => {
    logout();
    navigate("/login", { replace: true });
  };

  const openChangePwd = () => {
    setCpError(null);
    setCpSuccess(null);
    setCurPass("");
    setNewPass("");
    setConfirmPass("");
    setShowChangePwd(true);
  };

  const closeChangePwd = () => {
    setShowChangePwd(false);
    setCpError(null);
    setCpSuccess(null);
    setCpLoading(false);
  };
  Promise.all([
    fetchAllLocations(false),
    fetchUsersByRole("DOCTOR", false),
    fetchUsersByRole("RECEPTIONIST", false),
  ]);

  const submitChangePassword = async (e?: React.FormEvent) => {
    e?.preventDefault();
    setCpError(null);
    setCpSuccess(null);

    if (!curPass || !newPass || !confirmPass) {
      setCpError("Please fill all fields.");
      return;
    }
    if (newPass !== confirmPass) {
      setCpError("New password and confirmation do not match.");
      return;
    }

    setCpLoading(true);
    try {
      await updatePassword(user.email, curPass, newPass, false);

      setCpSuccess("Password changed successfully.");
      setTimeout(() => {
        closeChangePwd();
      }, 900);
    } catch (err) {
      const message =
        err?.response?.data?.message ||
        err?.message ||
        "Failed to change password. Please try again.";
      setCpError(message);
    } finally {
      setCpLoading(false);
    }
  };

  return (
    <>
      <header className="flex h-16 items-center justify-between border-b border-border bg-card px-6">
        <div className="flex items-center gap-4">
          <div className="flex items-center gap-3">
            <div className="flex h-10 w-10 items-center justify-center rounded-lg bg-primary text-primary-foreground">
              <img src="/ascendons.jpeg" alt="Logo" className="h-6 w-6" />
            </div>
            <div>
              <h2 className="text-lg font-semibold text-sidebar-foreground">
                Ascendons Appointments
              </h2>

            </div>
          </div>
        </div>

        <div className="flex items-center gap-3">
          <DropdownMenu>
            <DropdownMenuTrigger asChild>
              <Button variant="ghost" size="icon" aria-label="User menu">
                <User className="h-5 w-5" />
              </Button>
            </DropdownMenuTrigger>

            <DropdownMenuContent align="end" className="min-w-[220px]">
              {/* Name + email as label-style */}
              <div className="px-3 py-2">
                <div className="text-sm font-medium text-foreground">
                  {user?.name ?? "Unknown user"}
                </div>
                <div className="text-xs text-muted-foreground mt-0.5">
                  {user?.email ?? "No email"}
                </div>
                <div className="text-xs text-muted-foreground mt-1">
                  {role ?? "Guest"}
                </div>
              </div>

              <DropdownMenuSeparator />

              {/* Change password option */}
              <DropdownMenuItem
                onClick={() => {
                  openChangePwd();
                }}
              >
                <div className="flex items-center gap-2">
                  <Key className="h-4 w-4" />
                  <span>Change password</span>
                </div>
              </DropdownMenuItem>

              <DropdownMenuSeparator />
              <DropdownMenuItem
                onClick={() => {
                  handleLogout();
                }}
              >
                <div className="flex items-center gap-2">
                  <Power className="h-4 w-4" />
                  Logout
                </div>
              </DropdownMenuItem>
            </DropdownMenuContent>
          </DropdownMenu>
        </div>
      </header>

      {/* Simple modal for Change Password - accessible, no external deps */}
      {showChangePwd && (
        <div
          role="dialog"
          aria-modal="true"
          aria-labelledby="change-password-title"
          className="fixed inset-0 z-50 flex items-center justify-center px-4"
        >
          {/* backdrop */}
          <div
            className="fixed inset-0 bg-black/40 backdrop-blur-sm"
            onClick={closeChangePwd}
            aria-hidden="true"
          />

          {/* modal */}
          <form
            onSubmit={submitChangePassword}
            className="relative z-10 w-full max-w-lg rounded-lg bg-white p-6 shadow-lg"
            onClick={(e) => e.stopPropagation()} // prevent backdrop close when clicking inside
          >
            <h3
              id="change-password-title"
              className="text-lg font-semibold mb-2"
            >
              Change password
            </h3>
            <p className="text-sm text-muted-foreground mb-4">
              Enter current and new password.
            </p>

            <div className="space-y-3">
              <label className="block">
                <span className="text-sm">Current password</span>
                <input
                  type="password"
                  value={curPass}
                  onChange={(e) => setCurPass(e.target.value)}
                  className="mt-1 w-full rounded-md border px-3 py-2"
                  required
                  autoFocus
                />
              </label>

              <label className="block">
                <span className="text-sm">New password</span>
                <input
                  type="password"
                  value={newPass}
                  onChange={(e) => setNewPass(e.target.value)}
                  className="mt-1 w-full rounded-md border px-3 py-2"
                  required
                />
              </label>

              <label className="block">
                <span className="text-sm">Confirm new password</span>
                <input
                  type="password"
                  value={confirmPass}
                  onChange={(e) => setConfirmPass(e.target.value)}
                  className="mt-1 w-full rounded-md border px-3 py-2"
                  required
                />
              </label>
            </div>

            {cpError && (
              <div className="mt-3 text-sm text-red-600">{cpError}</div>
            )}
            {cpSuccess && (
              <div className="mt-3 text-sm text-green-600">{cpSuccess}</div>
            )}

            <div className="mt-6 flex items-center justify-end gap-3">
              <button
                type="button"
                onClick={closeChangePwd}
                className="rounded-md px-4 py-2 text-sm font-medium border"
                disabled={cpLoading}
              >
                Cancel
              </button>

              <button
                type="submit"
                className="rounded-md bg-indigo-600 px-4 py-2 text-sm font-medium text-white disabled:opacity-60"
                disabled={cpLoading}
              >
                {cpLoading ? "Saving..." : "Submit"}
              </button>
            </div>
          </form>
        </div>
      )}
    </>
  );
}
