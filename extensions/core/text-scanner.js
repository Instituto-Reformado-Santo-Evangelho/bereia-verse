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
                      document.querySelector('.post-body') || 
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
    
    // Nome do Livro (Obrigatório Capitalizado + pelo menos 1 minúscula)
    const bookName = `(?:[1-3]${s}?)?[A-ZÁ-Ú][a-zá-úçã]+\\.?`;
    
    // FullRef: Exige espaço antes do capítulo para evitar falsos positivos como "Filho d"
    const fullRef = `${bookName}\\s+${verseSuffix}`;
    const partialRef = `${verseSuffix}`;
    
    // SafePartialStart: Captura referências soltas que começam com separador (; , . e ou)
    const safePartialStart = `(?:(?:[;\\.,]|\\s+e\\s+|\\s+ou\\s+)\\s*(?!${bookName})${verseSuffix})`; 
    
    // Regex Principal
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
                    const match = curr.textContent.match(/^((?:[1-3]\s?)?[A-ZÁ-Ú][a-zá-úçã]+)/);
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
            const startsWithBook = /^(?:[1-3]\s?)?[A-ZÁ-Ú][a-zá-úçã]+\.?/.test(textToProcess.trim().replace(/^[;,\.\s]+/, ''));
            
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