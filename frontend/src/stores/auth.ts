import { defineStore } from "pinia";
import { ref } from "vue";
import axios from "axios";
import { api } from "../api";
import type { User } from "../types";

export const useAuth = defineStore("auth", () => {
  const user = ref<User | null>(null);
  let initialized = false;
  async function initialize() {
    if (initialized) return;
    try {
      user.value = await api.me();
      initialized = true;
    } catch (error) {
      if (axios.isAxiosError(error) && error.response?.status === 401) {
        user.value = null;
        initialized = true;
      } else {
        throw error;
      }
    }
  }
  async function login(username: string, password: string) {
    user.value = await api.login({ username, password });
    initialized = true;
  }
  async function logout() {
    await api.logout();
    user.value = null;
    initialized = true;
  }
  return { user, initialize, login, logout };
});
