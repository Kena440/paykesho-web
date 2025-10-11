# Placeholder Images Directory

This directory contains fallback/placeholder images used when actual files are missing or inaccessible.

## Default Placeholders

The system will automatically generate SVG placeholders if custom images are not provided.

## Custom Placeholders (Optional)

You can add custom placeholder images here to override the default SVG generation:

- `default-logo.png` - Used for missing logos
- `default-avatar.png` - Used for missing photos/avatars
- `default-document.png` - Used for missing payslips/documents
- `default-file.png` - Used for missing generic files

## Recommended Specifications

- **Format**: PNG or SVG
- **Size**: 200x200 pixels minimum
- **Background**: Transparent or light gray
- **Style**: Simple, professional icons

## How It Works

1. When a file is missing, the PlaceholderController tries to load a custom image from this directory
2. If no custom image exists, it generates an SVG placeholder dynamically
3. Placeholders are cached for 24 hours for optimal performance

## Examples

To add a custom logo placeholder:
```bash
# Copy your custom logo placeholder
cp /path/to/your/logo-placeholder.png webparent/file-service/src/main/resources/placeholders/default-logo.png
```

The system will automatically use it for missing logos.

