import { ReactNode } from "react";
import { SidebarProvider } from "@/components/ui/sidebar";
import { AppSidebar } from "./AppSidebar";
import { AppHeader } from "./AppHeader";

interface AppLayoutProps {
  children: ReactNode;
}

export const AppLayout = ({ children }: AppLayoutProps) => {
  return (
    <SidebarProvider>
      {/* Entire layout container */}
      <div className="h-screen w-screen overflow-hidden bg-background">
        {/* FIXED HEADER */}
        <div className="fixed top-0 left-0 right-0 z-50">
          <AppHeader />
        </div>

        {/* BODY: sidebar + main content */}
        <div className="flex h-full pt-[64px]">
          {/* Adjust pt to match header height */}

          {/* FIXED SIDEBAR */}
          <div className="fixed top-[64px] left-0 h-[calc(100vh-64px)] border-r bg-background z-40">
            <AppSidebar />
          </div>

          {/* MAIN SCROLLABLE CONTENT */}
          <main className="ml-16 flex-1 h-[calc(100vh-64px)] overflow-y-auto p-4">
            {children}
          </main>
        </div>
      </div>
    </SidebarProvider>
  );
};
