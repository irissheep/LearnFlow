<script setup lang="ts">
import { onMounted, ref } from "vue";
import { useAuth } from "../stores/auth";
import { api, errorText } from "../api";
import type { Stats, Task } from "../types";
import {
  ArrowRight,
  Plus,
  Notebook,
  Check,
  List,
  TrendCharts,
} from "@element-plus/icons-vue";
const auth = useAuth();
const stats = ref<Stats | null>(null);
const tasks = ref<Task[]>([]);
const loading = ref(true);
const error = ref("");
async function load() {
  loading.value = true;
  error.value = "";
  try {
    [stats.value, tasks.value] = await Promise.all([api.stats(), api.tasks()]);
  } catch (err) {
    error.value = errorText(err);
  } finally {
    loading.value = false;
  }
}
onMounted(load);
</script>
<template>
  <div class="page-heading">
    <div>
      <span class="eyebrow">LEARNING OVERVIEW</span>
      <h1>{{ auth.user?.nickname }}，今天也向前一步</h1>
      <p>看看你的学习进展，找到下一件值得专注的小事。</p>
    </div>
    <RouterLink to="/tasks"
      ><el-button type="primary" :icon="Plus"
        >创建学习任务</el-button
      ></RouterLink
    >
  </div>
  <el-alert v-if="error" :title="error" type="error" show-icon :closable="false"
    ><el-button text @click="load">重新加载</el-button></el-alert
  >
  <el-skeleton v-if="loading" :rows="8" animated />
  <template v-else-if="stats && !error">
    <div class="metric-grid">
      <div class="metric-card">
        <span class="metric-icon green"
          ><el-icon><List /></el-icon
        ></span>
        <p>全部学习任务</p>
        <strong>{{ stats.totalTasks }}</strong
        ><small>每个目标都有一个开始</small>
      </div>
      <div class="metric-card">
        <span class="metric-icon blue"
          ><el-icon><Check /></el-icon
        ></span>
        <p>已完成任务</p>
        <strong>{{ stats.completedTasks }}</strong
        ><small>已经迈出的坚实步伐</small>
      </div>
      <div class="metric-card">
        <span class="metric-icon orange"
          ><el-icon><TrendCharts /></el-icon
        ></span>
        <p>任务完成率</p>
        <strong>{{ stats.completionRate }}<em>%</em></strong
        ><small>持续行动，慢慢积累</small>
      </div>
      <div class="metric-card">
        <span class="metric-icon purple"
          ><el-icon><Notebook /></el-icon
        ></span>
        <p>知识笔记</p>
        <strong>{{ stats.totalNotes }}</strong
        ><small>属于你自己的知识积累</small>
      </div>
    </div>
    <div class="dashboard-grid">
      <section class="panel">
        <div class="panel-heading">
          <h2>接下来学什么</h2>
          <RouterLink to="/tasks"
            >全部任务 <el-icon><ArrowRight /></el-icon
          ></RouterLink>
        </div>
        <div
          v-for="task in tasks.filter((t) => t.status !== 'DONE').slice(0, 5)"
          :key="task.id"
          class="overview-task"
        >
          <span
            class="task-dot"
            :class="{ progressing: task.status === 'IN_PROGRESS' }"
          ></span>
          <div>
            <strong>{{ task.title }}</strong
            ><small>{{
              task.status === "IN_PROGRESS" ? "进行中" : "待开始"
            }}</small>
          </div>
          <RouterLink to="/tasks"
            ><el-icon><ArrowRight /></el-icon
          ></RouterLink>
        </div>
        <el-empty
          v-if="!tasks.some((t) => t.status !== 'DONE')"
          :image-size="72"
          description="暂时没有待办任务，给自己定一个新目标吧"
        />
      </section>
      <section class="panel progress-panel">
        <div class="panel-heading">
          <h2>学习进度</h2>
          <span class="pill">持续积累</span>
        </div>
        <div class="progress-number">
          {{ stats.completionRate }}<span>%</span>
        </div>
        <el-progress
          :percentage="stats.completionRate"
          :show-text="false"
          :stroke-width="10"
          color="#547b65"
        />
        <div class="progress-legend">
          <span
            ><i class="dot green-dot"></i>已完成
            {{ stats.completedTasks }}</span
          ><span
            ><i class="dot orange-dot"></i>进行中
            {{ stats.inProgressTasks }}</span
          ><span><i class="dot gray-dot"></i>待开始 {{ stats.todoTasks }}</span>
        </div>
      </section>
      <section class="panel recent-panel">
        <div class="panel-heading">
          <h2>最近的知识记录</h2>
          <RouterLink to="/notes"
            >打开笔记 <el-icon><ArrowRight /></el-icon
          ></RouterLink>
        </div>
        <RouterLink
          v-for="note in stats.recentNotes"
          :key="note.id"
          :to="{ path: '/notes', query: { id: note.id } }"
          class="recent-note"
          ><span class="note-icon"
            ><el-icon><Notebook /></el-icon
          ></span>
          <div>
            <strong>{{ note.title }}</strong
            ><small
              >{{ note.category }} ·
              {{ new Date(note.updatedAt).toLocaleDateString() }}</small
            >
          </div>
          <el-icon><ArrowRight /></el-icon
        ></RouterLink>
        <el-empty
          v-if="!stats.recentNotes.length"
          :image-size="72"
          description="把今天学到的第一件事写下来"
        />
      </section>
      <section class="ai-promo">
        <span class="eyebrow">CONNECT YOUR KNOWLEDGE</span>
        <h2>你记下的知识，<br />也可以成为答案。</h2>
        <p>向个人知识库提问，从自己的笔记中找到线索和引用。</p>
        <RouterLink to="/ask"
          ><el-button
            >开始知识问答 <el-icon><ArrowRight /></el-icon></el-button
        ></RouterLink>
      </section>
    </div>
  </template>
</template>
