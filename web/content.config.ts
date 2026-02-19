import { defineContentConfig, defineCollection, z } from '@nuxt/content'

export default defineContentConfig({
  collections: {
    content: defineCollection({
      type: 'page',
      source: 'apps/*.md',
      schema: z.object({
        title: z.string(),
        description: z.string(),
        image: z.string(),
        downloadUrl: z.string(),
        latestVersion: z.string(),
        type: z.enum(['extension', 'plugin'])
      })
    })
  }
})
