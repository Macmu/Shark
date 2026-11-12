from PIL import Image

im = Image.open('/tmp/icon_final2.png').convert('RGB')
W, H = im.size
px = im.load()

# 关于页图标方框大致区域：左上角 20dp 起，56dp 见方
x0, y0, x1, y1 = 40, 280, 380, 560
white = [(x, y) for y in range(y0, y1, 2) for x in range(x0, x1, 2)
         if px[x, y][0] > 248 and px[x, y][1] > 248 and px[x, y][2] > 248]
xs = [p[0] for p in white]
ys = [p[1] for p in white]
print('white box bounds', min(xs), min(ys), max(xs), max(ys))
bw, bh = max(xs) - min(xs), max(ys) - min(ys)
print('box size px', bw, bh)

# 方框内非白像素 = 图形
art = [(x, y) for y in range(min(ys), max(ys)) for x in range(min(xs), max(xs))
       if not (px[x, y][0] > 235 and px[x, y][1] > 235 and px[x, y][2] > 235)]
if art:
    axs = [p[0] for p in art]
    ays = [p[1] for p in art]
    aw, ah = max(axs) - min(axs), max(ays) - min(ays)
    print('art size px', aw, ah, 'ratio of box', round(aw / bw, 2), round(ah / bh, 2))
