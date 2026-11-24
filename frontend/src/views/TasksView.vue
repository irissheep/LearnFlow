<script setup lang="ts">
import { computed, onMounted, reactive, ref } from "vue";
import { ElMessage, ElMessageBox } from "element-plus";
import { Plus, Search, Edit, Delete, Check } from "@element-plus/icons-vue";
import { api, errorText } from "../api";
import type { Task, TaskStatus } from "../types";
const rows = ref<Task[]>([]);
const loading = ref(false);
const error = ref("");
const search = ref("");
const filter = ref("ALL");
const dialog = ref(false);
const saving = ref(false);
const form = reactive<{ id: number | null; title: string; status: TaskStatus }>(
  { id: null, title: "", status: "TODO" },
);
const labels: Record<TaskStatus, string> = {
  TODO: "待开始",
  IN_PROGRESS: "进行中",
  DONE: "已完成",
};
const visible = computed(() =>
  rows.value.filter(
    (task) =>
      (filter.value === "ALL" || task.status === filter.value) &&
      task.title.toLowerCase().includes(search.value.toLowerCase()),
  ),
);
async function load() {
  loading.value = true;
  error.value = "";
  try {
    rows.value = await api.tasks();
  } catch (err) {
    error.value = errorText(err);
  } finally {
    loading.value = false;
  }
}
function edit(task?: Task) {
  Object.assign(form, {
    id: task?.id || null,
    title: task?.title || "",
    status: task?.status || "TODO",
  });
  dialog.value = true;
}
async function save() {
  if (!form.title.trim()) {
    ElMessage.warning("请输入任务标题");
    return;
  }
  saving.value = true;
  try {
    if (form.id) await api.updateTask(form.id, form.title, form.status);
    else await api.createTask(form.title);
    dialog.value = false;
    await load();
    ElMessage.success("任务已保存");
  } catch (error) {
    ElMessage.error(errorText(error));
  } finally {
    saving.value = false;
  }
}
async function advance(task: Task) {
  try {
    await api.updateTask(
      task.id,
      task.title,
      task.status === "TODO" ? "IN_PROGRESS" : "DONE",
    );
    await load();
  } catch (error) {
    ElMessage.error(errorText(error));
  }
}
async function remove(task: Task) {
  try {
    await ElMessageBox.confirm(
      "删除后无法恢复，确定删除这条任务吗？",
      "删除任务",
      { type: "warning", confirmButtonText: "删除", cancelButtonText: "取消" },
    );
    await api.deleteTask(task.id);
    await load();
    ElMessage.success("任务已删除");
  } catch (error) {
    if (error !== "cancel" && error !== "close")
      ElMessage.error(errorText(error));
  }
}
onMounted(load);
</script>
<template>
  <div class="page-heading">
    <div>
      <span class="eyebrow">SMALL STEPS, REAL PROGRESS</span>
      <h1>学习任务</h1>
      <p>把想学的内容变成具体行动，一次专注一件事。</p>
    </div>
    <el-button type="primary" :icon="Plus" @click="edit()">新增任务</el-button>
  </div>
  <section class="panel">
    <div class="list-toolbar">
      <div class="filter-tabs">
        <button
          v-for="option in ['ALL', 'TODO', 'IN_PROGRESS', 'DONE']"
          :key="option"
          :class="{ selected: filter === option }"
          @click="filter = option"
        >
          {{ option === "ALL" ? "全部任务" : labels[option as TaskStatus]
          }}<span>{{
            rows.filter((t) => option === "ALL" || t.status === option).length
          }}</span>
        </button>
      </div>
      <el-input
        v-model="search"
        placeholder="搜索任务标题"
        :prefix-icon="Search"
        clearable
        class="search-input"
      />
    </div>
    <el-alert v-if="error" :title="error" type="error" :closable="false"
      ><el-button text @click="load">重新加载</el-button></el-alert
    >
    <el-skeleton v-if="loading" :rows="6" animated />
    <template v-else-if="!error">
      <div v-for="task in visible" :key="task.id" class="task-row">
        <button
          class="task-check"
          :class="{ checked: task.status === 'DONE' }"
          :disabled="task.status === 'DONE'"
          :aria-label="task.status === 'TODO' ? '开始任务' : '完成任务'"
          @click="advance(task)"
        >
          <el-icon v-if="task.status === 'DONE'"><Check /></el-icon>
        </button>
        <div class="task-details">
          <strong :class="{ completed: task.status === 'DONE' }">{{
            task.title
          }}</strong
          ><small
            >创建于 {{ new Date(task.createdAt).toLocaleDateString() }}</small
          >
        </div>
        <span class="status-badge" :class="task.status.toLowerCase()">{{
          labels[task.status]
        }}</span
        ><el-button v-if="task.status !== 'DONE'" text @click="advance(task)">{{
          task.status === "TODO" ? "开始学习" : "标记完成"
        }}</el-button
        ><el-button
          text
          :icon="Edit"
          aria-label="编辑任务"
          @click="edit(task)"
        /><el-button
          text
          :icon="Delete"
          aria-label="删除任务"
          @click="remove(task)"
        />
      </div>
      <el-empty
        v-if="!visible.length"
        :description="
          rows.length ? '没有符合条件的任务' : '创建你的第一个学习目标吧'
        "
      />
    </template>
  </section>
  <el-dialog
    v-model="dialog"
    :title="form.id ? '编辑学习任务' : '新增学习任务'"
    width="min(480px, 92vw)"
  >
    <el-form label-position="top" @submit.prevent="save"
      ><el-form-item label="任务标题"
        ><el-input
          v-model="form.title"
          maxlength="200"
          show-word-limit
          placeholder="例如：理解 Spring 的事务机制"
          @keyup.enter="save" /></el-form-item
      ><el-form-item v-if="form.id" label="任务状态"
        ><el-select v-model="form.status"
          ><el-option
            v-for="(label, value) in labels"
            :key="value"
            :label="label"
            :value="value" /></el-select></el-form-item
    ></el-form>
    <template #footer
      ><el-button @click="dialog = false">取消</el-button
      ><el-button type="primary" :loading="saving" @click="save"
        >保存任务</el-button
      ></template
    >
  </el-dialog>
</template>
