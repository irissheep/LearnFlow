import { defineConfig } from "vite";
import vue from "@vitejs/plugin-vue";

const proxy = {
  "/api": {
    target: process.env.BACKEND_URL || "http://127.0.0.1:8080",
    changeOrigin: true,
  },
};

export default defineConfig({
  plugins: [vue()],
  server: {
    port: 5173,
    strictPort: true,
    // 浏览器始终请求同源 /api，由开发代理转交 Java，Cookie 和 CSRF 都保留。
    proxy,
  },
  preview: { host: "127.0.0.1", port: 15173, strictPort: true, proxy },
});
