<script setup lang="ts">
import { reactive, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import { api, errorText } from "../api";
import { useAuth } from "../stores/auth";
const router = useRouter();
const auth = useAuth();
const mode = ref<"login" | "register">("login");
const form = reactive({ username: "", password: "", nickname: "" });
const loading = ref(false);
async function submit() {
  if (
    !form.username.trim() ||
    !form.password ||
    (mode.value === "register" && !form.nickname.trim())
  ) {
    ElMessage.warning("请填写完整信息");
    return;
  }
  loading.value = true;
  try {
    if (mode.value === "register")
      await api.register({ ...form, username: form.username.trim() });
    await auth.login(form.username.trim(), form.password);
    form.password = "";
    await router.push("/");
  } catch (error) {
    ElMessage.error(errorText(error));
  } finally {
    loading.value = false;
  }
}
</script>
<template>
  <div class="login-page">
    <section class="login-story">
      <div class="brand light">
        <span class="brand-mark">L<span>f</span></span
        ><span>LearnFlow</span>
      </div>
      <div class="story-main">
        <span class="eyebrow">YOUR PERSONAL LEARNING SPACE</span>
        <h1>让每一次学习，<br />都留下成长的轨迹。</h1>
        <p>管理学习任务，沉淀知识笔记，<br />让 AI 帮你连接已经学过的知识。</p>
        <div class="story-cards">
          <div><span>01</span> 从一个小目标开始</div>
          <div><span>02</span> 把理解写进自己的笔记</div>
          <div><span>03</span> 向你的知识库提问</div>
        </div>
      </div>
      <small>记录 · 理解 · 连接</small>
    </section>
    <section class="login-panel">
      <div class="login-box">
        <span class="eyebrow">WELCOME TO LEARNFLOW</span>
        <h2>{{ mode === "login" ? "欢迎回来" : "开始你的学习旅程" }}</h2>
        <p class="muted">
          {{
            mode === "login"
              ? "登录，继续今天的学习。"
              : "创建账号，拥有独立的任务与知识空间。"
          }}
        </p>
        <form @submit.prevent="submit">
          <el-form label-position="top">
            <el-form-item label="用户名"
              ><el-input
                v-model="form.username"
                size="large"
                placeholder="4–32 位字母、数字或下划线"
                autocomplete="username"
                maxlength="32"
            /></el-form-item>
            <el-form-item v-if="mode === 'register'" label="昵称"
              ><el-input
                v-model="form.nickname"
                size="large"
                placeholder="怎么称呼你？"
                maxlength="50"
                autocomplete="nickname"
            /></el-form-item>
            <el-form-item label="密码"
              ><el-input
                v-model="form.password"
                size="large"
                type="password"
                show-password
                :placeholder="
                  mode === 'register'
                    ? '至少 8 位，UTF-8 编码最多 72 字节'
                    : '输入你的密码'
                "
                :autocomplete="
                  mode === 'register' ? 'new-password' : 'current-password'
                "
            /></el-form-item>
          </el-form>
          <el-button
            native-type="submit"
            type="primary"
            size="large"
            class="full-width"
            :loading="loading"
            >{{ mode === "login" ? "登录学习空间" : "注册并登录" }}</el-button
          >
        </form>
        <p class="login-switch">
          {{ mode === "login" ? "还没有账号？" : "已经有账号？"
          }}<button @click="mode = mode === 'login' ? 'register' : 'login'">
            {{ mode === "login" ? "创建账号" : "去登录" }}
          </button>
        </p>
        <div class="login-hint">你的任务与笔记仅对自己的账号可见。</div>
      </div>
    </section>
  </div>
</template>
