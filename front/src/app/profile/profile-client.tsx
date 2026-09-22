"use client";

import { useEffect, useRef, useState, type ChangeEvent } from "react";
import { useRouter } from "next/navigation";
import Link from "next/link";
import { useAuth } from "@/features/auth/auth-context";
import { checkNicknameDuplicate, updateProfile, uploadProfileImage } from "@/features/auth/api";
import type { UserDto } from "@/features/auth/types";
import AuthNav from "@/components/auth/auth-nav";

import { getProfile, updateFavoriteGames } from "@/features/profile/api";
import type { FavoriteGame, GenreDistribution, ProfileResponse, ProfileStats, RecentGame, RecentReview, ScatterGame, TasteResponse } from "@/features/profile/types";
import { coverUrl, type Game} from "@/lib/games";

import styles from "./profile-client.module.css";

type FieldStatus = "idle" | "checking" | "available" | "duplicate" | "error";

const GENRE_COLORS = [
  "#B7D77A",
  "#7FB069",
  "#D8A657",
  "#6FA8A8",
  "#A98BC4",
  "#D27A7A",
  "#8CA6C0",
  "#C59B6D",
  "#86B7A3",
  "#B88AA8",
];

/* =========================================================
 * 공통 유틸
 * ========================================================= */

function formatHours(value: number | null | undefined) {
  if (value == null) return "0시간";

  if (value % 1 === 0) {
    return `${value.toFixed(0)}시간`;
  }

  return `${value.toFixed(1)}시간`;
}

function formatPercent(value: number) {
  return `${(value * 100).toFixed(0)}%`;
}

function formatDate(value: string) {
  const date = new Date(value);

  if (Number.isNaN(date.getTime())) {
    return value;
  }

  return date.toLocaleDateString("ko-KR", {
    year: "numeric",
    month: "short",
    day: "numeric",
  });
}

function renderStars(rating: number | null) {
  if (rating == null) return "☆☆☆☆☆";

  const rounded = Math.round(rating);

  return "★".repeat(rounded) + "☆".repeat(5 - rounded);
}

/* =========================================================
 * 게임 이미지
 * ========================================================= */

function GameCover({
  src,
  alt,
  className,
}: {
  src: string | null | undefined;
  alt: string;
  className?: string;
}) {
  const [broken, setBroken] = useState(false);

  const url = coverUrl(src ?? null);

  if (!url || broken) {
    return (
      <div
        className={`${styles.gameCoverFallback} ${
          className ?? ""
        }`}
      >
        <span aria-hidden="true">🎮</span>
      </div>
    );
  }

  return (
    // eslint-disable-next-line @next/next/no-img-element
    <img
      className={className}
      src={url}
      alt={alt}
      loading="lazy"
      onError={() => setBroken(true)}
    />
  );
}

