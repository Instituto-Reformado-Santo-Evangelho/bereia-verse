// Namespace Global para o Core
window.ACF_CORE = window.ACF_CORE || {};

// Caches para otimização
window.ACF_CORE.BOOK_CACHE = new Map();
window.ACF_CORE.CHAPTER_START_CACHE = new Map();

// --- 1. Mapeamento de Abreviações (PT-BR) ---
window.ACF_CORE.BOOK_ABBREVIATIONS = {
  "gn": "Gênesis", "genesis": "Gênesis",
  "ex": "Êxodo", "exodo": "Êxodo",
  "lv": "Levítico", "lev": "Levítico",
  "nm": "Números", "num": "Números",
  "dt": "Deuteronômio", "deut": "Deuteronômio",
  "js": "Josué", "jos": "Josué",
  "jz": "Juízes", "jui": "Juízes",
  "rt": "Rute", "rut": "Rute",
  "1 sm": "1 Samuel", "1sm": "1 Samuel", "i sm": "1 Samuel",
  "2 sm": "2 Samuel", "2sm": "2 Samuel", "ii sm": "2 Samuel",
  "1 rs": "1 Reis", "1rs": "1 Reis", "i rs": "1 Reis",
  "2 rs": "2 Reis", "2rs": "2 Reis", "ii rs": "2 Reis",
  "1 cr": "1 Crônicas", "1cr": "1 Crônicas", "i cr": "1 Crônicas",
  "2 cr": "2 Crônicas", "2cr": "2 Crônicas", "ii cr": "2 Crônicas",
  "ed": "Esdras", "esd": "Esdras",
  "ne": "Neemias", "nee": "Neemias",
  "et": "Ester", "est": "Ester",
  "jo": "Jó", "job": "Jó", 
  "jó": "Jó",
  "sl": "Salmos", "sal": "Salmos",
  "pv": "Provérbios", "prov": "Provérbios",
  "ec": "Eclesiastes", "ecl": "Eclesiastes",
  "ct": "Cânticos", "cant": "Cânticos",
  "is": "Isaías", "isa": "Isaías",
  "jr": "Jeremias", "jer": "Jeremias",
  "lm": "Lamentações", "lam": "Lamentações",
  "ez": "Ezequiel", "eze": "Ezequiel",
  "dn": "Daniel", "dan": "Daniel",
  "os": "Oséias", "ose": "Oséias",
  "jl": "Joel",
  "am": "Amós",
  "ob": "Obadias",
  "jn": "Jonas", "jon": "Jonas",
  "mq": "Miquéias", "miq": "Miquéias",
  "na": "Naum",
  "hc": "Habacuque", "hab": "Habacuque",
  "sf": "Sofonias", "sof": "Sofonias",
  "ag": "Ageu",
  "zc": "Zacarias", "zac": "Zacarias",
  "ml": "Malaquias", "mal": "Malaquias",
  "mt": "Mateus", "mat": "Mateus",
  "mc": "Marcos", "mar": "Marcos",
  "lc": "Lucas", "luc": "Lucas",
  "jo": "João", "joao": "João", 
  "at": "Atos", "atos": "Atos",
  "rm": "Romanos", "rom": "Romanos",
  "1 co": "1 Coríntios", "1co": "1 Coríntios",
  "2 co": "2 Coríntios", "2co": "2 Coríntios",
  "gl": "Gálatas", "gal": "Gálatas",
  "ef": "Efésios",
  "efesios": "Efésios",
  "fp": "Filipenses", "fil": "Filipenses",
  "cl": "Colossenses", "col": "Colossenses",
  "colossenses": "Colossenses",
  "1 ts": "1 Tessalonicenses", "1ts": "1 Tessalonicenses",
  "2 ts": "2 Tessalonicenses", "2ts": "2 Tessalonicenses",
  "1 tm": "1 Timóteo", "1tm": "1 Timóteo",
  "2 tm": "2 Timóteo", "2tm": "2 Timóteo",
  "tt": "Tito",
  "fm": "Filemom",
  "hb": "Hebreus", "heb": "Hebreus",
  "tg": "Tiago",
  "1 pe": "1 Pedro", "1pe": "1 Pedro",
  "2 pe": "2 Pedro", "2pe": "2 Pedro",
  "1 jo": "1 João", "1jo": "1 João",
  "2 jo": "2 João", "2jo": "2 João",
  "3 jo": "3 João", "3jo": "3 João",
  "jd": "Judas",
  "ap": "Apocalipse", "apoc": "Apocalipse"
};

