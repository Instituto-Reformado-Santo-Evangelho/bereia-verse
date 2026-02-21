const fs = require('fs');
const path = require('path');

const ROOT = path.resolve(__dirname, '..');
const CORE = path.join(ROOT, 'core');
const DATA = path.join(ROOT, 'data');

const CORE_FILES = [
    'bible-parser.js',
    'popup-ui.js',
    'text-scanner.js'
];

function concatFiles(fileList, outputFile) {
    let content = '';
    fileList.forEach(f => {
        const filePath = f;
        if (fs.existsSync(filePath)) {
            content += '// --- START ' + path.basename(filePath) + ' ---\n';
            content += fs.readFileSync(filePath, 'utf8') + '\n';
            content += '// --- END ' + path.basename(filePath) + ' ---\n\n';
        } else {
            console.error(`Error: File not found ${filePath}`);
        }
    });
    fs.writeFileSync(outputFile, content);
    console.log(`Build: Created ${path.relative(ROOT, outputFile)}`);
}

function copyFile(src, dest) {
    if (fs.existsSync(src)) {
        fs.copyFileSync(src, dest);
        console.log(`Build: Copied ${path.relative(ROOT, src)} to ${path.relative(ROOT, dest)}`);
    } else {
        console.error(`Error: Source file not found ${src}`);
    }
}

// 1. Build Chrome Extension
const chromeDest = path.join(ROOT, 'ext-bereia-verse');
concatFiles(
    [...CORE_FILES.map(f => path.join(CORE, f)), path.join(CORE, 'adapters', 'chrome.js')],
    path.join(chromeDest, 'content.js')
);
copyFile(path.join(CORE, 'styles.css'), path.join(chromeDest, 'styles.css'));
copyFile(path.join(DATA, 'mappings', 'bible_mapping.json'), path.join(chromeDest, 'bible_mapping.json'));
copyFile(path.join(ROOT, 'arrow-up-right.svg'), path.join(chromeDest, 'arrow.svg'));
copyFile(path.join(ROOT, 'logo.png'), path.join(chromeDest, 'logo.png'));

// 2. Build WordPress Plugin
const wpDest = path.join(ROOT, 'wp-bereia-verse', 'assets');
concatFiles(
    [...CORE_FILES.map(f => path.join(CORE, f)), path.join(CORE, 'adapters', 'wordpress.js')],
    path.join(wpDest, 'js', 'script.js')
);
copyFile(path.join(CORE, 'styles.css'), path.join(wpDest, 'css', 'style.css'));
copyFile(path.join(DATA, 'mappings', 'bible_mapping.json'), path.join(wpDest, 'bible_mapping.json'));
copyFile(path.join(ROOT, 'arrow-up-right.svg'), path.join(wpDest, 'img', 'arrow.svg'));
copyFile(path.join(ROOT, 'logo.png'), path.join(wpDest, 'img', 'logo.png'));

console.log('Build Core Complete!');
