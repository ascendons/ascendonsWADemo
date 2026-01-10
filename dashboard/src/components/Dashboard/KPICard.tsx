import { LucideIcon } from "lucide-react";
import { Card, CardContent } from "@/components/ui/card";
import React from "react"; // Import React to use React.HTMLAttributes

// Extend the interface with React.HTMLAttributes<HTMLDivElement>
// to accept standard HTML props like 'className', 'onClick', etc.
interface KPICardProps extends React.HTMLAttributes<HTMLDivElement> {
  title: string;
  value: string | number;
  subtitle?: string;
  icon: LucideIcon;
  trend?: {
    value: string;
    positive: boolean;
  };
}

export function KPICard({
  title,
  value,
  subtitle,
  icon: Icon,
  trend,
  className, // Destructure the className prop
  ...props // Capture any other valid HTML attributes
}: KPICardProps) {
  return (
    // Apply the className prop here, merging it with existing styles
    // Spreading other props allows for things like onClick or custom IDs
    <Card
      className={`transition-shadow hover:shadow-md ${className}`}
      {...props}
    >
      <CardContent className="flex items-center gap-4 p-6">
        <div className="flex h-12 w-12 items-center justify-center rounded-lg bg-primary-light">
          <Icon className="h-6 w-6 text-primary" />
        </div>
        <div className="flex-1">
          <p className="text-sm font-medium text-muted-foreground">{title}</p>
          <p className="text-2xl font-bold text-foreground">{value}</p>
          {subtitle && (
            <p className="text-xs text-muted-foreground">{subtitle}</p>
          )}
        </div>
        {trend && (
          <div
            className={`text-sm font-medium ${
              trend.positive ? "text-green-500" : "text-red-500" // Using Tailwind colors directly for better visibility
            }`}
          >
            {trend.value}
          </div>
        )}
      </CardContent>
    </Card>
  );
}
