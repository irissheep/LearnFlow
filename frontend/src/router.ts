import { createRouter, createWebHistory } from "vue-router";
import { useAuth } from "./stores/auth";

export const router = createRouter({
  history: createWebHistory(),
  routes: [
    {
      path: "/login",
      component: () => import("./views/LoginView.vue"),
      meta: { public: true },
    },
    {
      path: "/",
      component: () => import("./AppShell.vue"),
      children: [
        {
          path: "",
          component: () => import("./views/DashboardView.vue"),
          meta: { title: "学习概览" },
        },
        {
          path: "tasks",
          component: () => import("./views/TasksView.vue"),
          meta: { title: "学习任务" },
        },
        {
          path: "notes",
          component: () => import("./views/NotesView.vue"),
          meta: { title: "知识笔记" },
        },
        {
          path: "ask",
          component: () => import("./views/AskView.vue"),
          meta: { title: "知识问答" },
        },
        {
          path: "profile",
          component: () => import("./views/ProfileView.vue"),
          meta: { title: "个人资料" },
        },
      ],
    },
    { path: "/:pathMatch(.*)*", redirect: "/" },
  ],
});
router.beforeEach(async (to) => {
  const auth = useAuth();
  try {
    await auth.initialize();
  } catch {
    return to.path === "/login" ? true : "/login";
  }
  if (!to.meta.public && !auth.user) return "/login";
  if (to.path === "/login" && auth.user) return "/";
});
