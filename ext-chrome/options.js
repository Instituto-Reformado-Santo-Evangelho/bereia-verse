document.addEventListener('DOMContentLoaded', () => {
    chrome.storage.sync.get(['acf_style', 'acf_selection_mode', 'acf_hover_mode'], (items) => {
        document.getElementById('style').value = items.acf_style || 'underline_solid';
        document.getElementById('selection_mode').checked = items.acf_selection_mode !== false;
        document.getElementById('hover_mode').checked = items.acf_hover_mode !== false;
    });
});

document.getElementById('save').addEventListener('click', () => {
    const style = document.getElementById('style').value;
    const selectionMode = document.getElementById('selection_mode').checked;
    const hoverMode = document.getElementById('hover_mode').checked;

    chrome.storage.sync.set({ 
        acf_style: style,
        acf_selection_mode: selectionMode,
        acf_hover_mode: hoverMode
    }, () => {
        const status = document.getElementById('status');
        status.textContent = 'Configurações salvas!';
        setTimeout(() => { status.textContent = ''; }, 2000);
        
        // Recarregar aba ativa para aplicar
        chrome.tabs.query({active: true, currentWindow: true}, (tabs) => {
            if (tabs[0] && tabs[0].url && tabs[0].url.startsWith('http')) {
                chrome.tabs.reload(tabs[0].id);
            }
        });
    });
});