function ProfileSidebar({
  user,
  accessToken,
  onSaved,
}: {
  user: UserDto;
  accessToken: string;
  onSaved: (user: UserDto) => void;
}) {
  const [nickname, setNickname] = useState(user.nickname);
  const [bio, setBio] = useState(user.bio ?? "");
  const [profileImageUrl, setProfileImageUrl] = useState(user.profileImageUrl ?? "");

  const [editingNickname, setEditingNickname] = useState(false);
  const [nicknameDraft, setNicknameDraft] = useState(user.nickname);
  const [nicknameStatus, setNicknameStatus] = useState<FieldStatus>("idle");

  const [editingBio, setEditingBio] = useState(false);
  const [bioDraft, setBioDraft] = useState("");

  const [uploadingImage, setUploadingImage] = useState(false);
  const fileInputRef = useRef<HTMLInputElement>(null);

  const [saving, setSaving] = useState(false);
  const [error, setError] = useState("");
  const [message, setMessage] = useState("");

  async function persist(next: { nickname: string; bio: string; profileImageUrl: string }) {
    setError("");
    setMessage("");
    setSaving(true);
    try {
      const updated = await updateProfile(
        { nickname: next.nickname, bio: next.bio || null, profileImageUrl: next.profileImageUrl || null },
        accessToken,
      );
      onSaved(updated);
      setMessage("저장했어요.");
      return true;
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "저장하지 못했어요.");
      return false;
    } finally {
      setSaving(false);
    }
  }

  function openNicknameEdit() {
    setNicknameDraft(nickname);
    setNicknameStatus("idle");
    setError("");
    setEditingNickname(true);
  }

  async function checkNickname() {
    const value = nicknameDraft.trim();
    if (!value) { setError("닉네임을 입력해 주세요."); return; }
    setError("");
    setNicknameStatus("checking");
    try {
      const duplicate = await checkNicknameDuplicate(value);
      setNicknameStatus(duplicate ? "duplicate" : "available");
    } catch {
      setNicknameStatus("error");
    }
  }

  async function saveNickname() {
    const value = nicknameDraft.trim();
    if (!value) { setError("닉네임을 입력해 주세요."); return; }
    const changed = value !== nickname;
    if (changed && nicknameStatus !== "available") { setError("변경한 닉네임은 중복확인을 먼저 해주세요."); return; }
    const ok = await persist({ nickname: value, bio, profileImageUrl });
    if (ok) { setNickname(value); setEditingNickname(false); }
  }

  function openBioEdit() { setBioDraft(bio); setError(""); setEditingBio(true); }
  async function saveBio() {
    const ok = await persist({ nickname, bio: bioDraft.trim(), profileImageUrl });
    if (ok) { setBio(bioDraft.trim()); setEditingBio(false); }
  }

  function openImagePicker() {
    setError("");
    fileInputRef.current?.click();
  }

  async function handleImageSelected(event: ChangeEvent<HTMLInputElement>) {
    const file = event.target.files?.[0];
    event.target.value = "";
    if (!file) return;

    setError("");
    setMessage("");
    setUploadingImage(true);
    try {
      const url = await uploadProfileImage(file, accessToken);
      const ok = await persist({ nickname, bio, profileImageUrl: url });
      if (ok) setProfileImageUrl(url);
    } catch (reason) {
      setError(reason instanceof Error ? reason.message : "이미지 업로드에 실패했어요.");
    } finally {
      setUploadingImage(false);
    }
  }

  const anyEditing = editingNickname || editingBio;

  return (
    <aside className={styles.sidebar}>
      <div className={styles.avatarBlock}>
        {profileImageUrl
          // eslint-disable-next-line @next/next/no-img-element
          ? <img className={styles.avatar} src={profileImageUrl} alt="" />
          : <div className={styles.avatarFallback}>{nickname.slice(0, 1).toUpperCase()}</div>}
        <input
          ref={fileInputRef}
          type="file"
          accept="image/*"
          style={{ display: "none" }}
          onChange={handleImageSelected}
        />
        <button type="button" className={styles.linkBtn} onClick={openImagePicker} disabled={uploadingImage}>
          {uploadingImage ? "업로드 중…" : "이미지 변경"}
        </button>
      </div>

      <div className={styles.block}>
        {editingNickname ? (
          <div className={styles.inlineEdit}>
            <div className={styles.checkRow}>
              <input
                value={nicknameDraft}
                onChange={(event) => { setNicknameDraft(event.target.value); setNicknameStatus("idle"); }}
                autoComplete="nickname"
              />
              <button
                type="button"
                className={styles.checkButton}
                onClick={checkNickname}
                disabled={nicknameDraft.trim() === nickname || nicknameStatus === "checking"}
              >중복확인</button>
            </div>
            {nicknameDraft.trim() !== nickname && nicknameStatus === "checking" && <small className={styles.hint}>확인 중…</small>}
            {nicknameDraft.trim() !== nickname && nicknameStatus === "available" && <small className={styles.hintOk}>사용 가능한 닉네임입니다.</small>}
            {nicknameDraft.trim() !== nickname && nicknameStatus === "duplicate" && <small className={styles.hintError}>이미 사용 중인 닉네임입니다.</small>}
            <div className={styles.inlineActions}>
              <button type="button" onClick={() => setEditingNickname(false)} disabled={saving}>취소</button>
              <button type="button" className={styles.primaryBtn} onClick={saveNickname} disabled={saving}>저장</button>
            </div>
          </div>
        ) : (
          <h1 className={styles.nickname}>{nickname} <button type="button" className={styles.linkBtn} onClick={openNicknameEdit}>변경</button></h1>
        )}
        <span className={styles.email}>{user.email}</span>
      </div>

      <div className={styles.block}>
        <span className={styles.sectionLabel}>Bio</span>
        {editingBio ? (
          <div className={styles.inlineEdit}>
            <textarea rows={3} value={bioDraft} onChange={(event) => setBioDraft(event.target.value)} placeholder="나를 한 줄로 소개해 보세요" />
            <div className={styles.inlineActions}>
              <button type="button" onClick={() => setEditingBio(false)} disabled={saving}>취소</button>
              <button type="button" className={styles.primaryBtn} onClick={saveBio} disabled={saving}>저장</button>
            </div>
          </div>
        ) : bio ? (
          <p className={styles.bioText}>{bio} <button type="button" className={styles.linkBtn} onClick={openBioEdit}>수정</button></p>
        ) : (
          <p className={styles.bioEmpty}>아직 없어요 <button type="button" className={styles.linkBtn} onClick={openBioEdit}>+ 추가</button></p>
        )}
      </div>

      {error && <p className={styles.error} role="alert">{error}</p>}
      {message && !anyEditing && <p className={styles.success} role="status">{message}</p>}
    </aside>
  );
}

