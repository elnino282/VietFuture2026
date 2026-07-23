import React from 'react';
import { ShieldCheck, QrCode } from 'lucide-react';
import { useNavigate } from 'react-router-dom';

export interface TraceabilityButtonProps {
  productId: string;
  variant?: 'primary' | 'secondary' | 'outline';
  className?: string;
  fullWidth?: boolean;
}

export const TraceabilityButton: React.FC<TraceabilityButtonProps> = ({ 
  productId, 
  variant = 'primary',
  className = '',
  fullWidth = false
}) => {
  const navigate = useNavigate();

  const baseClasses = "inline-flex items-center justify-center gap-2 px-5 py-2.5 font-medium rounded-xl transition-all duration-200 active:scale-[0.98]";
  const widthClass = fullWidth ? "w-full" : "w-fit";
  
  const variants = {
    primary: "bg-emerald-600 hover:bg-emerald-700 text-white shadow-sm shadow-emerald-200",
    secondary: "bg-emerald-50 hover:bg-emerald-100 text-emerald-700",
    outline: "border-2 border-emerald-200 hover:border-emerald-300 hover:bg-emerald-50 text-emerald-700"
  };

  const handleTraceability = () => {
    navigate(`/marketplace/products/${productId}/trace`);
  };

  return (
    <button 
      onClick={handleTraceability}
      className={`${baseClasses} ${variants[variant]} ${widthClass} ${className}`}
    >
      <ShieldCheck className="w-5 h-5" />
      <span>Truy xuất nguồn gốc</span>
      <QrCode className="w-4 h-4 ml-1 opacity-70" />
    </button>
  );
};
