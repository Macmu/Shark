# -*- coding: utf-8 -*-
import pathlib, math

HEAD = ("M256,128 C308,128 346,158 352,206 C358,254 344,296 318,322 "
        "C298,342 277,352 256,352 C235,352 214,342 194,322 "
        "C168,296 154,254 160,206 C166,158 204,128 256,128 Z")

def pt(cx, cy, rx, ry, deg):
    r = math.radians(deg)
    return (cx + rx*math.cos(r), cy + ry*math.sin(r))

def eyes(eye, cy, dx, ex=24, ey=27):
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

def ring(cy, rx, ry, w, a0, a1, sweep, line, body):
    p0 = pt(256, cy, rx, ry, a0); p1 = pt(256, cy, rx, ry, a1)
    return (f'  <path d="M{p0[0]:.1f},{p0[1]:.1f} A{rx},{ry} 0 1 {sweep} {p1[0]:.1f},{p1[1]:.1f}" '
            f'fill="none" stroke="{line}" stroke-width="{w+14}" stroke-linecap="round"/>\n'
            f'  <path d="M{p0[0]:.1f},{p0[1]:.1f} A{rx},{ry} 0 1 {sweep} {p1[0]:.1f},{p1[1]:.1f}" '
            f'fill="none" stroke="{body}" stroke-width="{w}" stroke-linecap="round"/>\n')

def tail(body, line, sx, sy, up=True):
    d = -1 if up else 1
    return (f'  <path d="M{sx},{sy} C{sx+34},{sy+6*d} {sx+52},{sy-28*d} {sx+50},{sy-66*d} '
            f'C{sx+66},{sy-24*d} {sx+38},{sy+32*d} {sx-10},{sy+40*d} Z" '
            f'fill="{body}" stroke="{line}" stroke-width="9" stroke-linejoin="round"/>\n')

def build(bg, body, line, eye, head_scale, ring_cy, ring_rx, ring_ry, sweep, tail_up=True):
    rings = ring(ring_cy, ring_rx, ring_ry, 40, 150, 30, sweep, line, body)
    rings += ring(ring_cy+4, ring_rx*0.58, ring_ry*0.52, 32, 150, 30, sweep, line, body)
    hx = ring_rx + 18
    head = f'''  <g transform="translate(256,252) scale({head_scale}) translate(-256,-252)">
  <path d="{HEAD}" fill="{body}" stroke="{line}" stroke-width="13" stroke-linejoin="round"/>
{eyes(eye, 214, 44)}{face(line, 290)}  </g>
'''
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
{tail(body, line, hx, ring_cy - ring_ry*0.12, tail_up)}{rings}{head}</svg>'''

LIGHT = dict(bg="#FFFFFF", body="#2D7DFF", line="#123F8E", eye="#0B1220")
DARK  = dict(bg="#000000", body="#FFFFFF", line="#000000", eye="#0B1220")

variants = [
    ("D", "双环 · 头 0.80（sweep=1）", dict(head_scale=0.80, ring_cy=316, ring_rx=150, ring_ry=76, sweep=1)),
    ("E", "双环 · 头 0.80（sweep=0）", dict(head_scale=0.80, ring_cy=316, ring_rx=150, ring_ry=76, sweep=0)),
    ("F", "双环 · 头 0.74 盘身更宽", dict(head_scale=0.74, ring_cy=310, ring_rx=164, ring_ry=82, sweep=1)),
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
pathlib.Path("snake5.html").write_text(html)
print("ok")
