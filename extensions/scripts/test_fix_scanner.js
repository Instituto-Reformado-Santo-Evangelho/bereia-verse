
const s = "\\s*";
const d = "\\d+";
const sep = "[:\\.,]"; 
const rangeSep = "[\\–\\-\\\u2014]";

// Lookahead negativo para evitar que traços sejam consumidos incorretamente
const safeChar = `(?:[\\d\\s\\,]|${rangeSep}(?!${s}${d}${s}${sep}))`;
const versePart = `${d}(?:${s}${sep}${s}${safeChar}+)?`; 

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

// --- PROPOSED FIX ---
// Add negative lookahead to prevent matching numbered books as partial references
const safePartialStart = `(?:(?:[;\\.,]|\\s+e\\s+|\\s+ou\\s+)\\s*(?!${bookName})${verseSuffix})`; 
// --------------------

// Regex Principal: Aceita FullRef OU SafePartialStart, seguido de repetições
const scanRegexStr = `((?:${fullRef}|${safePartialStart})(?:${s}(?:;|${s})${s}(?:${fullRef}|${partialRef}))*)`;
const scanRegex = new RegExp(scanRegexStr, 'g');

const text = "Mateus 10:10 — ‘digno é o operário do seu alimento’ (cf. Lucas 10:7) — e 1 Coríntios 9:11 — ‘Se nós vos semeamos as coisas espirituais, será muito que de vós recolhamos as carnais?’.";

console.log("Testing text:", text);

let match;
while ((match = scanRegex.exec(text)) !== null) {
    console.log("MATCH FOUND:", match[0]);
}
