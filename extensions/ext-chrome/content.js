// --- START bible-parser.js ---
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

// --- END bible-parser.js ---

// --- START popup-ui.js ---
// Namespace Global para UI
window.ACF_CORE = window.ACF_CORE || {};
window.ACF_CORE.popup = null;
window.ACF_CORE.popupId = 'acf-tooltip-popup'; // Default, será sobrescrito pelo adapter

// --- Interface para Adaptador (Defaults) ---
window.ACF_CORE.getAssetUrl = function(filename) {
    return filename;
};

// --- Funções de UI ---

window.ACF_CORE.hideTimer = null;

window.ACF_CORE.hidePopupWithDelay = function() {
    if (window.ACF_CORE.hideTimer) clearTimeout(window.ACF_CORE.hideTimer);
    window.ACF_CORE.hideTimer = setTimeout(() => {
        if (window.ACF_CORE.popup) {
            window.ACF_CORE.popup.style.display = 'none';
        }
    }, 300);
};

window.ACF_CORE.cancelHidePopup = function() {
    if (window.ACF_CORE.hideTimer) {
        clearTimeout(window.ACF_CORE.hideTimer);
        window.ACF_CORE.hideTimer = null;
    }
};

window.ACF_CORE.createPopup = function() {
  const id = window.ACF_CORE.popupId;
  const existing = document.getElementById(id);
  if (existing) {
    window.ACF_CORE.popup = existing;
    return;
  }
  window.ACF_CORE.popup = document.createElement('div');
  window.ACF_CORE.popup.id = id;
  // Adiciona classe comum para CSS compartilhado
  window.ACF_CORE.popup.className = 'acf-popup-container'; 

  // Aplicar tema salvo
  const savedTheme = localStorage.getItem('acf_theme') || 'light';
  if (savedTheme === 'dark') {
    window.ACF_CORE.popup.classList.add('acf-dark-theme');
  }

  document.body.appendChild(window.ACF_CORE.popup);
  
  // Interação Robusta: Mouse entrou no popup, cancela fechamento
  window.ACF_CORE.popup.addEventListener('mouseenter', () => {
    window.ACF_CORE.cancelHidePopup();
  });

  // Mouse saiu do popup, inicia fechamento com delay
  window.ACF_CORE.popup.addEventListener('mouseleave', () => {
    window.ACF_CORE.hidePopupWithDelay();
  });

  // Delegar clique do botão de tema
  window.ACF_CORE.popup.addEventListener('click', (e) => {
    const toggle = e.target.closest('.acf-theme-toggle');
    if (toggle) {
        e.preventDefault();
        e.stopPropagation();
        const isDark = window.ACF_CORE.popup.classList.toggle('acf-dark-theme');
        localStorage.setItem('acf_theme', isDark ? 'dark' : 'light');
        // Atualizar ícone sem fechar o popup
        const iconContainer = toggle;
        iconContainer.innerHTML = isDark ? window.ACF_CORE.getSunIcon() : window.ACF_CORE.getMoonIcon();
    }
  });
  
  document.addEventListener('mousedown', (e) => {
    if (!window.ACF_CORE.popup || window.ACF_CORE.popup.style.display !== 'block') return;
    
    // Se clicar dentro do popup, não fecha
    if (window.ACF_CORE.popup.contains(e.target)) return;
    
    // Se clicar numa seta (feature WP), não fecha aqui (deixa o handler da seta cuidar)
    if (e.target.closest && e.target.closest('.acf-ref-arrow')) return; 

    window.ACF_CORE.popup.style.display = 'none';
  });
};

