const s = "\\s*";
const d = "\\d+";
const sep = "[:\\.,]"; 
const rangeSep = "[\\–\\-\\\u2014]";

const safeChar = `(?:[\\d\\s]|(?:${sep}|${rangeSep})(?=${s}\\d)(?!${s}${d}${s}:${s}\\d))`;
const versePart = `${d}(?:${s}${sep}(?=${s}\\d)${s}${safeChar}+)?`; 
const foot = `(?:${s}\\[\\d+\\])?`;
const endRange = `(?:${s}${rangeSep}${s}${versePart}${foot})?`;
const verseSuffix = `${versePart}${foot}${endRange}`;

// Regex original
const bookName = `(?:[1-3]${s})?[A-ZÁ-Ú][a-zá-úçã]+\\.?`;
const fullRef = `${bookName}${s}${verseSuffix}`;
const partialRef = `${verseSuffix}`;
const safePartialStart = `(?:(?:[;\\.,]|\\s+e\\s+|\\s+ou\\s+)\\s*(?!${bookName})${verseSuffix})`; 
const scanRegexStr = `((?:${fullRef}|${safePartialStart})(?:${s}(?:;|${s})${s}(?:${fullRef}|${partialRef}))*)`;
const scanRegex = new RegExp(scanRegexStr, 'g');

const testTexts = [
    "Aqui temos uma referência: 1Co 11.17-34",
    "Outra referência: (1Tm 4.13)",
    "Mais uma: (1Co 14.34-35)",
    "Referência comum: João 3:16",
    "Referência com ponto: 1Co. 11:17"
];

console.log("Testando regex original:");
testTexts.forEach(text => {
    console.log(`\nTexto: "${text}"`);
    let match;
    let found = false;
    scanRegex.lastIndex = 0;
    while ((match = scanRegex.exec(text)) !== null) {
        console.log("  Match encontrado:", match[0]);
        found = true;
    }
    if (!found) console.log("  ❌ Nenhum match encontrado.");
});
