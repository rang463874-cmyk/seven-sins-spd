from PIL import Image, ImageFilter
import numpy as np, cv2, sys
# The owner's title screen mock-up (Gemini, drawn over a phone screenshot) -> the title background.
# The menu buttons, version text, fade chevron and Gemini sparkle are painted out (the game draws its own).
# Usage: python3 tools/sinsart/make_title_art.py
import os
HERE = os.path.dirname(os.path.abspath(__file__))
src = os.path.join(HERE, 'title_art', 'title_mockup.png')
out = os.path.join(HERE, '..', '..', 'core', 'src', 'main', 'assets', 'interfaces', 'title_art.jpg')
a = cv2.imread(src)
m = np.zeros(a.shape[:2], np.uint8)
for y0, y1 in [(746, 842), (860, 960), (982, 1079), (1096, 1197), (1218, 1316)]:
    m[y0-5:y1+5, 38:668] = 255
m[1460:1510, 395:704] = 255   # version text
m[1464:1510, 318:388] = 255   # fade chevron
m[1372:1442, 552:622] = 255   # Gemini sparkle
r = cv2.inpaint(a, m, 9, cv2.INPAINT_TELEA)
blur = cv2.GaussianBlur(r, (0, 0), 14)
mm = cv2.GaussianBlur(m.astype(np.float32) / 255, (0, 0), 6)[..., None]
r = (r * (1 - mm) + blur * mm)
# darken the area under the logo a little so buttons read well
h = r.shape[0]
ys = np.arange(h)[:, None, None]
dark = np.clip((ys - 700) / 200.0, 0, 1) * 0.25
r = r * (1 - dark)
img = Image.fromarray(cv2.cvtColor(r.clip(0, 255).astype(np.uint8), cv2.COLOR_BGR2RGB))
img.save(out, quality=92)
