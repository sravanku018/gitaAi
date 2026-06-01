#!/usr/bin/env python3
# -*- coding: utf-8 -*-
"""
Logo Resizing Script for Gita Learning App
Resizes the main logo to all required Android sizes
"""

from PIL import Image
import os
import sys

# Set UTF-8 encoding for Windows console
if sys.platform == 'win32':
    import io
    sys.stdout = io.TextIOWrapper(sys.stdout.buffer, encoding='utf-8')

# Source image
source_image = r"C:\Users\srath\Downloads\16-10-2025\Gita\app\src\main\res\drawable\Gemini_Generated_Image_6bjgt36bjgt36bjg.png"
drawable_base = r"C:\Users\srath\Downloads\16-10-2025\Gita\app\src\main\res"

# Define sizes and output paths
resize_configs = [
    # (width, height, output_dir, output_filename)
    (1024, 1024, f"{drawable_base}\\drawable", "app_logo_gita.png"),
    (512, 512, f"{drawable_base}\\drawable", "splash_logo.png"),
    (192, 192, f"{drawable_base}\\mipmap-xxxhdpi", "ic_launcher.png"),
    (144, 144, f"{drawable_base}\\mipmap-xxhdpi", "ic_launcher.png"),
    (96, 96, f"{drawable_base}\\mipmap-xhdpi", "ic_launcher.png"),
    (72, 72, f"{drawable_base}\\mipmap-hdpi", "ic_launcher.png"),
    (48, 48, f"{drawable_base}\\mipmap-mdpi", "ic_launcher.png"),
]

def resize_logo():
    """Resize logo to all required sizes"""

    # Check if source image exists
    if not os.path.exists(source_image):
        print("ERROR: Source image not found at " + source_image)
        return False

    try:
        # Open the source image
        img = Image.open(source_image)
        print("Opened source image: " + source_image)
        print("  Original size: " + str(img.size))

        # Resize and save for each configuration
        for width, height, output_dir, filename in resize_configs:
            # Create output directory if it doesn't exist
            os.makedirs(output_dir, exist_ok=True)

            # Resize image
            resized_img = img.resize((width, height), Image.Resampling.LANCZOS)

            # Save image
            output_path = os.path.join(output_dir, filename)
            resized_img.save(output_path, "PNG", quality=95)

            print("Created " + str(width) + "x" + str(height) + " -> " + output_path)

        print("\nLogo resizing completed successfully!")
        print("Total variants created: " + str(len(resize_configs)))
        return True

    except Exception as e:
        print("ERROR: " + str(e))
        return False

if __name__ == "__main__":
    success = resize_logo()
    exit(0 if success else 1)
