/**
 * AddressAutocompleteInput Component
 *
 * A text input with autocomplete dropdown for selecting addresses from backend.
 * Shows search results as user types, with loading & empty states.
 */

import { useRef, useEffect, useCallback } from 'react';
import { Input, Label } from '@/shared/ui';
import { Loader2, Check, MapPin, ChevronDown } from 'lucide-react';
import { cn } from '@/shared/lib';

export interface AutocompleteItem {
  id: number;
  name: string;
  nameWithType?: string;
  type?: string;
}

export interface AddressAutocompleteInputProps {
  /** Current text value in the input */
  value: string;
  /** Called when the user types */
  onChange: (value: string) => void;
  /** List of items to show in dropdown */
  items: AutocompleteItem[];
  /** Called when user selects an item */
  onSelect: (item: AutocompleteItem) => void;
  /** Whether items are currently loading */
  isLoading?: boolean;
  /** Whether dropdown is open */
  isOpen: boolean;
  /** Control dropdown open state */
  onOpenChange: (open: boolean) => void;
  /** Selected item (for highlighting) */
  selectedItem?: AutocompleteItem | null;
  /** Field label */
  label: string;
  /** Required field indicator */
  required?: boolean;
  /** Placeholder text */
  placeholder?: string;
  /** Disabled state */
  disabled?: boolean;
  /** HTML id for the input */
  id?: string;
  /** Additional class for container */
  className?: string;
}

export function AddressAutocompleteInput({
  value,
  onChange,
  items,
  onSelect,
  isLoading = false,
  isOpen,
  onOpenChange,
  selectedItem,
  label,
  required = false,
  placeholder,
  disabled = false,
  id,
  className,
}: AddressAutocompleteInputProps) {
  const containerRef = useRef<HTMLDivElement>(null);
  const inputRef = useRef<HTMLInputElement>(null);

  // Close dropdown when clicking outside
  useEffect(() => {
    function handleClickOutside(event: MouseEvent) {
      if (containerRef.current && !containerRef.current.contains(event.target as Node)) {
        onOpenChange(false);
      }
    }
    if (isOpen) {
      document.addEventListener('mousedown', handleClickOutside);
      return () => document.removeEventListener('mousedown', handleClickOutside);
    }
  }, [isOpen, onOpenChange]);

  // Close on Escape
  const handleKeyDown = useCallback(
    (e: React.KeyboardEvent) => {
      if (e.key === 'Escape') {
        onOpenChange(false);
        inputRef.current?.blur();
      }
    },
    [onOpenChange]
  );

  const inputClass =
    'h-11 rounded-xl border-gray-200 bg-white text-gray-900 placeholder:text-gray-400 transition-colors focus-visible:border-emerald-400 focus-visible:ring-emerald-400/20 pr-9';

  return (
    <div ref={containerRef} className={cn('relative', className)}>
      <Label htmlFor={id} className="text-sm font-medium text-gray-700">
        {label} {required && <span className="text-red-400">*</span>}
      </Label>
      <div className="relative mt-1.5">
        <Input
          ref={inputRef}
          id={id}
          value={value}
          onChange={(e) => {
            onChange(e.target.value);
            onOpenChange(true);
          }}
          onFocus={() => onOpenChange(true)}
          onKeyDown={handleKeyDown}
          placeholder={placeholder}
          disabled={disabled}
          autoComplete="off"
          className={cn(inputClass, isOpen && 'border-emerald-400 ring-1 ring-emerald-400/20')}
        />
        <div className="pointer-events-none absolute right-3 top-1/2 -translate-y-1/2">
          {isLoading ? (
            <Loader2 className="h-4 w-4 animate-spin text-gray-400" />
          ) : (
            <ChevronDown
              className={cn(
                'h-4 w-4 text-gray-400 transition-transform duration-200',
                isOpen && 'rotate-180'
              )}
            />
          )}
        </div>
      </div>

      {/* Dropdown */}
      {isOpen && (
        <div className="absolute z-50 mt-1 w-full overflow-hidden rounded-xl border border-gray-200 bg-white shadow-lg animate-in fade-in-0 zoom-in-95 slide-in-from-top-2 duration-200">
          <div className="max-h-[220px] overflow-y-auto overscroll-contain">
            {isLoading ? (
              <div className="flex items-center gap-2 px-4 py-3">
                <Loader2 className="h-4 w-4 animate-spin text-emerald-500" />
                <span className="text-sm text-gray-500">Đang tìm kiếm...</span>
              </div>
            ) : items.length === 0 ? (
              <div className="px-4 py-3 text-center">
                <MapPin className="mx-auto h-5 w-5 text-gray-300" />
                <p className="mt-1 text-sm text-gray-400">
                  {value.trim() ? 'Không tìm thấy kết quả' : 'Nhập để tìm kiếm'}
                </p>
              </div>
            ) : (
              items.map((item) => {
                const isSelected = selectedItem?.id === item.id;
                return (
                  <button
                    key={item.id}
                    type="button"
                    className={cn(
                      'flex w-full items-center gap-2 px-4 py-2.5 text-left text-sm transition-colors',
                      isSelected
                        ? 'bg-emerald-50 text-emerald-700 font-medium'
                        : 'text-gray-700 hover:bg-gray-50'
                    )}
                    onClick={() => {
                      onSelect(item);
                    }}
                  >
                    {isSelected && <Check className="h-3.5 w-3.5 shrink-0 text-emerald-600" />}
                    <span className={cn(!isSelected && 'ml-5.5')}>
                      {item.nameWithType || item.name}
                    </span>
                    {item.type && (
                      <span className="ml-auto text-xs text-gray-400">{item.type}</span>
                    )}
                  </button>
                );
              })
            )}
          </div>
        </div>
      )}
    </div>
  );
}
