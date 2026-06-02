import { describe, expect, it } from "vitest";
import {
  IMAGE_SEARCH_ORIGINAL_MAX_BYTES,
  validateMarketplaceImageFile,
} from "./imageProcessing";

describe("validateMarketplaceImageFile", () => {
  it("accepts jpg png and webp images within the frontend cap", () => {
    expect(validateMarketplaceImageFile(new File(["x"], "produce.jpg", { type: "image/jpeg" }))).toBeNull();
    expect(validateMarketplaceImageFile(new File(["x"], "produce.png", { type: "image/png" }))).toBeNull();
    expect(validateMarketplaceImageFile(new File(["x"], "produce.webp", { type: "image/webp" }))).toBeNull();
  });

  it("rejects empty unsupported and oversized files", () => {
    expect(validateMarketplaceImageFile(null)).toBe("empty");
    expect(validateMarketplaceImageFile(new File(["x"], "produce.gif", { type: "image/gif" }))).toBe("unsupportedType");
    expect(
      validateMarketplaceImageFile(
        new File([new Uint8Array(IMAGE_SEARCH_ORIGINAL_MAX_BYTES + 1)], "large.jpg", {
          type: "image/jpeg",
        }),
      ),
    ).toBe("tooLarge");
  });
});
