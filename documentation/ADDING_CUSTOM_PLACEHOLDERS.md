# Adding Custom Placeholder Images

## Overview

The file-service supports custom placeholder images. If you want to use your own branded placeholders instead of the auto-generated SVG ones, follow this guide.

## Directory Location

```
webparent/file-service/src/main/resources/placeholders/
```

## Supported Placeholder Types

| Filename | Used For | Recommended Size |
|----------|----------|------------------|
| `default-logo.png` | Missing company/organization logos | 200x200px |
| `default-avatar.png` | Missing user photos/avatars | 200x200px |
| `default-document.png` | Missing payslips/documents | 200x200px |
| `default-file.png` | Missing generic files | 200x200px |

## Image Requirements

### Format
- **Preferred**: PNG with transparency
- **Alternative**: SVG, JPG

### Dimensions
- **Minimum**: 200x200 pixels
- **Recommended**: 400x400 pixels (for retina displays)
- **Aspect Ratio**: 1:1 (square)

### Design Guidelines
- ✅ Simple, clean icons
- ✅ Transparent or light background
- ✅ High contrast for readability
- ✅ Professional appearance
- ❌ Avoid text (it won't scale well)
- ❌ Avoid complex details

## How to Add Custom Placeholders

### Method 1: Direct Copy

```bash
# Navigate to the placeholders directory
cd webparent/file-service/src/main/resources/placeholders/

# Copy your custom images
cp /path/to/your/logo.png default-logo.png
cp /path/to/your/avatar.png default-avatar.png
cp /path/to/your/document.png default-document.png
cp /path/to/your/file.png default-file.png
```

### Method 2: Download from URL

```bash
cd webparent/file-service/src/main/resources/placeholders/

# Download placeholders (example URLs)
curl -o default-logo.png https://your-cdn.com/placeholders/logo.png
curl -o default-avatar.png https://your-cdn.com/placeholders/avatar.png
curl -o default-document.png https://your-cdn.com/placeholders/document.png
curl -o default-file.png https://your-cdn.com/placeholders/file.png
```

### Method 3: Create with Image Editor

Use tools like:
- **Photoshop**: Create 400x400px canvas with transparent background
- **GIMP**: Free alternative to Photoshop
- **Figma**: Web-based design tool
- **Canva**: Simple icon creation

## Example: Creating a Simple Placeholder

### Using ImageMagick (Command Line)

```bash
# Create a simple logo placeholder
convert -size 400x400 xc:transparent \
  -fill '#4F46E5' \
  -draw "circle 200,200 200,100" \
  -fill white \
  -font Arial -pointsize 48 \
  -gravity center \
  -annotate +0+0 'LOGO' \
  default-logo.png

# Create a simple avatar placeholder
convert -size 400x400 xc:transparent \
  -fill '#10B981' \
  -draw "circle 200,200 200,100" \
  -fill white \
  -font Arial -pointsize 120 \
  -gravity center \
  -annotate +0+0 '👤' \
  default-avatar.png
```

### Using Python + PIL

```python
from PIL import Image, ImageDraw, ImageFont

def create_placeholder(filename, color, text):
    # Create image with transparent background
    img = Image.new('RGBA', (400, 400), (0, 0, 0, 0))
    draw = ImageDraw.Draw(img)
    
    # Draw circle background
    draw.ellipse([50, 50, 350, 350], fill=color)
    
    # Add text
    font = ImageFont.truetype("arial.ttf", 60)
    bbox = draw.textbbox((0, 0), text, font=font)
    text_width = bbox[2] - bbox[0]
    text_height = bbox[3] - bbox[1]
    position = ((400 - text_width) / 2, (400 - text_height) / 2)
    draw.text(position, text, fill='white', font=font)
    
    # Save
    img.save(filename, 'PNG')

# Create placeholders
create_placeholder('default-logo.png', '#4F46E5', 'LOGO')
create_placeholder('default-avatar.png', '#10B981', 'USER')
create_placeholder('default-document.png', '#F59E0B', 'DOC')
create_placeholder('default-file.png', '#6B7280', 'FILE')
```

## Rebuild and Deploy

After adding custom placeholders:

### For Development
```bash
# Rebuild the file-service
cd webparent/file-service
mvn clean package

# Restart the service
# The new placeholders will be included in the JAR
```

### For Docker
```bash
# Rebuild the Docker image
cd deploy
docker-compose build file-service

# Restart the container
docker-compose up -d file-service
```

## Testing Custom Placeholders

### Test Endpoints

```bash
# Test logo placeholder
curl http://localhost:8086/placeholders/logo -o test-logo.png

# Test avatar placeholder
curl http://localhost:8086/placeholders/photo -o test-avatar.png

# Test document placeholder
curl http://localhost:8086/placeholders/payslip -o test-document.png
```

### Test with Missing File

```bash
# Request a file that doesn't exist
# Should return your custom placeholder
curl http://localhost:8086/uploads/logo/nonexistent.png -o test.png

# Open test.png - you should see your custom logo placeholder
```

## Branding Tips

### 1. Use Your Brand Colors

Match your placeholders to your application's color scheme:

```
Logo:     Use primary brand color
Avatar:   Use secondary or accent color
Document: Use neutral or tertiary color
```

### 2. Consistent Style

Keep all placeholders in the same visual style:
- Same stroke width for icons
- Same corner radius
- Same padding
- Same background treatment

### 3. Professional Icons

Use high-quality icons from:
- [Material Design Icons](https://materialdesignicons.com/)
- [Font Awesome](https://fontawesome.com/)
- [Heroicons](https://heroicons.com/)
- [Iconify](https://iconify.design/)

### 4. Optimize File Size

```bash
# Optimize PNG files
pngquant default-logo.png --output default-logo.png --force
optipng -o7 default-logo.png

# Convert to WebP for even smaller size
cwebp -q 85 default-logo.png -o default-logo.webp
```

## Fallback Behavior

### Priority Order

1. **Custom Image** - If exists in `placeholders/` directory
2. **Generated SVG** - Automatically generated if no custom image
3. **Error Response** - Only if SVG generation fails (very rare)

### Example Flow

```
Request for missing logo
        ↓
Check for custom: placeholders/default-logo.png
        ↓
[FOUND] → Return custom image ✅
        ↓
[NOT FOUND] → Generate SVG ✅
        ↓
Return placeholder (custom or SVG)
```

## Troubleshooting

### Issue: Custom Placeholder Not Showing

**Possible Causes**:
1. Filename mismatch (must be exact)
2. File not included in JAR
3. File permissions issue

**Solution**:
```bash
# Verify file exists
ls -la webparent/file-service/src/main/resources/placeholders/

# Check JAR contents
jar tf webparent/file-service/target/file-service-1.0-SNAPSHOT.jar | grep placeholders

# Rebuild
mvn clean package
```

### Issue: Placeholder Looks Blurry

**Cause**: Image too small

**Solution**: Use at least 400x400px images

### Issue: Large File Size

**Cause**: Unoptimized images

**Solution**:
```bash
# Optimize all placeholders
cd webparent/file-service/src/main/resources/placeholders/
for f in *.png; do
  pngquant "$f" --output "$f" --force
  optipng -o7 "$f"
done
```

## Best Practices

### ✅ DO
- Use transparent backgrounds
- Keep file sizes under 100KB
- Use consistent dimensions
- Test on different backgrounds
- Version control your placeholders

### ❌ DON'T
- Use copyrighted images without permission
- Use low-resolution images
- Use images with text (won't scale)
- Forget to optimize file sizes
- Use different sizes for different types

## Examples Gallery

### Minimalist Style
```
□ Simple outline icons
□ Single color
□ Lots of white space
□ Clean and modern
```

### Material Design Style
```
■ Filled shapes
■ Bold colors
■ Subtle shadows
■ Google Material guidelines
```

### Flat Style
```
▢ Solid colors
▢ No gradients
▢ No shadows
▢ Web 2.0 inspired
```

## Conclusion

Custom placeholders help maintain your brand identity even when files are missing. Follow this guide to create professional, optimized placeholder images that enhance your application's user experience.

---

*For questions or issues, refer to the main FILE_SERVICE documentation.*