window.ACF_CORE.getBookData = function(rawBookName, mapping) {
  if (!mapping || !rawBookName) return null;
  
  if (window.ACF_CORE.BOOK_CACHE.has(rawBookName)) {
      return window.ACF_CORE.BOOK_CACHE.get(rawBookName);
  }

  let cleanName = rawBookName.toLowerCase().replace(/\./g, '').trim();
  let fullName = window.ACF_CORE.BOOK_ABBREVIATIONS[cleanName];
  
  if (!fullName) {
    fullName = Object.keys(mapping).find(k => k.toLowerCase() === cleanName);
  }

  if (!fullName && cleanName === 'jo') fullName = "João";
  if (!fullName && cleanName === 'jó') fullName = "Jó";

  if (!fullName) {
      window.ACF_CORE.BOOK_CACHE.set(rawBookName, null); // Cache negative result too
      return null;
  }
  
  const result = { name: fullName, data: mapping[fullName] };
  window.ACF_CORE.BOOK_CACHE.set(rawBookName, result);
  return result;
};

window.ACF_CORE.getChapterStartId = function(bookData, chapter) {
  if (chapter < 1 || chapter > bookData.chapters.length) return null;
  
  const cacheKey = `${bookData.start}-${chapter}`; // Unique key based on book start ID and chapter
  if (window.ACF_CORE.CHAPTER_START_CACHE.has(cacheKey)) {
      return window.ACF_CORE.CHAPTER_START_CACHE.get(cacheKey);
  }

  let id = bookData.start;
  for (let i = 0; i < chapter - 1; i++) {
    id += bookData.chapters[i];
  }
  
  window.ACF_CORE.CHAPTER_START_CACHE.set(cacheKey, id);
  return id;
};

window.ACF_CORE.parseVerses = function(bookData, chapter, versePart) {
  let cleanPart = versePart.replace(/[–—]/g, '-').replace(/\s+/g, '');
  const ids = [];
  const startId = window.ACF_CORE.getChapterStartId(bookData.data, chapter);
  const maxVerse = bookData.data.chapters[chapter - 1];

  if (!startId) return [];

  const groups = cleanPart.split(',');
  for (const group of groups) {
    if (group.includes('-')) {
      const parts = group.split('-');
      if (parts.length >= 2) {
        const startV = parseInt(parts[0]);
        const endV = parseInt(parts[1]);
        if (!isNaN(startV) && !isNaN(endV)) {
          for (let v = startV; v <= endV; v++) {
            if (v >= 1 && v <= maxVerse) ids.push({ id: startId + v - 1, book: bookData.name, chapter, verse: v });
          }
        }
      }
    } else {
      const v = parseInt(group);
      if (!isNaN(v) && v >= 1 && v <= maxVerse) {
        ids.push({ id: startId + v - 1, book: bookData.name, chapter, verse: v });
      }
    }
  }
  return ids;
};

window.ACF_CORE.getMinMaxVerses = function(versePart) {
  let cleanPart = versePart.replace(/[–—]/g, '-').replace(/\s+/g, '');
  const groups = cleanPart.split(',');
  let min = 9999;
  let max = -1;

  for (const group of groups) {
      if (group.includes('-')) {
          const parts = group.split('-');
          const s = parseInt(parts[0]);
          const e = parseInt(parts[1]);
          if (!isNaN(s) && s < min) min = s;
          if (!isNaN(e) && e > max) max = e;
      } else {
          const v = parseInt(group);
          if (!isNaN(v)) {
              if (v < min) min = v;
              if (v > max) max = v;
          }
      }
  }
  return { min: (min === 9999 ? 1 : min), max: (max === -1 ? 1 : max) };
};

