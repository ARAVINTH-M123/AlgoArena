import "./globals.css";
import type { Metadata } from "next";
import Navbar from "@/components/Navbar";
import AppFrame from "@/components/AppFrame";
import PresenceHeartbeat from "@/components/PresenceHeartbeat";

export const metadata: Metadata = {
  title: "AlgoArena",
  description: "Competitive coding platform"
};

export default function RootLayout({ children }: { children: React.ReactNode }) {
  return (
    <html lang="en">
      <body>
        <Navbar />
        <PresenceHeartbeat />
        <AppFrame>{children}</AppFrame>
      </body>
    </html>
  );
}