window.ACF_CORE.getSunIcon = () => `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12 7c-2.76 0-5 2.24-5 5s2.24 5 5 5 5-2.24 5-5-2.24-5-5-5zM2 13h2c.55 0 1-.45 1-1s-.45-1-1-1H2c-.55 0-1 .45-1 1s.45 1 1 1zm18 0h2c.55 0 1-.45 1-1s-.45-1-1-1h-2c-.55 0-1 .45-1 1s.45 1 1 1zM11 2v2c0 .55.45 1 1 1s1-.45 1-1V2c0-.55-.45-1-1-1s-1 .45-1 1zm0 18v2c0 .55.45 1 1 1s1-.45 1-1v-2c0-.55-.45-1-1-1s-1 .45-1 1zM5.99 4.58c-.39-.39-1.03-.39-1.41 0s-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41L5.99 4.58zm12.37 12.37c-.39-.39-1.03-.39-1.41 0s-.39 1.03 0 1.41l1.06 1.06c.39.39 1.03.39 1.41 0s.39-1.03 0-1.41l-1.06-1.06zm1.06-10.96c.39-.39.39-1.03 0-1.41s-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06zM7.05 18.36c.39-.39.39-1.03 0-1.41s-1.03-.39-1.41 0l-1.06 1.06c-.39.39-.39 1.03 0 1.41s1.03.39 1.41 0l1.06-1.06z"/></svg>`;
window.ACF_CORE.getMoonIcon = () => `<svg viewBox="0 0 24 24" xmlns="http://www.w3.org/2000/svg"><path d="M12.1 22c4.9 0 9-3.6 9.8-8.3.2-.8-.7-1.3-1.4-.9-1 .6-2.2 1-3.5 1-4.4 0-8-3.6-8-8 0-1.3.3-2.5 1-3.5.4-.7-.1-1.6-.9-1.4C3.6 2.9 0 7 0 11.9 0 17.5 4.5 22 10.1 22c.7 0 1.4-.1 2-.1z"/></svg>`;

window.ACF_CORE.showPopup = function(x, y, title) {
  if (!window.ACF_CORE.popup) window.ACF_CORE.createPopup();
  const popup = window.ACF_CORE.popup;
  if (!popup) return;

  // Largura dinâmica para cálculos de colisão
  const windowWidth = window.innerWidth;
  const margin = 10;
  const popupWidth = Math.min(450, windowWidth - (margin * 2)); 

  // Ajuste de colisão lateral
  if (x + popupWidth > windowWidth - margin) {
      x = windowWidth - popupWidth - margin;
  }
  if (x < margin) {
      x = margin;
  }

  popup.style.width = `${popupWidth}px`;
  popup.style.left = `${x}px`;
  popup.style.top = `${y}px`;
  popup.style.display = 'block';
  
  const logoUrl = window.ACF_CORE.getAssetUrl('logo.png');
  const isDark = popup.classList.contains('acf-dark-theme');
  const themeIcon = isDark ? window.ACF_CORE.getSunIcon() : window.ACF_CORE.getMoonIcon();
  
  popup.innerHTML = `
    <div class="acf-header">
        <div class="acf-header-left">
            <a href="https://santoevangelho.com.br" target="_blank" rel="noopener noreferrer">
                <img src="${logoUrl}" class="acf-logo" alt="Logo" />
            </a>
            <span>${title} | IRSE</span>
        </div>
        <button class="acf-theme-toggle" title="Alternar Tema">
            ${themeIcon}
        </button>
    </div>
    <div class="acf-body acf-loading">Carregando referências...</div>
    <div class="acf-footer">
        <a href="https://biblias.com.br/acfonline" target="_blank" rel="noopener noreferrer">ACF2011 - SBTB</a>
    </div>
  `;
};

window.ACF_CORE.updatePopupContent = function(html, title) {
  const popup = window.ACF_CORE.popup;
  if (popup) {
    const headerSpan = popup.querySelector('.acf-header span');
    if (headerSpan) headerSpan.innerText = `${title} | IRSE`;
    
    popup.querySelector('.acf-body').innerHTML = html;
    popup.querySelector('.acf-body').classList.remove('acf-loading');
  }
};

window.ACF_CORE.updatePopupError = function() {
  const popup = window.ACF_CORE.popup;
  if (popup) {
    popup.querySelector('.acf-body').innerHTML = `<div class="acf-error">Não foi possível carregar os versículos.</div>`;
    popup.querySelector('.acf-body').classList.remove('acf-loading');
  }
};

