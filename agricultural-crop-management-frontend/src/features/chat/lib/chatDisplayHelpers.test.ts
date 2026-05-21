import { describe, expect, it } from "vitest";
import { getInitials, joinDefinedParts } from "./chatDisplayHelpers";

describe("chatDisplayHelpers", () => {
  it("builds initials from farm name", () => {
    expect(
      getInitials({
        userId: 11,
        firebaseUid: "u_11",
        displayName: "Le Thi Farmer 2",
        representativeName: "Le Thi Farmer 2",
        farmName: "Nong trai Cao Nguyen Xanh",
        address: "Lam Dong",
        role: "FARMER",
      })
    ).toBe("NX");
  });

  it("builds initials from person name", () => {
    expect(
      getInitials({
        userId: 3,
        firebaseUid: "u_3",
        displayName: "Tran Thi Buyer",
        representativeName: "Tran Thi Buyer",
        farmName: null,
        address: null,
        role: "BUYER",
      })
    ).toBe("TB");
  });

  it("falls back to A for account labels", () => {
    expect(getInitials(null, "u_22")).toBe("A");
  });

  it("joins only defined parts", () => {
    expect(joinDefinedParts(["Le Thi Farmer 2", null, "Xa Xuan Truong"], " · ")).toBe(
      "Le Thi Farmer 2 · Xa Xuan Truong"
    );
  });
});
