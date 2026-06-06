/**
 * useAddressFormBackend Hook
 *
 * Manages cascading Province → Ward autocomplete state using the backend
 * address API with debounced keyword search.
 *
 * Backend hierarchy: Province → Ward (2 levels, no District)
 */

import { useState, useEffect, useCallback, useRef } from 'react';
import { useQuery } from '@tanstack/react-query';
import {
  fetchProvinces,
  fetchWardsByProvince,
  type ProvinceResponse,
  type WardResponse,
} from '@/shared/api/backendAddressApi';
import type { AutocompleteItem } from '../components/AddressAutocompleteInput';

// ─── Debounce delay ───────────────────────────────────────────
const DEBOUNCE_MS = 300;

function useDebouncedValue(value: string, delay = DEBOUNCE_MS): string {
  const [debounced, setDebounced] = useState(value);
  useEffect(() => {
    const timer = setTimeout(() => setDebounced(value), delay);
    return () => clearTimeout(timer);
  }, [value, delay]);
  return debounced;
}

// ─── Hook Options ─────────────────────────────────────────────
export interface UseAddressFormBackendOptions {
  /** Pre-fill province name (for edit mode) */
  initialProvince?: string;
  /** Pre-fill ward name (for edit mode) */
  initialWard?: string;
}

// ─── Hook Return ──────────────────────────────────────────────
export interface UseAddressFormBackendReturn {
  // Province
  provinceQuery: string;
  setProvinceQuery: (q: string) => void;
  provinces: ProvinceResponse[];
  isLoadingProvinces: boolean;
  selectedProvince: ProvinceResponse | null;
  handleProvinceSelect: (item: AutocompleteItem) => void;
  isProvinceDropdownOpen: boolean;
  setIsProvinceDropdownOpen: (open: boolean) => void;

  // Ward
  wardQuery: string;
  setWardQuery: (q: string) => void;
  wards: WardResponse[];
  isLoadingWards: boolean;
  selectedWard: WardResponse | null;
  handleWardSelect: (item: AutocompleteItem) => void;
  isWardDropdownOpen: boolean;
  setIsWardDropdownOpen: (open: boolean) => void;
  isWardDisabled: boolean;

  // Utility
  getSelectedProvinceName: () => string;
  getSelectedWardName: () => string;
}

export function useAddressFormBackend({
  initialProvince = '',
  initialWard = '',
}: UseAddressFormBackendOptions = {}): UseAddressFormBackendReturn {
  // ── Province state ──────────────────────────────────────────
  const [provinceQuery, setProvinceQuery] = useState(initialProvince);
  const [selectedProvince, setSelectedProvince] = useState<ProvinceResponse | null>(null);
  const [isProvinceDropdownOpen, setIsProvinceDropdownOpen] = useState(false);

  // ── Ward state ──────────────────────────────────────────────
  const [wardQuery, setWardQuery] = useState(initialWard);
  const [selectedWard, setSelectedWard] = useState<WardResponse | null>(null);
  const [isWardDropdownOpen, setIsWardDropdownOpen] = useState(false);

  // Track whether we're programmatically setting the query (to skip re-opening dropdown)
  const isSelectingRef = useRef(false);

  // ── Debounced search keywords ───────────────────────────────
  const debouncedProvinceKeyword = useDebouncedValue(provinceQuery);
  const debouncedWardKeyword = useDebouncedValue(wardQuery);

  // ── Province query ──────────────────────────────────────────
  const {
    data: provinces = [],
    isLoading: isLoadingProvinces,
  } = useQuery({
    queryKey: ['backend-address', 'provinces', debouncedProvinceKeyword],
    queryFn: () => fetchProvinces(debouncedProvinceKeyword || undefined),
    staleTime: 1000 * 60 * 60,
    gcTime: 1000 * 60 * 60 * 2,
  });

  // ── Ward query (only when province selected) ────────────────
  const {
    data: wards = [],
    isLoading: isLoadingWards,
  } = useQuery({
    queryKey: ['backend-address', 'wards', selectedProvince?.id, debouncedWardKeyword],
    queryFn: () => fetchWardsByProvince(selectedProvince!.id, debouncedWardKeyword || undefined),
    enabled: selectedProvince !== null,
    staleTime: 1000 * 60 * 30,
    gcTime: 1000 * 60 * 60,
  });

  // ── Province selected → reset ward ──────────────────────────
  const handleProvinceSelect = useCallback((item: AutocompleteItem) => {
    const province = provinces.find((p) => p.id === item.id);
    if (!province) return;

    isSelectingRef.current = true;
    setSelectedProvince(province);
    setProvinceQuery(province.nameWithType || province.name);
    setIsProvinceDropdownOpen(false);

    // Reset ward
    setSelectedWard(null);
    setWardQuery('');
    setIsWardDropdownOpen(false);

    // Reset flag after a tick
    setTimeout(() => { isSelectingRef.current = false; }, 0);
  }, [provinces]);

  // ── Ward selected ───────────────────────────────────────────
  const handleWardSelect = useCallback((item: AutocompleteItem) => {
    const ward = wards.find((w) => w.id === item.id);
    if (!ward) return;

    isSelectingRef.current = true;
    setSelectedWard(ward);
    setWardQuery(ward.nameWithType || ward.name);
    setIsWardDropdownOpen(false);

    setTimeout(() => { isSelectingRef.current = false; }, 0);
  }, [wards]);

  // ── Edit mode: try to match initial values from loaded data ─
  useEffect(() => {
    if (initialProvince && !selectedProvince && provinces.length > 0) {
      const match = provinces.find(
        (p) =>
          p.name === initialProvince ||
          p.nameWithType === initialProvince
      );
      if (match) {
        setSelectedProvince(match);
      }
    }
  }, [initialProvince, selectedProvince, provinces]);

  useEffect(() => {
    if (initialWard && !selectedWard && wards.length > 0 && selectedProvince) {
      const match = wards.find(
        (w) =>
          w.name === initialWard ||
          w.nameWithType === initialWard
      );
      if (match) {
        setSelectedWard(match);
      }
    }
  }, [initialWard, selectedWard, wards, selectedProvince]);

  // ── Utility ─────────────────────────────────────────────────
  const getSelectedProvinceName = useCallback(
    () => selectedProvince?.nameWithType || selectedProvince?.name || provinceQuery,
    [selectedProvince, provinceQuery]
  );

  const getSelectedWardName = useCallback(
    () => selectedWard?.nameWithType || selectedWard?.name || wardQuery,
    [selectedWard, wardQuery]
  );

  return {
    // Province
    provinceQuery,
    setProvinceQuery: (q: string) => {
      setProvinceQuery(q);
      if (!isSelectingRef.current) {
        setSelectedProvince(null);
        // Also reset ward since province changed
        setSelectedWard(null);
        setWardQuery('');
      }
    },
    provinces,
    isLoadingProvinces,
    selectedProvince,
    handleProvinceSelect,
    isProvinceDropdownOpen,
    setIsProvinceDropdownOpen,

    // Ward
    wardQuery,
    setWardQuery: (q: string) => {
      setWardQuery(q);
      if (!isSelectingRef.current) {
        setSelectedWard(null);
      }
    },
    wards,
    isLoadingWards,
    selectedWard,
    handleWardSelect,
    isWardDropdownOpen,
    setIsWardDropdownOpen,
    isWardDisabled: selectedProvince === null,

    // Utility
    getSelectedProvinceName,
    getSelectedWardName,
  };
}
