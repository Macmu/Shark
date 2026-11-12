# -*- coding: utf-8 -*-
import pathlib

def eyes(eye, hi="#FFFFFF", ex=27, ey=30, cy=206):
    return f'''  <ellipse cx="212" cy="{cy}" rx="{ex}" ry="{ey}" fill="{eye}"/>
  <ellipse cx="300" cy="{cy}" rx="{ex}" ry="{ey}" fill="{eye}"/>
  <circle cx="201" cy="{cy-13}" r="10" fill="{hi}"/>
  <circle cx="289" cy="{cy-13}" r="10" fill="{hi}"/>
  <circle cx="221" cy="{cy+14}" r="5" fill="{hi}" opacity="0.9"/>
  <circle cx="309" cy="{cy+14}" r="5" fill="{hi}" opacity="0.9"/>
'''

def face(line, eye, mouthY=258, fang=True):
    s = f'''  <ellipse cx="245" cy="{mouthY-6}" rx="4.5" ry="3.6" fill="{line}"/>
  <ellipse cx="267" cy="{mouthY-6}" rx="4.5" ry="3.6" fill="{line}"/>
  <path d="M222,{mouthY+8} Q256,{mouthY+36} 290,{mouthY+8}" fill="none" stroke="{line}" stroke-width="8.5" stroke-linecap="round"/>
'''
    if fang:
        s += f'''  <path d="M231,{mouthY+16} L237,{mouthY+38} L244,{mouthY+18} Z" fill="#FFFFFF" stroke="{line}" stroke-width="3.4" stroke-linejoin="round"/>
  <path d="M268,{mouthY+18} L275,{mouthY+38} L281,{mouthY+16} Z" fill="#FFFFFF" stroke="{line}" stroke-width="3.4" stroke-linejoin="round"/>
'''
    return s

def tail(body, line, sx, sy, w=11):
    # 从盘身右端甩出并向上勾起的尾巴（填充形，带渐细）
    return (f'  <path d="M{sx},{sy} C{sx+46},{sy-6} {sx+70},{sy-40} {sx+72},{sy-86} '
            f'C{sx+88},{sy-40} {sx+56},{sy+34} {sx-6},{sy+42} Z" '
            f'fill="{body}" stroke="{line}" stroke-width="{w}" stroke-linejoin="round"/>\n')

def v4(bg, body, line, eye):
    # 两圈盘身：后面一圈 + 前面一圈，形成"卷了两匝"的层次
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
{tail(body, line, 372, 352)}
  <ellipse cx="256" cy="346" rx="118" ry="46" fill="{body}" stroke="{line}" stroke-width="12"/>
  <ellipse cx="256" cy="396" rx="138" ry="48" fill="{body}" stroke="{line}" stroke-width="12"/>
  <ellipse cx="256" cy="212" rx="92" ry="84" fill="{body}" stroke="{line}" stroke-width="12"/>
{eyes(eye)}{face(line, eye)}</svg>'''

def v5(bg, body, line, eye):
    # 单圈盘身 + 底部一道弧线暗示卷曲，头略小、下巴收窄
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
{tail(body, line, 378, 356)}
  <ellipse cx="256" cy="374" rx="140" ry="62" fill="{body}" stroke="{line}" stroke-width="13"/>
  <path d="M136,398 Q256,452 376,398" fill="none" stroke="{line}" stroke-width="8" stroke-linecap="round" opacity="0.45"/>
  <path d="M256,96 C300,96 332,120 340,158 C348,196 336,236 310,262 C288,284 270,292 256,292
           C242,292 224,284 202,262 C176,236 164,196 172,158 C180,120 212,96 256,96 Z"
        fill="{body}" stroke="{line}" stroke-width="12" stroke-linejoin="round"/>
{eyes(eye, cy=196)}{face(line, eye, mouthY=246)}</svg>'''

def v6(bg, body, line, eye):
    # 正面蛇头（宽扁、下颌收窄）+ 盘身压在头下，尾巴从右侧卷起
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
{tail(body, line, 380, 360)}
  <ellipse cx="256" cy="378" rx="142" ry="60" fill="{body}" stroke="{line}" stroke-width="13"/>
  <path d="M256,120 C312,120 356,152 372,204 C384,244 378,286 356,314
           C330,346 292,362 256,362 C220,362 182,346 156,314
           C134,286 128,244 140,204 C156,152 200,120 256,120 Z"
        fill="{body}" stroke="{line}" stroke-width="12" stroke-linejoin="round"/>
{eyes(eye, ex=28, ey=31, cy=212)}{face(line, eye, mouthY=262)}</svg>'''

LIGHT = dict(bg="#FFFFFF", body="#2D7DFF", line="#123F8E", eye="#0B1220")
DARK  = dict(bg="#000000", body="#FFFFFF", line="#000000", eye="#0B1220")

fns = [("v4", "方案 4 · 双圈盘身", "两匝盘身叠出厚度，尾巴向上勾起"),
       ("v5", "方案 5 · 单圈 + 收窄头", "头部改水滴形，下巴收窄，腹部弧线暗示卷曲"),
       ("v6", "方案 6 · 宽扁蛇头", "头做成宽扁的蛇头，下颌内收，攻击性弱")]

html = """<html><head><meta charset="utf-8"><style>
*{box-sizing:border-box}body{margin:0;padding:22px;background:#EDEEF1;font-family:-apple-system,"PingFang SC",sans-serif}
.row{display:flex;gap:20px;align-items:flex-start;margin-bottom:20px}
.cell{width:236px}.art{width:236px;height:236px;border-radius:34px;overflow:hidden;box-shadow:0 1px 6px rgba(0,0,0,.14)}
.art svg{display:block;width:100%;height:100%}
.cap{font-size:12px;color:#5A5F68;margin-top:8px;line-height:1.5}.cap b{color:#1D2128;font-size:13px}
.small{width:92px}.small .art{width:92px;height:92px;border-radius:15px}
</style></head><body>"""
for key, title, sub in fns:
    svgs = {"v4": v4, "v5": v5, "v6": v6}
    html += '<div class="row">'
    html += f'<div class="cell"><div class="art">{svgs[key](**LIGHT)}</div><div class="cap"><b>{title} · 亮色</b><br>{sub}</div></div>'
    html += f'<div class="cell"><div class="art">{svgs[key](**DARK)}</div><div class="cap"><b>{title} · 暗色</b><br>黑底白蛇</div></div>'
    html += f'<div class="cell small"><div class="art">{svgs[key](**LIGHT)}</div><div class="cap" style="font-size:10.5px">48px</div></div>'
    html += f'<div class="cell small"><div class="art">{svgs[key](**DARK)}</div><div class="cap" style="font-size:10.5px">48px 暗</div></div>'
    html += '</div>'
html += "</body></html>"
pathlib.Path("snake3.html").write_text(html)
print("ok")
