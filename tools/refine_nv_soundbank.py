"""Soften the existing CC0-derived NV bank and generate original feedback tones.

Bounded peaks, mono 44.1kHz, no runtime DSP. Run once against a source bank.
"""
from pathlib import Path
import json
import numpy as np
import soundfile as sf

ROOT=Path(__file__).resolve().parents[1]
ASSETS=ROOT/'src/main/resources/assets/nv'
BANK=ASSETS/'sounds/nv_glass'
backup=ROOT/'build/identity/sound-source'; backup.mkdir(parents=True,exist_ok=True)
for p in BANK.glob('*.ogg'):
    if p.name in ('error.ogg','notify.ogg'): continue
    source=backup/p.name
    if not source.exists(): source.write_bytes(p.read_bytes())
    signal,rate=sf.read(source)
    # Symmetric low-pass kernel keeps the click shape but removes brittle highs.
    signal=np.convolve(signal,np.array([1,3,5,3,1])/13,mode='same')
    peak=max(float(np.max(np.abs(signal))),.001)
    target=.07 if p.stem=='slider' else (.13 if p.stem=='button' else .16)
    signal*=min(1,target/peak)
    t=np.arange(len(signal))/rate
    signal*=np.minimum(1,t/.004)*np.minimum(1,(len(signal)/rate-t)/.015)
    sf.write(p,signal,rate,format='OGG',subtype='VORBIS')
events=json.loads((ASSETS/'sounds.json').read_text(encoding='utf-8-sig'))
for name,notes,duration in [('error',(523.25,440),.22),('notify',(659.25,880),.23)]:
    rate=44100;t=np.arange(int(rate*duration))/rate
    signal=np.zeros_like(t)
    for i,freq in enumerate(notes):
        local=np.maximum(0,t-i*.07)
        signal+=np.sin(2*np.pi*freq*local)*np.exp(-local*22)*(t>=i*.07)*.055*np.minimum(1,local/.006)
    signal*=np.minimum(1,(duration-t)/.025)
    sf.write(BANK/f'{name}.ogg',signal,rate,format='OGG',subtype='VORBIS')
    events[f'nv_glass_{name}']={'sounds':[f'nv:nv_glass/{name}']}
(ASSETS/'sounds.json').write_text(json.dumps(events,ensure_ascii=False,indent=4)+'\n',encoding='utf-8')
for p in BANK.glob('*.ogg'):
    signal,rate=sf.read(p)
    assert rate==44100 and signal.ndim==1 and np.max(np.abs(signal))<.3,p
    print(p.name,round(len(signal)/rate*1000),'ms',round(float(np.max(np.abs(signal))),3))