/* =========================================================
 * 활동 통계
 * ========================================================= */

function Stats({ stats }: { stats: ProfileStats }) {
  return (
    <section className={styles.statsSection}>
      <div className={styles.sectionHeading}>
        <h3>게임 활동</h3>
      </div>

      <div className={styles.statsGrid}>
        <div className={styles.statCard}>
          <span className={styles.statLabel}>
            플레이한 게임
          </span>

          <strong className={styles.statValue}>
            {stats.playedGameCount}
          </strong>

          <span className={styles.statUnit}>
            games
          </span>
        </div>

        <div className={styles.statCard}>
          <span className={styles.statLabel}>
            평균 평점
          </span>

          <strong className={styles.statValue}>
            {stats.averageRating == null
              ? "-"
              : stats.averageRating.toFixed(1)}
          </strong>

          <span className={styles.statUnit}>
            / 5.0
          </span>
        </div>

        <div className={styles.statCard}>
          <span className={styles.statLabel}>
            총 플레이 시간
          </span>

          <strong className={styles.statValue}>
            {stats.totalPlayTime.toFixed(1)}
          </strong>

          <span className={styles.statUnit}>
            hours
          </span>
        </div>
      </div>
    </section>
  );
}

/* =========================================================
 * 인생게임
 * ========================================================= */
function FavoriteGameSearchModal({
  favorites,
  onAdd,
  onClose,
}: {
  favorites: FavoriteGame[];
  onAdd: (game: Game) => void;
  onClose: () => void;
}) {
  const router = useRouter();

  const [keyword, setKeyword] = useState("");
  const [games, setGames] = useState<Game[]>([]);
  const [loading, setLoading] = useState(false);
  const [error, setError] = useState("");

  useEffect(() => {
    const value = keyword.trim();

    if (!value) {
      setGames([]);
      setError("");
      return;
    }

    const controller = new AbortController();

    const timer = setTimeout(async () => {
      try {
        setLoading(true);
        setError("");

        const query = new URLSearchParams({
          keyword: value,
        });

        const response = await fetch(
          `/api/games/suggestions?${query}`,
          {
            signal: controller.signal,
          },
        );

        const body = await response.json();

        if (!response.ok) {
          throw new Error(
            body.msg ||
              "게임 검색에 실패했습니다.",
          );
        }

        setGames(body.data ?? []);
      } catch (reason) {
        if (
          reason instanceof Error &&
          reason.name === "AbortError"
        ) {
          return;
        }

        setGames([]);
        setError(
          reason instanceof Error
            ? reason.message
            : "게임 검색에 실패했습니다.",
        );
      } finally {
        if (!controller.signal.aborted) {
          setLoading(false);
        }
      }
    }, 300);

    return () => {
      clearTimeout(timer);
      controller.abort();
    };
  }, [keyword]);

  const favoriteIds = new Set(
    favorites.map((game) => game.gameId),
  );

  function handleAdd(game: Game) {
    if (favoriteIds.has(game.id)) {
      return;
    }

    onAdd(game);
  }

  return (
    <div
      className={styles.modalBackdrop}
      onMouseDown={(event) => {
        if (event.target === event.currentTarget) {
          onClose();
        }
      }}
    >
      <div
        className={styles.favoriteModal}
        role="dialog"
        aria-modal="true"
        aria-labelledby="favorite-modal-title"
      >
        <div className={styles.modalHeader}>
          <div>
            <span className={styles.modalEyebrow}>
              ADD FAVORITE
            </span>

            <h3 id="favorite-modal-title">
              인생게임 추가
            </h3>

            <p>
              게임을 검색해서 인생게임으로
              추가해보세요.
            </p>
          </div>

          <button
            type="button"
            className={styles.modalClose}
            onClick={onClose}
            aria-label="닫기"
          >
            ×
          </button>
        </div>

        <div className={styles.favoriteSearch}>
          <span aria-hidden="true">⌕</span>

          <input
            autoFocus
            value={keyword}
            onChange={(event) =>
              setKeyword(event.target.value)
            }
            placeholder="게임 제목을 검색해보세요"
          />
        </div>

        <div className={styles.favoriteSearchResult}>
          {!keyword.trim() ? (
            <div className={styles.modalEmpty}>
              추가하고 싶은 게임을 검색해보세요.
            </div>
          ) : loading ? (
            <div className={styles.modalEmpty}>
              검색 중…
            </div>
          ) : error ? (
            <div className={styles.modalError}>
              {error}
            </div>
          ) : !games.length ? (
            <div className={styles.modalEmpty}>
              검색 결과가 없어요.
            </div>
          ) : (
            <div className={styles.searchGameList}>
              {games.map((game) => {
                const alreadyFavorite =
                  favoriteIds.has(game.id);

                return (
                  <div
                    key={game.id}
                    className={styles.searchGameItem}
                  >
                    <button
                      type="button"
                      className={styles.searchGameInfo}
                      onClick={() =>
                        router.push(
                          `/games/${game.id}`,
                        )
                      }
                    >
                      <GameCover
                        src={game.coverImageUrl}
                        alt={game.title}
                        className={
                          styles.searchGameCover
                        }
                      />

                      <span>
                        <strong>
                          {game.title}
                        </strong>

                        <small>
                          {game.releaseDate?.slice(
                            0,
                            4,
                          ) ||
                            "출시일 미정"}
                        </small>
                      </span>
                    </button>

                    <button
                      type="button"
                      className={
                        styles.searchGameAdd
                      }
                      disabled={alreadyFavorite}
                      onClick={() =>
                        handleAdd(game)
                      }
                    >
                      {alreadyFavorite
                        ? "등록됨"
                        : "추가"}
                    </button>
                  </div>
                );
              })}
            </div>
          )}
        </div>

        <p className={styles.modalNotice}>
          ※ 내 라이브러리에 등록된 게임만
          인생게임으로 저장할 수 있어요.
        </p>
      </div>
    </div>
  );
}

