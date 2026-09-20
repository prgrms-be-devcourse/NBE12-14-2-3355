import type { Metadata } from "next";
import SignupClient from "./signup-client";

export const metadata: Metadata = { title: "회원가입 | GameLog" };

export default function SignupPage() {
  return <SignupClient />;
}
