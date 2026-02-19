<script setup>
const route = useRoute()
const { data: page } = await useAsyncData(`page-${route.params.slug}`, () => {
  return queryCollection('content').path(`/apps/${route.params.slug}`).first()
})

if (!page.value) {
  throw createError({ statusCode: 404, statusMessage: 'Página não encontrada' })
}

useHead({
  title: `${page.value.title} - Histórico de Versões`,
  meta: [
    { name: 'description', content: page.value.description },
    { property: 'og:title', content: `${page.value.title} - Histórico de Versões` },
    { property: 'og:description', content: page.value.description },
    { property: 'og:image', content: page.value.image },
    { property: 'og:type', content: 'article' },
    { name: 'twitter:card', content: 'summary_large_image' },
    { name: 'twitter:title', content: `${page.value.title} - Histórico de Versões` },
    { name: 'twitter:description', content: page.value.description },
    { name: 'twitter:image', content: page.value.image }
  ]
})
</script>

<template>
  <main v-if="page" class="doc-page">
    <!-- Hero Section colada sem margem superior -->
    <div class="hero-section" style="background-image: linear-gradient(rgba(0,0,0,0.7), rgba(5,5,5,1)), url('/assets/default-post.webp');">
      <div class="container">
        <div class="hero-content">
          <div class="title-meta">
            <h1>{{ page.title }}</h1>
            <span class="badge">Versão Atual {{ page.latestVersion }}</span>
          </div>
        </div>
      </div>
    </div>

    <div class="container content-wrapper">
      <article class="prose-container">
        <!-- Removida duplicidade de imagem aqui -->
        
        <p class="lead">{{ page.description }}</p>

        <section class="doc-content">
          <ContentRenderer :value="page" />
        </section>

        <div class="doc-footer">
            <a :href="page.downloadUrl" class="btn-download">
                <svg viewBox="0 0 24 24" width="22" height="22" fill="currentColor"><path d="M19 9h-4V3H9v6H5l7 7 7-7zM5 18v2h14v-2H5z"/></svg>
                Baixar Versão Recente
            </a>
            
            <nav class="breadcrumb-bottom">
              <NuxtLink to="/" class="nav-link">
                <svg xmlns="http://www.w3.org/2000/svg" width="20" height="20" viewBox="0 0 24 24" fill="none" stroke="currentColor" stroke-width="2.5" stroke-linecap="round" stroke-linejoin="round"><polyline points="15 18 9 12 15 6"></polyline></svg>
                Voltar para Início
              </NuxtLink>
            </nav>
        </div>
      </article>
    </div>
  </main>
</template>

<style scoped>
.doc-page {
    background-color: var(--bg-color);
    padding: 0 !important;
    margin: 0 !important;
    max-width: 100% !important;
    width: 100%;
}

.hero-section {
    width: 100%;
    background-size: cover;
    background-position: center;
    padding: 6rem 0 6rem;
    border-bottom: 1px solid rgba(255,255,255,0.05);
    margin-top: 0;
    position: relative;
}

.hero-section::after {
    content: '';
    position: absolute;
    bottom: 0;
    left: 0;
    width: 100%;
    height: 100px;
    background: linear-gradient(to top, var(--bg-color), transparent);
}

.container {
    max-width: 900px;
    margin: 0 auto;
    padding: 0 2rem;
    position: relative;
    z-index: 2;
}

.breadcrumb-bottom { 
    margin-top: 3rem;
    display: flex;
    justify-content: center;
}

.nav-link {
    display: inline-flex;
    align-items: center;
    gap: 10px;
    color: var(--gold-primary);
    font-weight: 500;
    padding: 10px 0;
    font-size: 0.9rem;
    text-transform: uppercase;
    letter-spacing: 1px;
    transition: all 0.3s;
}

.hero-content {
    display: flex;
    align-items: center;
    gap: 4rem;
}

.title-meta h1 { 
    font-size: 3.5rem; 
    color: #fff; 
    line-height: 1.1; 
    margin: 0 0 1rem 0;
    text-shadow: 0 4px 10px rgba(0,0,0,0.5);
}

.badge { 
    color: var(--gold-primary); 
    font-weight: 600; 
    text-transform: uppercase; 
    font-size: 1rem; 
    letter-spacing: 2px;
    display: block; 
}

.content-wrapper { margin-top: 6rem; padding-bottom: 10rem; }

.lead { 
    font-size: 1.4rem; 
    color: var(--text-secondary); 
    line-height: 1.8; 
    margin-bottom: 6rem;
    border-left: 3px solid var(--gold-primary);
    padding-left: 2rem;
}

.doc-content { font-size: 1.2rem; line-height: 1.8; color: var(--text-primary); }

.doc-content :deep(h2) {
    color: var(--gold-primary);
    font-size: 2rem;
    margin: 5rem 0 2rem;
    padding-bottom: 1rem;
    border-bottom: 1px solid rgba(255,255,255,0.05);
}

.doc-content :deep(p) { margin-bottom: 2rem; }

.doc-content :deep(li) { margin-bottom: 1rem; color: var(--text-secondary); }

.doc-footer {
    margin-top: 6rem;
    padding-top: 2rem;
    border-top: none;
    text-align: center;
}

.doc-footer a {
    text-decoration: none;
    border-bottom: none;
}

@media (max-width: 768px) {
    .hero-content { flex-direction: column; text-align: center; gap: 3rem; }
    .title-meta h1 { font-size: 2.5rem; }
    .app-icon { width: 120px; height: 120px; }
}
</style>
