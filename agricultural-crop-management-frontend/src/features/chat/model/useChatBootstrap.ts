import { useEffect, useState } from "react";
import { signInWithCustomToken, type User } from "firebase/auth";
import { useAuth } from "@/features/auth/context/AuthContext";
import {
  firebaseAuth,
  getFirebaseChatDisabledReason,
  isFirebaseChatEnabled,
} from "@/shared/lib/firebase/firebaseApp";
import { requestFirebaseChatToken } from "../api/firebaseChatTokenApi";

export type ChatBootstrapState =
  | {
    status: "disabled";
    user: null;
    appUid: null;
    role: null;
    error: string;
  }
  | {
    status: "loading";
    user: null;
    appUid: null;
    role: null;
    error: null;
  }
  | {
    status: "ready";
    user: User;
    appUid: string;
    role: string;
    error: null;
  }
  | {
    status: "error";
    user: null;
    appUid: null;
    role: null;
    error: string;
  };

export function useChatBootstrap(): ChatBootstrapState {
  const { isAuthenticated, user } = useAuth();
  const [state, setState] = useState<ChatBootstrapState>({
    status: "loading",
    user: null,
    appUid: null,
    role: null,
    error: null,
  });

  useEffect(() => {
    let cancelled = false;

    async function bootstrap() {
      if (!isAuthenticated || typeof user?.id !== "number") {
        setState({
          status: "error",
          user: null,
          appUid: null,
          role: null,
          error: "Authenticated user is required to bootstrap chat.",
        });
        return;
      }

      if (!isFirebaseChatEnabled || !firebaseAuth) {
        setState({
          status: "disabled",
          user: null,
          appUid: null,
          role: null,
          error: getFirebaseChatDisabledReason(),
        });
        return;
      }

      try {
        setState({
          status: "loading",
          user: null,
          appUid: null,
          role: null,
          error: null,
        });

        const tokenResponse = await requestFirebaseChatToken();
        const expectedUid = `u_${user.id}`;
        if (tokenResponse.appUid !== expectedUid) {
          throw new Error(
            `Firebase UID mapping mismatch. Expected ${expectedUid}, received ${tokenResponse.appUid}.`
          );
        }

        const credential =
          firebaseAuth.currentUser?.uid === tokenResponse.appUid
            ? { user: firebaseAuth.currentUser }
            : await signInWithCustomToken(firebaseAuth, tokenResponse.customToken);
        const firebaseUid = credential.user?.uid;

        if (!firebaseUid) {
          throw new Error("Firebase sign-in completed without an authenticated UID.");
        }

        if (firebaseUid !== tokenResponse.appUid) {
          throw new Error(
            `Firebase UID mismatch after sign-in. Expected ${tokenResponse.appUid}, received ${firebaseUid}.`
          );
        }

        if (!cancelled) {
          setState({
            status: "ready",
            user: credential.user,
            appUid: firebaseUid,
            role: tokenResponse.role,
            error: null,
          });
        }
      } catch (error) {
        if (!cancelled) {
          setState({
            status: "error",
            user: null,
            appUid: null,
            role: null,
            error: error instanceof Error ? error.message : "Failed to bootstrap chat.",
          });
        }
      }
    }

    void bootstrap();

    return () => {
      cancelled = true;
    };
  }, [isAuthenticated, user?.id]);

  return state;
}
