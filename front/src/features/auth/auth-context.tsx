"use client";

import { createContext, useCallback, useContext, useEffect, useRef, useState } from "react";
import * as authApi from "./api";
import { AuthApiError } from "./api";
import type { LoginRequestBody, SignupRequestBody, UserDto } from "./types";

type AuthStatus = "loading" | "authenticated" | "unauthenticated";

type AuthContextValue = {
  status: AuthStatus;
  user: UserDto | null;
  accessToken: string | null;
  login: (body: LoginRequestBody) => Promise<UserDto>;
  signup: (body: SignupRequestBody) => Promise<UserDto>;
  logout: () => Promise<void>;
  refresh: () => Promise<string | null>;
  setUser: (user: UserDto) => void;
};

const AuthContext = createContext<AuthContextValue | null>(null);

export function AuthProvider({ children }: { children: React.ReactNode }) {
  const [status, setStatus] = useState<AuthStatus>("loading");
  const [user, setUserState] = useState<UserDto | null>(null);
  const [accessToken, setAccessToken] = useState<string | null>(null);
  const refreshInFlight = useRef<Promise<string | null> | null>(null);

  const refresh = useCallback((): Promise<string | null> => {
    if (refreshInFlight.current) return refreshInFlight.current;

    const attempt = (async () => {
      try {
        const { accessToken: newToken } = await authApi.refresh();
        const me = await authApi.getMe(newToken);
        setAccessToken(newToken);
        setUserState(me);
        setStatus("authenticated");
        return newToken;
      } catch {
        setAccessToken(null);
        setUserState(null);
        setStatus("unauthenticated");
        return null;
      } finally {
        refreshInFlight.current = null;
      }
    })();

    refreshInFlight.current = attempt;
    return attempt;
  }, []);

  useEffect(() => {
    void refresh();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  const login = useCallback(async (body: LoginRequestBody) => {
    const result = await authApi.login(body);
    setAccessToken(result.accessToken);
    setUserState(result.user);
    setStatus("authenticated");
    return result.user;
  }, []);

  const signup = useCallback(async (body: SignupRequestBody) => {
    // 백엔드가 자동 로그인을 시켜주지 않으므로 user/accessToken/status는 건드리지 않는다.
    return authApi.signup(body);
  }, []);

  const logout = useCallback(async () => {
    try {
      await authApi.logout();
    } catch {
      // 네트워크 실패해도 프론트 상태는 항상 로그아웃으로 정리한다.
    } finally {
      setAccessToken(null);
      setUserState(null);
      setStatus("unauthenticated");
    }
  }, []);

  const setUser = useCallback((next: UserDto) => {
    setUserState(next);
  }, []);

  return (
    <AuthContext.Provider value={{ status, user, accessToken, login, signup, logout, refresh, setUser }}>
      {children}
    </AuthContext.Provider>
  );
}

export function useAuth(): AuthContextValue {
  const ctx = useContext(AuthContext);
  if (!ctx) throw new Error("useAuth must be used within AuthProvider");
  return ctx;
}

export { AuthApiError };
