import {
  ResponsiveContainer,
  BarChart,
  Bar,
  XAxis,
  YAxis,
  CartesianGrid,
  Tooltip,
} from "recharts";

function getLastNMonthsLabels(n = 3) {
  const now = new Date();
  const labels: string[] = [];
  for (let i = n - 1; i >= 0; i--) {
    const d = new Date(now.getFullYear(), now.getMonth() - i, 1);
    labels.push(
      d.toLocaleString(undefined, { month: "short", year: "numeric" }),
    );
  }
  return labels;
}

/** Generate dummy numbers for the three months */
function generateDummyGroupedData() {
  const months = getLastNMonthsLabels(3);
  const mVals = months.map(() => {
    const appts = Math.floor(800 + Math.random() * 700);
    const patients = Math.floor(appts * (0.45 + Math.random() * 0.5));
    const noShow = parseFloat((2 + Math.random() * 8).toFixed(1));
    return { appts, patients, noShow };
  });

  // Build rows where only relevant fields exist for each attribute
  const rows = [
    {
      attribute: "Total Appointments",
      m0_count: mVals[0].appts,
      m1_count: mVals[1].appts,
      m2_count: mVals[2].appts,
      m0_pct: null,
      m1_pct: null,
      m2_pct: null,
    },
    {
      attribute: "Total Patients",
      m0_count: mVals[0].patients,
      m1_count: mVals[1].patients,
      m2_count: mVals[2].patients,
      m0_pct: null,
      m1_pct: null,
      m2_pct: null,
    },
    {
      attribute: "No-show Rate (%)",
      m0_count: null,
      m1_count: null,
      m2_count: null,
      m0_pct: mVals[0].noShow,
      m1_pct: mVals[1].noShow,
      m2_pct: mVals[2].noShow,
    },
  ];

  return { months, rows };
}

export default function ReportsGroupedByAttribute() {
  const { months, rows } = generateDummyGroupedData();

  // Colors for months
  const colors = ["#0ea5a4", "#0369a1", "#7c3aed"];

  return (
    <div className="space-y-6">
      <div>
        <h2 className="text-lg font-semibold">Reports</h2>
      </div>

      <div className="w-full h-72">
        <ResponsiveContainer width="100%" height="100%">
          <BarChart
            data={rows}
            margin={{ top: 20, right: 40, left: 20, bottom: 20 }}
            barCategoryGap="20%"
          >
            <CartesianGrid strokeDasharray="3 3" />
            <XAxis dataKey="attribute" tick={{ fontSize: 13 }} />
            <YAxis
              yAxisId="left"
              label={{
                value: "Count",
                angle: -90,
                position: "insideLeft",
                offset: 0,
              }}
              tick={{ fontSize: 12 }}
            />
            <YAxis
              yAxisId="right"
              orientation="right"
              domain={[0, "dataMax + 5"]}
              tickFormatter={(v) => `${v}%`}
              label={{
                value: "No-show %",
                angle: 90,
                position: "insideRight",
                offset: 0,
              }}
              tick={{ fontSize: 12 }}
            />
            <Tooltip
              formatter={(value: any, name: string) => {
                if (name.endsWith("_pct"))
                  return [`${value}%`, name.replace("_pct", "")];
                return [value, name.replace("_count", "")];
              }}
            />

            {/* Bars for counts (left axis) — three bars (months) */}
            <Bar
              dataKey="m0_count"
              name={months[0]}
              fill={colors[0]}
              yAxisId="left"
              radius={[6, 6, 0, 0]}
            />
            <Bar
              dataKey="m1_count"
              name={months[1]}
              fill={colors[1]}
              yAxisId="left"
              radius={[6, 6, 0, 0]}
            />
            <Bar
              dataKey="m2_count"
              name={months[2]}
              fill={colors[2]}
              yAxisId="left"
              radius={[6, 6, 0, 0]}
            />

            {/* Bars for percentages (right axis) — only the No-show row has values; for other rows these are null */}
            <Bar
              dataKey="m0_pct"
              name={months[0]}
              fill="#ef4444"
              yAxisId="right"
              radius={[6, 6, 0, 0]}
            />
            <Bar
              dataKey="m1_pct"
              name={months[1]}
              fill="#f97316"
              yAxisId="right"
              radius={[6, 6, 0, 0]}
            />
            <Bar
              dataKey="m2_pct"
              name={months[2]}
              fill="#dc2626"
              yAxisId="right"
              radius={[6, 6, 0, 0]}
            />
          </BarChart>
        </ResponsiveContainer>
      </div>

      {/* Manual legend for month colors */}
      <div className="flex items-center gap-4">
        <div className="flex items-center gap-2">
          <span
            className="inline-block h-3 w-6"
            style={{ background: colors[0] }}
          />
          <span className="text-sm">{months[0]}</span>
        </div>
        <div className="flex items-center gap-2">
          <span
            className="inline-block h-3 w-6"
            style={{ background: colors[1] }}
          />
          <span className="text-sm">{months[1]}</span>
        </div>
        <div className="flex items-center gap-2">
          <span
            className="inline-block h-3 w-6"
            style={{ background: colors[2] }}
          />
          <span className="text-sm">{months[2]}</span>
        </div>
        <div className="ml-4 text-xs text-muted-foreground">
          Note: counts use left axis; No-show % uses right axis.
        </div>
      </div>

      {/* Small summary cards (optional) */}
      <div className="grid grid-cols-1 sm:grid-cols-3 gap-4">
        {rows.map((r) => (
          <div key={r.attribute} className="p-4 border rounded">
            <div className="text-sm text-muted-foreground">{r.attribute}</div>
            <div className="mt-2 text-lg font-bold">
              {/* show latest month value */}
              {r.m2_count != null
                ? r.m2_count.toLocaleString()
                : `${r.m2_pct}%`}
            </div>
            <div className="text-xs text-muted-foreground">Current month</div>
          </div>
        ))}
      </div>
    </div>
  );
}
