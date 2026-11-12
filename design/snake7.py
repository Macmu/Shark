# -*- coding: utf-8 -*-
import pathlib, math

HEAD = ("M256,120 C310,120 348,152 354,202 C360,252 346,296 320,324 "
        "C299,346 278,356 256,356 C234,356 213,346 192,324 "
        "C166,296 152,252 158,202 C164,152 202,120 256,120 Z")

def pt(cx, cy, rx, ry, deg):
    r = math.radians(deg)
    return (cx + rx*math.cos(r), cy + ry*math.sin(r))

def arc(cx, cy, rx, ry, a0, a1, w, color, sweep=0):
    p0 = pt(cx, cy, rx, ry, a0); p1 = pt(cx, cy, rx, ry, a1)
    return (f'  <path d="M{p0[0]:.1f},{p0[1]:.1f} A{rx},{ry} 0 1 {sweep} {p1[0]:.1f},{p1[1]:.1f}" '
            f'fill="none" stroke="{color}" stroke-width="{w}" stroke-linecap="round"/>\n')

def band(cx, cy, rx, ry, a0, a1, w, line, body):
    return arc(cx, cy, rx, ry, a0, a1, w+15, line) + arc(cx, cy, rx, ry, a0, a1, w, body)

def eyes(eye, cy, dx, ex, ey):
    return f'''  <ellipse cx="{256-dx}" cy="{cy}" rx="{ex}" ry="{ey}" fill="{eye}"/>
  <ellipse cx="{256+dx}" cy="{cy}" rx="{ex}" ry="{ey}" fill="{eye}"/>
  <circle cx="{256-dx-9}" cy="{cy-11}" r="9" fill="#FFFFFF"/>
  <circle cx="{256+dx-9}" cy="{cy-11}" r="9" fill="#FFFFFF"/>
  <circle cx="{256-dx+8}" cy="{cy+12}" r="4.5" fill="#FFFFFF" opacity="0.9"/>
  <circle cx="{256+dx+8}" cy="{cy+12}" r="4.5" fill="#FFFFFF" opacity="0.9"/>
'''

def face(line, my, w=7.5):
    return f'''  <ellipse cx="247" cy="{my-8}" rx="4.2" ry="3.4" fill="{line}"/>
  <ellipse cx="265" cy="{my-8}" rx="4.2" ry="3.4" fill="{line}"/>
  <path d="M229,{my} Q256,{my+24} 283,{my}" fill="none" stroke="{line}" stroke-width="{w}" stroke-linecap="round"/>
  <path d="M236,{my+6} L241,{my+26} L247,{my+8} Z" fill="#FFFFFF" stroke="{line}" stroke-width="3" stroke-linejoin="round"/>
  <path d="M265,{my+8} L271,{my+26} L276,{my+6} Z" fill="#FFFFFF" stroke="{line}" stroke-width="3" stroke-linejoin="round"/>
'''

def build(bg, body, line, eye, ring_cy, ring_rx, ring_ry, inner, hs, hcy):
    parts = band(256, ring_cy, ring_rx, ring_ry, 126, 54, 42, line, body)
    if inner:
        parts += band(256, ring_cy+4, ring_rx*0.54, ring_ry*0.46, 118, 62, 28, line, body)
    ex, ey = pt(256, ring_cy, ring_rx, ring_ry, 54)
    parts += (f'  <path d="M{ex-4:.0f},{ey-22:.0f} '
              f'C{ex+40:.0f},{ey-24:.0f} {ex+62:.0f},{ey-48:.0f} {ex+66:.0f},{ey-96:.0f} '
              f'C{ex+80:.0f},{ey-52:.0f} {ex+74:.0f},{ey+16:.0f} {ex+42:.0f},{ey+26:.0f} '
              f'C{ex+24:.0f},{ey+32:.0f} {ex+4:.0f},{ey+22:.0f} {ex-4:.0f},{ey+20:.0f} Z" '
              f'fill="{body}" stroke="{line}" stroke-width="9" stroke-linejoin="round"/>\n')
    head = (f'  <g transform="translate(256,{hcy}) scale({hs}) translate(-256,-238)">\n'
            f'  <path d="{HEAD}" fill="{body}" stroke="{line}" stroke-width="14" stroke-linejoin="round"/>\n'
            f'{eyes(eye, 200, 46, 26, 29)}{face(line, 282)}  </g>\n')
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
{parts}{head}</svg>'''

LIGHT = dict(bg="#FFFFFF", body="#2D7DFF", line="#123F8E", eye="#0B1220")
DARK  = dict(bg="#000000", body="#FFFFFF", line="#000000", eye="#0B1220")

variants = [
    ("J", "双环 · 头 0.70", dict(ring_cy=224, ring_rx=152, ring_ry=78, inner=True, hs=0.70, hcy=300)),
    ("K", "单环 · 头 0.70", dict(ring_cy=224, ring_rx=156, ring_ry=80, inner=False, hs=0.70, hcy=300)),
    ("L", "双环 · 头 0.78 更大", dict(ring_cy=218, ring_rx=148, ring_ry=76, inner=True, hs=0.78, hcy=306)),
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
    html += f'<div class="cell"><div class="art">{build(**LIGHT, **kw)}</div><div class="cap"><b>{key} · {title} · 亮色</b></div></div>'
    html += f'<div class="cell"><div class="art">{build(**DARK, **kw)}</div><div class="cap"><b>{key} · 暗色</b></div></div>'
    html += f'<div class="cell small"><div class="art">{build(**LIGHT, **kw)}</div><div class="cap" style="font-size:10.5px">48px</div></div>'
    html += f'<div class="cell small"><div class="art">{build(**DARK, **kw)}</div><div class="cap" style="font-size:10.5px">48px 暗</div></div>'
    html += '</div>'
html += "</body></html>"
pathlib.Path("snake7.html").write_text(html)
print("ok")
