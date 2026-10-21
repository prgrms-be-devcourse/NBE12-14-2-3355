"use client";

import { useState } from "react";
import type { UserDto } from "@/features/auth/types";
import PasswordChangeDialog from "./password-change-dialog";
import WithdrawDialog from "./withdraw-dialog";
import styles from "./profile.module.css";

// 프로필 Settings 탭: 계정 정보, 비밀번호 변경, 회원탈퇴
export default function AccountSettings({ user, accessToken }: { user: UserDto; accessToken: string }) {
  const [changingPassword, setChangingPassword] = useState(false);
  const [withdrawing, setWithdrawing] = useState(false);

  return <section aria-labelledby="account-settings-title">
    <h2 id="account-settings-title" className={styles.contentTitle}>계정 설정</h2>

    <div className={styles.settingsRow}>
      <div>
        <p className={styles.settingsLabel}>이메일</p>
        <p className={styles.settingsValue}>{user.email}</p>
      </div>
    </div>

    <div className={styles.settingsRow}>
      <div>
        <p className={styles.settingsLabel}>비밀번호</p>
        <p className={styles.settingsHint}>변경하면 다른 기기에서는 로그아웃됩니다.</p>
      </div>
      <button type="button" className={styles.profileEditButton} onClick={() => setChangingPassword(true)}>비밀번호 변경</button>
    </div>

    <div className={styles.settingsRow}>
      <div>
        <p className={styles.settingsLabel}>회원탈퇴</p>
        <p className={styles.settingsHint}>
          {user.role === "ADMIN" ? "관리자 계정은 탈퇴할 수 없습니다." : "탈퇴하면 작성한 리뷰와 기록을 복구할 수 없습니다."}
        </p>
      </div>
      {user.role !== "ADMIN" && <button type="button" className={`${styles.profileEditButton} ${styles.profileLogoutButton}`} onClick={() => setWithdrawing(true)}>탈퇴하기</button>}
    </div>

    {changingPassword && <PasswordChangeDialog accessToken={accessToken} onClose={() => setChangingPassword(false)} />}
    {withdrawing && <WithdrawDialog accessToken={accessToken} onClose={() => setWithdrawing(false)} />}
  </section>;
}
