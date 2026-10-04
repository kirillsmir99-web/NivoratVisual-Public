# NivoratClient icon pack

All 28 original SVG files from NivoratClient are preserved byte for byte in
`nivoratclient-original/` and packaged in `assets/nv/textures/gui/icons/`.
`svg/` contains the merged set of 66 icons used by NV. Existing Unicode assignments
remain stable; additional imported icons have new assignments in `codepoints.json`.
The heart uses the original `donate.svg`. Menu icons reuse the imported set.

Build raster textures and stroke outlines with `node tools/build_imported_icons.js`,
then the font with `node tools/build_font.js`. Run the bundled msdf-atlas-gen on
`build/icon_font/nv.ttf` with Unicode range `[0xe001,0xe042]`, MTSDF size 64 and
distance range 6, saving `build/icon_font/nv_atlas.png` and `nv_atlas.json`.
`python tools/publish_imported_icons.py` publishes the font and compatibility aliases.
`tools/build_nv_identity.py` also preserves and builds this imported set.

Use `Fonts.NV` and the NV icon registry. Centre controls using `msdfBounds`, which
accounts for visible glyph bounds rather than advance width.
The client ships generated assets and needs no asset build tools at runtime.
