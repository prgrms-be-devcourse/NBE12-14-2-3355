import type { FollowUserResponseDto } from "@/features/follows/types";
import styles from "./follow-list.module.css";

type Props = {
  user: FollowUserResponseDto;
  pending: boolean;
  disabled: boolean;
  onClick: () => void;
};

export default function FollowButton({ user, pending, disabled, onClick }: Props) {
  return <button
    type="button"
    className={`${styles.followButton} ${user.followedByMe ? styles.following : ""}`}
    disabled={disabled}
    aria-label={`${user.nickname}님 ${user.followedByMe ? "언팔로우" : "팔로우"}`}
    onClick={onClick}
  >
    {pending
      ? "처리 중…"
      : user.followedByMe
        ? <><span className={styles.followingLabel}>Following</span><span className={styles.unfollowLabel}>Unfollow</span></>
        : "Follow"}
  </button>;
}
