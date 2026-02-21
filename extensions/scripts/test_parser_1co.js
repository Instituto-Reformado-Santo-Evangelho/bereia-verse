const mapping = {
  "1 Coríntios": { start: 28365, chapters: [31, 16, 23, 21, 13, 20, 40, 13, 27, 33, 34, 31, 13, 40, 58, 24] },
  "1 Timóteo": { start: 29698, chapters: [20, 15, 16, 16, 25, 21] }
};

const BOOK_ABBREVIATIONS = {
  "1 co": "1 Coríntios", "1co": "1 Coríntios",
  "1 tm": "1 Timóteo", "1tm": "1 Timóteo"
};

function getBookData(rawBookName, mapping) {
  let cleanName = rawBookName.toLowerCase().replace(/\./g, '').trim();
  let fullName = BOOK_ABBREVIATIONS[cleanName];
  if (!fullName) return null;
  return { name: fullName, data: mapping[fullName] };
}

function parseVerses(bookData, chapter, versePart) {
    return [{ id: 1 }]; // Mock
}

function processSelection(text, mapping) {
  const refRegexLiteral = /((?:[1-3]\s?)?[A-Za-zá-úÁ-Úçã]{2,}\.?\s*)?(\d+)(?:\s*[:\.,]\s*((?:[\d\s\,]|[\u2013\u002d\u2014](?!\s*\d+\s*[:\.,]))+))?(?:\s*[\u2013\u002d\u2014]\s*(\d+)(?:\s*[:\.,]\s*((?:[\d\s\,]|[\u2013\u002d\u2014](?!\s*\d+\s*[:\.,]))+))?)?/g;
  const refRegex = new RegExp(refRegexLiteral);
  let currentBook = null;
  let allRequests = [];
  let match;
  while ((match = refRegex.exec(text)) !== null) {
      console.log("  Regex Match:", match[0], "| Book Group:", match[1]);
      const rawBook = match[1];
      if (rawBook) {
          const found = getBookData(rawBook.trim(), mapping);
          if (found) {
              console.log("    Book found:", found.name);
              currentBook = found;
          } else {
              console.log("    Book NOT found for:", rawBook);
          }
      }
      if (currentBook) {
          allRequests.push({}); // dummy
      }
  }
  return allRequests;
}

const tests = [
    "1Co 11.17-34",
    "1Tm 4.13",
    "1Co 14.34-35"
];

tests.forEach(t => {
    console.log(`
Testando processSelection para: "${t}"`);
    const results = processSelection(t, mapping);
    console.log(`  Resultado: ${results.length} versículos encontrados.`);
});
