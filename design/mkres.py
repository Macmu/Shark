from PIL import Image, ImageDraw
import os

DESIGN = '/Users/ranmuhan/WorkBuddy/2026-11-12-09-00-02/research/Snake/design'
RES = '/Users/ranmuhan/WorkBuddy/2026-11-12-09-00-02/research/Snake/app/app/src/main/res'

art = Image.open(os.path.join(DESIGN, 'icon-src-alpha.png')).convert('RGBA')
aw, ah = art.size


def fit(canvas, ratio):
    """把主体按高度占比 ratio 放进正方形画布，水平垂直居中。"""
    target_h = canvas * ratio
    scale = target_h / ah
    return art.resize((max(1, int(aw * scale)), max(1, int(target_h))), Image.LANCZOS)


# ---- 1) 自适应图标前景：108dp 画布，主体占 62%（系统会再裁到 66% 安全区） ----
fg_size = 432
fg = Image.new('RGBA', (fg_size, fg_size), (0, 0, 0, 0))
a = fit(fg_size, 0.54)
fg.alpha_composite(a, ((fg_size - a.width) // 2, (fg_size - a.height) // 2))
os.makedirs(os.path.join(RES, 'drawable-nodpi'), exist_ok=True)
fg.save(os.path.join(RES, 'drawable-nodpi', 'ic_launcher_foreground.png'))
print('foreground', fg.size)

# ---- 2) 传统方形/圆形图标（API < 26）：白底 ----
for dens, size in (('mdpi', 48), ('hdpi', 72), ('xhdpi', 96), ('xxhdpi', 144), ('xxxhdpi', 192)):
    sq = Image.new('RGBA', (size, size), (255, 255, 255, 255))
    s = fit(size, 0.80)
    sq.alpha_composite(s, ((size - s.width) // 2, (size - s.height) // 2))
    sq.convert('RGB').save(os.path.join(RES, f'mipmap-{dens}', 'ic_launcher.png'))

    rd = Image.new('RGBA', (size, size), (0, 0, 0, 0))
    mask = Image.new('L', (size, size), 0)
    ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
    white = Image.new('RGBA', (size, size), (255, 255, 255, 255))
    rd.paste(white, (0, 0), mask)
    s2 = fit(int(size * 0.92), 0.80)
    rd.alpha_composite(s2, ((size - s2.width) // 2, (size - s2.height) // 2))
    rd.putalpha(mask)
    rd.convert('RGB').save(os.path.join(RES, f'mipmap-{dens}', 'ic_launcher_round.png'))
print('legacy png done')
