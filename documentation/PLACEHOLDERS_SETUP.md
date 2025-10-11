# Placeholders Directory - Quick Setup

## ✅ Directory Created Successfully!

Location: `webparent/file-service/src/main/resources/placeholders/`

## Current Status

✅ Folder structure created  
✅ README.md added (instructions)  
✅ .gitkeep added (git tracking)  
⚠️ No custom images yet (will use auto-generated SVG placeholders)

## Auto-Generated Placeholders

The system will automatically generate SVG placeholders for:
- **Logo** - Shield icon, indigo color
- **Photo/Avatar** - Person icon, green color
- **Payslip/Document** - File icon, amber color
- **Generic File** - Box icon, gray color

## Want Custom Placeholders?

Add these files to this directory:

```
placeholders/
├── default-logo.png       (200x200px or larger)
├── default-avatar.png     (200x200px or larger)
├── default-document.png   (200x200px or larger)
└── default-file.png       (200x200px or larger)
```

Then rebuild:
```bash
cd webparent/file-service
mvn clean package
```

## How It Works

1. **File Missing?** → System checks this folder for custom image
2. **Custom Found?** → Returns your custom image
3. **Not Found?** → Generates SVG placeholder automatically
4. **Result:** User always sees something (never a broken image)

## Test It

After starting file-service:

```bash
# Test placeholder endpoints
curl http://localhost:8086/placeholders/logo
curl http://localhost:8086/placeholders/photo
curl http://localhost:8086/placeholders/payslip

# Test missing file (returns placeholder automatically)
curl http://localhost:8086/uploads/logo/test-missing-file.png
```

## Next Steps

### Option 1: Use Auto-Generated (No Action Needed)
The SVG placeholders work great out of the box!

### Option 2: Add Custom Placeholders
Follow the guide in `documentation/ADDING_CUSTOM_PLACEHOLDERS.md`

---

**Status**: ✅ Ready to use with auto-generated SVG placeholders!

