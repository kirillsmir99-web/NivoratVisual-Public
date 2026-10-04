const fs=require('fs');
const path=require('path');
const fixer=require('C:/Users/Administrator/AppData/Roaming/npm/node_modules/oslllo-svg-fixer');
const { Resvg }=require('C:/Users/Administrator/AppData/Roaming/npm/node_modules/oslllo-svg-fixer/node_modules/@resvg/resvg-js');
async function main() {
  const source=path.resolve('nivorat_nv_icons_pack/svg');
  const destination=path.resolve('build/svg_fixed');
  fs.mkdirSync(destination,{recursive:true});
  const cp=JSON.parse(fs.readFileSync('nivorat_nv_icons_pack/codepoints.json','utf8'));
  for(const name of Object.keys(cp)) {
    let svg=fs.readFileSync(path.join(source,name+'.svg'),'utf8').replace(/currentColor/g,'white');
    const rendered=new Resvg(svg,{fitTo:{mode:'width',value:192}}).render().asPng();
    fs.writeFileSync(path.join('src/main/resources/assets/nv/textures/icons',name+'.png'),rendered);
    const fixed=await fixer.fixString(svg.replace(/white/g,'black'),1200);
    fs.writeFileSync(path.join(destination,name+'.svg'),fixed);
  }
  console.log('Imported icons rasterized and outlined:',Object.keys(cp).length);
}
main().catch(e=>{console.error(e);process.exit(1)});
