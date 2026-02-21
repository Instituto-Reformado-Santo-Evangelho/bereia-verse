const fs = require('fs');

// Carregar o mapeamento
const mapping = JSON.parse(fs.readFileSync('bible_mapping.json', 'utf8'));

/**
 * Converte referência bíblica para ID único (1-31102)
 * @param {string} book - Nome do livro (ex: "Gênesis", "João")
 * @param {number} chapter - Número do capítulo
 * @param {number} verse - Número do versículo
 * @returns {number|null} - ID único ou null se inválido
 */
function getVerseId(book, chapter, verse) {
    const bookData = mapping[book];
    
    if (!bookData) {
        console.error(`Livro não encontrado: ${book}`);
        return null;
    }

    if (chapter < 1 || chapter > bookData.chapters.length) {
        console.error(`Capítulo inválido para ${book}: ${chapter}`);
        return null;
    }

    const versesInChapter = bookData.chapters[chapter - 1]; // Array é 0-based
    if (verse < 1 || verse > versesInChapter) {
        console.error(`Versículo inválido para ${book} ${chapter}: ${verse} (Máx: ${versesInChapter})`);
        return null;
    }

    // Algoritmo:
    // 1. Pega o offset inicial do livro (start)
    // 2. Soma todos os versículos dos capítulos anteriores deste livro
    // 3. Soma o versículo atual
    
    let id = bookData.start;
    
    // Somar capítulos anteriores
    for (let i = 0; i < chapter - 1; i++) {
        id += bookData.chapters[i];
    }
    
    // Adicionar o versículo atual (e subtrair 1 porque o start já conta como base 1? 
    // Não, start é o ID do primeiro verso. Então se for verso 1, é start + 0.
    // Se for verso 2, é start + 1.
    // Então id += verse - 1.
    
    id += (verse - 1);

    return id;
}

// --- TESTES ---

console.log("--- Testando Mapeamento ---");

const tests = [
    { book: "Gênesis", c: 1, v: 1, expected: 1 },
    { book: "Malaquias", c: 4, v: 6, expected: 23145 },
    { book: "Mateus", c: 1, v: 1, expected: 23146 },
    { book: "Apocalipse", c: 22, v: 21, expected: 31102 },
    { book: "João", c: 3, v: 16, expected: null } // Vamos descobrir o ID
];

tests.forEach(t => {
    const result = getVerseId(t.book, t.c, t.v);
    if (t.expected) {
        const status = result === t.expected ? "✅ PASS" : `❌ FAIL (Esp: ${t.expected}, Rec: ${result})`;
        console.log(`${t.book} ${t.c}:${t.v} -> ID ${result} [${status}]`);
    } else {
        console.log(`${t.book} ${t.c}:${t.v} -> ID ${result} (Check Manual)`);
    }
});
