export type Option = { id: number; name: string };
export type Game = { id: number; title: string; coverImageUrl: string | null; releaseDate: string | null; igdbRating: number | null; genres?: Option[]; platforms?: Option[]; description?: string; developer?: string };
export type GameStatistics = {
  playedCount: number;
  playingCount: number;
  backlogCount: number;
  wishlistCount: number;
  likeCount: number;
  averageRating: number;
  reviewCount: number;
  ratingDistribution: { rating: number; count: number }[];
  averagePlayTimeHours: number;
  playTimeUserCount: number;
};
export type GameDetail = Game & {
  igdbId: number;
  genres: Option[];
  platforms: Option[];
  series: Option[];
  statistics: GameStatistics;
};
export type GamePage = { content: Game[]; totalElements: number; totalPages: number; number: number };
export type Filters = { genres: number[]; platforms: number[] };
export const emptyFilters: Filters = { genres: [], platforms: [] };
export const demoOptions = {
  genres: [{ id: 1, name: "액션" }, { id: 2, name: "RPG" }, { id: 3, name: "어드벤처" }, { id: 4, name: "시뮬레이션" }, { id: 5, name: "플랫포머" }],
  platforms: [{ id: 1, name: "PC" }, { id: 2, name: "PlayStation" }, { id: 3, name: "Nintendo Switch" }, { id: 4, name: "Xbox" }],
};
// User-selected preview data, never a silent fallback for an API error.
const samples: [string, number, string, number[], number[]][] = [
  ["Hades", 1145360, "2020-09-17", [1, 2], [1, 2, 3, 4]],
  ["Hollow Knight", 367520, "2017-02-24", [1, 3, 5], [1, 2, 3, 4]],
  ["ELDEN RING", 1245620, "2022-02-25", [1, 2], [1, 2, 4]],
  ["Stardew Valley", 413150, "2016-02-26", [2, 4], [1, 2, 3, 4]],
  ["Red Dead Redemption 2", 1174180, "2019-12-05", [1, 3], [1, 2, 4]],
  ["Cyberpunk 2077", 1091500, "2020-12-10", [1, 2], [1, 2, 4]],
  ["Celeste", 504230, "2018-01-25", [3, 5], [1, 2, 3, 4]],
  ["Disco Elysium", 632470, "2019-10-15", [2, 3], [1, 2, 3, 4]],
  ["Outer Wilds", 753640, "2020-06-18", [3], [1, 2, 3, 4]],
  ["Baldur’s Gate 3", 1086940, "2023-08-03", [2, 3], [1, 2, 4]],
  ["Portal 2", 620, "2011-04-19", [3], [1, 3]],
  ["Death Stranding", 1190460, "2020-07-14", [1, 3], [1, 2]],
  ["Terraria", 105600, "2011-05-16", [1, 3], [1, 2, 3, 4]],
  ["Sekiro: Shadows Die Twice", 814380, "2019-03-22", [1, 3], [1, 2, 4]],
  ["No Man’s Sky", 275850, "2016-08-12", [3, 4], [1, 2, 3, 4]],
  ["The Witcher 3: Wild Hunt", 292030, "2015-05-19", [2, 3], [1, 2, 3, 4]],
];
export const demoGames: Game[] = samples.map(([title, app, releaseDate, genres, platforms], i) => ({
  id: i + 1, title, releaseDate, igdbRating: null,
  coverImageUrl: `https://cdn.cloudflare.steamstatic.com/steam/apps/${app}/library_600x900.jpg`,
  genres: demoOptions.genres.filter(x => genres.includes(x.id)),
  platforms: demoOptions.platforms.filter(x => platforms.includes(x.id)),
  description: "디자인 미리보기용 게임입니다. 실제 게임 정보는 서버 연결 후 확인할 수 있어요.",
}));
export function coverUrl(url: string | null) {
  if (!url) return null;
  const normalized = url.startsWith("//") ? `https:${url}` : url;
  return /^https?:\/\//.test(normalized) ? normalized.replace("/t_thumb/", "/t_cover_big/") : null;
}
