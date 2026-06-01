import { createContext, useContext } from 'react';

export type BuyerAiAssistantOpenInput = {
  context?: string;
  prompt?: string;
};

type BuyerAiAssistantContextValue = {
  openAssistant: (input?: BuyerAiAssistantOpenInput) => void;
};

export const BuyerAiAssistantContext = createContext<BuyerAiAssistantContextValue>({
  openAssistant: () => undefined,
});

export function useBuyerAiAssistant() {
  return useContext(BuyerAiAssistantContext);
}
