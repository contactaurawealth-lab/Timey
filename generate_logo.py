from PIL import Image, ImageDraw, ImageFont
import os

# --- CONFIGURATION ---
TARGET_SIZE = 1024  # Standard Google Play Store asset resolution
SCALE = 3           # Internal supersampling buffer
SIZE = TARGET_SIZE * SCALE

# Theme Palette (Deep Slate & Focus Cyan)
CANVAS_BG = '#0B0F19'      # Pitch dark viewport background
CARD_BG = '#111827'        # Elevated slate card
CARD_BORDER = '#1F2937'    # Subtle surface border
PRIMARY = '#F9FAFB'        # Crisp high-contrast white
ACCENT = '#06B6D4'         # Electric Cyan (Active Pomodoro)
ACCENT_MUTED = '#0E7490'   # Cyan track background
MUTED_TICKS = '#374151'    # Subdued clock ticks

OUTPUT_FILE = 'timey_icon_1024.png'

# --- BUFFER SETUP ---
img = Image.new('RGBA', (SIZE, SIZE), color=(0, 0, 0, 0))
draw = ImageDraw.Draw(img)

# --- 1. SQUIRCLE APP ICON CONTAINER ---
pad = 40 * SCALE
corner_rad = 220 * SCALE
draw.rounded_rectangle(
    [pad, pad, SIZE - pad, SIZE - pad],
    radius=corner_rad,
    fill=CARD_BG,
    outline=CARD_BORDER,
    width=6 * SCALE
)

# Icon Center point
cx = SIZE // 2
cy = SIZE // 2 - (24 * SCALE)

# --- 2. CLOCK TICK MARKS (12, 3, 6, 9) ---
tick_dist = 300 * SCALE
tick_size = 14 * SCALE
tick_len = 28 * SCALE

# 12 o'clock
draw.rounded_rectangle([cx - tick_size//2, cy - tick_dist - tick_len, cx + tick_size//2, cy - tick_dist], radius=6*SCALE, fill=MUTED_TICKS)
# 6 o'clock
draw.rounded_rectangle([cx - tick_size//2, cy + tick_dist, cx + tick_size//2, cy + tick_dist + tick_len], radius=6*SCALE, fill=MUTED_TICKS)
# 9 o'clock
draw.rounded_rectangle([cx - tick_dist - tick_len, cy - tick_size//2, cx - tick_dist, cy + tick_size//2], radius=6*SCALE, fill=MUTED_TICKS)
# 3 o'clock
draw.rounded_rectangle([cx + tick_dist, cy - tick_size//2, cx + tick_dist + tick_len, cy + tick_size//2], radius=6*SCALE, fill=MUTED_TICKS)

# --- 3. POMODORO TIMER TRACK & PROGRESS ARC ---
ring_radius = 240 * SCALE
ring_w = 22 * SCALE
ring_bbox = [cx - ring_radius, cy - ring_radius, cx + ring_radius, cy + ring_radius]

# Base track (Full circle, subtle)
draw.arc(ring_bbox, start=0, end=360, fill='#1E293B', width=ring_w)

# Active session arc (~25 min Pomodoro sweep: -90° to 150°)
draw.arc(ring_bbox, start=-90, end=140, fill=ACCENT, width=ring_w)

# --- 4. THE 'T' IDENTITY (Rounded & Geometric) ---
t_w = 200 * SCALE
t_thick = 36 * SCALE
t_corner = 18 * SCALE
t_y = cy - (60 * SCALE)
stem_h = 160 * SCALE

# Crossbar
draw.rounded_rectangle(
    [cx - t_w//2, t_y, cx + t_w//2, t_y + t_thick],
    radius=t_corner,
    fill=PRIMARY
)

# Stem
draw.rounded_rectangle(
    [cx - t_thick//2, t_y, cx + t_thick//2, t_y + stem_h],
    radius=t_corner,
    fill=PRIMARY
)

# --- 5. PULSE FOCUS DOT ---
dot_r = 22 * SCALE
dot_x = cx + (t_w // 2) + (18 * SCALE)
dot_y = t_y + (t_thick // 2)

# Subtle glow/halo ring around focus dot
draw.ellipse([dot_x - dot_r - 8*SCALE, dot_y - dot_r - 8*SCALE, dot_x + dot_r + 8*SCALE, dot_y + dot_r + 8*SCALE], fill='#164E63')
# Solid Cyan Core
draw.ellipse([dot_x - dot_r, dot_y - dot_r, dot_x + dot_r, dot_y + dot_r], fill=ACCENT)

# --- 6. SESSION PROGRESS PILLS (Bottom Indicator) ---
dot_spacing = 38 * SCALE
dot_w = 12 * SCALE
bar_y = cy + (150 * SCALE)

# 4 session markers (3 complete off-white, 1 active cyan)
for i, offset in enumerate([-1.5, -0.5, 0.5, 1.5]):
    dx = cx + int(offset * dot_spacing)
    color = ACCENT if i == 2 else '#334155'
    height = 20 * SCALE if i == 2 else 10 * SCALE
    draw.rounded_rectangle([dx - dot_w//2, bar_y - height//2, dx + dot_w//2, bar_y + height//2], radius=6*SCALE, fill=color)

# --- 7. DOWNSAMPLE & EXPORT ---
final_img = img.resize((TARGET_SIZE, TARGET_SIZE), Image.Resampling.LANCZOS)
final_img.save(OUTPUT_FILE, format="PNG", quality=95)

print(f"🚀 Icon generated successfully: {OUTPUT_FILE} ({TARGET_SIZE}x{TARGET_SIZE})")

# Also generate web and android mipmap assets
web_dest = os.path.join("web", "timey_logo.png")
final_img.save(web_dest, format="PNG", quality=95)
print(f"Saved web logo: {web_dest}")

# Android standard launcher sizes
res_dir = os.path.join("app", "src", "main", "res")
densities = {
    "mipmap-mdpi": 48,
    "mipmap-hdpi": 72,
    "mipmap-xhdpi": 96,
    "mipmap-xxhdpi": 144,
    "mipmap-xxxhdpi": 192
}

for folder, s in densities.items():
    folder_path = os.path.join(res_dir, folder)
    os.makedirs(folder_path, exist_ok=True)
    icon_resized = final_img.resize((s, s), Image.Resampling.LANCZOS)
    icon_resized.save(os.path.join(folder_path, "ic_launcher.png"), format="PNG")
    icon_resized.save(os.path.join(folder_path, "ic_launcher_round.png"), format="PNG")
    print(f"Generated Android icon: {folder} ({s}x{s})")
