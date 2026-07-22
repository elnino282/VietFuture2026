export type AiChatSource = {
  file_name: string;
  heading: string;
  page?: number;
  snippet?: string;
};

export type AiChatRequest = {
  message: string;
  top_k: number;
};

export type AiChatResponse = {
  answer: string;
  sources: AiChatSource[];
};

export const OFFLINE_AI_SERVICE_MESSAGE =
  'Không thể kết nối AI Chatbox. Vui lòng kiểm tra AI_CHATBOX đã chạy ở port 8000 chưa.';

export const EMPTY_AI_RESPONSE_MESSAGE =
  'AI chưa trả về nội dung phù hợp. Vui lòng thử hỏi lại rõ hơn.';

const AI_CHAT_ENDPOINT = '/ai-api/v1/ai/chat';
const DEFAULT_TOP_K = 4;
export const AI_CHAT_TIMEOUT_MS = 45000;

function isRecord(value: unknown): value is Record<string, unknown> {
  return typeof value === 'object' && value !== null && !Array.isArray(value);
}

function normalizeString(value: unknown): string {
  return typeof value === 'string' ? value.trim() : '';
}

function normalizePage(value: unknown): number | undefined {
  if (typeof value === 'number' && Number.isFinite(value) && value > 0) {
    return value;
  }

  if (typeof value === 'string') {
    const parsed = Number.parseInt(value, 10);
    if (Number.isFinite(parsed) && parsed > 0) {
      return parsed;
    }
  }

  return undefined;
}

function normalizeSource(value: unknown): AiChatSource | null {
  if (!isRecord(value)) {
    return null;
  }

  const source: AiChatSource = {
    file_name: normalizeString(value.file_name),
    heading: normalizeString(value.heading),
  };
  const page = normalizePage(value.page);
  const snippet = normalizeString(value.snippet);

  if (page !== undefined) {
    source.page = page;
  }
  if (snippet) {
    source.snippet = snippet;
  }

  if (!source.file_name && !source.heading && page === undefined && !snippet) {
    return null;
  }

  return source;
}

function normalizeResponse(payload: unknown): AiChatResponse {
  if (!isRecord(payload)) {
    return {
      answer: EMPTY_AI_RESPONSE_MESSAGE,
      sources: [],
    };
  }

  const answer = normalizeString(payload.answer) || EMPTY_AI_RESPONSE_MESSAGE;
  const rawSources = Array.isArray(payload.sources) ? payload.sources : [];

  return {
    answer,
    sources: rawSources
      .map(normalizeSource)
      .filter((source): source is AiChatSource => source !== null),
  };
}

export async function sendAiChatMessage(
  message: string,
  topK: number = DEFAULT_TOP_K,
): Promise<AiChatResponse> {
  const trimmedMessage = message.trim();

  if (!trimmedMessage) {
    return {
      answer: EMPTY_AI_RESPONSE_MESSAGE,
      sources: [],
    };
  }

  const controller = new AbortController();
  const timeoutId = window.setTimeout(() => controller.abort(), AI_CHAT_TIMEOUT_MS);
  const request: AiChatRequest = {
    message: trimmedMessage,
    top_k: topK,
  };

  try {
    const response = await fetch(AI_CHAT_ENDPOINT, {
      method: 'POST',
      headers: { 'Content-Type': 'application/json' },
      body: JSON.stringify(request),
      signal: controller.signal,
    });

    if (!response.ok) {
      throw new Error(OFFLINE_AI_SERVICE_MESSAGE);
    }

    const payload = await response.json().catch(() => null);
    return normalizeResponse(payload);
  } catch {
    throw new Error(OFFLINE_AI_SERVICE_MESSAGE);
  } finally {
    window.clearTimeout(timeoutId);
  }
}
