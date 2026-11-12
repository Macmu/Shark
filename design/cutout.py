from PIL import Image, ImageDraw, ImageFilter
import os

SRC = '/Users/ranmuhan/.workbuddy/clipboard-images/clipboard-2026-11-12T16-02-43-194Z-79c9ba09.jpg'
OUT = '/Users/ranmuhan/WorkBuddy/2026-11-12-09-00-02/research/Shark/design'

im = Image.open(SRC).convert('RGBA')
w, h = im.size
px = im.load()
corners = [px[4, 4], px[w - 5, 4], px[4, h - 5], px[w - 5, h - 5]]
bg = tuple(sorted(c[i] for c in corners)[len(corners) // 2] for i in range(3))
print('src', im.size, 'bg', bg)

# 1) 先把「接近背景色」的像素标成 255
T = 62.0
near = Image.new('L', (w, h), 0)
np_ = near.load()
for y in range(h):
    for x in range(w):
        c = px[x, y]
        d = ((c[0]-bg[0])**2 + (c[1]-bg[1])**2 + (c[2]-bg[2])**2) ** 0.5
        if d < T:
            np_[x, y] = 255

# 2) 只把和画布边界连通的这部分当作背景（蛋壳内部因此不会被挖空）
flood = near.copy()
ImageDraw.floodfill(flood, (0, 0), 128, thresh=0)
fp = flood.load()

alpha = Image.new('L', (w, h), 255)
ap = alpha.load()
for y in range(h):
    for x in range(w):
        if fp[x, y] == 128:
            ap[x, y] = 0

# 3) 向内收 1px 去掉残留的绿色描边，再轻微羽化
alpha = alpha.filter(ImageFilter.MinFilter(3)).filter(ImageFilter.GaussianBlur(1.1))

im.putalpha(alpha)
bbox = alpha.getbbox()
im = im.crop(bbox)
im.save(os.path.join(OUT, 'icon-src-alpha.png'))
print('cropped', im.size)
