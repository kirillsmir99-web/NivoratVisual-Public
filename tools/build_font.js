const path = require('path');
const fs = require('fs');

// Monkey-patch path.join so fantasticon's globPath uses forward slashes on Windows
const origJoin = path.join;
path.join = function(...args) {
    return origJoin.apply(this, args).replace(/\\/g, '/');
};

const { generateFonts } = require('C:/Users/Administrator/AppData/Roaming/npm/node_modules/fantasticon');

async function build() {
    const rawCp = JSON.parse(fs.readFileSync('nivorat_nv_icons_pack/codepoints.json', 'utf8'));
    const codepoints = {};
    for (const [k, v] of Object.entries(rawCp)) {
        codepoints[k] = parseInt(v.replace('U+', ''), 16);
    }

    const inputDir = path.resolve('build/svg_fixed').replace(/\\/g, '/');
    const outputDir = path.resolve('build/icon_font').replace(/\\/g, '/');
    fs.mkdirSync(outputDir, { recursive: true });

    console.log('Generating TTF font with fantasticon from fixed SVGs...');
    await generateFonts({
        inputDir: inputDir,
        outputDir: outputDir,
        name: 'nv',
        fontTypes: ['ttf'],
        assetTypes: ['json'],
        fontHeight: 1000,
        descent: 0,
        normalize: true,
        codepoints: codepoints
    });
    console.log('Font generated successfully!');
}

build().catch(err => {
    console.error('Build error:', err);
    process.exit(1);
});
