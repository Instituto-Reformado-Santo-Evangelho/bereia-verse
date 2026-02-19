<script setup>
const { data: apps } = await useAsyncData('apps', () => {
  return queryCollection('content').all()
})
</script>

<template>
  <div>
    <main>
        <div class="intro">
            <p>Desenvolvemos ferramentas tecnológicas para facilitar o estudo, a disseminação e a compreensão das Sagradas Escrituras na era digital. Nossas soluções visam integrar a riqueza da Bíblia Sagrada (ACF) diretamente ao seu fluxo de leitura e publicação.</p>
        </div>

        <div class="cards-container" v-if="apps">
            <div v-for="app in apps" :key="app.path" class="card">
                <div class="card-image">
                    <img :src="app.image" :alt="'Preview de ' + app.title">
                </div>
                <div class="card-content">
                    <div class="card-title-row">
                        <h2>{{ app.title }}</h2>
                        <span class="version-tag">v{{ app.latestVersion }}</span>
                    </div>
                    <p>{{ app.description }}</p>
                    
                    <a :href="app.downloadUrl" class="btn-download">
                        <svg viewBox="0 0 24 24" width="20" height="20" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
                        Baixar {{ app.type === 'extension' ? 'Extensão' : 'Plugin' }}
                    </a>
                    
                    <NuxtLink :to="app.path" class="history-link">
                        Histórico de Alterações
                        <svg viewBox="0 0 24 24" width="16" height="16" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><line x1="5" y1="12" x2="19" y2="12"></line><polyline points="12 5 19 12 12 19"></polyline></svg>
                    </NuxtLink>
                </div>
            </div>
        </div>

        <section class="installation-guide" id="como-instalar">
            <h2>Como Instalar</h2>

            <!-- Chrome Guide -->
            <div class="guide-block">
                <h3>Extensão para Chrome</h3>
                <div class="steps-container">
                    <div class="step-card">
                        <div class="step-number">1</div>
                        <div class="step-image">
                            <img src="/assets/extrair-extension.png" alt="Extrair arquivo ZIP">
                        </div>
                        <div class="step-content">
                            <h4>Baixar e Extrair</h4>
                            <p>Após baixar o arquivo .zip, clique com o botão direito e escolha "Extrair" ou extraia para uma pasta de sua preferência.</p>
                        </div>
                    </div>
                    
                    <div class="step-card">
                        <div class="step-number">2</div>
                        <div class="step-image">
                            <img src="/assets/instalar-extension.png" alt="Carregar sem compactação no Chrome">
                        </div>
                        <div class="step-content">
                            <h4>Instalar no Navegador</h4>
                            <p>Acesse <code>chrome://extensions</code>, ative o <strong>Modo do desenvolvedor</strong> (canto superior direito) e clique em <strong>Carregar sem compactação</strong>. Selecione a pasta extraída.</p>
                        </div>
                    </div>

                    <div class="step-card">
                        <div class="step-number">3</div>
                        <div class="step-image">
                            <img src="/assets/config-fixar-extension.jpeg" alt="Fixar e configurar extensão">
                        </div>
                        <div class="step-content">
                            <h4>Fixar e Usar</h4>
                            <p>A extensão aparecerá na sua lista. Clique no ícone de quebra-cabeça e fixe a extensão para facilitar o acesso às configurações.</p>
                        </div>
                    </div>
                </div>
            </div>

            <!-- WP Guide -->
            <div class="guide-block">
                <h3>Plugin para WordPress</h3>
                <div class="steps-container">
                    <div class="step-card">
                        <div class="step-number">1</div>
                        <div class="step-image">
                            <img src="/assets/upload-plugin.png" alt="Upload de plugin no WordPress">
                        </div>
                        <div class="step-content">
                            <h4>Enviar Plugin</h4>
                            <p>No painel do WordPress, vá em <strong>Plugins > Adicionar Novo > Enviar Plugin</strong>. Selecione o arquivo .zip baixado e clique em Instalar Agora.</p>
                        </div>
                    </div>

                    <div class="step-card">
                        <div class="step-number">2</div>
                        <div class="step-image">
                            <img src="/assets/ativar-plugin.png" alt="Configurações do Plugin">
                        </div>
                        <div class="step-content">
                            <h4>Ativar e Configurar</h4>
                            <p>Após ativar o plugin, acesse o menu <strong>Bereia Versículos</strong> em <strong>configurações</strong> na barra lateral para personalizar a marcação de referências.</p>
                        </div>
                    </div>
                </div>
            </div>
        </section>
    </main>
  </div>
</template>
