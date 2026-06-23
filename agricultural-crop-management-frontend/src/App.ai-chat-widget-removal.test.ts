import { readFileSync } from 'node:fs';
import { describe, expect, it } from 'vitest';

describe('App AI entry points', () => {
  it('does not mount the duplicate standalone ACM AI widget', () => {
    const appSource = readFileSync('src/App.tsx', 'utf8');

    expect(appSource).not.toContain('AiChatWidget');
    expect(appSource).not.toContain('@/features/ai-chat');
  });
});
