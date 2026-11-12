from PIL import Image

im = Image.open('/tmp/z_align.png').convert('L')
w, h = im.size
px = im.load()


def runs(x0, x1, y0, y1, thr=140):
    hit = []
    for y in range(y0, y1):
        for x in range(x0, x1):
            if px[x, y] < thr:
                hit.append(y)
                break
    out = []
    for y in hit:
        if out and y == out[-1][1] + 1:
            out[-1][1] = y
        else:
            out.append([y, y])
    return out


print('size', w, h)
print('title column runs:', runs(40, 620, 120, 560))
print('icon  column runs:', runs(1240, 1380, 120, 560))
