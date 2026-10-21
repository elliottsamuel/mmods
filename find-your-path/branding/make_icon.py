"""Find Your Path icon: one glowing path that branches into five colored paths, on a dark angled device panel. 64px pixel art."""
from PIL import Image, ImageDraw
import os
N = 64
img = Image.new("RGBA", (N, N), (0, 0, 0, 0))
d = ImageDraw.Draw(img)
CYAN = (79, 227, 255); MAG = (208, 107, 255)
def chamfer(x0, y0, x1, y1, c, fill):
    d.polygon([(x0 + c, y0), (x1 - c, y0), (x1, y0 + c), (x1, y1 - c), (x1 - c, y1), (x0 + c, y1), (x0, y1 - c), (x0, y0 + c)], fill=fill)
chamfer(0, 0, 63, 63, 10, CYAN + (70,))          # glow
chamfer(1, 1, 62, 62, 9, CYAN + (255,))          # frame
chamfer(3, 3, 60, 60, 8, (12, 15, 34, 255))      # glass
for y in range(5, 59, 3): d.line([(5, y), (58, y)], fill=(19, 24, 48, 255))   # scanlines
# corner nodes
for (x, y) in [(6, 6), (55, 6), (6, 55), (55, 55)]: d.rectangle([x, y, x + 2, y + 2], fill=MAG + (220,))
# trunk: from the bottom center up to a hub
hub = (32, 36)
def glow_line(p0, p1, col, w=2):
    d.line([p0, p1], fill=col + (90,), width=w + 2)
    d.line([p0, p1], fill=col + (255,), width=w)
glow_line((32, 54), hub, (230, 240, 255))
# five branches fanning out to colored nodes
ends = [((11, 26), (255, 170, 0)), ((18, 13), (85, 255, 255)), ((32, 9), (255, 85, 85)), ((46, 13), (255, 85, 255)), ((53, 26), (85, 255, 85))]
for (e, col) in ends:
    glow_line(hub, e, col)
for (e, col) in ends:
    x, y = e
    d.rectangle([x - 3, y - 3, x + 3, y + 3], fill=col + (90,))
    d.rectangle([x - 2, y - 2, x + 2, y + 2], fill=col + (255,))
    d.rectangle([x - 1, y - 1, x, y], fill=(255, 255, 255, 230))
# hub and start point
d.rectangle([hub[0] - 3, hub[1] - 3, hub[0] + 3, hub[1] + 3], fill=(255, 255, 255, 255))
d.rectangle([hub[0] - 2, hub[1] - 2, hub[0] + 2, hub[1] + 2], fill=CYAN + (255,))
d.rectangle([29, 52, 35, 57], fill=(230, 240, 255, 255))
out_jar = os.path.join(os.path.dirname(os.path.abspath(__file__)), "..", "src/main/resources/assets/findyourpath/icon.png")
img.resize((128, 128), Image.NEAREST).save(out_jar)
img.resize((512, 512), Image.NEAREST).save(os.path.join(os.path.dirname(os.path.abspath(__file__)), "icon-512.png"))
print("ok")
