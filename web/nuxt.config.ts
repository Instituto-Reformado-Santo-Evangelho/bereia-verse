// https://nuxt.com/docs/api/configuration/nuxt-config
export default defineNuxtConfig({
  compatibilityDate: '2024-11-01',
  nitro: {
    preset: 'cloudflare-pages'
  },
  devtools: { enabled: true },
  modules: ['@nuxt/content'],
  css: ['~/assets/css/main.css'],
  app: {
    head: {
      title: 'Tecnologia - IRSE',
      meta: [
        { charset: 'utf-8' },
        { name: 'viewport', content: 'width=device-width, initial-scale=1' },
        { property: 'og:type', content: 'website' },
        { property: 'og:title', content: 'Tecnologia - IRSE' },
        { property: 'og:description', content: 'Tecnologia a serviço do Reino. Ferramentas gratuitas para estudo bíblico: Extensão Chrome e Plugin WordPress.' },
        { property: 'og:image', content: '/assets/logo-3d.webp' }, // Path adjusted for public/
        { name: 'twitter:card', content: 'summary_large_image' },
        { name: 'twitter:title', content: 'Tecnologia - IRSE' },
        { name: 'twitter:description', content: 'Tecnologia a serviço do Reino. Ferramentas gratuitas para estudo bíblico.' },
        { name: 'twitter:image', content: '/assets/logo-3d.webp' }
      ],
      link: [
        { rel: 'icon', type: 'image/png', href: '/assets/logo.png' },
        { rel: 'stylesheet', href: 'https://fonts.googleapis.com/css2?family=Cinzel:wght@400;700&family=Inter:wght@300;400;600&display=swap' }
      ]
    }
  }
})