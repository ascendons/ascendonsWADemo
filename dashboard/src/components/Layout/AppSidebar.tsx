import React from "react";
import { Calendar, Home, Users, Settings, FileText } from "lucide-react";
import { NavLink } from "react-router-dom";

import { useAuth } from "../../context/AuthContext";
import {
  SidebarItem,
  SidebarShell,
  SidebarToggle,
  SimpleSidebarProvider,
} from "../ui/SimpleSidebar";

const mainItems = [
  { title: "Dashboard", url: "/", icon: <Home className="h-4 w-4" /> },
  {
    title: "Calendar",
    url: "/calendar",
    icon: <Calendar className="h-4 w-4" />,
  },
  { title: "Patients", url: "/patients", icon: <Users className="h-4 w-4" /> },
  {
    title: "Settings",
    url: "/settings",
    icon: <Settings className="h-4 w-4" />,
  },
];

const adminItems = [];

export function AppSidebar() {
  const { role } = useAuth();
  const isAdmin = role === "ADMIN";

  return (
    <SimpleSidebarProvider defaultCollapsed={true}>
      <SidebarShell>
        {/* TOP BAR: always visible toggle + optional title / logo */}
        <div className="px-2 py-3 border-b sticky top-0 z-20 bg-background/90 backdrop-blur-sm">
          <div className="flex items-center justify-between gap-2">
            {/* Moved toggle to top so it stays visible even when menu scrolls */}
            <SidebarToggle aria-label="Toggle sidebar" />
          </div>
        </div>

        {/* Menu (scrollable) */}
        <div className="flex-1 overflow-auto p-2 space-y-2">
          <div>
            <div className="space-y-1">
              {mainItems.map((it) => (
                <SidebarNavLink
                  key={it.title}
                  to={it.url}
                  icon={it.icon}
                  label={it.title}
                />
              ))}
            </div>
          </div>

          {isAdmin && (
            <div className="mt-4">
              <div className="space-y-1">
                {adminItems.map((it) => (
                  <SidebarNavLink
                    key={it.title}
                    to={it.url}
                    icon={it.icon}
                    label={it.title}
                  />
                ))}
              </div>
            </div>
          )}
        </div>
      </SidebarShell>
    </SimpleSidebarProvider>
  );
}

/** Small adapter: render NavLink wrapped with SidebarItem */
function SidebarNavLink({
  to,
  icon,
  label,
}: {
  to: string;
  icon: React.ReactNode;
  label: string;
}) {
  return (
    <NavLink
      to={to}
      end
      className={({ isActive }) =>
        // We return nothing visual here because SidebarItem handles styling.
        // Instead use NavLink to render an anchor and the active state is forwarded via 'aria-current'
        "block"
      }
    >
      {({ isActive }) => (
        <SidebarItem
          label={label}
          icon={icon}
          active={isActive}
          // Provide onClick to optionally close mobile overlays (if you extend)
          onClick={() => {
            /* no-op */
          }}
        />
      )}
    </NavLink>
  );
}
