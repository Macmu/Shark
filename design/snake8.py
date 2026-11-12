# -*- coding: utf-8 -*-
import pathlib

def build(bg, body, line, eye, belly=None, inner_arc=None):
    belly_svg = f'  <ellipse cx="252" cy="382" rx="46" ry="47" fill="{belly}"/>\n' if belly else ''
    arc_svg = (f'  <path d="M206,404 Q252,428 298,404" fill="none" stroke="{inner_arc}" '
               f'stroke-width="7" stroke-linecap="round" opacity="0.55"/>\n') if inner_arc else ''
    return f'''<svg xmlns="http://www.w3.org/2000/svg" viewBox="0 0 512 512" width="512" height="512">
  <rect width="512" height="512" fill="{bg}"/>
  <path d="M368,346 C404,342 430,322 446,294 C450,322 440,352 418,372 C400,389 382,398 362,402 Z"
        fill="{body}" stroke="{line}" stroke-width="11" stroke-linejoin="round"/>
  <ellipse cx="252" cy="376" rx="130" ry="60" fill="{body}" stroke="{line}" stroke-width="13"/>
{belly_svg}{arc_svg}  <ellipse cx="252" cy="226" rx="104" ry="97" fill="{body}" stroke="{line}" stroke-width="13"/>
  <ellipse cx="208" cy="212" rx="28" ry="31" fill="{eye}"/>
  <ellipse cx="296" cy="212" rx="28" ry="31" fill="{eye}"/>
  <circle cx="197" cy="199" r="10.5" fill="#FFFFFF"/>
  <circle cx="285" cy="199" r="10.5" fill="#FFFFFF"/>
  <circle cx="217" cy="226" r="5.2" fill="#FFFFFF" opacity="0.9"/>
  <circle cx="305" cy="226" r="5.2" fill="#FFFFFF" opacity="0.9"/>
  <ellipse cx="240" cy="260" rx="5" ry="4" fill="{line}"/>
  <ellipse cx="264" cy="260" rx="5" ry="4" fill="{line}"/>
  <path d="M216,276 Q252,308 288,276" fill="none" stroke="{line}" stroke-width="9" stroke-linecap="round"/>
  <path d="M226,285 L232,308 L239,287 Z" fill="#FFFFFF" stroke="{line}" stroke-width="3.4" stroke-linejoin="round"/>
  <path d="M265,287 L272,308 L278,285 Z" fill="#FFFFFF" stroke="{line}" stroke-width="3.4" stroke-linejoin="round"/>
</svg>'''

LIGHT = dict(bg="#FFFFFF", body="#2D7DFF", line="#123F8E", eye="#0B1220", belly="#E8F1FF", inner_arc=None)
DARK  = dict(bg="#000000", body="#FFFFFF", line="#000000", eye="#0B1220", belly="#D6DEE9", inner_arc=None)

html = """<html><head><meta charset="utf-8"><style>
*{box-sizing:border-box}body{margin:0;padding:24px;background:#EDEEF1;font-family:-apple-system,"PingFang SC",sans-serif}
.row{display:flex;gap:24px;align-items:flex-start}
.cell{width:250px}.art{width:250px;height:250px;border-radius:36px;overflow:hidden;box-shadow:0 1px 6px rgba(0,0,0,.14)}
.art svg{display:block;width:100%;height:100%}
.cap{font-size:12px;color:#5A5F68;margin-top:8px}.cap b{color:#1D2128;font-size:13px}
.small{width:96px}.small .art{width:96px;height:96px;border-radius:16px}
</style></head><body><div class="row">"""
html += f'<div class="cell"><div class="art">{build(**LIGHT)}</div><div class="cap"><b>亮色 · 白底蓝蛇</b></div></div>'
html += f'<div class="cell"><div class="art">{build(**DARK)}</div><div class="cap"><b>暗色 · 黑底白蛇</b></div></div>'
html += f'<div class="cell small"><div class="art">{build(**LIGHT)}</div><div class="cap" style="font-size:10.5px">48px</div></div>'
html += f'<div class="cell small"><div class="art">{build(**DARK)}</div><div class="cap" style="font-size:10.5px">48px 暗</div></div>'
html += "</div></body></html>"
pathlib.Path("snake8.html").write_text(html)
print("ok")
