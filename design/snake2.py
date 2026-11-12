# -*- coding: utf-8 -*-
import pathlib

def head_eyes(line, eye, hi="#FFFFFF", r=11, r2=5.5, ex=29, ey=32, cx1=208, cx2=298, cy=214):
    return f'''    <ellipse cx="{cx1}" cy="{cy}" rx="{ex}" ry="{ey}" fill="{eye}"/>
    <ellipse cx="{cx2}" cy="{cy}" rx="{ex}" ry="{ey}" fill="{eye}"/>
    <circle cx="{cx1-11}" cy="{cy-13}" r="{r}" fill="{hi}"/>
    <circle cx="{cx2-11}" cy="{cy-13}" r="{r}" fill="{hi}"/>
    <circle cx="{cx1+8}" cy="{cy+14}" r="{r2}" fill="{hi}" opacity="0.9"/>
    <circle cx="{cx2+8}" cy="{cy+14}" r="{r2}" fill="{hi}" opacity="0.9"/>
'''

def fangs(line, fill="#FFFFFF", w=3.5):
    return f'''    <path d="M226,287 L232,310 L239,289 Z" fill="{fill}" stroke="{line}" stroke-width="{w}" stroke-linejoin="round"/>
    <path d="M265,289 L272,310 L278,287 Z" fill="{fill}" stroke="{line}" stroke-width="{w}" stroke-linejoin="round"/>
'''

def mouth_nose(line):
    return f'''    <ellipse cx="240" cy="262" rx="5" ry="4" fill="{line}"/>
    <ellipse cx="264" cy="262" rx="5" ry="4" fill="{line}"/>
    <path d="M216,278 Q252,310 288,278" fill="none" stroke="{line}" stroke-width="9" stroke-linecap="round"/>
'''

def v1(bg, body, line, belly, eye):
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
  <path d="M366,350 C404,346 436,338 452,326 C446,344 434,358 418,368 C400,380 386,390 366,396 Z" fill="{body}" stroke="{line}" stroke-width="11" stroke-linejoin="round"/>
  <ellipse cx="252" cy="378" rx="128" ry="58" fill="{body}" stroke="{line}" stroke-width="12"/>
  <ellipse cx="252" cy="388" rx="46" ry="50" fill="{belly}"/>
  <ellipse cx="252" cy="228" rx="106" ry="98" fill="{body}" stroke="{line}" stroke-width="12"/>
{head_eyes(line, eye)}    <ellipse cx="240" cy="262" rx="5" ry="4" fill="{line}"/>
    <ellipse cx="264" cy="262" rx="5" ry="4" fill="{line}"/>
    <path d="M216,278 Q252,310 288,278" fill="none" stroke="{line}" stroke-width="9" stroke-linecap="round"/>
{fangs(line)}</svg>'''

def v2(bg, body, line, belly, eye):
    # 精修：去掉腹部色块，用一道盘身弧线暗示卷曲；头略收、尾巴翘得更明显
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
  <path d="M356,352 C400,352 442,342 462,322 C456,352 436,376 406,390 C388,398 372,402 358,404 Z" fill="{body}" stroke="{line}" stroke-width="12" stroke-linejoin="round"/>
  <ellipse cx="252" cy="382" rx="134" ry="56" fill="{body}" stroke="{line}" stroke-width="13"/>
  <path d="M150,392 Q252,436 354,392" fill="none" stroke="{line}" stroke-width="7" stroke-linecap="round" opacity="0.55"/>
  <ellipse cx="252" cy="224" rx="110" ry="100" fill="{body}" stroke="{line}" stroke-width="13"/>
{head_eyes(line, eye)}    <ellipse cx="240" cy="260" rx="5" ry="4" fill="{line}"/>
    <ellipse cx="264" cy="260" rx="5" ry="4" fill="{line}"/>
    <path d="M214,276 Q252,310 290,276" fill="none" stroke="{line}" stroke-width="9" stroke-linecap="round"/>
{fangs(line)}</svg>'''

def v3(bg, body, line, belly, eye):
    # 极简：头 + 一圈粗环 + 尾巴，只留眼睛
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
  <path d="M368,352 C412,350 448,340 466,318 C462,352 440,378 408,392 C388,400 372,404 362,406 Z" fill="{body}" stroke="{line}" stroke-width="12" stroke-linejoin="round"/>
  <ellipse cx="252" cy="384" rx="136" ry="58" fill="{body}" stroke="{line}" stroke-width="13"/>
  <ellipse cx="252" cy="384" rx="62" ry="20" fill="{bg}" stroke="{line}" stroke-width="11"/>
  <ellipse cx="252" cy="216" rx="108" ry="98" fill="{body}" stroke="{line}" stroke-width="13"/>
{head_eyes(line, eye, r=0, r2=0)}</svg>'''

def tile(svg, label, sub):
    return f'<div class="cell"><div class="art">{svg}</div><div class="cap"><b>{label}</b><br>{sub}</div></div>'

LIGHT = dict(bg="#FFFFFF", body="#2D7DFF", line="#123F8E", belly="#E8F1FF", eye="#0B1220")
DARK  = dict(bg="#000000", body="#FFFFFF", line="#000000", belly="#C9D0DA", eye="#0B1220")

rows = [
    ("v1", "方案 1 · 现设计换色", "保留现在的盘蛇形态，只换配色"),
    ("v2", "方案 2 · 精修盘蛇", "去掉腹部色块，加一道盘身弧线，尾巴翘起"),
    ("v3", "方案 3 · 极简剪影", "只留头、一圈盘身和眼睛，缩到 48px 也清楚"),
]
fn = {"v1": v1, "v2": v2, "v3": v3}

html = """<html><head><meta charset="utf-8"><style>
*{box-sizing:border-box}
body{margin:0;padding:24px;background:#EDEEF1;font-family:-apple-system,"PingFang SC",sans-serif}
.row{display:flex;gap:22px;align-items:flex-start;margin-bottom:22px}
.cell{width:240px}
.art{width:240px;height:240px;border-radius:34px;overflow:hidden;box-shadow:0 1px 6px rgba(0,0,0,.14)}
.art svg{display:block;width:100%;height:100%}
.cap{font-size:12px;color:#5A5F68;margin-top:8px;line-height:1.5}
.cap b{color:#1D2128;font-size:13px}
.small{width:96px}
.small .art{width:96px;height:96px;border-radius:16px}
.lbl{font-size:10.5px;color:#7A808A;margin-top:5px;text-align:center}
</style></head><body>"""

for key, title, sub in rows:
    html += '<div class="row">'
    html += tile(fn[key](**LIGHT), title + " · 亮色", sub).replace('class="cell"', 'class="cell"', 1)
    html += tile(fn[key](**DARK), title + " · 暗色", "黑底白蛇，描边与底色同色形成切缝")
    for kw, lbl in ((LIGHT, "48px"), (DARK, "48px 暗")):
        html += f'<div class="cell small"><div class="art">{fn[key](**kw)}</div><div class="lbl">{lbl}</div></div>'
    html += '</div>'

html += '</body></html>'
pathlib.Path("snake2.html").write_text(html)
print("ok")