window.ACF_CORE.processSelection = function(text, mapping) {
  if (!text || text.length < 3) return null;

  // Remove notas de rodapé [1], [12] etc. antes de processar
  const cleanText = text.replace(/\[\d+\]/g, '').replace(/\s+/g, ' '); 
  const chunks = cleanText.split(';');

  let currentBook = null;
  let allRequests = []; 

  // Regex atualizado com Lookahead Negativo para consistência
  // Adicionado flag 'g' para suportar múltiplas referências no mesmo chunk
  // Usando regex literal para evitar problemas de escape de string
  // FIX: Corrigidos caracteres de traço e o lookahead negativo que estavam corrompidos.
  // Usando escapes unicode para os dashes e removido ',' do lookahead.
  // FIX: Book name requires at least 2 chars to avoid matching 'e' as book.
  const refRegexLiteral = /((?:[1-3]\s?)?[A-Za-zá-úÁ-Úçã]{2,}\.?\s*)?(\d+)(?:\s*[:\.,]\s*((?:[\d\s\,]|[\u2013\u002d\u2014](?!\s*\d+\s*[:\.,]))+))?(?:\s*[\u2013\u002d\u2014]\s*(\d+)(?:\s*[:\.,]\s*((?:[\d\s\,]|[\u2013\u002d\u2014](?!\s*\d+\s*[:\.,]))+))?)?/g;
  
  // Create regex once
  const refRegex = new RegExp(refRegexLiteral);

  for (let chunk of chunks) {
    chunk = chunk.trim();
    if (!chunk) continue;

    chunk = chunk.replace(/^(cf\.|e\s|and\s|ver\s)/i, '').trim();

    // Reset regex for each chunk
    refRegex.lastIndex = 0;
    
    let match;
    
    while ((match = refRegex.exec(chunk)) !== null) {
      // Proteção contra loops infinitos em matches de tamanho zero (embora \d+ impeça isso)
      if (match[0].length === 0) {
          refRegex.lastIndex++;
          continue;
      }

      const rawBook = match[1];
      const startCap = parseInt(match[2]);
      let startVersePart = match[3] || "1-999";
      const endCap = match[4] ? parseInt(match[4]) : null;
      let endVersePart = match[5] || "1-999";

      // Clean trailing dashes from verse parts
      if (startVersePart) startVersePart = startVersePart.replace(/[\s\u2013\u002d\u2014]+$/, '');
      if (endVersePart) endVersePart = endVersePart.replace(/[\s\u2013\u002d\u2014]+$/, '');

      if (rawBook) {
        const found = window.ACF_CORE.getBookData(rawBook.trim(), mapping);
        if (found) currentBook = found;
      }

      if (currentBook) {
        if (endCap && endCap > startCap) {
             // Lógica de intervalo de capítulos
             const startRange = window.ACF_CORE.getMinMaxVerses(startVersePart);
             // Para o fim, pegamos o max do intervalo final
             const endRange = window.ACF_CORE.getMinMaxVerses(endVersePart);

             // 1. Processar Cap Start (do min até o fim do cap)
             const maxVerseStartCap = currentBook.data.chapters[startCap - 1];
             if (maxVerseStartCap) {
                 const part = startRange.min + "-" + maxVerseStartCap;
                 allRequests = allRequests.concat(window.ACF_CORE.parseVerses(currentBook, startCap, part));
             }

             // 2. Processar Caps do Meio (inteiros)
             for (let c = startCap + 1; c < endCap; c++) {
                 const maxV = currentBook.data.chapters[c - 1];
                 if (maxV) {
                    allRequests = allRequests.concat(window.ACF_CORE.parseVerses(currentBook, c, "1-" + maxV));
                 }
             }

             // 3. Processar Cap End (do 1 até o max do endVersePart)
             const maxVerseEndCap = currentBook.data.chapters[endCap - 1];
             if (maxVerseEndCap) {
                 // Limita o pedido ao max existente no capítulo se o texto pedir mais
                 let finalV = endRange.max;
                 if (finalV > maxVerseEndCap) finalV = maxVerseEndCap;
                 const part = "1-" + finalV;
                 allRequests = allRequests.concat(window.ACF_CORE.parseVerses(currentBook, endCap, part));
             }

        } else {
             // Lógica normal (apenas um capítulo neste trecho)
             const verses = window.ACF_CORE.parseVerses(currentBook, startCap, startVersePart);
             allRequests = allRequests.concat(verses);
        }
      }
    }
  }

  const unique = [];
  const map = new Map();
  for (const item of allRequests) {
    if(!map.has(item.id)){
        map.set(item.id, true);
        unique.push(item);
    }
  }
  
  return unique;
};
