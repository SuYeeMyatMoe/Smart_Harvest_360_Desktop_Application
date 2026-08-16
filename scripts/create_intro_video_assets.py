from pathlib import Path
from PIL import Image, ImageDraw, ImageFont, ImageFilter

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "media" / "video_assets"
OUT.mkdir(parents=True, exist_ok=True)

W, H = 1920, 1080
FONT_BOLD = Path(r"C:\Windows\Fonts\segoeuib.ttf")
FONT_REG = Path(r"C:\Windows\Fonts\segoeui.ttf")

slides = [
    (
        ROOT / "src/main/resources/images/smartharvest-cinematic-intro.png",
        "SMART HARVEST 360",
        "AI-powered decisions. Smarter seasons. Stronger outcomes.",
        "01  INTRODUCTION",
    ),
    (
        ROOT / "target/farm-setup-preview.png",
        "START WITH THE FARM",
        "Location, soil, land, budget, water and fertilizer — one connected profile.",
        "02  FARM INTELLIGENCE",
    ),
    (
        ROOT / "src/main/resources/video/ai-farm-dashboard.png",
        "TURN DATA INTO ADVICE",
        "Weka J48 recommends crops and predicts quality using real farm conditions.",
        "03  AI ADVISOR",
    ),
    (
        ROOT / "target/simulation-3d-preview.png",
        "WATCH THE SEASON GROW",
        "Interactive 3D crops respond to weather, water, fertilizer and daily care.",
        "04  3D SIMULATION",
    ),
    (
        ROOT / "src/main/resources/images/smartharvest-cinematic-intro.png",
        "FROM SOIL TO PROFIT",
        "Compare markets. Predict grade. Estimate revenue. Choose with confidence.",
        "05  SMARTER HARVEST",
    ),
]


def cover(image: Image.Image) -> Image.Image:
    ratio = max(W / image.width, H / image.height)
    resized = image.resize((int(image.width * ratio), int(image.height * ratio)), Image.Resampling.LANCZOS)
    left = (resized.width - W) // 2
    top = (resized.height - H) // 2
    return resized.crop((left, top, left + W, top + H)).convert("RGB")


def wrap(draw, text, font, max_width):
    lines, current = [], ""
    for word in text.split():
        candidate = (current + " " + word).strip()
        if draw.textbbox((0, 0), candidate, font=font)[2] <= max_width:
            current = candidate
        else:
            lines.append(current)
            current = word
    if current:
        lines.append(current)
    return lines


for index, (source, title, subtitle, chapter) in enumerate(slides, start=1):
    image = cover(Image.open(source))
    overlay = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    od = ImageDraw.Draw(overlay)
    for x in range(W):
        alpha = int(205 * (1 - x / W) + 45)
        od.line((x, 0, x, H), fill=(3, 20, 16, min(230, alpha)))
    od.rectangle((0, 0, W, H), outline=(103, 229, 170, 45), width=18)
    image = Image.alpha_composite(image.convert("RGBA"), overlay)

    draw = ImageDraw.Draw(image)
    chapter_font = ImageFont.truetype(str(FONT_BOLD), 30)
    title_font = ImageFont.truetype(str(FONT_BOLD), 82)
    subtitle_font = ImageFont.truetype(str(FONT_REG), 38)
    small_font = ImageFont.truetype(str(FONT_BOLD), 25)

    draw.rounded_rectangle((118, 130, 475, 190), radius=30, fill=(14, 60, 47, 220), outline=(105, 225, 169, 150), width=2)
    draw.text((145, 143), chapter, font=chapter_font, fill=(188, 244, 214))
    draw.rectangle((120, 250, 185, 260), fill=(241, 182, 91))
    draw.text((118, 292), title, font=title_font, fill="white", stroke_width=1, stroke_fill=(0, 0, 0))

    lines = wrap(draw, subtitle, subtitle_font, 1050)
    y = 420
    for line in lines:
        draw.text((122, y), line, font=subtitle_font, fill=(218, 234, 226))
        y += 54

    draw.rounded_rectangle((120, 850, 530, 927), radius=18, fill=(237, 173, 73, 235))
    draw.text((158, 870), "MALAYSIA AGRITECH", font=small_font, fill=(9, 47, 36))
    draw.text((120, 980), "SMART HARVEST 360  •  JAVA  •  JAVAFX  •  WEKA J48", font=small_font, fill=(176, 211, 194))
    image.convert("RGB").save(OUT / f"slide-{index:02d}.png", quality=95)

print(OUT)
