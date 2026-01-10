import React from "react";
import {
  Tooltip,
  TooltipProvider,
  TooltipTrigger,
  TooltipContent,
} from "@/components/ui/tooltip";

const SIDEBAR_EXPANDED = "16rem";
const SIDEBAR_COLLAPSED = "4rem";
const SIDEBAR_STORAGE_KEY = "app:sidebar:collapsed";

type SimpleSidebarContext = {
  collapsed: boolean;
  toggle: () => void;
  setCollapsed: (v: boolean) => void;
};

const SimpleSidebarContext = React.createContext<SimpleSidebarContext | null>(
  null,
);

export function useSimpleSidebar() {
  const ctx = React.useContext(SimpleSidebarContext);
  if (!ctx)
    throw new Error(
      "useSimpleSidebar must be used inside SimpleSidebarProvider",
    );
  return ctx;
}

export const SimpleSidebarProvider: React.FC<{
  defaultCollapsed?: boolean;
  children: React.ReactNode;
}> = ({ defaultCollapsed = false, children }) => {
  const [collapsed, setCollapsed] = React.useState<boolean>(() => {
    try {
      const raw = localStorage.getItem(SIDEBAR_STORAGE_KEY);
      return raw ? raw === "true" : defaultCollapsed;
    } catch {
      return defaultCollapsed;
    }
  });

  React.useEffect(() => {
    try {
      localStorage.setItem(SIDEBAR_STORAGE_KEY, String(collapsed));
    } catch {
      // ignore
    }
  }, [collapsed]);

  const toggle = React.useCallback(() => setCollapsed((c) => !c), []);

  // Provide TooltipProvider once at the top so items can use Tooltip pieces.
  return (
    <SimpleSidebarContext.Provider value={{ collapsed, toggle, setCollapsed }}>
      <TooltipProvider>{children}</TooltipProvider>
    </SimpleSidebarContext.Provider>
  );
};

/** Shell: sets width and base styles */
export const SidebarShell: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  children,
  className = "",
  ...props
}) => {
  const ctx = React.useContext(SimpleSidebarContext);
  const collapsed = ctx?.collapsed ?? false;

  const style: React.CSSProperties = {
    ["--sidebar-width" as any]: collapsed
      ? SIDEBAR_COLLAPSED
      : SIDEBAR_EXPANDED,
  };

  return (
    <div
      {...props}
      style={style}
      className={`flex flex-col h-screen bg-white border-r border-gray-200 text-gray-800 transition-all duration-200 ${className}`}
      data-collapsed={collapsed ? "true" : "false"}
    >
      <div
        className="flex h-full"
        style={{
          width: "var(--sidebar-width)",
          minWidth: "var(--sidebar-width)",
          maxWidth: "var(--sidebar-width)",
        }}
      >
        <div className="flex flex-col w-full">{children}</div>
      </div>
    </div>
  );
};

type SidebarItemProps = {
  label: string;
  icon: React.ReactNode;
  to?: string;
  onClick?: () => void;
  active?: boolean;
  title?: string;
};

/**
 * SidebarItem:
 * - When collapsed: show tooltip with `label`
 * - When expanded: render icon + label normally (no tooltip)
 *
 * Note: We support both anchor-like items (via `to`) and plain buttons.
 */
export const SidebarItem: React.FC<SidebarItemProps> = ({
  label,
  icon,
  to,
  onClick,
  active,
  title,
}) => {
  const { collapsed } = useSimpleSidebar();
  const displayTitle = title ?? label;

  const base =
    "flex items-center gap-3 w-full px-3 py-2 text-sm rounded-md transition-colors duration-150 " +
    (active ? "bg-blue-600 text-white" : "text-gray-700 hover:bg-gray-100");

  const content = (
    <div
      className={`${base} ${collapsed ? "justify-center" : "justify-start"}`}
      onClick={onClick}
      role={to ? "link" : "button"}
      aria-current={active ? "page" : undefined}
    >
      <span className="flex items-center justify-center w-5 h-5">{icon}</span>
      {!collapsed && <span className="truncate">{label}</span>}
    </div>
  );

  // If collapsed, wrap with Radix Tooltip (TooltipTrigger + TooltipContent)
  if (collapsed) {
    return (
      <Tooltip delayDuration={0}>
        <TooltipTrigger asChild>
          {to ? <a href={to}>{content}</a> : content}
        </TooltipTrigger>
        <TooltipContent side="right" align="center">
          {displayTitle}
        </TooltipContent>
      </Tooltip>
    );
  }

  // expanded — no tooltip
  return to ? (
    <a href={to} className="w-full">
      {content}
    </a>
  ) : (
    content
  );
};

export const SidebarFooter: React.FC<React.HTMLAttributes<HTMLDivElement>> = ({
  children,
  className = "",
  ...props
}) => {
  return (
    <div className={`p-3 border-t border-gray-100 ${className}`} {...props}>
      {children}
    </div>
  );
};

export const SidebarToggle: React.FC<
  React.ButtonHTMLAttributes<HTMLButtonElement>
> = (props) => {
  const { collapsed, toggle } = useSimpleSidebar();
  return (
    <button
      {...props}
      onClick={(e) => {
        props.onClick?.(e);
        toggle();
      }}
      aria-label={collapsed ? "Expand sidebar" : "Collapse sidebar"}
      className={`flex items-center gap-2 px-3 py-2 text-sm rounded-md hover:bg-gray-100 transition-colors duration-150 w-full ${props.className ?? ""}`}
    >
      <svg
        width="14"
        height="14"
        viewBox="0 0 24 24"
        fill="none"
        stroke="currentColor"
      >
        {collapsed ? (
          <path
            d="M8 6l8 6-8 6V6z"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        ) : (
          <path
            d="M16 6L8 12l8 6V6z"
            strokeWidth="1.5"
            strokeLinecap="round"
            strokeLinejoin="round"
          />
        )}
      </svg>
      {!collapsed && <span>{collapsed ? "Expand" : "Collapse"}</span>}
    </button>
  );
};
