import type { Metadata } from "next";
import "@/app/globals.css";
import { APP_NAME } from "@/lib/constants";
import { Providers } from "@/components/providers";

export const metadata: Metadata = {
  title: APP_NAME,
  description: "Developer and admin portal for payment gateway"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <Providers>{children}</Providers>
      </body>
    </html>
  );
}
