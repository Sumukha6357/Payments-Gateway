"use client";

import { useState } from "react";
import { cn } from "@/lib/utils/cn";

export function Dialog({
  trigger,
  title,
  children,
  footer,
  className
}: {
  trigger: React.ReactNode;
  title: string;
  children: React.ReactNode;
  footer?: React.ReactNode;
  className?: string;
}) {
  const [open, setOpen] = useState(false);
  return (
    <>
      <span onClick={() => setOpen(true)} className="inline-flex cursor-pointer">
        {trigger}
      </span>
      {open ? (
        <div className="fixed inset-0 z-50 grid place-items-center bg-slate-900/40 p-4">
          <div className={cn("w-full max-w-lg rounded-xl bg-white p-4 shadow-lg", className)}>
            <div className="mb-4 flex items-center justify-between">
              <h3 className="text-lg font-semibold">{title}</h3>
              <button onClick={() => setOpen(false)} className="text-sm text-slate-500">
                Close
              </button>
            </div>
            {children}
            {footer ? <div className="mt-4">{footer}</div> : null}
          </div>
        </div>
      ) : null}
    </>
  );
}
