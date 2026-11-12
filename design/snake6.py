# -*- coding: utf-8 -*-
import pathlib, math

HEAD = ("M256,128 C308,128 346,158 352,206 C358,254 344,296 318,322 "
        "C298,342 277,352 256,352 C235,352 214,342 194,322 "
        "C168,296 154,254 160,206 C166,158 204,128 256,128 Z")

def pt(cx, cy, rx, ry, deg):
    r = math.radians(deg)
    return (cx + rx*math.cos(r), cy + ry*math.sin(r))

def arc(cx, cy, rx, ry, a0, a1, sweep, w, color):
    p0 = pt(cx, cy, rx, ry, a0); p1 = pt(cx, cy, rx, ry, a1)
    return (f'  <path d="M{p0[0]:.1f},{p0[1]:.1f} A{rx},{ry} 0 1 {sweep} {p1[0]:.1f},{p1[1]:.1f}" '
            f'fill="none" stroke="{color}" stroke-width="{w}" stroke-linecap="round"/>\n')

def band(cx, cy, rx, ry, a0, a1, sweep, w, line, body):
    return arc(cx, cy, rx, ry, a0, a1, sweep, w+15, line) + arc(cx, cy, rx, ry, a0, a1, sweep, w, body)

def eyes(eye, cy, dx, ex=25, ey=28):
    return f'''  <ellipse cx="{256-dx}" cy="{cy}" rx="{ex}" ry="{ey}" fill="{eye}"/>
  <ellipse cx="{256+dx}" cy="{cy}" rx="{ex}" ry="{ey}" fill="{eye}"/>
  <circle cx="{256-dx-9}" cy="{cy-11}" r="9" fill="#FFFFFF"/>
  <circle cx="{256+dx-9}" cy="{cy-11}" r="9" fill="#FFFFFF"/>
  <circle cx="{256-dx+8}" cy="{cy+12}" r="4.5" fill="#FFFFFF" opacity="0.9"/>
  <circle cx="{256+dx+8}" cy="{cy+12}" r="4.5" fill="#FFFFFF" opacity="0.9"/>
'''

def face(line, my):
    return f'''  <ellipse cx="247" cy="{my-8}" rx="4.2" ry="3.4" fill="{line}"/>
  <ellipse cx="265" cy="{my-8}" rx="4.2" ry="3.4" fill="{line}"/>
  <path d="M229,{my} Q256,{my+24} 283,{my}" fill="none" stroke="{line}" stroke-width="7.5" stroke-linecap="round"/>
  <path d="M236,{my+6} L241,{my+26} L247,{my+8} Z" fill="#FFFFFF" stroke="{line}" stroke-width="3" stroke-linejoin="round"/>
  <path d="M265,{my+8} L271,{my+26} L276,{my+6} Z" fill="#FFFFFF" stroke="{line}" stroke-width="3" stroke-linejoin="round"/>
'''

def build(bg, body, line, eye, head_scale, ring_cy, ring_rx, ring_ry, inner, tx, ty, tail_len=1.0):
    # 盘身：外层一环 + 内层一环，缺口在正下方（头从这里探出来）
    parts = band(256, ring_cy, ring_rx, ring_ry, 128, 52, 0, 40, line, body)
    if inner:
        parts += band(256, ring_cy+6, ring_rx*0.56, ring_ry*0.5, 120, 60, 0, 30, line, body)
    # 尾巴：从环的右端甩出去再向上勾起
    ex, ey = pt(256, ring_cy, ring_rx, ring_ry, 52)
    parts += (f'  <path d="M{ex:.0f},{ey:.0f} '
              f'C{ex+30:.0f},{ey+22*tail_len:.0f} {ex+58:.0f},{ey+2*tail_len:.0f} {ex+56:.0f},{ey-38*tail_len:.0f} '
              f'C{ex+74:.0f},{ey+6*tail_len:.0f} {ex+56:.0f},{ey+52:.0f} {ex-4:.0f},{ey+42:.0f} Z" '
              f'fill="{body}" stroke="{line}" stroke-width="9" stroke-linejoin="round"/>\n')
    # 头：压在盘的缺口上
    head = f'''  <g transform="translate({tx},{ty})">
  <g transform="translate(256,250) scale({head_scale}) translate(-256,-250)">
  <path d="{HEAD}" fill="{body}" stroke="{line}" stroke-width="14" stroke-linejoin="round"/>
{eyes(eye, 212, 45)}{face(line, 288)}  </g></g>
'''
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
{parts}{head}</svg>'''

LIGHT = dict(bg="#FFFFFF", body="#2D7DFF", line="#123F8E", eye="#0B1220")
DARK  = dict(bg="#000000", body="#FFFFFF", line="#000000", eye="#0B1220")

variants = [
    ("G", "头 0.68 压缺口 · 双环", dict(head_scale=0.68, ring_cy=272, ring_rx=150, ring_ry=78, inner=True, tx=0, ty=54)),
    ("H", "头 0.62 · 单环 + 尾巴", dict(head_scale=0.62, ring_cy=268, ring_rx=156, ring_ry=80, inner=False, tx=0, ty=58)),
    ("I", "头 0.72 · 盘身收窄", dict(head_scale=0.72, ring_cy=266, ring_rx=140, ring_ry=72, inner=True, tx=0, ty=50)),
]

html = """<html><head><meta charset="utf-8"><style>
*{box-sizing:border-box}body{margin:0;padding:22px;background:#EDEEF1;font-family:-apple-system,"PingFang SC",sans-serif}
.row{display:flex;gap:20px;align-items:flex-start;margin-bottom:16px}
.cell{width:236px}.art{width:236px;height:236px;border-radius:34px;overflow:hidden;box-shadow:0 1px 6px rgba(0,0,0,.14)}
.art svg{display:block;width:100%;height:100%}
.cap{font-size:12px;color:#5A5F68;margin-top:8px;line-height:1.5}.cap b{color:#1D2128;font-size:13px}
.small{width:92px}.small .art{width:92px;height:92px;border-radius:15px}
</style></head><body>"""
for key, title, kw in variants:
    html += '<div class="row">'
    html += f'<div class="cell"><div class="art">{build(**LIGHT, **kw)}</div><div class="cap"><b>{key} · {title}</b></div></div>'
    html += f'<div class="cell"><div class="art">{build(**DARK, **kw)}</div><div class="cap"><b>{key} · 暗色</b></div></div>'
    html += f'<div class="cell small"><div class="art">{build(**LIGHT, **kw)}</div><div class="cap" style="font-size:10.5px">48px</div></div>'
    html += f'<div class="cell small"><div class="art">{build(**DARK, **kw)}</div><div class="cap" style="font-size:10.5px">48px 暗</div></div>'
    html += '</div>'
html += "</body></html>"
pathlib.Path("snake6.html").write_text(html)
print("ok")
