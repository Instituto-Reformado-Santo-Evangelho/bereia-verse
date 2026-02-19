import { defineEventHandler, getRequestURL, H3Event } from 'h3';

// The HTMLRewriter class is available globally in the Cloudflare Pages environment.
// We declare it here to satisfy TypeScript.
declare class HTMLRewriter {
  on(selector: string, handlers: { 
    element?: (element: Element) => void;
    text?: (text: TextChunk) => void;
  }): this;
  transform(response: Response): Response;
}

interface Element {
  getAttribute(name: string): string | null;
  setAttribute(name: string, value: string): void;
}

interface TextChunk {
  readonly text: string;
  readonly lastInTextNode: boolean;
  replace(content: string, options?: { html?: boolean }): void;
}

export default defineEventHandler(async (event: H3Event) => {
  const url = getRequestURL(event);
  const pathname = url.pathname;

  // Handle CORS preflight requests
  if (event.method === 'OPTIONS') {
    return new Response(null, {
      headers: {
        'Access-Control-Allow-Origin': '*',
        'Access-Control-Allow-Methods': 'GET, HEAD, POST, OPTIONS',
        'Access-Control-Allow-Headers': '*',
        'Access-Control-Max-Age': '86400',
      },
    });
  }

  // Paths served by the local Nuxt app remain the same
  const nuxtInternalAssets = ['/_nuxt', '/assets', '/downloads'];
  const nuxtAppRoutes = ['/apps', '/auth', '/bereia-versiculos'];
  const nuxtPublicFiles = ['/favicon.ico', '/robots.txt'];

  if (
    nuxtInternalAssets.some(p => pathname.startsWith(p)) ||
    nuxtAppRoutes.some(p => pathname.startsWith(p)) ||
    nuxtPublicFiles.includes(pathname)
  ) {
    return; // Let Nuxt handle its own routes
  }

  // Determine the proxy target URL
  let targetUrl;
  if (pathname.startsWith('/wp-')) {
    targetUrl = `https://santoevangelho.com.br${pathname}${url.search}`;
  } else {
    const targetPath = pathname === '/' ? '/tecnologia/' : `/tecnologia${pathname}`;
    targetUrl = `https://santoevangelho.com.br${targetPath}${url.search}`;
  }

  try {
    // Fetch from the origin
    const response = await fetch(targetUrl, {
      headers: {
        'host': 'santoevangelho.com.br',
        'User-Agent': event.headers.get('user-agent') || 'Mozilla/5.0',
      }
    });

    const contentType = response.headers.get('content-type') || '';

    // Create a base for our new headers
    const newHeaders = new Headers(response.headers);
    newHeaders.set('Access-Control-Allow-Origin', '*');
    newHeaders.set('Access-Control-Allow-Methods', 'GET, HEAD, POST, OPTIONS');
    newHeaders.set('Access-Control-Allow-Headers', '*');
    
    // Remove headers that might cause security/rendering issues when proxied
    newHeaders.delete('content-security-policy');
    newHeaders.delete('x-frame-options');

    // Only rewrite text-based content
    const isHtml = contentType.includes('text/html');
    const isCss = contentType.includes('text/css');
    const isJs = contentType.includes('javascript') || contentType.includes('application/x-javascript');
    const isJson = contentType.includes('json');

    if (isHtml) {
      const rewriter = new HTMLRewriter();

      // Define a handler to rewrite specific attributes on various elements
      const attributeHandler = {
        element: (element: Element) => {
          const attributesToRewrite = ['href', 'src', 'action', 'srcset', 'data-src', 'data-href'];
          for (const attr of attributesToRewrite) {
            const value = element.getAttribute(attr);
            if (value && value.includes('santoevangelho.com.br')) {
              const newValue = value.replace(/https?:\/\/santoevangelho\.com\.br/g, '');
              element.setAttribute(attr, newValue);
            }
          }
        },
      };

      // Rewrite absolute URLs in style tags (safe for CSS)
      const styleHandler = {
        text: (text: TextChunk) => {
          if (text.text.includes('santoevangelho.com.br')) {
            const newContent = text.text.replace(/https?:\/\/santoevangelho\.com\.br/g, '');
            text.replace(newContent);
          }
        },
      };

      // Apply the handlers (avoiding 'script' text to prevent syntax errors)
      rewriter.on('a, link, script, img, form, iframe, source', attributeHandler);
      rewriter.on('style', styleHandler);
      
      const transformedResponse = rewriter.transform(response);
      return new Response(transformedResponse.body, {
        status: transformedResponse.status,
        statusText: transformedResponse.statusText,
        headers: newHeaders
      });
    }

    // Rewrite CSS, JS, and JSON files as well - Safe as we process the full text
    if (isCss || isJs || isJson) {
      let text = await response.text();
      // Handle both normal and escaped URLs (https:\/\/...)
      const rewrittenText = text.replace(/https?(:|\\:|%3A)(\/|\\\/|%2F)(\/|\\\/|%2F)santoevangelho\.com\.br/g, '');
      
      return new Response(rewrittenText, {
        status: response.status,
        statusText: response.statusText,
        headers: newHeaders
      });
    }

    // For other content (images, fonts), return as-is with CORS headers
    return new Response(response.body, {
      status: response.status,
      statusText: response.statusText,
      headers: newHeaders
    });

  } catch (error) {
    console.error(`[Proxy Error] Failed to fetch "${targetUrl}".`, error);
    return new Response(`Proxy request to origin server failed.`, { status: 502 });
  }
});