window.ACF_CORE.getAbbreviatedIdsParam = function(requests) {
  if (!requests || requests.length === 0) return "";

  // Ensure requests are sorted by ID, though they should already be, as processSelection sorts them.
  const sortedRequests = [...requests].sort((a, b) => a.id - b.id);

  const parts = [];
  if (sortedRequests.length > 0) {
    let currentRangeStart = sortedRequests[0].id;
    let currentRangeEnd = sortedRequests[0].id;

    for (let i = 1; i < sortedRequests.length; i++) {
      if (sortedRequests[i].id === currentRangeEnd + 1) {
        currentRangeEnd = sortedRequests[i].id;
      } else {
        if (currentRangeStart === currentRangeEnd) {
          parts.push(currentRangeStart.toString());
        } else {
          parts.push(`${currentRangeStart}-${currentRangeEnd}`);
        }
        currentRangeStart = sortedRequests[i].id;
        currentRangeEnd = sortedRequests[i].id;
      }
    }

    // Add the last range/single ID
    if (currentRangeStart === currentRangeEnd) {
      parts.push(currentRangeStart.toString());
    } else {
      parts.push(`${currentRangeStart}-${currentRangeEnd}`);
    }
  }

  return parts.join(',');
};

window.ACF_CORE.requestCache = new Map();

// Orquestrador: Recebe requests processados, mostra popup e faz fetch
window.ACF_CORE.fetchAndShow = async function(requests, x, y) {
    if (!requests || requests.length === 0) return;

    let title = "Versículos Selecionados";
    const isSingleVerse = requests.length === 1;
    let allSame = false;

    if (isSingleVerse) {
      title = `${requests[0].book} ${requests[0].chapter}:${requests[0].verse}`;
    } else {
      const first = requests[0];
      allSame = requests.every(r => r.book === first.book && r.chapter === first.chapter);
      if (allSame) {
        title = `${first.book} ${first.chapter}`;
      } else {
        title = "Versículos Selecionados";
      }
    }

    window.ACF_CORE.showPopup(x, y, title);

    try {
      const safeRequests = requests.slice(0, 100); // Revert limit to 100 distinct requests
      const idsParam = window.ACF_CORE.getAbbreviatedIdsParam(safeRequests);
      
      let data;
      if (window.ACF_CORE.requestCache.has(idsParam)) {
          data = window.ACF_CORE.requestCache.get(idsParam);
      } else {
          const apiUrl = `https://acf-extension.helenosalgado19.workers.dev/api/verses/batch?ids=${idsParam}`;
          const res = await fetch(apiUrl);
          if (!res.ok) throw new Error('Erro API');
          data = await res.json();
          window.ACF_CORE.requestCache.set(idsParam, data);
      }
      
      let html = "";
      let lastHeader = "";

      const contentMap = new Map();
      data.forEach(d => {
        contentMap.set(d.id, d.content);
      });

      safeRequests.forEach(req => {
        const verseContent = contentMap.get(req.id);
        if (!verseContent) {
          console.warn("ACF Popup Debug: Conteúdo do versículo não encontrado no mapa para ID:", req.id);
          return;
        }

        if (isSingleVerse) {
            html = `<p class="acf-verse">${verseContent}</p>`;
        } else {
            const currentHeader = `${req.book} ${req.chapter}`;
            // Só exibe o título do grupo se os versículos NÃO forem todos do mesmo capítulo
            if (!allSame && currentHeader !== lastHeader) {
              html += `<div class="acf-group-title">${currentHeader}</div>`;
              lastHeader = currentHeader;
            }
            html += `<p class="acf-verse"><strong>${req.verse}.</strong> ${verseContent}</p>`;
        }
      });

      if (requests.length > 100) { // Update message to reflect new limit
        html += `<p class="acf-loading">Exibindo apenas os primeiros 100 versículos...</p>`;
      }
      
      // Update UI and remove loading state
      window.ACF_CORE.popup.querySelector('.acf-body').innerHTML = html;
      window.ACF_CORE.popup.querySelector('.acf-body').classList.remove('acf-loading');

    } catch (error) {
      window.ACF_CORE.updatePopupError();
    }
  };