function FavoriteGames({
  favorites,
  onSave,
  accessToken,
}: {
  favorites: FavoriteGame[];
  onSave: (gameIds: number[]) => Promise<void>;
  accessToken: string;
}) {
  const [editing, setEditing] = useState(false);
  const [showAddModal, setShowAddModal] =
    useState(false);

  const [draftFavorites, setDraftFavorites] =
    useState<FavoriteGame[]>(favorites);

  const [saving, setSaving] = useState(false);

  useEffect(() => {
    setDraftFavorites(favorites);
  }, [favorites]);

  async function handleSave() {
    try {
      setSaving(true);

      await onSave(
        draftFavorites.map(
          (game) => game.gameId,
        ),
      );

      setEditing(false);
    } finally {
      setSaving(false);
    }
  }


  function handleAddGame(game: Game) {
    if (draftFavorites.length >= 5) {
      alert(
        "인생게임은 최대 5개까지 등록할 수 있습니다.",
      );
      return;
    }

    if (
      draftFavorites.some(
        (item) => item.gameId === game.id,
      )
    ) {
      return;
    }

    setDraftFavorites((prev) => [
      ...prev,
      {
        gameId: game.id,
        title: game.title,
        coverImageUrl: game.coverImageUrl,
        displayOrder: prev.length + 1,
      },
    ]);
  }

  function handleDragStart(
    event: React.DragEvent<HTMLDivElement>,
    gameId: number,
  ) {
    event.dataTransfer.effectAllowed = "move";
    event.dataTransfer.setData(
      "text/plain",
      String(gameId),
    );
  }

  function handleDragOver(
    event: React.DragEvent<HTMLDivElement>,
  ) {
    event.preventDefault();
    event.dataTransfer.dropEffect = "move";
  }

  function handleDrop(
    event: React.DragEvent<HTMLDivElement>,
    targetGameId: number,
  ) {
    event.preventDefault();

    const draggedGameId = Number(
      event.dataTransfer.getData("text/plain"),
    );

    if (
      !draggedGameId ||
      draggedGameId === targetGameId
    ) {
      return;
    }

    setDraftFavorites((prev) => {
      const fromIndex = prev.findIndex(
        (game) => game.gameId === draggedGameId,
      );

      const toIndex = prev.findIndex(
        (game) => game.gameId === targetGameId,
      );

      if (fromIndex === -1 || toIndex === -1) {
        return prev;
      }

      const next = [...prev];

      const [moved] = next.splice(fromIndex, 1);

      next.splice(toIndex, 0, moved);

      return next.map((game, index) => ({
        ...game,
        displayOrder: index + 1,
      }));
    });
  }

  const displayGames = editing
    ? draftFavorites
    : favorites;

  return (
    <>
      <section className={styles.favoriteSection}>
        <div className={styles.sectionHeading}>
          <div>
            <h3>인생게임</h3>

            <p>
              내가 가장 좋아하는 게임을
              전시해보세요.
            </p>
          </div>

          <div className={styles.favoriteHeaderActions}>
            {editing && (
              <button
                type="button"
                className={styles.favoriteAddButton}
                onClick={() =>
                  setShowAddModal(true)
                }
                disabled={
                  draftFavorites.length >= 5
                }
              >
                + 게임 추가
              </button>
            )}

            {!editing ? (
              <button
                type="button"
                className={
                  styles.favoriteEditButton
                }
                onClick={() => {
                  setDraftFavorites(
                    favorites,
                  );
                  setEditing(true);
                }}
              >
                수정하기
              </button>
            ) : (
              <div
                className={
                  styles.favoriteActions
                }
              >
                <button
                  type="button"
                  onClick={() => {
                    setDraftFavorites(
                      favorites,
                    );
                    setEditing(false);
                  }}
                >
                  취소
                </button>

                <button
                  type="button"
                  className={
                    styles.favoriteSaveButton
                  }
                  disabled={saving}
                  onClick={handleSave}
                >
                  {saving
                    ? "저장 중..."
                    : "저장"}
                </button>
              </div>
            )}
          </div>
        </div>

        {displayGames.length === 0 ? (
          <div className={styles.emptyBox}>
            인생 게임을 전시해보세요!
          </div>
        ) : (
          <div className={styles.favoriteGrid}>
            {displayGames.map((game) => (
              <div
                key={game.gameId}
                className={styles.favoriteCard}
                draggable={editing}
                onDragStart={(event) =>
                  handleDragStart(event, game.gameId)
                }
                onDragOver={handleDragOver}
                onDrop={(event) =>
                  handleDrop(event, game.gameId)
                }
              >
                <Link
                  href={`/games/${game.gameId}`}
                  className={
                    styles.favoriteLink
                  }
                  draggable={false}
                  aria-label={`${game.title} 상세 페이지`}
                >
                  <GameCover
                    src={game.coverImageUrl}
                    alt={game.title}
                  />

                  <span
                    className={
                      styles.favoriteTitle
                    }
                  >
                    {game.title}
                  </span>
                </Link>

                {editing && (
                  <button
                    type="button"
                    className={
                      styles.favoriteRemove
                    }
                    aria-label={`${game.title} 인생게임에서 제거`}
                    onClick={() => {
                      setDraftFavorites(
                        (prev) =>
                          prev.filter(
                            (item) =>
                              item.gameId !==
                              game.gameId,
                          ),
                      );
                    }}
                  >
                    ×
                  </button>
                )}
              </div>
            ))}
          </div>
        )}
      </section>

      {showAddModal && (
        <FavoriteGameSearchModal
          favorites={draftFavorites}
          onAdd={handleAddGame}
          onClose={() =>
            setShowAddModal(false)
          }
        />
      )}
    </>
  );
}

