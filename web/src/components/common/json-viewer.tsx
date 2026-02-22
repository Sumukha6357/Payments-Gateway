export function JsonViewer({ value }: { value: unknown }) {
  return (
    <pre className="overflow-x-auto rounded-md bg-slate-950 p-4 text-xs text-slate-100">
      {JSON.stringify(value, null, 2)}
    </pre>
  );
}
