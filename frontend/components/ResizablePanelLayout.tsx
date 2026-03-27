"use client";

import { ReactNode, useEffect, useRef, useState } from "react";

type Props = {
  leftPanel: ReactNode;
  rightTopPanel: ReactNode;
  rightBottomPanel: ReactNode;
};

export default function ResizablePanelLayout({
  leftPanel,
  rightTopPanel,
  rightBottomPanel
}: Props) {
  const [isSidebarCollapsed, setIsSidebarCollapsed] = useState(false);
  const containerRef = useRef<HTMLDivElement>(null);
  const rightPanelRef = useRef<HTMLDivElement>(null);
  const rightTopPanelRef = useRef<HTMLDivElement>(null);

  const [verticalSplit, setVerticalSplit] = useState(50); // Left side percentage
  const [horizontalSplit, setHorizontalSplit] = useState(50); // Top side percentage

  const [isDraggingVertical, setIsDraggingVertical] = useState(false);
  const [isDraggingHorizontal, setIsDraggingHorizontal] = useState(false);

  const handleVerticalMouseDown = () => {
    setIsDraggingVertical(true);
  };

  const handleHorizontalMouseDown = () => {
    setIsDraggingHorizontal(true);
  };

  const handleMouseUp = () => {
    setIsDraggingVertical(false);
    setIsDraggingHorizontal(false);
  };

  const handleMouseMove = (e: React.MouseEvent) => {
    if (!isDraggingVertical && !isDraggingHorizontal) return;

    if (isDraggingVertical && containerRef.current) {
      const containerRect = containerRef.current.getBoundingClientRect();
      const newPercentage = ((e.clientX - containerRect.left) / containerRect.width) * 100;
      if (newPercentage > 20 && newPercentage < 80) {
        setVerticalSplit(newPercentage);
      }
    }

    if (isDraggingHorizontal && rightPanelRef.current) {
      const rightPanelRect = rightPanelRef.current.getBoundingClientRect();
      const newPercentage = ((e.clientY - rightPanelRect.top) / rightPanelRect.height) * 100;
      if (newPercentage > 20 && newPercentage < 80) {
        setHorizontalSplit(newPercentage);
      }
    }
  };

  useEffect(() => {
    const syncSidebarState = () => {
      setIsSidebarCollapsed(localStorage.getItem("sidebarCollapsed") === "true");
    };

    syncSidebarState();
    window.addEventListener("sidebar-toggle", syncSidebarState);
    window.addEventListener("storage", syncSidebarState);

    return () => {
      window.removeEventListener("sidebar-toggle", syncSidebarState);
      window.removeEventListener("storage", syncSidebarState);
    };
  }, []);

  return (
    <div
      ref={containerRef}
      className={`fixed inset-0 top-16 left-0 right-0 flex gap-0 select-none bg-platform-bg ${
        isSidebarCollapsed ? "md:left-14" : "md:left-56"
      }`}
      onMouseMove={handleMouseMove}
      onMouseUp={handleMouseUp}
      onMouseLeave={handleMouseUp}
    >
      {/* Left Panel - Code Editor */}
      <div style={{ width: `${verticalSplit}%` }} className="flex flex-col overflow-hidden">
        {leftPanel}
      </div>

      {/* Vertical Divider */}
      <div
        onMouseDown={handleVerticalMouseDown}
        className={`w-1 cursor-col-resize bg-platform-border hover:bg-platform-accent transition-colors ${
          isDraggingVertical ? "bg-platform-accent" : ""
        }`}
      />

      {/* Right Panel */}
      <div
        ref={rightPanelRef}
        style={{ width: `${100 - verticalSplit}%` }}
        className="flex flex-col overflow-hidden"
      >
        {/* Right Top Panel - Problem Description */}
        <div
          ref={rightTopPanelRef}
          style={{ height: `${horizontalSplit}%` }}
          className="overflow-y-auto bg-platform-bg border-b border-platform-border"
        >
          {rightTopPanel}
        </div>

        {/* Horizontal Divider */}
        <div
          onMouseDown={handleHorizontalMouseDown}
          className={`h-1 cursor-row-resize bg-platform-border hover:bg-platform-accent transition-colors ${
            isDraggingHorizontal ? "bg-platform-accent" : ""
          }`}
        />

        {/* Right Bottom Panel - Custom Test Cases */}
        <div
          style={{ height: `${100 - horizontalSplit}%` }}
          className="overflow-y-auto bg-platform-bg"
        >
          {rightBottomPanel}
        </div>
      </div>
    </div>
  );
}
