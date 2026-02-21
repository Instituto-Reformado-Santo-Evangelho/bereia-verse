const s = "\\s*";
const d = "\\d+"; // No script de teste, 'd' é string "\d+", mas no JS real 'const d = "\\d+"'
const sep = "[:\\.,]"; 
const rangeSep = "[\\–\\-\\\u2014]";

const safeChar = `(?:[\\d\\s]|(?:${sep}|${rangeSep})(?=${s}\\d)(?!${s}${d}${s}:${s}\\d))`;
const versePart = `${d}(?:${s}${sep}(?=${s}\\d)${s}${safeChar}+)?`; 
const foot = `(?:${s}\\[\\d+\\])?`;
const endRange = `(?:${s}${rangeSep}${s}${versePart}${foot})?`;
const verseSuffix = `${versePart}${foot}${endRange}`;

// BookName aprimorado: Deve ter pelo menos 2 letras após a inicial maiúscula se não houver número na frente? 
// Não, Fp tem 2 letras no total. Então: inicial + pelo menos 1 minúscula.
const bookName = `(?:[1-3]${s}?)?[A-ZÁ-Ú][a-zá-úçã]+\\.?`;

// O segredo é garantir que após o bookName venha obrigatoriamente um ESPAÇO e um NÚMERO (capítulo)
const fullRef = `${bookName}${s}${d}${s}${sep}${s}${verseSuffix}|${bookName}\\s+${d}`;

// Vamos simplificar para o teste entender o que está acontecendo
const simpleFullRef = `${bookName}\\s+${verseSuffix}`;

const scanRegexStr = `(${simpleFullRef})`;
const scanRegex = new RegExp(scanRegexStr, 'g');

const testText = `O que ele faz? Ele põe em cena a teologia mais enorme que pode convocar: a encarnação do eterno Filho de Deus. Jesus, que “em forma de Deus, não julgou como usurpação o ser igual a Deus”, talvez no sentido de que Ele não se apegou a Sua divindade de uma forma que dissesse não à humildade de Sua encarnação (Fp. 2:6). Apesar de Jesus ser “verdadeiro Deus de verdadeiro Deus, gerado, não feito, de uma só substância com o Pai, por quem foram feitas todas as coisas”, como o Credo Niceno de 325 afirmou, Ele “a si mesmo se esvaziou, assumindo a forma de servo” (Fp. 2:7). Tão repleto de perigo teológico é o termo “esvaziou” que muitas versões bíblicas se esquivaram de uma tradução literal, empregando um eufemismo em seu lugar (e.g., “abriu mão de tudo o que era seu,” NTLH). A passagem em questão merece um tratamento mais completo, mas o ponto precisa ser sublinhado. Paulo quer que os Filipenses (e você e eu) demonstrem a “mentalidade” de Cristo: “Não tenha cada um em vista o que é propriamente seu, senão também cada qual o que é dos outros. Tende em vós o mesmo sentimento que houve também em Cristo Jesus” (vv. 4-5). A doutrina colossal da encarnação é empregada no interesse de demonstrar humildade, a “verdade segundo a piedade” (Tt. 1:1).`;

console.log("Iniciando varredura de teste...");
let match;
while ((match = scanRegex.exec(testText)) !== null) {
    console.log(`Match: "${match[0]}"`);
}
