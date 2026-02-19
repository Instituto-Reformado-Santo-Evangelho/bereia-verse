<script setup>
/**
 * Página de Autenticação para o Bereia Versículos
 * Local esperado: pages/auth.vue
 */

const route = useRoute()
const authCode = computed(() => route.query.code)
const copied = ref(false)

definePageMeta({
  layout: 'auth'
})

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
    <div class="auth-content">
      <div class="auth-icon-wrapper">
        <img src="./logo.png" alt="Bereia Logo" class="logo-auth" />
      </div>
      
      <h1 v-if="authCode">Autorização</h1>
      <h1 v-else class="error">Falha no Acesso</h1>

      <p v-if="authCode" class="description">
        Seu código de acesso foi gerado com sucesso. Use-o para entrar no aplicativo Bereia Versículos.
      </p>
      <p v-else class="description">
        Não foi possível processar sua autorização. Por favor, tente iniciar o login novamente a partir do app.
      </p>

      <div v-if="authCode" class="code-section">
        <div class="success-badge" :class="{ show: copied }">
          ✓ CÓDIGO COPIADO
        </div>
        <div class="code-box" @click="copyCode" title="Clique para copiar">
          {{ authCode }}
        </div>
      </div>

      <div class="actions" v-if="authCode">
        <button class="btn-primary" @click="copyCode">
          {{ copied ? 'COPIADO!' : 'COPIAR CÓDIGO' }}
        </button>
        <button class="btn-secondary" @click="tryOpenApp">
          ABRIR O APLICATIVO
        </button>
      </div>

      <p class="footer-info">
        Caso o redirecionamento automático não funcione, você pode colar o código manualmente na tela de login do app.
      </p>
    </div>
  </div>
</template>

<style scoped>
.auth-container {
  font-family: var(--font-body);
  background-color: var(--bg-color);
  min-height: 100vh;
  display: flex;
  flex-direction: column;
  justify-content: center;
  align-items: center;
  padding: 40px 20px;
  position: relative;
}

.auth-content {
  text-align: center;
  max-width: 600px;
  width: 100%;
  z-index: 1;
}

.auth-icon-wrapper {
  margin-bottom: 40px;
  position: relative;
  display: inline-block;
}

.logo-auth {
  width: 120px;
  height: 120px;
  border-radius: 50%;
  background: var(--gold-primary);
  padding: 10px;
  box-shadow: 0 0 40px rgba(255, 174, 0, 0.2);
  filter: drop-shadow(0 0 15px rgba(255, 174, 0, 0.3));
}

h1 {
  font-family: var(--font-heading);
  color: #fff;
  margin-bottom: 16px;
  font-size: 2.5rem;
  letter-spacing: 2px;
  text-transform: uppercase;
}

h1.error {
  color: #f87171;
}

.description {
  color: var(--text-secondary);
  line-height: 1.8;
  margin-bottom: 40px;
  font-size: 1.1rem;
  max-width: 500px;
  margin-left: auto;
  margin-right: auto;
}

.code-section {
  width: 100%;
  position: relative;
  margin-bottom: 48px;
}

.code-box {
  background: var(--surface-dark);
  border: 1px solid rgba(255, 174, 0, 0.3);
  padding: 24px;
  border-radius: var(--border-radius);
  font-family: 'JetBrains Mono', 'Cascadia Code', monospace;
  font-size: 1.5rem;
  color: var(--gold-primary);
  word-break: break-all;
  cursor: pointer;
  transition: var(--transition-slow);
  user-select: all;
  box-shadow: 0 10px 30px rgba(0, 0, 0, 0.5);
  letter-spacing: 2px;
}

.code-box:hover {
  border-color: var(--gold-primary);
  background: var(--surface-light);
  transform: translateY(-2px);
  box-shadow: 0 15px 40px rgba(0, 0, 0, 0.6), 0 0 20px rgba(255, 174, 0, 0.1);
}

.success-badge {
  position: absolute;
  top: -15px;
  left: 50%;
  transform: translateX(-50%) translateY(10px);
  background: #10b981;
  color: white;
  padding: 6px 16px;
  border-radius: 50px;
  font-size: 0.85rem;
  font-weight: 700;
  box-shadow: 0 4px 12px rgba(0, 0, 0, 0.3);
  opacity: 0;
  transition: all 0.3s cubic-bezier(0.175, 0.885, 0.32, 1.275);
  pointer-events: none;
  z-index: 10;
}

.success-badge.show {
  transform: translateX(-50%) translateY(0);
  opacity: 1;
}

.actions {
  display: flex;
  flex-direction: column;
  gap: 16px;
  width: 100%;
  max-width: 320px;
  margin: 0 auto;
}

.btn-primary {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 1.1rem;
  background: var(--gold-primary);
  color: #000;
  font-weight: 600;
  text-transform: uppercase;
  letter-spacing: 2px;
  border: none;
  border-radius: 50px;
  transition: all 0.3s ease;
  cursor: pointer;
  font-size: 0.9rem;
}

.btn-primary:hover {
  background: #fff;
  transform: translateY(-2px);
  box-shadow: 0 0 20px rgba(255, 255, 255, 0.2);
}

.btn-secondary {
  display: flex;
  align-items: center;
  justify-content: center;
  width: 100%;
  padding: 1.1rem;
  background: transparent;
  color: var(--text-secondary);
  font-weight: 500;
  text-transform: uppercase;
  letter-spacing: 2px;
  border: 1px solid rgba(255, 255, 255, 0.1);
  border-radius: 50px;
  transition: all 0.3s ease;
  cursor: pointer;
  font-size: 0.8rem;
}

.btn-secondary:hover {
  background: rgba(255, 255, 255, 0.05);
  color: #fff;
  border-color: rgba(255, 255, 255, 0.3);
}

.footer-info {
  margin-top: 60px;
  font-size: 0.9rem;
  color: var(--text-secondary);
  max-width: 400px;
  margin-left: auto;
  margin-right: auto;
  opacity: 0.6;
}

@media (max-width: 600px) {
  h1 { font-size: 1.8rem; }
  .code-box { font-size: 1.2rem; padding: 16px; }
  .logo-auth { width: 90px; height: 90px; }
}
</style>
