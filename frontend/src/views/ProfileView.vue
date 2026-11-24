<script setup lang="ts">
import { useAuth } from "../stores/auth";
const auth = useAuth();
</script>
<template>
  <div class="page-heading">
    <div>
      <span class="eyebrow">YOUR LEARNING SPACE</span>
      <h1>个人资料</h1>
      <p>这是属于你自己的学习空间。</p>
    </div>
  </div>
  <section class="panel profile-card">
    <span class="profile-avatar">{{ auth.user?.nickname.slice(0, 1) }}</span>
    <h2>{{ auth.user?.nickname }}</h2>
    <p class="muted">@{{ auth.user?.username }}</p>
    <el-descriptions :column="1" border
      ><el-descriptions-item label="账号 ID">{{
        auth.user?.id
      }}</el-descriptions-item
      ><el-descriptions-item label="加入时间">{{
        auth.user ? new Date(auth.user.createdAt).toLocaleString() : ""
      }}</el-descriptions-item></el-descriptions
    >
    <p class="profile-note">在这里管理任务、记录笔记、向自己的知识库提问。</p>
  </section>
</template>