// --- END popup-ui.js ---

// --- START text-scanner.js ---
// Namespace Global para Scanner
window.ACF_CORE = window.ACF_CORE || {};

window.ACF_CORE.autoScanAndLink = function(mapping, style = 'arrow', interactionMode = 'click') {
    // --- Mobile Detection ---
    const isMobile = ('ontouchstart' in window) || (navigator.maxTouchPoints > 0) || (window.innerWidth <= 800);
    const effectiveInteractionMode = isMobile ? 'click' : interactionMode;

    // Se estilo for 'none', aborta
    if (style === 'none') return;

    // Tenta encontrar o container principal de conteúdo. 
    const container = document.querySelector('.entry-content') || 
                      document.querySelector('.post-content') || 
                      document.querySelector('article') || 
                      document.body;

    if (!container) return;

    // Busca URL da seta via adaptador (apenas se for usada)
    const arrowUrl = (style === 'arrow') ? window.ACF_CORE.getAssetUrl('arrow.svg') : null;

    const walker = document.createTreeWalker(
        container,
        NodeFilter.SHOW_TEXT,
        {
            acceptNode: function(node) {
                const parent = node.parentNode;
                const tag = parent.tagName.toLowerCase();
                // Ignorar tags interativas ou de script
                if (['a', 'script', 'style', 'textarea', 'input', 'button', 'sup'].includes(tag)) {
                    return NodeFilter.FILTER_REJECT;
                }
                // Ignorar elementos já processados pelo plugin
                if (parent.classList.contains('acf-ref-arrow') || parent.classList.contains('acf-ref-underline') || parent.classList.contains('acf-ref-underline-solid')) {
                    return NodeFilter.FILTER_REJECT;
                }
                return NodeFilter.FILTER_ACCEPT;
            }
        },
        false
    );

    const nodesToProcess = [];
    while (walker.nextNode()) {
        nodesToProcess.push(walker.currentNode);
    }

    // --- Definição das Expressões Regulares ---
    
    const s = "\\s*";
    const d = "\\d+";
    const sep = "[:\\.,]"; 
    const rangeSep = "[\\–\\-\\\u2014]";
    
    // Lookahead negativo para evitar que traços sejam consumidos incorretamente
    // FIX: Dash must be followed by a digit. Separators (comma/dot) must also be followed by a digit.
    // FIX: Negative lookahead restricted to colon (:) followed by digit. 
    // This allows "1, 2, 3" and "9:6, 7: Text" but stops at "1, 2:3" (Chapter:Verse).
    const safeChar = `(?:[\\d\\s]|(?:${sep}|${rangeSep})(?=${s}\\d)(?!${s}${d}${s}:${s}\\d))`;
    // FIX: Verse part separator (like :) must be followed by a digit to avoid consuming trailing punctuation.
    const versePart = `${d}(?:${s}${sep}(?=${s}\\d)${s}${safeChar}+)?`; 
    
    // Nota de rodapé opcional [12] - Escapado corretamente para RegExp
    const foot = `(?:${s}\\[\\d+\\])?`;
    
    // Fim de intervalo opcional (- 12:14)
    const endRange = `(?:${s}${rangeSep}${s}${versePart}${foot})?`;
    
    // Sufixo completo de versículos
    const verseSuffix = `${versePart}${foot}${endRange}`;
    
    // Nome do Livro (Obrigatório Capitalizado)
    const bookName = `(?:[1-3]${s})?[A-ZÁ-Ú][a-zá-úçã]+\\.?`;
    
    const fullRef = `${bookName}${s}${verseSuffix}`;
    const partialRef = `${verseSuffix}`;
    
    // SafePartialStart: Captura referências soltas que começam com separador (; , . e ou)
    // Essencial para casos onde o livro ficou em um nó de texto anterior (separado por link de rodapé)
    // FIX: Added negative lookahead (?!${bookName}) to prevent matching numbered books (e.g. "e 1 Coríntios") as partial refs
    const safePartialStart = `(?:(?:[;\\.,]|\\s+e\\s+|\\s+ou\\s+)\\s*(?!${bookName})${verseSuffix})`; 
    
    // Regex Principal: Aceita FullRef OU SafePartialStart, seguido de repetições
    const scanRegexStr = `((?:${fullRef}|${safePartialStart})(?:${s}(?:;|${s})${s}(?:${fullRef}|${partialRef}))*)`;
    const scanRegex = new RegExp(scanRegexStr, 'g');

    // Helper para buscar contexto anterior (Livro) em nós irmãos no DOM
    window.ACF_CORE.findLastBookContext = function(node) {
        let curr = node.previousSibling;
        let attempts = 0;
        const maxAttempts = 10; // Limite para evitar travar a página em estruturas muito profundas

        while (curr && attempts < maxAttempts) {
            if (curr.nodeType === Node.TEXT_NODE) {
                // Se encontrar texto significativo (não pontuação), interrompe a busca (contexto perdido)
                if (/[a-zA-Z0-9]/.test(curr.textContent)) {
                    return null; 
                }
            } else if (curr.nodeType === Node.ELEMENT_NODE) {
                const tag = curr.tagName.toLowerCase();
                
                // Pular notas de rodapé (links ou sup com números como [11])
                if (tag === 'a' || tag === 'sup') {
                     if (/^\\[?\\d+\\]?$/.test(curr.textContent.trim())) {
                         curr = curr.previousSibling;
                         attempts++;
                         continue;
                     }
                }
                
                // Se encontrar uma referência já processada (span com classe), extrai o livro dela
                if (curr.classList.contains('acf-ref-underline') || curr.classList.contains('acf-ref-underline-solid')) {
                    const match = curr.textContent.match(/^((?:[1-3]\s)?[A-ZÁ-Ú][a-zá-úçã]+)/);
                    if (match) return match[1];
                }
                
                // Ignora ícones de seta injetados
                if (curr.classList.contains('acf-ref-arrow')) {
                    curr = curr.previousSibling;
                    continue;
                }
            }
            curr = curr.previousSibling;
            attempts++;
        }
        return null;
    };

    // --- Processamento dos Nós ---

    nodesToProcess.forEach(node => {
        const text = node.nodeValue;
        let match;
        let lastIndex = 0;
        const fragments = [];
        let hasMatch = false;
        
        while ((match = scanRegex.exec(text)) !== null) {
            let textToProcess = match[0];
            const originalMatchText = match[0];
            
            // Verifica se o match começa com um nome de livro
            const startsWithBook = /^(?:[1-3]\s)?[A-ZÁ-Ú][a-zá-úçã]+\.?/.test(textToProcess.trim().replace(/^[;,\.\s]+/, ''));
            
            if (!startsWithBook) {
                // Se não tem livro, tenta recuperar do contexto DOM (nós anteriores)
                const contextBook = window.ACF_CORE.findLastBookContext(node);
                if (contextBook) {
                    // Prepara o texto para o parser:
                    // Remove separadores iniciais (; e ou) para evitar quebras de chunk incorretas
                    const cleanSuffix = textToProcess.replace(/^[;,\.\s]+(?:e\s+|ou\s+)?/, '');
                    // Concatena o livro encontrado no contexto com o versículo atual
                    textToProcess = contextBook + " " + cleanSuffix;
                } else {
                    // Sem livro e sem contexto: ignora (provável falso positivo)
                    continue;
                }
            }

            const isValid = window.ACF_CORE.processSelection(textToProcess, mapping);

            if (isValid && isValid.length > 0) {
                hasMatch = true;
                
                // Separate surrounding whitespace from the core match
                const leadingSpaceMatch = originalMatchText.match(/^\s+/);
                const trailingSpaceMatch = originalMatchText.match(/\s+$/);
                const leadingSpace = leadingSpaceMatch ? leadingSpaceMatch[0] : '';
                const trailingSpace = trailingSpaceMatch ? trailingSpaceMatch[0] : '';
                
                let trimmedText = originalMatchText;
                if (leadingSpace.length + trailingSpace.length < originalMatchText.length) {
                     trimmedText = originalMatchText.substring(leadingSpace.length, originalMatchText.length - trailingSpace.length);
                } else {
                     trimmedText = originalMatchText.trim(); 
                }
                
                // Adiciona o texto anterior ao match
                fragments.push(document.createTextNode(text.substring(lastIndex, match.index)));

                // Adiciona leading space
                if (leadingSpace) {
                    fragments.push(document.createTextNode(leadingSpace));
                }

                if (style === 'arrow') {
                    fragments.push(document.createTextNode(trimmedText));
                    const arrowImg = document.createElement('img');
                    arrowImg.src = arrowUrl;
                    arrowImg.className = 'acf-ref-arrow';
                    arrowImg.alt = "Ler";
                    
                    if (effectiveInteractionMode === 'click') {
                        arrowImg.title = "Ler Versículo";
                        const show = (e) => {
                            e.preventDefault(); e.stopPropagation();
                            const rect = e.target.getBoundingClientRect();
                            const x = Math.max(10, rect.right + window.scrollX); 
                            const y = rect.bottom + window.scrollY + 5;
                            window.ACF_CORE.fetchAndShow(isValid, x, y);
                        };
                        arrowImg.addEventListener('click', show);
                    } else if (effectiveInteractionMode === 'hover') {
                        arrowImg.addEventListener('mouseenter', (e) => {
                            if (window.ACF_CORE.cancelHidePopup) window.ACF_CORE.cancelHidePopup();
                            
                            const rect = e.target.getBoundingClientRect();
                            const x = Math.max(10, rect.right + window.scrollX); 
                            const y = rect.bottom + window.scrollY + 5;
                            window.ACF_CORE.fetchAndShow(isValid, x, y);
                        });
                        arrowImg.addEventListener('mouseleave', () => {
                            if (window.ACF_CORE.hidePopupWithDelay) window.ACF_CORE.hidePopupWithDelay();
                        });
                    }
                    fragments.push(arrowImg);

                } else if (style === 'underline' || style === 'underline_solid') {
                    const span = document.createElement('span');
                    span.className = (style === 'underline_solid') ? 'acf-ref-underline-solid' : 'acf-ref-underline';
                    span.textContent = trimmedText;
                    
                    if (effectiveInteractionMode === 'click') {
                        span.title = "Clique para ler o versículo";
                        const show = (e) => {
                            e.preventDefault(); e.stopPropagation();
                            const rect = e.target.getBoundingClientRect();
                            const x = Math.max(10, rect.left + (rect.width / 2) + window.scrollX);
                            const y = rect.bottom + window.scrollY + 5;
                            window.ACF_CORE.fetchAndShow(isValid, x, y);
                        };
                        span.addEventListener('click', show);
                    } else if (effectiveInteractionMode === 'hover') {
                        span.addEventListener('mouseenter', (e) => {
                            if (window.ACF_CORE.cancelHidePopup) window.ACF_CORE.cancelHidePopup();

                            const rect = e.target.getBoundingClientRect();
                            const x = Math.max(10, rect.left + (rect.width / 2) + window.scrollX);
                            const y = rect.bottom + window.scrollY + 5;
                            window.ACF_CORE.fetchAndShow(isValid, x, y);
                        });
                        span.addEventListener('mouseleave', () => {
                            if (window.ACF_CORE.hidePopupWithDelay) window.ACF_CORE.hidePopupWithDelay();
                        });
                    }
                    fragments.push(span);
                }
                
                // Adiciona trailing space
                if (trailingSpace) {
                    fragments.push(document.createTextNode(trailingSpace));
                }
                
                lastIndex = scanRegex.lastIndex;
            }
        }

        if (hasMatch) {
            if (lastIndex < text.length) {
                fragments.push(document.createTextNode(text.substring(lastIndex)));
            }
            const parent = node.parentNode;
            const docFragment = document.createDocumentFragment();
            fragments.forEach(frag => docFragment.appendChild(frag));
            parent.replaceChild(docFragment, node);
        }
    });
};
// --- END text-scanner.js ---

