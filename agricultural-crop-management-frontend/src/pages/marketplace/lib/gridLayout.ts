export function getGridColsClass(count: number, maxCols: number): string {
  const safeCount = Math.max(0, count);

  if (maxCols === 2) {
    if (safeCount === 1) {
      return "grid grid-cols-1 max-w-2xl mx-auto";
    }
    return "grid grid-cols-1 lg:grid-cols-2";
  }

  if (maxCols === 4) {
    if (safeCount === 1) {
      return "grid grid-cols-1 max-w-sm mx-auto";
    }
    if (safeCount === 2) {
      return "grid grid-cols-1 sm:grid-cols-2 max-w-3xl mx-auto";
    }
    if (safeCount === 3) {
      return "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-3 max-w-5xl mx-auto";
    }
    return "grid grid-cols-1 sm:grid-cols-2 lg:grid-cols-4";
  }

  return "grid grid-cols-1";
}
