import { ReactNode } from "react";

export interface FeatureItem {
  id: string;
  icon: ReactNode;
  title: string;
  description: string;
  imageUrl?: string;
}