/* =========================================================
 * 산점도
 * ========================================================= */

function ScatterPlot({
  data,
  averagePlayTime,
}: {
  data: ScatterGame[];
  averagePlayTime: number;
}) {
  const [activeGameId, setActiveGameId] =
  useState<number | null>(null);
  const router = useRouter();

  const width = 760;
  const height = 500;

  const padding = {
    top: 65,
    right: 50,
    bottom: 65,
    left: 60,
  };

  const chartWidth =
    width - padding.left - padding.right;

  const chartHeight =
    height - padding.top - padding.bottom;

  const maxPlayTime = Math.max(
    ...data.map((game) => game.playTime),
    averagePlayTime,
    1,
  );

  const xMax = Math.max(
    maxPlayTime * 1.15,
    averagePlayTime * 1.5,
    1,
  );

  const x = (playTime: number) =>
    padding.left +
    (playTime / xMax) * chartWidth;

  const y = (rating: number) =>
    padding.top +
    ((5 - rating) / 5) * chartHeight;

  const averageX = x(averagePlayTime);
  const middleY = y(2.5);

  if (!data.length) {
    return (
      <section className={styles.scatterSection}>
        <div className={styles.sectionHeading}>
          <h3>게임 기록</h3>
        </div>

        <div className={styles.emptyBox}>
          게임을 기록해보세요!
        </div>
      </section>
    );
  }

  return (
    <section className={styles.scatterSection}>
      <div className={styles.sectionHeading}>
        <div>
          <h3>게임 기록</h3>
          <p>
            플레이 시간과 평점으로 보는 나의 게임 기록
          </p>
        </div>
      </div>

      <div className={styles.scatterChart}>
        <div className={styles.quadrantLabels}>
          <div className={styles.quadrantLabel}>
            <strong>Hidden Gems</strong>
            <span>
              짧게 즐겼지만 만족도는 높았던 게임
            </span>
          </div>

          <div
            className={`${styles.quadrantLabel} ${styles.quadrantRight}`}
          >
            <strong>My Favorites</strong>
            <span>
              오래 플레이했고 만족도도 높은 게임
            </span>
          </div>
        </div>

        <svg
          viewBox={`0 0 ${width} ${height}`}
          className={styles.scatterSvg}
          role="img"
          aria-label="플레이 시간과 평점 산점도"
        >
          {/* 세로 기준선 */}
          <line
            x1={averageX}
            y1={padding.top}
            x2={averageX}
            y2={height - padding.bottom}
            className={styles.scatterAxis}
          />

          {/* 가로 기준선 */}
          <line
            x1={padding.left}
            y1={middleY}
            x2={width - padding.right}
            y2={middleY}
            className={styles.scatterAxis}
          />

          {/* Y축 */}
          <line
            x1={padding.left}
            y1={padding.top}
            x2={padding.left}
            y2={height - padding.bottom}
            className={styles.scatterMainAxis}
          />

          {/* X축 */}
          <line
            x1={padding.left}
            y1={height - padding.bottom}
            x2={width - padding.right}
            y2={height - padding.bottom}
            className={styles.scatterMainAxis}
          />

          {/* Y축 숫자 */}
          {[0, 1, 2, 3, 4, 5].map(
            (rating) => (
              <text
                key={rating}
                x={padding.left - 12}
                y={y(rating)}
                textAnchor="end"
                dominantBaseline="middle"
                className={styles.scatterTick}
              >
                {rating}
              </text>
            ),
          )}

          {/* X축 */}
          <text
            x={padding.left}
            y={height - 25}
            className={styles.scatterTick}
          >
            0h
          </text>

          <text
            x={width - padding.right}
            y={height - 25}
            textAnchor="end"
            className={styles.scatterTick}
          >
            {Math.ceil(xMax)}h
          </text>

          {/* 평균 플레이 시간 */}
          <text
            x={averageX}
            y={height - padding.bottom + 22}
            textAnchor="middle"
            className={styles.averageLabel}
          >
            평균 {averagePlayTime.toFixed(1)}h
          </text>

          {/* 점 */}
          {data.map((game) => {
            const rating = game.rating ?? 0;

            const pointX = x(game.playTime);
            const pointY = y(rating);

            const isActive =
              activeGameId === game.gameId;

            const tooltipWidth = 170;
            const tooltipHeight = 58;

            const tooltipX = Math.min(
              Math.max(pointX + 12, padding.left),
              width - padding.right - tooltipWidth,
            );

            const tooltipY = Math.min(
              Math.max(pointY - tooltipHeight - 12, padding.top),
              height - padding.bottom - tooltipHeight,
            );

            return (
              <g
                key={game.gameId}
                className={styles.scatterPoint}
                onMouseEnter={() =>
                  setActiveGameId(game.gameId)
                }
                onMouseLeave={() =>
                  setActiveGameId(null)
                }
                onClick={() => router.push(`/games/${game.gameId}`)}
              >
                <circle
                  cx={pointX}
                  cy={pointY}
                  r={isActive ? 6 : 4}
                  className={styles.scatterPointBg}
                />

                {isActive && (
                  <g
                    transform={`translate(${tooltipX}, ${tooltipY})`}
                    className={styles.scatterTooltip}
                  >
                    <rect
                      width={tooltipWidth}
                      height={tooltipHeight}
                      rx="6"
                      className={styles.scatterTooltipBox}
                    />

                    <text
                      x="12"
                      y="18"
                      className={styles.scatterTooltipTitle}
                    >
                      {game.title}
                    </text>

                    <text
                      x="12"
                      y="36"
                      className={styles.scatterTooltipText}
                    >
                      플레이 {formatHours(game.playTime)}
                    </text>

                    <text
                      x="12"
                      y="50"
                      className={styles.scatterTooltipText}
                    >
                      평점 {game.rating?.toFixed(1) ?? "-"} / 5.0
                    </text>
                  </g>
                )}
              </g>
            );
          })}
        </svg>
        <div className={styles.quadrantLabels}>
          <div className={styles.quadrantLabel}>
            <strong>Not for Me</strong>
            <span>
              오래하지 않았고 만족도도 낮았던 게임
            </span>
          </div>

          <div
            className={`${styles.quadrantLabel} ${styles.quadrantRight}`}
          >
            <strong>Time Sink</strong>
            <span>
              시간은 많이 썼지만 평가는 낮은 게임
            </span>
          </div>
        </div>
      </div>
    </section>
  );
}

