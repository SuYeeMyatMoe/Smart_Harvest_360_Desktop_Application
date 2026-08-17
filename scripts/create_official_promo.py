from pathlib import Path
from PIL import Image, ImageDraw, ImageFont
import math
import struct
import wave

ROOT = Path(__file__).resolve().parents[1]
OUT = ROOT / "media" / "promo_assets"
OUT.mkdir(parents=True, exist_ok=True)
W, H = 1920, 1080
BOLD = Path(r"C:\Windows\Fonts\segoeuib.ttf")
REGULAR = Path(r"C:\Windows\Fonts\segoeui.ttf")

slides = [
    (OUT / "soil-intelligence.png", "SMARTER STARTS HERE", "Understand the farm before the season begins.", "left"),
    (ROOT / "target/crop-selection-readable.png", "AI THAT FITS THE FARM", "Advice shaped by soil, resources and location.", "left"),
    (OUT / "smart-crops.png", "SEE THE SEASON AHEAD", "Predict growth, quality and harvest outcomes.", "left"),
    (ROOT / "target/simulation-3d-preview.png", "GROW. ADAPT. IMPROVE.", "Interactive 3D simulation, day by day.", "left"),
    (OUT / "harvest-market.png", "SMART HARVEST 360", "From soil to profit.", "left"),
]


def cover(path: Path) -> Image.Image:
    image = Image.open(path).convert("RGB")
    scale = max(W / image.width, H / image.height)
    image = image.resize((round(image.width * scale), round(image.height * scale)), Image.Resampling.LANCZOS)
    left = (image.width - W) // 2
    top = (image.height - H) // 2
    return image.crop((left, top, left + W, top + H))


for index, (path, title, subtitle, _) in enumerate(slides, 1):
    frame = cover(path).convert("RGBA")
    shade = Image.new("RGBA", (W, H), (0, 0, 0, 0))
    sd = ImageDraw.Draw(shade)
    for x in range(W):
        strength = int(220 - 165 * (x / W))
        sd.line((x, 0, x, H), fill=(2, 20, 15, strength))
    sd.rectangle((0, H - 18, W, H), fill=(31, 132, 91, 255))
    frame = Image.alpha_composite(frame, shade)
    draw = ImageDraw.Draw(frame)
    title_font = ImageFont.truetype(str(BOLD), 86 if index < 5 else 94)
    subtitle_font = ImageFont.truetype(str(REGULAR), 38)
    brand_font = ImageFont.truetype(str(BOLD), 25)

    draw.rounded_rectangle((105, 130, 185, 143), radius=7, fill=(242, 181, 89, 255))
    draw.text((102, 188), title, font=title_font, fill=(255, 255, 255), stroke_width=1, stroke_fill=(0, 18, 13))
    draw.text((108, 310), subtitle, font=subtitle_font, fill=(220, 237, 229))
    draw.text((108, 945), "SMART HARVEST 360", font=brand_font, fill=(191, 232, 211))
    frame.convert("RGB").save(OUT / f"promo-{index:02d}.png", quality=95)


def create_music(path: Path, seconds: float = 30.0, rate: int = 44100):
    progression = [
        (146.83, 220.00, 293.66),
        (174.61, 261.63, 349.23),
        (196.00, 293.66, 392.00),
        (164.81, 246.94, 329.63),
        (146.83, 220.00, 293.66),
    ]
    total = int(seconds * rate)
    with wave.open(str(path), "wb") as output:
        output.setnchannels(2)
        output.setsampwidth(2)
        output.setframerate(rate)
        frames = bytearray()
        for n in range(total):
            t = n / rate
            section = min(4, int(t // 6))
            local = t - section * 6
            chord = progression[section]
            fade = min(1.0, t / 1.2, (seconds - t) / 1.5)
            pad = sum(math.sin(2 * math.pi * f * t + i * 0.55) for i, f in enumerate(chord)) / 3
            low = math.sin(2 * math.pi * (chord[0] / 2) * t) * 0.35
            pulse_env = max(0, 1 - ((local * 2) % 1) * 3.2)
            pulse = math.sin(2 * math.pi * chord[1] * t) * pulse_env * 0.16
            chime_env = math.exp(-2.3 * local) if local < 2.4 else 0
            chime = math.sin(2 * math.pi * chord[2] * 2 * t) * chime_env * 0.20
            sample = (pad * 0.24 + low * 0.18 + pulse + chime) * max(0, fade)
            left = int(max(-1, min(1, sample * 0.96)) * 32767)
            right = int(max(-1, min(1, sample * 0.88 + pad * 0.018)) * 32767)
            frames.extend(struct.pack("<hh", left, right))
        output.writeframes(frames)


create_music(OUT / "promo-music.wav")
print(OUT)
