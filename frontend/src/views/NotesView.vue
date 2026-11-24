<script setup lang="ts">
import {
  computed,
  onBeforeUnmount,
  onMounted,
  reactive,
  ref,
  watch,
} from "vue";
import { useRoute, useRouter } from "vue-router";
import { ElMessage, ElMessageBox } from "element-plus";
import {
  Plus,
  Search,
  Delete,
  Refresh,
  Notebook,
} from "@element-plus/icons-vue";
import { api, errorText } from "../api";
import type { Note } from "../types";
import MarkdownContent from "../components/MarkdownContent.vue";
const route = useRoute();
const router = useRouter();
const notes = ref<Note[]>([]);
const categories = ref<string[]>([]);
const selected = ref<Note | null>(null);
const creating = ref(false);
const loading = ref(false);
const saving = ref(false);
const keyword = ref("");
const category = ref("");
const page = ref(1);
const total = ref(0);
const editorTab = ref("edit");
const form = reactive({ title: "", category: "", content: "" });
const indexLabels = {
  PENDING: "等待索引",
  READY: "已索引",
  FAILED: "索引失败",
};
const canEdit = computed(() => creating.value || !!selected.value);
let searchTimer: ReturnType<typeof setTimeout> | undefined;
async function loadList() {
  loading.value = true;
  try {
    const result = await api.notes(
      page.value - 1,
      keyword.value,
      category.value,
    );
    notes.value = result.items;
    total.value = result.total;
  } catch (error) {
    ElMessage.error(errorText(error));
  } finally {
    loading.value = false;
  }
}
async function loadCategories() {
  try {
    categories.value = await api.categories();
  } catch (error) {
    ElMessage.error(errorText(error));
  }
}
async function open(id: number) {
  try {
    selected.value = await api.note(id);
    creating.value = false;
    Object.assign(form, {
      title: selected.value.title,
      category: selected.value.category,
      content: selected.value.content,
    });
  } catch (error) {
    ElMessage.error(errorText(error));
  }
}
function newNote() {
  creating.value = true;
  selected.value = null;
  editorTab.value = "edit";
  Object.assign(form, { title: "", category: "", content: "" });
  router.replace("/notes");
}
async function save() {
  if (!form.title.trim() || !form.content.trim()) {
    ElMessage.warning("请填写标题和正文");
    return;
  }
  saving.value = true;
  try {
    const saved = selected.value
      ? await api.updateNote(selected.value.id, { ...form })
      : await api.createNote({ ...form });
    selected.value = saved;
    creating.value = false;
    await router.replace({ path: "/notes", query: { id: saved.id } });
    await Promise.all([loadList(), loadCategories()]);
    ElMessage.success("笔记已保存，索引将在后台同步");
  } catch (error) {
    ElMessage.error(errorText(error));
  } finally {
    saving.value = false;
  }
}
async function remove() {
  if (!selected.value) return;
  try {
    await ElMessageBox.confirm(
      "确定删除这篇笔记吗？它也会从问答知识库中移除。",
      "删除笔记",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" },
    );
    await api.deleteNote(selected.value.id);
    selected.value = null;
    creating.value = false;
    await router.replace("/notes");
    await Promise.all([loadList(), loadCategories()]);
  } catch (error) {
    if (error !== "cancel" && error !== "close")
      ElMessage.error(errorText(error));
  }
}
async function retry() {
  if (!selected.value) return;
  try {
    selected.value = await api.retryIndex(selected.value.id);
    await loadList();
    ElMessage.success("已加入索引队列");
  } catch (error) {
    ElMessage.error(errorText(error));
  }
}
async function refreshIndexStatus() {
  if (!selected.value) return;
  try {
    const fresh = await api.note(selected.value.id);
    // 只同步索引字段，保留尚未保存的正文，同时更新左侧列表的状态。
    selected.value.indexStatus = fresh.indexStatus;
    selected.value.indexError = fresh.indexError;
    const listed = notes.value.find((note) => note.id === fresh.id);
    if (listed) {
      listed.indexStatus = fresh.indexStatus;
      listed.indexError = fresh.indexError;
    }
  } catch (error) {
    ElMessage.error(errorText(error));
  }
}
watch(
  () => route.query.id,
  (id) => {
    const value = Number(id);
    if (Number.isInteger(value) && value > 0) open(value);
  },
  { immediate: true },
);
watch([keyword, category], () => {
  page.value = 1;
  clearTimeout(searchTimer);
  searchTimer = setTimeout(loadList, 300);
});
onMounted(() => {
  loadList();
  loadCategories();
});
onBeforeUnmount(() => clearTimeout(searchTimer));
</script>
<template>
  <div class="page-heading">
    <div>
      <span class="eyebrow">BUILD YOUR KNOWLEDGE</span>
      <h1>知识笔记</h1>
      <p>用自己的话记录理解，让知识真正属于你。</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="newNote">新建笔记</el-button>
  </div>
  <div class="notes-workspace">
    <section class="panel notes-list">
      <div class="notes-filters">
        <el-input
          v-model="keyword"
          :prefix-icon="Search"
          placeholder="搜索标题或正文"
          clearable
        /><el-select v-model="category" placeholder="全部分类" clearable
          ><el-option
            v-for="item in categories"
            :key="item"
            :label="item"
            :value="item"
        /></el-select>
      </div>
      <el-skeleton v-if="loading" :rows="5" animated />
      <button
        v-for="note in notes"
        v-else
        :key="note.id"
        class="note-list-item"
        :class="{ selected: selected?.id === note.id }"
        @click="router.push({ path: '/notes', query: { id: note.id } })"
      >
        <div>
          <span class="pill">{{ note.category }}</span
          ><small>{{ new Date(note.updatedAt).toLocaleDateString() }}</small>
        </div>
        <strong>{{ note.title }}</strong>
        <p>{{ note.content.slice(0, 90) }}</p>
        <small :class="{ 'error-text': note.indexStatus === 'FAILED' }">{{
          indexLabels[note.indexStatus]
        }}</small>
      </button>
      <el-empty
        v-if="!loading && !notes.length"
        :image-size="64"
        description="这里还没有笔记"
      />
      <el-pagination
        v-if="total > 12"
        v-model:current-page="page"
        small
        layout="prev, pager, next"
        :total="total"
        :page-size="12"
        @current-change="loadList"
      />
    </section>
    <section class="panel note-editor">
      <template v-if="canEdit"
        ><div class="editor-actions">
          <span class="muted">{{ creating ? "新笔记" : "编辑笔记" }}</span>
          <div>
            <el-button v-if="selected" text :icon="Refresh" @click="retry"
              >重试索引</el-button
            ><el-button
              v-if="selected"
              text
              :icon="Delete"
              aria-label="删除笔记"
              @click="remove"
            /><el-button type="primary" :loading="saving" @click="save"
              >保存笔记</el-button
            >
          </div>
        </div>
        <el-input
          v-model="form.title"
          class="note-title-input"
          maxlength="200"
          placeholder="给这篇笔记起个标题"
        />
        <div class="note-meta">
          <el-select
            v-model="form.category"
            filterable
            allow-create
            default-first-option
            clearable
            placeholder="选择或输入分类"
            ><el-option
              v-for="item in categories"
              :key="item"
              :label="item"
              :value="item" /></el-select
          ><span
            v-if="selected"
            class="status-badge"
            :class="selected.indexStatus === 'READY' ? 'done' : 'todo'"
            >{{ indexLabels[selected.indexStatus] }}</span
          ><el-button
            v-if="selected"
            text
            size="small"
            @click="refreshIndexStatus"
            >刷新状态</el-button
          >
        </div>
        <el-alert
          v-if="selected?.indexError"
          :title="selected.indexError"
          type="warning"
          :closable="false"
        />
        <el-tabs v-model="editorTab"
          ><el-tab-pane label="Markdown 编辑" name="edit"
            ><el-input
              v-model="form.content"
              type="textarea"
              :autosize="{ minRows: 18, maxRows: 32 }"
              maxlength="100000"
              placeholder="# 今天学到了什么？&#10;&#10;写下概念、示例，以及你自己的理解。" /></el-tab-pane
          ><el-tab-pane label="阅读预览" name="preview"
            ><MarkdownContent
              :text="
                form.content || '还没有内容，先写下第一句话吧。'
              " /></el-tab-pane
        ></el-tabs>
      </template>
      <div v-else class="note-placeholder">
        <el-icon><Notebook /></el-icon>
        <h2>给知识一个安放的地方</h2>
        <p>选择左侧笔记，或从一篇新笔记开始。</p>
        <el-button type="primary" plain @click="newNote"
          >写下第一篇笔记</el-button
        >
      </div>
    </section>
  </div>
</template>
