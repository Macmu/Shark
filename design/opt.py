# -*- coding: utf-8 -*-
import pathlib
TAIL = "M368,346 C404,342 430,322 446,294 C450,322 440,352 418,372 C400,389 382,398 362,402 Z"
COIL = "M122,376 a130,60 0 1,0 260,0 a130,60 0 1,0 -260,0 Z"
HEAD = "M148,226 a104,97 0 1,0 208,0 a104,97 0 1,0 -208,0 Z"
LINE = "M152,398 Q252,434 352,398"

def eyes(eye="#0B1220"):
    return f'''  <ellipse cx="208" cy="212" rx="28" ry="31" fill="{eye}"/>
  <ellipse cx="296" cy="212" rx="28" ry="31" fill="{eye}"/>
  <circle cx="197" cy="199" r="10.5" fill="#FFFFFF"/>
  <circle cx="285" cy="199" r="10.5" fill="#FFFFFF"/>
  <circle cx="217" cy="226" r="5.2" fill="#FFFFFF" opacity="0.9"/>
  <circle cx="305" cy="226" r="5.2" fill="#FFFFFF" opacity="0.9"/>
'''

def face(line):
    return f'''  <ellipse cx="240" cy="260" rx="5" ry="4" fill="{line}"/>
  <ellipse cx="264" cy="260" rx="5" ry="4" fill="{line}"/>
  <path d="M216,276 Q252,308 288,276" fill="none" stroke="{line}" stroke-width="9" stroke-linecap="round"/>
  <path d="M226,285 L232,308 L239,287 Z" fill="#FFFFFF" stroke="{line}" stroke-width="3.4" stroke-linejoin="round"/>
  <path d="M265,287 L272,308 L278,285 Z" fill="#FFFFFF" stroke="{line}" stroke-width="3.4" stroke-linejoin="round"/>
'''

# (a) 单色平涂：无描边，头与盘身之间留白缝
def a_l():
    return f'''  <g transform="translate(-6,-24)">
  <path d="{TAIL}" fill="#2D7DFF"/>
  <path d="{COIL}" fill="#2D7DFF"/>
  <path d="{LINE}" fill="none" stroke="#1E5FC8" stroke-width="7" stroke-linecap="round" stroke-opacity="0.55"/>
  <path d="{HEAD}" fill="#2D7DFF" stroke="#FFFFFF" stroke-width="14"/>
{eyes()}{face("#123F8E")}  </g>'''

# (b) 双色蓝：盘身略深，无描边
def b_l():
    return f'''  <g transform="translate(-6,-24)">
  <path d="{TAIL}" fill="#2B6FE0"/>
  <path d="{COIL}" fill="#2B6FE0"/>
  <path d="{LINE}" fill="none" stroke="#1B4FA8" stroke-width="7" stroke-linecap="round" stroke-opacity="0.6"/>
  <path d="{HEAD}" fill="#3585FF" stroke="#FFFFFF" stroke-width="12"/>
{eyes()}{face("#123F8E")}  </g>'''

# 暗色（保持现状）
D = f'''  <g transform="translate(-6,-24)">
  <path d="{TAIL}" fill="#FFFFFF" stroke="#000000" stroke-width="11" stroke-linejoin="round"/>
  <path d="{COIL}" fill="#FFFFFF" stroke="#000000" stroke-width="13"/>
  <path d="{LINE}" fill="none" stroke="#000000" stroke-width="7" stroke-linecap="round" stroke-opacity="0.55"/>
  <path d="{HEAD}" fill="#FFFFFF" stroke="#000000" stroke-width="13"/>
{eyes()}{face("#000")}  </g>'''

def svg(bg, art):
    return f'<svg xmlns="http://www.w3.org/2000/svg" width="512" height="512" viewBox="0 0 512 512"><rect width="512" height="512" fill="{bg}"/>{art}</svg>'

html = ('<html><head><style>body{margin:0;padding:20px;background:#EDEEF1;display:flex;gap:18px;align-items:flex-start;'
        'font-family:-apple-system}.c{width:220px;height:220px;border-radius:32px;overflow:hidden}'
        '.s{width:92px;height:92px;border-radius:15px;overflow:hidden}svg{display:block;width:100%;height:100%}'
        '.cap{font-size:12px;color:#5A5F68;margin-top:8px;width:220px}</style></head><body>')
for name, art in (("a", a_l()), ("b", b_l())):
    html += f'<div><div class="c">{svg("#FFFFFF", art)}</div><div class="cap">候选 {name}</div></div>'
    html += f'<div><div class="s">{svg("#FFFFFF", art)}</div><div class="cap" style="width:92px">48px</div></div>'
html += f'<div><div class="c">{svg("#000000", D)}</div><div class="cap">暗色（不变）</div></div>'
html += "</body></html>"
pathlib.Path("opt.html").write_text(html)
print("ok")
