export interface Env {
	DB: D1Database;
}

export default {
	async fetch(request: Request, env: Env, ctx: ExecutionContext): Promise<Response> {
		const url = new URL(request.url);
		
		// 1. Configurar Cache
		const cache = caches.default;
		
		// Tentar encontrar resposta no cache
		// Importante: O cache usa a URL completa (incluindo query params) como chave
		let cachedResponse = await cache.match(request);

		if (cachedResponse) {
			console.log(`Cache HIT: ${url.pathname}${url.search}`);
			return cachedResponse;
		}

		console.log(`Cache MISS: ${url.pathname}${url.search}`);

		// Headers padrão (CORS + Cache)
		const corsHeaders = {
			"Content-Type": "application/json",
			"Access-Control-Allow-Origin": "*",
			"Access-Control-Allow-Methods": "GET, OPTIONS",
			"Access-Control-Allow-Headers": "Content-Type",
			// Cache por 1 ano, imutável
			"Cache-Control": "public, max-age=31536000, immutable" 
		};

		// Handle OPTIONS (Preflight)
		if (request.method === "OPTIONS") {
			return new Response(null, { headers: corsHeaders });
		}

		const pathParts = url.pathname.split('/').filter(Boolean);
		let resultResponse: Response | null = null;

		// --- Lógica da Aplicação ---

		try {
			// Endpoint: Batch (Vários IDs)
			if (pathParts[0] === 'api' && pathParts[1] === 'verses' && pathParts[2] === 'batch') {
				const idsParam = url.searchParams.get('ids');
				
				if (!idsParam) {
					return new Response(JSON.stringify({ error: "Missing ids parameter" }), { status: 400, headers: corsHeaders });
				}

				const expandedIds: number[] = [];
				const parts = idsParam.split(',');

				for (const part of parts) {
					const trimmedPart = part.trim();
					if (trimmedPart.includes('-')) {
						const [startStr, endStr] = trimmedPart.split('-');
						const start = parseInt(startStr);
						const end = parseInt(endStr);
						if (!isNaN(start) && !isNaN(end) && start <= end) {
							for (let i = start; i <= end; i++) {
								expandedIds.push(i);
							}
						}
					} else {
						const singleId = parseInt(trimmedPart);
						if (!isNaN(singleId)) {
							expandedIds.push(singleId);
						}
					}
				}

				// Filter out duplicates and sort, ensuring unique and ordered IDs
				const uniqueSortedIds = Array.from(new Set(expandedIds)).sort((a, b) => a - b);

				if (uniqueSortedIds.length === 0 || uniqueSortedIds.length > 500) { // Increased limit for expanded IDs
					return new Response(JSON.stringify({ error: "Invalid IDs or too many requested (max 500 expanded IDs)." }), { status: 400, headers: corsHeaders });
				}

				const placeholders = uniqueSortedIds.map(() => '?').join(',');
				const query = `SELECT * FROM verses WHERE id IN (${placeholders}) ORDER BY id ASC`;
				
				const stmt = env.DB.prepare(query).bind(...uniqueSortedIds);
				const { results } = await stmt.all();

				resultResponse = new Response(JSON.stringify(results), { headers: corsHeaders });
			}

			// Endpoint: Single ID
			else if (pathParts[0] === 'api' && pathParts[1] === 'verses' && pathParts[2]) {
				const verseId = parseInt(pathParts[2]);
				
				if (!isNaN(verseId)) {
					const stmt = env.DB.prepare('SELECT content FROM verses WHERE id = ?').bind(verseId);
					const result = await stmt.first();

					if (!result) {
						resultResponse = new Response(JSON.stringify({ error: "Verse not found" }), { status: 404, headers: corsHeaders });
					} else {
						resultResponse = new Response(JSON.stringify(result), { headers: corsHeaders });
					}
				}
			}
		} catch (e) {
			return new Response(JSON.stringify({ error: (e as Error).message }), { status: 500, headers: corsHeaders });
		}

		// --- Salvar no Cache e Retornar ---

		if (resultResponse) {
			// Se a resposta for sucesso (200), salvamos no cache
			if (resultResponse.status === 200) {
				// ctx.waitUntil permite que o worker responda ao usuário antes de terminar de salvar no cache
				// Usamos .clone() porque o corpo da resposta só pode ser lido uma vez
				ctx.waitUntil(cache.put(request, resultResponse.clone()));
			}
			return resultResponse;
		}

		return new Response("ACF Extension API.", { status: 200 });
	},
};