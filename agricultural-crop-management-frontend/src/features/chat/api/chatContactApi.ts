import { z } from "zod";
import { apiClient } from "@/shared/api/apiClient";
import { parseApiResponse } from "@/shared/api/types";
import type { ChatContactProfile } from "../model/types";

const ChatContactProfileSchema = z.object({
  userId: z.number().int().positive(),
  firebaseUid: z.string().min(1),
  displayName: z.string().min(1),
  representativeName: z.string().min(1),
  farmName: z.string().nullable(),
  farmId: z.number().int().positive().nullable().optional().default(null),
  address: z.string().nullable(),
  role: z.string().nullable().default(null),
  avatarUrl: z
    .string()
    .nullable()
    .optional()
    .transform((value) => value?.trim() || null),
  ratingAverage: z.number().nullable().optional().default(null),
  ratingCount: z.number().int().nonnegative().nullable().optional().default(null),
});

const ChatContactProfileListSchema = z.array(ChatContactProfileSchema);

export async function searchChatContacts(params: {
  query?: string;
  userIds?: number[];
  limit?: number;
}): Promise<ChatContactProfile[]> {
  const query = params.query?.trim();
  const uniqueUserIds = [...new Set(params.userIds ?? [])].filter((id) =>
    Number.isInteger(id) && id > 0
  );

  const requestParams: Record<string, string | number> = {};
  if (query) {
    requestParams.q = query;
  }
  if (uniqueUserIds.length > 0) {
    requestParams.userIds = uniqueUserIds.join(",");
  }
  if (typeof params.limit === "number" && params.limit > 0) {
    requestParams.limit = params.limit;
  }

  const response = await apiClient.get("/api/v1/firebase/chat-contacts", {
    params: requestParams,
  });

  return parseApiResponse(response.data, ChatContactProfileListSchema);
}
