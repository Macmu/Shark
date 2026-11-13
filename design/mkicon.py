from PIL import Image
import os

OUT = '/Users/ranmuhan/WorkBuddy/2026-11-12-09-00-02/research/Snake/design'
art = Image.open(os.path.join(OUT, 'icon-src-alpha.png')).convert('RGBA')
aw, ah = art.size
print('art', art.size)


def compose(bg, size, pad_ratio=0.12, radius=None):
    canvas = Image.new('RGBA', (size, size), bg)
    inner = int(size * (1 - pad_ratio * 2))
    scale = min(inner / aw, inner / ah)
    w2, h2 = max(1, int(aw * scale)), max(1, int(ah * scale))
    art2 = art.resize((w2, h2), Image.LANCZOS)
    canvas.alpha_composite(art2, ((size - w2) // 2, (size - h2) // 2))
    if radius is not None:
        mask = Image.new('L', (size, size), 0)
        from PIL import ImageDraw
        ImageDraw.Draw(mask).ellipse((0, 0, size - 1, size - 1), fill=255)
        canvas.putalpha(mask)
    return canvas


# 预览：白底 / 黑底
for name, bg in (('preview-white', (255, 255, 255, 255)), ('preview-black', (0, 0, 0, 255))):
    compose(bg, 512).convert('RGB').save(os.path.join(OUT, name + '.png'))
print('previews done')
