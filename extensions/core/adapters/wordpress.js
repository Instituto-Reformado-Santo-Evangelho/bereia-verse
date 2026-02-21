(async function() {
    // Verifica configurações
    if (typeof acfSettings === 'undefined') {
        return;
    }

    // Configuração de ID Único para WP
    window.ACF_CORE.popupId = 'acf-wp-popup';

    // 1. Implementar getAssetUrl
    window.ACF_CORE.getAssetUrl = function(path) {
        // Mapear nomes de arquivos para as URLs fornecidas pelo PHP
        if (path === 'logo.png') return acfSettings.logoUrl;
        if (path === 'arrow.svg') return acfSettings.arrowUrl;
        return path; 
    };

    // 2. Carregar Mapping
    let mapping = null;
    try {
        const url = acfSettings.mappingUrl;
        const response = await fetch(url);
        mapping = await response.json();
    } catch (e) {
        console.error("ACF WP: Erro ao carregar mapeamento.", e);
        return;
    }

    // 3. Auto Scan (Feature WP)
    // Suporte legado para showArrows
    let style = 'arrow';
    if (acfSettings.markerStyle) {
        style = acfSettings.markerStyle;
    } else if (typeof acfSettings.showArrows !== 'undefined') {
        style = (acfSettings.showArrows == '1') ? 'arrow' : 'none';
    }

    const hoverMode = acfSettings.hoverMode != '0';

    if (style !== 'none') {
        const initScan = () => window.ACF_CORE.autoScanAndLink(mapping, style, hoverMode ? 'hover' : 'click');
        if (document.readyState === 'loading') {
            document.addEventListener('DOMContentLoaded', initScan);
        } else {
            initScan();
        }
    }

    // 4. Seleção de Texto
    const handleSelection = async () => {
        const selection = window.getSelection();
        const selectedText = selection.toString().trim();
        if (!selectedText || selectedText.length > 500) return; 

        const requests = window.ACF_CORE.processSelection(selectedText, mapping);

        if (requests && requests.length > 0) {
            // Validação de "Pureza" da Seleção:
            let clean = selectedText;
            requests.forEach(r => {
                const escapedBook = r.book.replace(/[.*+?^${}()|[\]\\]/g, '\\$&');
                clean = clean.replace(new RegExp(escapedBook, 'gi'), '');
            });
            clean = clean.replace(/[0-9:.,\-\–\—]/g, '');
            clean = clean.replace(/\s+/g, '');
            
            if (clean.length > 8) return;

            const range = selection.getRangeAt(0);
            const rect = range.getBoundingClientRect();
            const x = Math.max(10, rect.left + window.scrollX);
            const y = rect.bottom + window.scrollY + 10;
            window.ACF_CORE.fetchAndShow(requests, x, y);
        }
    };

    document.addEventListener('mouseup', handleSelection);
    document.addEventListener('touchend', handleSelection);

})();