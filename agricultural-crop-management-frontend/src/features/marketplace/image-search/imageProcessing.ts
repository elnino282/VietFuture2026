export const IMAGE_SEARCH_ALLOWED_TYPES = ["image/jpeg", "image/png", "image/webp"] as const;
export const IMAGE_SEARCH_ORIGINAL_MAX_BYTES = 8 * 1024 * 1024;
export const IMAGE_SEARCH_RESIZE_MAX_SIDE = 1024;
export const IMAGE_SEARCH_QUALITY = 0.78;

export type ImageSearchValidationError = "empty" | "unsupportedType" | "tooLarge";

export function validateMarketplaceImageFile(file: File | null | undefined): ImageSearchValidationError | null {
  if (!file || file.size <= 0) {
    return "empty";
  }
  if (file.size > IMAGE_SEARCH_ORIGINAL_MAX_BYTES) {
    return "tooLarge";
  }
  if (!IMAGE_SEARCH_ALLOWED_TYPES.includes(file.type as (typeof IMAGE_SEARCH_ALLOWED_TYPES)[number])) {
    return "unsupportedType";
  }
  return null;
}

export async function resizeMarketplaceImage(file: File): Promise<File> {
  const objectUrl = URL.createObjectURL(file);
  try {
    const image = await loadImage(objectUrl);
    const scale = Math.min(1, IMAGE_SEARCH_RESIZE_MAX_SIDE / Math.max(image.width, image.height));
    const width = Math.max(1, Math.round(image.width * scale));
    const height = Math.max(1, Math.round(image.height * scale));

    const canvas = document.createElement("canvas");
    canvas.width = width;
    canvas.height = height;

    const context = canvas.getContext("2d");
    if (!context) {
      throw new Error("Canvas is not available.");
    }

    context.fillStyle = "#ffffff";
    context.fillRect(0, 0, width, height);
    context.drawImage(image, 0, 0, width, height);

    const blob = await canvasToBlob(canvas, "image/jpeg", IMAGE_SEARCH_QUALITY);
    return new File([blob], "marketplace-image-search.jpg", {
      type: "image/jpeg",
      lastModified: Date.now(),
    });
  } finally {
    URL.revokeObjectURL(objectUrl);
  }
}

function loadImage(src: string): Promise<HTMLImageElement> {
  return new Promise((resolve, reject) => {
    const image = new Image();
    image.onload = () => resolve(image);
    image.onerror = () => reject(new Error("Image cannot be decoded."));
    image.src = src;
  });
}

function canvasToBlob(canvas: HTMLCanvasElement, type: string, quality: number): Promise<Blob> {
  return new Promise((resolve, reject) => {
    canvas.toBlob((blob) => {
      if (!blob) {
        reject(new Error("Image cannot be compressed."));
        return;
      }
      resolve(blob);
    }, type, quality);
  });
}