// --- START chrome.js ---
(async function() {
    // Configuração de ID Único para Extensão
    window.ACF_CORE.popupId = 'acf-ext-popup';

    // DETECÇÃO DE CONFLITO:
    if (typeof acfSettings !== 'undefined') return;

    // 1. Implementar getAssetUrl
    window.ACF_CORE.getAssetUrl = function(path) {
        try {
            return chrome.runtime.getURL(path);
        } catch (e) {
            return '';
        }
    };

    try {
        if (!chrome.runtime?.id) return;

        // 2. Carregar Configurações (Storage) e Mapping (JSON)
        // Default style: 'underline_solid' (preserva comportamento padrão anterior se não houver config)
        // Mas espere, antes não tinha config, então o padrão era o que?
        // Antes não rodava autoScanAndLink.
        // Agora vamos rodar. Padrão 'underline_solid' é bom.
        
        const [settings, mappingResponse] = await Promise.all([
            new Promise(resolve => chrome.storage.sync.get(['acf_style', 'acf_selection_mode', 'acf_hover_mode'], resolve)),
            fetch(chrome.runtime.getURL('bible_mapping.json'))
        ]);

        const style = settings.acf_style || 'underline_solid';
        const selectionMode = settings.acf_selection_mode !== false; 
        const hoverMode = settings.acf_hover_mode !== false;
        const mapping = await mappingResponse.json();

        // Se AMBOS estiverem desativados, a extensão para aqui.
        if (style === 'none' && !selectionMode) {
            console.log("ACF Extension: Totalmente desativada pelo usuário.");
            return;
        }

        // 3. Scanner Automático (Visual)
        // Só roda se o estilo for diferente de 'none'
        if (style !== 'none' && window.ACF_CORE.autoScanAndLink) {
            window.ACF_CORE.autoScanAndLink(mapping, style, hoverMode ? 'hover' : 'click');
        }

        // 4. Setup de Eventos (Seleção Manual)
        // Só adiciona o listener se o modo de seleção estiver ativado
        if (selectionMode) {
            document.addEventListener('mouseup', async () => {
                if (!chrome.runtime?.id) return;
                if (typeof acfSettings !== 'undefined') return;

                const selection = window.getSelection();
                const selectedText = selection.toString().trim();

                if (!selectedText || selectedText.length < 3 || selectedText.length > 500) return; 

                // Usa a mesma lógica de validação do Core
                const requests = window.ACF_CORE.processSelection(selectedText, mapping);

                if (requests && requests.length > 0) {
                    // Validação de "Pureza" da Seleção:
                    // Se o texto selecionado contiver muito conteúdo além das referências (ex: "Gen 1:1 texto..."), ignora.
                    let clean = selectedText;
                    requests.forEach(r => {
                        // Remove nome do livro
                        const escapedBook = r.book.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                        clean = clean.replace(new RegExp(escapedBook, 'gi'), '');
                    });
                    // Remove dígitos e pontuação de referência
                    clean = clean.replace(/[0-9:.,\-\–\—]/g, '');
                    // Remove espaços
                    clean = clean.replace(/\s+/g, '');
                    
                    // Se sobrar mais de 8 caracteres (tolerância para conectivos "e", "ou", "cf"), aborta.
                    if (clean.length > 8) return;

                    const range = selection.getRangeAt(0);
                    const rect = range.getBoundingClientRect();
                    const x = rect.left + (rect.width / 2) + window.scrollX;
                    const y = rect.bottom + window.scrollY + 10;

                    window.ACF_CORE.fetchAndShow(requests, x, y);
                }
            });
        }

    } catch (e) {
        if (e.message && !e.message.includes('invalidated')) {
             console.error("ACF Chrome: Init Error", e);
        }
    }

})();

// --- END chrome.js ---

