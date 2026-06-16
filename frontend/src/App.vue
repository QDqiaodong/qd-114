<template>
  <div class="app-container">
    <header class="app-header">
      <div class="header-content">
        <h1 class="app-title">
          <span class="logo">🌸</span>
          花卉种子培育跟踪表
        </h1>
        <p class="app-subtitle">全程跟踪花卉播种培育全周期</p>
      </div>
    </header>

    <nav class="app-nav">
      <router-link to="/" class="nav-item" exact-active-class="active">
        <span class="nav-icon">🏠</span>
        <span>首页概览</span>
      </router-link>
      <router-link to="/seeds" class="nav-item" active-class="active">
        <span class="nav-icon">🌱</span>
        <span>种子管理</span>
      </router-link>
      <router-link to="/sowings" class="nav-item" active-class="active">
        <span class="nav-icon">🌾</span>
        <span>播种记录</span>
      </router-link>
      <router-link to="/growth" class="nav-item" active-class="active">
        <span class="nav-icon">🌿</span>
        <span>生长跟踪</span>
      </router-link>
      <router-link to="/transplants" class="nav-item" active-class="active">
        <span class="nav-icon">🪴</span>
        <span>移栽分盆</span>
      </router-link>
      <router-link to="/recovery" class="nav-item" active-class="active">
        <span class="nav-icon">🌿</span>
        <span>恢复观察</span>
      </router-link>
    </nav>

    <main class="app-main">
      <router-view v-slot="{ Component }">
        <transition name="fade" mode="out-in">
          <component :is="Component" />
        </transition>
      </router-view>
    </main>

    <footer class="app-footer">
      <p>© 2024 花卉种子培育跟踪系统</p>
    </footer>
  </div>
</template>

<script setup>
</script>

<style scoped>
</style>