/* =========================================================
 * 게임 취향
 * ========================================================= */

function TasteSection({
  taste,
}: {
  taste: TasteResponse;
}) {
  const metrics = [
    {
      label: "플레이 시간 성향",
      metric: taste.longPlay,
    },
    {
      label: "평점 성향",
      metric: taste.rating,
    },
    {
      label: "완료 성향",
      metric: taste.completion,
    },
  ];

  return (
    <section className={styles.tasteSection}>
      <div className={styles.sectionHeading}>
        <h3>내 게임 취향 한번에 보기</h3>
      </div>

      <div className={styles.tasteList}>
        {metrics.map(({ label, metric }) => (
          <div
            key={label}
            className={styles.tasteRow}
          >
            <div className={styles.tasteHeader}>
              <strong>{label}</strong>
              <span>
                {formatPercent(metric.ratio)}
              </span>
            </div>

            <div className={styles.tasteBar}>
              <div
                className={styles.tasteBarValue}
                style={{
                  width: `${Math.min(
                    metric.ratio * 100,
                    100,
                  )}%`,
                }}
              />
            </div>

            <strong className={styles.tasteMessage}>
              {metric.message}
            </strong>

            <p>{metric.description}</p>
          </div>
        ))}
      </div>
    </section>
  );
}

/* =========================================================
 * 장르 분포
 * ========================================================= */

