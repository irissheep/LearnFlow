<script setup lang="ts">
import { computed } from "vue";
import { marked } from "marked";
import DOMPurify from "dompurify";
const props = defineProps<{ text: string }>();
// 笔记和模型输出都不能直接 v-html；先转换 Markdown，再清除脚本、表单和外部图片。
const html = computed(() =>
  DOMPurify.sanitize(marked.parse(props.text, { async: false }), {
    FORBID_TAGS: ["img", "iframe", "form", "style"],
  }),
);
</script>
<template><div class="markdown-body" v-html="html"></div></template>
