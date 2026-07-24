import React, { Component, ErrorInfo, ReactNode } from "react";

interface Props {
  children: ReactNode;
  fallback?: ReactNode;
}

interface State {
  hasError: boolean;
  error?: Error;
}

export class ErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false
  };

  public static getDerivedStateFromError(error: Error): State {
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error("Uncaught error:", error, errorInfo);
  }

  public render() {
    if (this.state.hasError) {
      if (this.props.fallback) {
        return this.props.fallback;
      }
      return (
        <div className="rounded-xl border border-destructive/20 bg-destructive/5 p-6 text-center text-destructive">
          <h2 className="mb-2 text-lg font-semibold">Đã xảy ra lỗi</h2>
          <p className="text-sm">Vui lòng tải lại trang hoặc thử lại sau.</p>
        </div>
      );
    }

    return this.props.children;
  }
}
