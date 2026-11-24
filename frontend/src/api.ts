import axios from "axios";
import type {
  Answer,
  AiStatus,
  Note,
  Page,
  Stats,
  Task,
  TaskStatus,
  User,
} from "./types";

const http = axios.create({
  baseURL: "/api",
  withCredentials: true,
  timeout: 45000,
});
let csrf: { token: string; headerName: string } | null = null;
let pendingCsrf: Promise<void> | null = null;

// 登录身份由 HttpOnly 会话 Cookie 管理；前端只持有 CSRF Token，不在 localStorage 存登录令牌。
http.interceptors.request.use(async (config) => {
  if (
    !["get", "head", "options"].includes((config.method || "get").toLowerCase())
  ) {
    if (!csrf) {
      pendingCsrf ||= http
        .get("/auth/csrf")
        .then((response) => {
          csrf = response.data;
        })
        .finally(() => {
          pendingCsrf = null;
        });
      await pendingCsrf;
    }
    if (csrf) config.headers.set(csrf.headerName, csrf.token);
  }
  return config;
});

http.interceptors.response.use(
  (response) => response,
  (error) => {
    // 会话过期后回到登录页；完整刷新也会清除当前页面内存中的私人数据。
    if (
      axios.isAxiosError(error) &&
      error.response?.status === 401 &&
      !error.config?.url?.startsWith("/auth/") &&
      window.location.pathname !== "/login"
    ) {
      window.location.assign("/login");
    }
    return Promise.reject(error);
  },
);

export function errorText(error: unknown): string {
  if (axios.isAxiosError(error)) {
    const problem = error.response?.data;
    if (problem?.errors) return Object.values(problem.errors).join("；");
    return (
      problem?.detail ||
      (error.response?.status === 401
        ? "请重新登录"
        : "请求失败，请检查服务是否启动")
    );
  }
  return error instanceof Error ? error.message : "操作失败，请稍后重试";
}
export const api = {
  me: async () => (await http.get<User>("/auth/me")).data,
  register: async (body: {
    username: string;
    password: string;
    nickname: string;
  }) => (await http.post<User>("/auth/register", body)).data,
  login: async (body: { username: string; password: string }) => {
    const response = await http.post<User>("/auth/login", body);
    csrf = null; // 认证成功会轮换 Token，下次写请求重新获取。
    return response.data;
  },
  logout: async () => {
    await http.post("/auth/logout");
    csrf = null;
  },
  tasks: async () => (await http.get<Task[]>("/tasks")).data,
  createTask: async (title: string) =>
    (await http.post<Task>("/tasks", { title })).data,
  updateTask: async (id: number, title: string, status: TaskStatus) =>
    (await http.put<Task>("/tasks/" + id, { title, status })).data,
  deleteTask: async (id: number) => {
    await http.delete("/tasks/" + id);
  },
  notes: async (page = 0, keyword = "", category = "") =>
    (
      await http.get<Page<Note>>("/notes", {
        params: { page, size: 12, keyword, category },
      })
    ).data,
  note: async (id: number) => (await http.get<Note>("/notes/" + id)).data,
  categories: async () => (await http.get<string[]>("/notes/categories")).data,
  createNote: async (body: {
    title: string;
    category: string;
    content: string;
  }) => (await http.post<Note>("/notes", body)).data,
  updateNote: async (
    id: number,
    body: { title: string; category: string; content: string },
  ) => (await http.put<Note>("/notes/" + id, body)).data,
  deleteNote: async (id: number) => {
    await http.delete("/notes/" + id);
  },
  retryIndex: async (id: number) =>
    (await http.post<Note>("/notes/" + id + "/retry-index")).data,
  stats: async () => (await http.get<Stats>("/stats")).data,
  aiStatus: async () => (await http.get<AiStatus>("/ai/status")).data,
  ask: async (question: string) =>
    (await http.post<Answer>("/ai/ask", { question })).data,
  reindex: async () =>
    (await http.post<{ queued: number }>("/ai/reindex")).data,
};
