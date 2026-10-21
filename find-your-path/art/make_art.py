"""Generates the carved-stone GUI art for Find Your Path (pixel art, 1 texel = 1 GUI pixel).
Nine-slice sprites: details (moss, cracks, chips, corner carvings) live in the corners so they never stretch."""
import random, os, json, math
from PIL import Image

HERE = os.path.dirname(os.path.abspath(__file__))
OUT = os.path.join(HERE, "..", "src/main/resources/assets/findyourpath/textures/gui/sprites")
os.makedirs(OUT, exist_ok=True)

OUTLINE = (24,24,22)
MOSS = [(70,112,38),(92,140,48),(58,92,32),(110,158,58),(80,126,44)]
T = (0,0,0,0)

def clamp(v): return max(0, min(255, int(v)))
def shade(c, f): return tuple(clamp(v*f) for v in c[:3]) + (255,)

def stone_field(w, h, rnd, base=(142,141,133)):
    """Minecraft-like stone: 2x2 grain plus soft darker blotches."""
    img = Image.new("RGBA", (w, h), T)
    blobs = [(rnd.randrange(w), rnd.randrange(h), rnd.uniform(6, 16), rnd.uniform(.88, .95)) for _ in range(w*h//1400)]
    for by in range(0, h, 2):
        for bx in range(0, w, 2):
            f = rnd.choice([1.0,1.0,1.0,.94,.94,1.06,.9,1.1])
            for (cx, cy, r, d) in blobs:
                if (bx-cx)**2 + (by-cy)**2 < r*r: f *= d
            for y in range(by, min(by+2, h)):
                for x in range(bx, min(bx+2, w)):
                    img.putpixel((x, y), shade(base, f * rnd.choice([1, 1, .97, 1.03])))
    return img

def profile(n, rnd, lo=0, hi=4):
    """A wobbly edge: how many pixels are bitten off at each position."""
    v, out = rnd.randint(lo, hi), []
    for i in range(n):
        if rnd.random() < .35: v += rnd.choice([-1, 1])
        v = max(lo, min(hi, v)); out.append(v)
    return out

def tablet(w, h, seed, b=28, marks=True, moss_scale=1.0):
    rnd = random.Random(seed)
    img = stone_field(w, h, rnd)
    px = img.putpixel; gp = img.getpixel
    # irregular silhouette: every edge wobbles, corners are bitten off more deeply
    top, bot, lef, rig = profile(w, rnd), profile(w, rnd), profile(h, rnd), profile(h, rnd)
    for x in range(w):
        for y in range(top[x]): px((x, y), T)
        for y in range(bot[x]): px((x, h-1-y), T)
    for y in range(h):
        for x in range(lef[y]): px((x, y), T)
        for x in range(rig[y]): px((w-1-x, y), T)
    def bite(cx, cy, sx, sy, size):
        for j in range(size):
            for i in range(max(0, size - j - rnd.randrange(3))):
                px((cx+sx*i, cy+sy*j), T)
    bite(0, 0, 1, 1, 7); bite(w-1, 0, -1, 1, 9); bite(0, h-1, 1, -1, 8); bite(w-1, h-1, -1, -1, 6)
    # shading follows the rough outline: outline, then lit (top/left) or shadowed (bottom/right) rim
    src = img.copy()
    def ray(x, y, dx, dy):
        for k in range(1, 7):
            nx, ny = x+dx*k, y+dy*k
            if not (0 <= nx < w and 0 <= ny < h) or src.getpixel((nx, ny))[3] == 0: return k
        return 99
    for y in range(h):
        for x in range(w):
            if src.getpixel((x, y))[3] == 0: continue
            du, dd, dl, dr = ray(x,y,0,-1), ray(x,y,0,1), ray(x,y,-1,0), ray(x,y,1,0)
            m = min(du, dd, dl, dr)
            if m == 1: px((x, y), OUTLINE+(255,))
            elif m <= 6:
                c = src.getpixel((x, y))
                if min(du, dl) <= min(dd, dr): px((x, y), shade(c, 1.34 - (m-2)*.07))
                else: px((x, y), shade(c, .6 + (m-2)*.08))
    # small engraved marks in the corners
    mark = ["..#..", ".#.#.", "#.#.#", ".#.#.", "..#.."]
    for (cx, cy) in ([(12, 12), (w-17, 12), (12, h-17), (w-17, h-17)] if marks else []):
        for j, row in enumerate(mark):
            for i, ch in enumerate(row):
                if ch == "#":
                    px((cx+i, cy+j), shade(gp((cx+i, cy+j)), .55)); px((cx+i+1, cy+j+1), shade(gp((cx+i+1, cy+j+1)), 1.15))
    # cracks with a lit edge, inside the corner zones
    def crack(x, y, dx, dy, n):
        for _ in range(n):
            if gp((x, y))[3]:
                px((x, y), shade(gp((x, y)), .45))
                if 0 < x+1 < w and gp((x+1, y))[3]: px((x+1, y), shade(gp((x+1, y)), 1.12))
            if rnd.random() < .55: x += dx
            else: y += dy
            x = max(1, min(w-2, x)); y = max(1, min(h-2, y))
    if h >= 64:
        crack(w-9, 5, -1, 1, 20); crack(w-16, 3, -1, 1, 9)
        crack(6, h-7, 1, -1, 16); crack(w-5, h-22, -1, -1, 8)
    else:
        crack(w-9, 4, -1, 1, 8)
    def moss(cx, cy, sx, sy, n, spread):
        for _ in range(n):
            r = abs(rnd.gauss(0, spread)); a = rnd.random() * math.pi / 2
            x = int(cx + sx*r*math.cos(a)); y = int(cy + sy*r*math.sin(a))
            if 0 <= x < w and 0 <= y < h and gp((x, y))[3]:
                px((x, y), rnd.choice(MOSS)+(255,))
    moss(3, 3, 1, 1, int(280*moss_scale), 8*moss_scale); moss(w-4, h-4, -1, -1, int(210*moss_scale), 7*moss_scale)
    for dx in ((5, 7, 10) if moss_scale >= 1 else ()):
        for k in range(rnd.randrange(6, 14)):
            if gp((dx, 6+k))[3]: px((dx, 6+k), rnd.choice(MOSS)+(255,))
    return img

def button(w, h, seed, hover):
    """A recess chiselled into the tablet, with a slightly uneven outline."""
    rnd = random.Random(seed)
    base = (122,121,114) if not hover else (150,146,128)
    img = stone_field(w, h, rnd, base)
    face = stone_field(w, h, random.Random(seed + 99))   # what shows around the chiselled edge: the tablet's own stone
    px = img.putpixel
    top, bot, lef, rig = profile(w, rnd, 0, 1), profile(w, rnd, 0, 1), profile(h, rnd, 0, 1), profile(h, rnd, 0, 1)
    for x in range(w):
        for y in range(top[x]): px((x, y), T)
        for y in range(bot[x]): px((x, h-1-y), T)
    for y in range(h):
        for x in range(lef[y]): px((x, y), T)
        for x in range(rig[y]): px((w-1-x, y), T)
    src = img.copy()
    def ray(x, y, dx, dy):
        for k in range(1, 4):
            nx, ny = x+dx*k, y+dy*k
            if not (0 <= nx < w and 0 <= ny < h) or src.getpixel((nx, ny))[3] == 0: return k
        return 99
    for y in range(h):
        for x in range(w):
            c = src.getpixel((x, y))
            if c[3] == 0: continue
            du, dd, dl, dr = ray(x,y,0,-1), ray(x,y,0,1), ray(x,y,-1,0), ray(x,y,1,0)
            m = min(du, dd, dl, dr)
            if m == 1 and min(du, dl) <= min(dd, dr): px((x, y), OUTLINE+(255,))
            elif m == 1: px((x, y), shade(c, 1.35))
            elif m == 2 and min(du, dl) <= min(dd, dr): px((x, y), (222,186,96,255) if hover else shade(c, .62))
    # fill the bitten-off edge with tablet stone so the sprite is fully opaque (it covers the vanilla button)
    for y in range(h):
        for x in range(w):
            if img.getpixel((x, y))[3] == 0: px((x, y), face.getpixel((x, y)))
    return img

def slot(seed):
    rnd = random.Random(seed)
    img = stone_field(18, 18, rnd, (104,103,97))
    px = img.putpixel; gp = img.getpixel
    for i in range(18):
        px((i, 0), OUTLINE+(255,)); px((0, i), OUTLINE+(255,))
        px((i, 1), shade(gp((i, 1)), .7)); px((1, i), shade(gp((1, i)), .7))
        px((i, 17), shade(gp((i, 17)), 1.4)); px((17, i), shade(gp((17, i)), 1.4))
    return img

def save(img, name, border):
    img.save(os.path.join(OUT, name + ".png"))
    meta = {"gui": {"scaling": {"type": "nine_slice", "width": img.width, "height": img.height, "border": border}}} if border else None
    p = os.path.join(OUT, name + ".png.mcmeta")
    if meta:
        with open(p, "w") as f: json.dump(meta, f)
    elif os.path.exists(p): os.remove(p)

tab = tablet(256, 256, 7); save(tab, "tablet", 28)
small = tablet(128, 128, 11); save(small, "tablet_small", 28)
btn = button(200, 20, 3, False); save(btn, "button", 3)
btnh = button(200, 20, 3, True); save(btnh, "button_hover", 3)
sl = slot(5); save(sl, "slot", 0)
plaque = tablet(230, 32, 13, marks=False, moss_scale=.55); save(plaque, "plaque", 0)

if __name__ == "__main__":
    prev = Image.new("RGBA", (256 + 20 + 200, 300), (40,60,40,255))
    for im, pos in [(tab, (0,0)), (small, (276,0)), (btn, (276,150)), (btnh, (276,180)), (sl, (276,210)), (sl, (296,210)), (plaque, (0,262))]:
        prev.alpha_composite(im, pos)
    prev.resize((prev.width*3, prev.height*3), Image.NEAREST).save(os.path.join(os.environ.get("PREVIEW_DIR", HERE), "art_preview.png"))
    print("ok")
