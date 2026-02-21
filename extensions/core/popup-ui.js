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
