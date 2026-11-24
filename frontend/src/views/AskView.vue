<script setup lang="ts">
import { nextTick, onMounted, ref } from "vue";
import { useRouter } from "vue-router";
import { ElMessage } from "element-plus";
import {
  ChatDotRound,
  Promotion,
  Refresh,
  Notebook,
} from "@element-plus/icons-vue";
import { api, errorText } from "../api";
import type { AiStatus, Source } from "../types";
import MarkdownContent from "../components/MarkdownContent.vue";
const router = useRouter();
const status = ref<AiStatus | null>(null);
const question = ref("");
const pending = ref(false);
const chat = ref<HTMLElement | null>(null);
const messages = ref<
  {
    role: "user" | "assistant";
    text: string;
    sources?: Source[];
    error?: boolean;
  }[]
>([]);
async function checkStatus() {
  try {
    status.value = await api.aiStatus();
  } catch (error) {
    ElMessage.error(errorText(error));
  }
}
async function send() {
  const text = question.value.trim();
  if (!text || pending.value) return;
  question.value = "";
  pending.value = true;
  messages.value.push({ role: "user", text });
  await scroll();
  try {
    const answer = await api.ask(text);
    messages.value.push({
      role: "assistant",
      text: answer.answer,
      sources: answer.sources,
    });
  } catch (error) {
    messages.value.push({
      role: "assistant",
      text: errorText(error),
      error: true,
    });
  } finally {
    pending.value = false;
    await scroll();
  }
}
async function scroll() {
  await nextTick();
  chat.value?.scrollTo({ top: chat.value.scrollHeight, behavior: "smooth" });
}
async function reindex() {
  try {
    const result = await api.reindex();
    ElMessage.success("已将 " + result.queued + " 篇笔记加入重建队列");
  } catch (error) {
    ElMessage.error(errorText(error));
  }
}
onMounted(checkStatus);
</script>
<template>
  <div class="page-heading">
    <div>
      <span class="eyebrow">ASK YOUR OWN KNOWLEDGE</span>
      <h1>知识问答</h1>
      <p>从自己的笔记中寻找答案，每一条引用都有出处。</p>
    </div>
    <div>
      <el-button :icon="Refresh" @click="checkStatus">刷新连接</el-button
      ><el-button @click="reindex">重建索引</el-button>
    </div>
  </div>
  <el-alert
    v-if="status && !status.available"
    title="知识问答服务尚未连接。你仍可正常管理任务与笔记，服务启动后再刷新连接。"
    type="warning"
    :closable="false"
    show-icon
  />
  <div v-if="status?.available" class="mode-note">
    <span class="small-dot"></span
    >{{
      status.mode === "local"
        ? "本地检索模式 · 返回笔记摘录，未调用大模型"
        : "模型问答模式 · 根据你的笔记生成回答"
    }}
  </div>
  <section class="panel chat-panel">
    <div ref="chat" class="chat-content">
      <div v-if="!messages.length" class="chat-welcome">
        <span class="chat-logo"
          ><el-icon><ChatDotRound /></el-icon
        ></span>
        <h2>把零散的知识，连接起来。</h2>
        <p>先保存几篇笔记并等待索引完成，再问一个你正在思考的问题。</p>
        <div class="question-examples">
          <button
            v-for="example in [
              '我的笔记中如何解释 Spring 事务？',
              '帮我梳理笔记里的关键概念',
              '关于数据库索引，我记录了哪些要点？',
            ]"
            :key="example"
            @click="question = example"
          >
            {{ example }}
          </button>
        </div>
      </div>
      <div
        v-for="(message, index) in messages"
        :key="index"
        class="chat-message"
        :class="message.role"
      >
        <span class="message-avatar">{{
          message.role === "user" ? "我" : "Lf"
        }}</span>
        <div class="message-body">
          <p class="message-label">
            {{ message.role === "user" ? "你的问题" : "LearnFlow 知识助手" }}
          </p>
          <el-alert
            v-if="message.error"
            :title="message.text"
            type="error"
            :closable="false"
          /><MarkdownContent v-else :text="message.text" />
          <div v-if="message.sources?.length" class="source-list">
            <span class="muted">参考笔记</span
            ><button
              v-for="(source, i) in message.sources"
              :key="source.noteId"
              @click="
                router.push({ path: '/notes', query: { id: source.noteId } })
              "
            >
              <el-icon><Notebook /></el-icon>[{{ i + 1 }}] {{ source.title }}
            </button>
          </div>
        </div>
      </div>
      <div v-if="pending" class="chat-message assistant">
        <span class="message-avatar">Lf</span>
        <div class="message-body muted">正在检索你的笔记并组织回答…</div>
      </div>
    </div>
    <form class="chat-composer" @submit.prevent="send">
      <el-input
        v-model="question"
        type="textarea"
        :autosize="{ minRows: 2, maxRows: 5 }"
        maxlength="2000"
        placeholder="向你的知识库提一个问题…（Ctrl + Enter 发送）"
        @keydown.ctrl.enter.prevent="send"
      />
      <div>
        <small>回答以个人笔记为依据，请结合引用核对。</small
        ><el-button
          native-type="submit"
          type="primary"
          :icon="Promotion"
          :loading="pending"
          :disabled="!question.trim() || !status?.available"
          >发送问题</el-button
        >
      </div>
    </form>
  </section>
</template>
