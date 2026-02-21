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
