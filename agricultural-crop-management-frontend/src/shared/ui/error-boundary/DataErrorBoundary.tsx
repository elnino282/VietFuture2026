import React, { Component, ErrorInfo, ReactNode } from 'react';
import { AlertTriangle, RefreshCcw } from 'lucide-react';
import { Card, CardContent } from '@/shared/ui/card';
import { Button } from '@/shared/ui/button';

interface Props {
  children: ReactNode;
  fallbackMessage?: string;
  onRetry?: () => void;
}

interface State {
  hasError: boolean;
  error: Error | null;
}

export class DataErrorBoundary extends Component<Props, State> {
  public state: State = {
    hasError: false,
    error: null
  };

  public static getDerivedStateFromError(error: Error): State {
    // Update state so the next render will show the fallback UI.
    return { hasError: true, error };
  }

  public componentDidCatch(error: Error, errorInfo: ErrorInfo) {
    console.error('DataErrorBoundary caught an error:', error, errorInfo);
  }

  private handleRetry = () => {
    this.setState({ hasError: false, error: null });
    if (this.props.onRetry) {
      this.props.onRetry();
    }
  };

  public render() {
    if (this.state.hasError) {
      return (
        <Card className="border-rose-200 shadow-sm bg-rose-50/30">
          <CardContent className="p-8 flex flex-col items-center justify-center text-center space-y-4">
            <div className="w-12 h-12 rounded-full bg-rose-100 flex items-center justify-center text-rose-500 mb-2">
              <AlertTriangle className="w-6 h-6" />
            </div>
            <div>
              <h3 className="text-lg font-semibold text-rose-800">Đã xảy ra lỗi tải dữ liệu</h3>
              <p className="text-sm text-rose-600/80 max-w-sm mt-1">
                {this.props.fallbackMessage || 'Dữ liệu đang được đồng bộ hoặc máy chủ đang bận. Vui lòng thử lại sau.'}
              </p>
            </div>
            <Button 
              variant="outline" 
              className="mt-4 bg-white border-rose-200 text-rose-700 hover:bg-rose-50 hover:text-rose-800"
              onClick={this.handleRetry}
            >
              <RefreshCcw className="w-4 h-4 mr-2" />
              Thử lại
            </Button>
          </CardContent>
        </Card>
      );
    }

    return this.props.children;
  }
}
