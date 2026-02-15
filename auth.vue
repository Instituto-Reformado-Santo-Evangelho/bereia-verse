<script setup lang="ts">
/**
 * Página de Autenticação para o Bereia Versículos
 * Local esperado: pages/auth.vue
 */

const route = useRoute()
const authCode = computed(() => route.query.code as string)
const copied = ref(false)

// Tenta abrir o app automaticamente
const tryOpenApp = () => {
  if (authCode.value) {
    window.location.href = `bereia-verse://auth?code=${authCode.value}`
  }
}

const copyCode = async () => {
  if (!authCode.value) return
  await navigator.clipboard.writeText(authCode.value)
  copied.value = true
  setTimeout(() => { copied.value = false }, 3000)
}

onMounted(() => {
  if (authCode.value) {
    // Tenta o redirecionamento automático após 1.5s para dar tempo da página carregar
    setTimeout(tryOpenApp, 1500)
  }
})
</script>

<template>
  <div class="auth-container">
    <div class="card">
      <img src="/logo.png" alt="Bereia Logo" class="logo" />
      
      <h1 v-if="authCode">Autorização Concluída</h1>
      <h1 v-else class="error">Erro na Autorização</h1>

      <p v-if="authCode">
        Estamos tentando redirecionar você de volta para o aplicativo automaticamente.
      </p>
      <p v-else>
        Não foi possível localizar o código de autorização na URL.
      </p>

      <div v-if="authCode" class="success-badge" :class="{ show: copied }">
        ✓ Código Copiado!
      </div>

      <div v-if="authCode" class="code-box" @click="copyCode">
        {{ authCode }}
      </div>

      <div class="actions" v-if="authCode">
        <button class="btn-primary" @click="copyCode">
          Copiar Código Manualmente
        </button>
        <button class="btn-secondary" @click="tryOpenApp">
          Tentar Abrir o App Novamente
        </button>
      </div>

      <p class="footer">
        Se o aplicativo não abrir sozinho, copie o código acima e cole no campo de login do Bereia Versículos.
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  font-family: 'Segoe UI', system-ui, -apple-system, sans-serif;
  background-color: #f4f4f9;
  min-height: 100vh;
  display: flex;
  justify-content: center;
  align-items: center;
  padding: 20px;
}

.card {
  background: white;
  padding: 40px;
  border-radius: 20px;
  box-shadow: 0 10px 30px rgba(0,0,0,0.08);
  text-align: center;
  max-width: 480px;
  width: 100%;
}

.logo {
  width: 80px;
  height: 80px;
  margin-bottom: 20px;
}

h1 {
  color: #FFB300;
  margin-bottom: 10px;
  font-size: 1.8rem;
}

h1.error {
  color: #f44336;
}

p {
  color: #666;
  line-height: 1.6;
  margin-bottom: 25px;
}

.code-box {
  background: #fffcf0;
  border: 2px dashed #FFB300;
  padding: 15px;
  border-radius: 12px;
  font-family: 'Cascadia Code', monospace;
  font-size: 1.1rem;
  word-break: break-all;
  margin-bottom: 25px;
  cursor: pointer;
  transition: transform 0.2s;
}

.code-box:hover {
  transform: scale(1.02);
}

.success-badge {
  color: #2e7d32;
  font-weight: bold;
  margin-bottom: 15px;
  opacity: 0;
  transition: opacity 0.3s;
}

.success-badge.show {
  opacity: 1;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 10px;
}

button {
  padding: 12px 24px;
  border-radius: 10px;
  font-weight: 600;
  cursor: pointer;
  transition: all 0.2s;
  border: none;
  font-size: 1rem;
}

.btn-primary {
  background-color: #FFB300;
  color: white;
}

.btn-primary:hover {
  background-color: #FFA000;
}

.btn-secondary {
  background-color: #f0f0f0;
  color: #555;
}

.btn-secondary:hover {
  background-color: #e5e5e5;
}

.footer {
  margin-top: 30px;
  font-size: 0.85rem;
  color: #999;
}
</style>