function GenreDistribution({
  data,
}: {
  data: GenreDistribution[];
}) {
  const total = data.reduce(
    (sum, genre) => sum + genre.ratio,
    0,
  );

  let accumulated = 0;

  const gradient = data
    .map((genre, index) => {
      const start =
        (accumulated / total) * 100;

      accumulated += genre.ratio;

      const end =
        (accumulated / total) * 100;

      return `${GENRE_COLORS[index % GENRE_COLORS.length]} ${start}% ${end}%`;
    })
    .join(", ");

  return (
    <section className={styles.genreSection}>
      <div className={styles.sectionHeading}>
        <h3>장르 분포</h3>
      </div>

      {!data.length ? (
        <div className={styles.emptyBox}>
          아직 장르 데이터가 없어요.
        </div>
      ) : (
        <div className={styles.genreLayout}>
          <div
            className={styles.genreChart}
            style={{
              background: `conic-gradient(${gradient})`,
            }}
          >
            <div
              className={styles.genreChartInner}
            />
          </div>

          <div className={styles.genreLegend}>
            {data.map((genre, index) => (
              <div
                key={genre.genreName}
                className={styles.genreItem}
              >
                <span
                  className={styles.genreDot}
                  style={{
                    backgroundColor:
                      GENRE_COLORS[
                        index %
                          GENRE_COLORS.length
                      ],
                  }}
                />

                <span
                  className={styles.genreName}
                >
                  {genre.genreName}
                </span>

                <span
                  className={
                    styles.genrePercent
                  }
                >
                  {formatPercent(genre.ratio)}
                </span>
              </div>
            ))}
          </div>
        </div>
      )}
    </section>
  );
}

/* =========================================================
 * 최근 플레이
 * ========================================================= */

function RecentGames({
  games,
}: {
  games: RecentGame[];
}) {
  if (!games.length) {
    return null;
  }

  return (
    <section className={styles.recentSection}>
      <div className={styles.sectionHeading}>
        <h3>최근 플레이한 게임</h3>
      </div>

      <div className={styles.recentGames}>
        {games.map((game) => (
          <Link
            key={game.gameId}
            href={`/games/${game.gameId}`}
            className={styles.recentGame}
          >
            <GameCover
              src={game.coverImageUrl}
              alt={game.title}
            />

            <span>{game.title}</span>
          </Link>
        ))}
      </div>
    </section>
  );
}

/* =========================================================
 * 최근 리뷰
 * ========================================================= */

function RecentReviews({
  reviews,
}: {
  reviews: RecentReview[];
}) {
  if (!reviews.length) {
    return null;
  }

  return (
    <section className={styles.recentSection}>
      <div className={styles.sectionHeading}>
        <h3>최근 리뷰</h3>
      </div>

      <div className={styles.reviewList}>
        {reviews.map((review) => (
          <Link
            key={review.reviewId}
            href={`/games/${review.gameId}`}
            className={styles.reviewCard}
          >
            <GameCover
              src={review.gameCoverImageUrl}
              alt={review.gameTitle}
              className={styles.reviewCover}
            />

            <div className={styles.reviewBody}>
              <div
                className={
                  styles.reviewHeader
                }
              >
                <div>
                  <strong>
                    {review.gameTitle}
                  </strong>

                  <span>
                    {review.platform ?? ""}
                  </span>
                </div>

                <div
                  className={
                    styles.reviewRating
                  }
                >
                  <span>
                    {renderStars(review.rating)}
                  </span>

                  <strong>
                    {review.rating?.toFixed(1) ??
                      "-"}
                  </strong>
                </div>
              </div>

              <p>
                {review.content ||
                  "작성한 리뷰가 없습니다."}
              </p>

              <time>
                {formatDate(
                  review.lastModifiedDate,
                )}
              </time>
            </div>
          </Link>
        ))}
      </div>
    </section>
  );
}

