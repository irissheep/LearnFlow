export interface User {
  id: number;
  username: string;
  nickname: string;
  createdAt: string;
}
export type TaskStatus = "TODO" | "IN_PROGRESS" | "DONE";
export interface Task {
  id: number;
  title: string;
  status: TaskStatus;
  createdAt: string;
  updatedAt: string;
}
export interface Note {
  id: number;
  title: string;
  category: string;
  content: string;
  revision: number;
  indexStatus: "PENDING" | "READY" | "FAILED";
  indexError: string | null;
  createdAt: string;
  updatedAt: string;
}
export interface Page<T> {
  items: T[];
  page: number;
  size: number;
  total: number;
}
export interface Stats {
  totalTasks: number;
  completedTasks: number;
  inProgressTasks: number;
  todoTasks: number;
  completionRate: number;
  totalNotes: number;
  recentNotes: {
    id: number;
    title: string;
    category: string;
    updatedAt: string;
  }[];
}
export interface Source {
  noteId: number;
  title: string;
  excerpt: string;
}
export interface Answer {
  answer: string;
  sources: Source[];
  mode: string;
}
export interface AiStatus {
  available: boolean;
  mode: string;
}
