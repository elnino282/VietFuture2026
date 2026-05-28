import React from 'react';
import { AlertCircle, RefreshCw, WifiOff, ServerCrash } from 'lucide-react';
import { Button, Card, CardContent } from '@/shared/ui';

// ═══════════════════════════════════════════════════════════════
// ERROR STATE COMPONENT
// ═══════════════════════════════════════════════════════════════

type ErrorType = 'network' | 'server' | 'validation' | 'generic';

interface ErrorStateProps {
    /** Type of error for icon and message styling */
    type?: ErrorType;
    /** Main error title */
    title?: string;
    /** Detailed error message */
    message?: string;
    /** Callback for retry action */
    onRetry?: () => void;
    /** Whether retry is in progress */
    isRetrying?: boolean;
    /** Custom className for styling */
    className?: string;
}

const errorConfig = {
    network: {
        icon: WifiOff,
        defaultTitle: 'Connection Error',
        defaultMessage: 'Unable to connect to the server. Please check your internet connection.',
        iconColor: 'text-amber-500',
        bgColor: 'bg-amber-50',
    },
    server: {
        icon: ServerCrash,
        defaultTitle: 'Server Error',
        defaultMessage: 'Something went wrong on our end. Please try again later.',
        iconColor: 'text-red-500',
        bgColor: 'bg-red-50',
    },
    validation: {
        icon: AlertCircle,
        defaultTitle: 'Validation Error',
        defaultMessage: 'The data could not be processed. Please check your inputs.',
        iconColor: 'text-orange-500',
        bgColor: 'bg-orange-50',
    },
    generic: {
        icon: AlertCircle,
        defaultTitle: 'Error',
        defaultMessage: 'An unexpected error occurred. Please try again.',
        iconColor: 'text-gray-500',
        bgColor: 'bg-gray-50',
    },
};

/**
 * ErrorState Component
 * 
 * Displays a user-friendly error message with optional retry functionality.
 * Use this component when API calls fail or data cannot be loaded.
 * 
 * @example
 * <ErrorState
 *   type="network"
 *   onRetry={() => refetchData()}
 *   isRetrying={isRefetching}
 * />
 */
export const ErrorState: React.FC<ErrorStateProps> = ({
    type = 'generic',
    title,
    message,
    onRetry,
    isRetrying = false,
    className = '',
}) => {
    const config = errorConfig[type];
    const Icon = config.icon;
    const displayTitle = title ?? config.defaultTitle;
    const displayMessage = message ?? config.defaultMessage;

    return (
        <Card className={`rounded-[18px] border-border bg-card shadow-sm ${className}`}>
            <CardContent className="py-12 px-6">
                <div className="flex flex-col items-center justify-center text-center space-y-4">
                    {/* Icon */}
                    <div className={`w-16 h-16 rounded-full ${config.bgColor} flex items-center justify-center`}>
                        <Icon className={`w-8 h-8 ${config.iconColor}`} />
                    </div>

                    {/* Title */}
                    <h3 className="text-lg font-semibold text-foreground">
                        {displayTitle}
                    </h3>

                    {/* Message */}
                    <p className="text-sm text-muted-foreground max-w-md">
                        {displayMessage}
                    </p>

                    {/* Retry Button */}
                    {onRetry && (
                        <Button
                            onClick={onRetry}
                            disabled={isRetrying}
                            variant="outline"
                            className="mt-4 gap-2 rounded-[14px]"
                        >
                            <RefreshCw className={`w-4 h-4 ${isRetrying ? 'animate-spin' : ''}`} />
                            {isRetrying ? 'Retrying...' : 'Try Again'}
                        </Button>
                    )}
                </div>
            </CardContent>
        </Card>
    );
};

// ═══════════════════════════════════════════════════════════════
// INLINE ERROR BANNER (for smaller areas)
// ═══════════════════════════════════════════════════════════════

interface ErrorBannerProps {
    message: string;
    onRetry?: () => void;
    isRetrying?: boolean;
}

export const ErrorBanner: React.FC<ErrorBannerProps> = ({
    message,
    onRetry,
    isRetrying = false,
}) => {
    return (
        <div className="flex items-center justify-between rounded-[14px] border border-destructive/30 bg-destructive/10 p-3">
            <div className="flex items-center gap-2">
                <AlertCircle className="w-4 h-4 text-destructive" />
                <span className="text-sm text-destructive">{message}</span>
            </div>
            {onRetry && (
                <Button
                    onClick={onRetry}
                    disabled={isRetrying}
                    variant="ghost"
                    size="sm"
                    className="text-destructive hover:bg-destructive/10 hover:text-destructive"
                >
                    <RefreshCw className={`w-3 h-3 mr-1 ${isRetrying ? 'animate-spin' : ''}`} />
                    Retry
                </Button>
            )}
        </div>
    );
};

export default ErrorState;