export default function ProfileClient() {
  const router = useRouter();
  const auth = useAuth();

  const [profile, setProfile] =
    useState<ProfileResponse | null>(null);

  const [profileLoading, setProfileLoading] =
    useState(true);

  const [profileError, setProfileError] =
    useState("");

  useEffect(() => {
    if (auth.status === "unauthenticated") router.replace("/login?next=/profile");
  }, [auth.status, router]);

  useEffect(() => {
    if (
      auth.status !== "authenticated" ||
      !auth.accessToken
    ) {
      return;
    }

    const loadProfile = async () => {
      try {
        setProfileLoading(true);
        setProfileError("");

        const response = await getProfile(
          auth.accessToken!,
        );

        setProfile(response);
      } catch (error) {
        setProfileError(
          error instanceof Error
            ? error.message
            : "프로필 정보를 불러오지 못했습니다.",
        );
      } finally {
        setProfileLoading(false);
      }
    };

    loadProfile();
  }, [
    auth.status,
    auth.accessToken,
  ]);


  const handleFavoriteSave = async (
    gameIds: number[],
  ) => {
    if (!auth.accessToken) return;

    const updatedFavorites =
      await updateFavoriteGames(
        gameIds,
        auth.accessToken,
      );

    setProfile((prev) =>
      prev
        ? {
            ...prev,
            favorite: updatedFavorites,
          }
        : prev,
    );
  };

  return (
    <>
      <header className="header"><div className="header-inner">
        <Link className="logo" href="/" aria-label="GameLog 홈">GameLog<span className="lime">.</span></Link>
        <nav aria-label="주요 메뉴"><Link href="/">게임 탐색</Link></nav>
        <AuthNav />
        <span className="header-note">PLAY. RECORD. DISCOVER.</span>
      </div></header>

      <main className="main">
        {auth.status === "authenticated" && auth.user && auth.accessToken ? (
          <div className={styles.layout}>
            <ProfileSidebar key={auth.user.id} user={auth.user} accessToken={auth.accessToken} onSaved={auth.setUser} />
            <div className={styles.content}>
              <span className={styles.eyebrow}>MY GAME LOG</span>
              {profileLoading ? (
                <div className={styles.loading}>
                  게임 기록을 불러오는 중…
                </div>
              ) : profileError ? (
                <div className={styles.errorBox}>
                  {profileError}
                </div>
              ) : profile ? (
                <>
                  {/* 1. 인생게임 */}
                  <FavoriteGames
                    favorites={profile.favorite}
                    onSave={handleFavoriteSave}
                    accessToken={auth.accessToken}
                  />

                  <h2 className={styles.contentTitle}>게임 기록 통계</h2>

                  {/* 2. 플레이 통계 */}
                  <Stats
                    stats={profile.stats}
                  />

                  {/* 3. 산점도 */}
                  <ScatterPlot
                    data={
                      profile.scatterData
                    }
                    averagePlayTime={
                      profile.stats
                        .playedGameCount > 0
                        ? profile.stats
                            .totalPlayTime /
                          profile.stats
                            .playedGameCount
                        : 0
                    }
                  />

                  {/* 4. 취향 + 장르 */}
                  <div
                    className={
                      styles.preferenceGrid
                    }
                  >
                    <TasteSection
                      taste={
                        profile.tasteResponse
                      }
                    />

                    <GenreDistribution
                      data={
                        profile.genreDistribution
                      }
                    />
                  </div>

                  {/* 5. 최근 플레이 */}
                  <RecentGames
                    games={
                      profile.recentGames
                    }
                  />

                  {/* 6. 최근 리뷰 */}
                  <RecentReviews
                    reviews={
                      profile.recentReviews
                    }
                  />
                </>
              ) : null}
            </div>
          </div>
        ) : (
          <p className={styles.loading}>{auth.status === "loading" ? "확인 중…" : "로그인 페이지로 이동 중…"}</p>
        )}
      </main>
    </>
  );
}
