<script setup lang="ts">
import { computed } from "vue";
import { useRoute, useRouter } from "vue-router";
import {
  Grid,
  List,
  Notebook,
  ChatDotRound,
  User,
  SwitchButton,
  ArrowRight,
} from "@element-plus/icons-vue";
import { ElMessage } from "element-plus";
import { useAuth } from "./stores/auth";
import { errorText } from "./api";
const auth = useAuth();
const route = useRoute();
const router = useRouter();
const date = new Intl.DateTimeFormat("zh-CN", {
  month: "long",
  day: "numeric",
  weekday: "long",
}).format(new Date());
const title = computed(() => route.meta.title || "LearnFlow");
const links = [
  { to: "/", text: "学习概览", icon: Grid },
  { to: "/tasks", text: "学习任务", icon: List },
  { to: "/notes", text: "知识笔记", icon: Notebook },
  { to: "/ask", text: "知识问答", icon: ChatDotRound },
];
async function logout() {
  try {
    await auth.logout();
    await router.push("/login");
  } catch (error) {
    ElMessage.error(errorText(error));
  }
}
</script>
<template>
  <div class="app-layout">
    <aside class="sidebar">
      <RouterLink to="/" class="brand"
        ><span class="brand-mark">L<span>f</span></span
        ><span>LearnFlow<small>让学习有迹可循</small></span></RouterLink
      >
      <p class="nav-label">我的学习空间</p>
      <nav>
        <RouterLink
          v-for="link in links"
          :key="link.to"
          :to="link.to"
          :class="{ active: route.path === link.to }"
        >
          <el-icon><component :is="link.icon" /></el-icon
          ><span>{{ link.text }}</span>
        </RouterLink>
      </nav>
      <div class="sidebar-note">
        <span class="small-dot"></span> 每一次记录，都是进步。
        <p>把任务变成行动，把笔记变成知识。</p>
      </div>
      <div class="sidebar-user">
        <RouterLink to="/profile" class="user-link"
          ><span class="avatar">{{ auth.user?.nickname.slice(0, 1) }}</span
          ><span
            >{{ auth.user?.nickname
            }}<small>{{ auth.user?.username }}</small></span
          ><el-icon><User /></el-icon
        ></RouterLink>
        <button class="logout" @click="logout">
          <el-icon><SwitchButton /></el-icon>退出登录
        </button>
      </div>
    </aside>
    <main class="main-area">
      <header class="topbar">
        <div>
          我的空间 <el-icon><ArrowRight /></el-icon><strong>{{ title }}</strong>
        </div>
        <span class="topbar-date">{{ date }}</span>
        <div class="mobile-account">
          <RouterLink to="/profile" aria-label="个人资料"
            ><el-icon><User /></el-icon></RouterLink
          ><button aria-label="退出登录" @click="logout">
            <el-icon><SwitchButton /></el-icon>
          </button>
        </div>
      </header>
      <div class="page-content"><RouterView /></div>
      <footer class="page-footer">
        LearnFlow · 一点一滴，构建自己的知识体系
      </footer>
    </main>
  </div>
</template>
