"use client";

import { ResponsiveContainer, AreaChart, Area, CartesianGrid, XAxis, Tooltip } from "recharts";

export function TransferVolumeChart({
  data
}: {
  data: Array<{ hour: string; volume: number }>;
}) {
  return (
    <ResponsiveContainer width="100%" height="100%">
      <AreaChart data={data} margin={{ top: 8, right: 16, left: 0, bottom: 8 }}>
        <defs>
          <linearGradient id="vol" x1="0" y1="0" x2="0" y2="1">
            <stop offset="5%" stopColor="#155dfc" stopOpacity={0.4} />
            <stop offset="95%" stopColor="#155dfc" stopOpacity={0} />
          </linearGradient>
        </defs>
        <CartesianGrid strokeDasharray="3 3" />
        <XAxis dataKey="hour" />
        <Tooltip />
        <Area type="monotone" dataKey="volume" stroke="#155dfc" fill="url(#vol)" />
      </AreaChart>
    </ResponsiveContainer>
  );
}
