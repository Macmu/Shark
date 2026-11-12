# -*- coding: utf-8 -*-
import pathlib

HEAD = ("M256,128 C308,128 346,158 352,206 C358,254 344,296 318,322 "
        "C298,342 277,352 256,352 C235,352 214,342 194,322 "
        "C168,296 154,254 160,206 C166,158 204,128 256,128 Z")

def eyes(eye, cy=214, dx=44, ex=26, ey=29):
    return f'''  <ellipse cx="{256-dx}" cy="{cy}" rx="{ex}" ry="{ey}" fill="{eye}"/>
  <ellipse cx="{256+dx}" cy="{cy}" rx="{ex}" ry="{ey}" fill="{eye}"/>
  <circle cx="{256-dx-10}" cy="{cy-12}" r="10" fill="#FFFFFF"/>
  <circle cx="{256+dx-10}" cy="{cy-12}" r="10" fill="#FFFFFF"/>
  <circle cx="{256-dx+9}" cy="{cy+13}" r="5" fill="#FFFFFF" opacity="0.9"/>
  <circle cx="{256+dx+9}" cy="{cy+13}" r="5" fill="#FFFFFF" opacity="0.9"/>
'''

def face(line, my=292, fang=True):
    s = f'''  <ellipse cx="246" cy="{my-8}" rx="4.5" ry="3.6" fill="{line}"/>
  <ellipse cx="266" cy="{my-8}" rx="4.5" ry="3.6" fill="{line}"/>
  <path d="M226,{my} Q256,{my+26} 286,{my}" fill="none" stroke="{line}" stroke-width="8" stroke-linecap="round"/>
'''
    if fang:
        s += f'''  <path d="M234,{my+7} L240,{my+28} L247,{my+9} Z" fill="#FFFFFF" stroke="{line}" stroke-width="3.2" stroke-linejoin="round"/>
  <path d="M265,{my+9} L272,{my+28} L278,{my+7} Z" fill="#FFFFFF" stroke="{line}" stroke-width="3.2" stroke-linejoin="round"/>
'''
    return s

def tail(body, line, sx, sy):
    return (f'  <path d="M{sx},{sy} C{sx+40},{sy-4} {sx+62},{sy-34} {sx+66},{sy-74} '
            f'C{sx+82},{sy-30} {sx+50},{sy+30} {sx-8},{sy+38} Z" '
            f'fill="{body}" stroke="{line}" stroke-width="10" stroke-linejoin="round"/>\n')

def coil(body, line, cy, rx, ry, arcs=2):
    s = f'  <ellipse cx="256" cy="{cy}" rx="{rx}" ry="{ry}" fill="{body}" stroke="{line}" stroke-width="13"/>\n'
    # 盘身内部的卷曲弧线：让它看起来是"绕了两圈"，而不是一块饼
    if arcs >= 1:
        s += (f'  <path d="M{256-rx*0.62:.0f},{cy+ry*0.52:.0f} '
              f'Q256,{cy+ry*1.42:.0f} {256+rx*0.62:.0f},{cy+ry*0.52:.0f}" '
              f'fill="none" stroke="{line}" stroke-width="7.5" stroke-linecap="round" opacity="0.5"/>\n')
    if arcs >= 2:
        s += (f'  <path d="M{256-rx*0.34:.0f},{cy+ry*0.28:.0f} '
              f'Q256,{cy+ry*0.74:.0f} {256+rx*0.34:.0f},{cy+ry*0.28:.0f}" '
              f'fill="none" stroke="{line}" stroke-width="6" stroke-linecap="round" opacity="0.32"/>\n')
    return s

def build(bg, body, line, eye, head_scale=1.0, coil_cy=378, coil_rx=146, coil_ry=64):
    head = f'''  <g transform="translate(256,240) scale({head_scale}) translate(-256,-240)">
  <path d="{HEAD}" fill="{body}" stroke="{line}" stroke-width="12" stroke-linejoin="round"/>
{eyes(eye)}{face(line)}  </g>
'''
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
{tail(body, line, coil_rx+32, coil_cy-10)}{coil(body, line, coil_cy, coil_rx, coil_ry, 2)}{head}</svg>'''

LIGHT = dict(bg="#FFFFFF", body="#2D7DFF", line="#123F8E", eye="#0B1220")
DARK  = dict(bg="#000000", body="#FFFFFF", line="#000000", eye="#0B1220")

variants = [
    ("A", "头 1.0 · 盘身 146", dict(head_scale=1.0, coil_cy=380, coil_rx=146, coil_ry=64)),
    ("B", "头 0.92 · 盘身 152", dict(head_scale=0.92, coil_cy=386, coil_rx=152, coil_ry=66)),
    ("C", "头 0.86 · 盘身 158", dict(head_scale=0.86, coil_cy=392, coil_rx=158, coil_ry=68)),
]

html = """<html><head><meta charset="utf-8"><style>
*{box-sizing:border-box}body{margin:0;padding:22px;background:#EDEEF1;font-family:-apple-system,"PingFang SC",sans-serif}
.row{display:flex;gap:20px;align-items:flex-start;margin-bottom:18px}
.cell{width:236px}.art{width:236px;height:236px;border-radius:34px;overflow:hidden;box-shadow:0 1px 6px rgba(0,0,0,.14)}
.art svg{display:block;width:100%;height:100%}
.cap{font-size:12px;color:#5A5F68;margin-top:8px;line-height:1.5}.cap b{color:#1D2128;font-size:13px}
.small{width:92px}.small .art{width:92px;height:92px;border-radius:15px}
</style></head><body>"""
for key, title, kw in variants:
    html += '<div class="row">'
    html += f'<div class="cell"><div class="art">{build(**LIGHT, **kw)}</div><div class="cap"><b>{key} · {title}</b><br>亮色：白底蓝蛇</div></div>'
    html += f'<div class="cell"><div class="art">{build(**DARK, **kw)}</div><div class="cap"><b>{key} · 暗色</b><br>黑底白蛇</div></div>'
    html += f'<div class="cell small"><div class="art">{build(**LIGHT, **kw)}</div><div class="cap" style="font-size:10.5px">48px</div></div>'
    html += f'<div class="cell small"><div class="art">{build(**DARK, **kw)}</div><div class="cap" style="font-size:10.5px">48px 暗</div></div>'
    html += '</div>'
html += "</body></html>"
pathlib.Path("snake4.html").write_text(html)
print("ok")
