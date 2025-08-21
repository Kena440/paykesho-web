#!/bin/bash

# Setup Tailwind CSS for all Next.js apps in the monorepo

echo "Setting up Tailwind CSS for all apps..."

# Array of app names
apps=("backoffice" "client-portal" "merchant-portal" "organization-portal")

# Setup Tailwind for each app
for app in "${apps[@]}"; do
    echo "Setting up Tailwind CSS for $app..."
    cd "apps/$app"
    
    # Install Tailwind CSS and its dependencies
    npm install -D tailwindcss postcss autoprefixer
    
    # Initialize Tailwind CSS
    npx tailwindcss init -p
    
    # Update tailwind.config.js
    cat > tailwind.config.js << 'EOF'
/** @type {import('tailwindcss').Config} */
module.exports = {
  content: [
    './pages/**/*.{js,ts,jsx,tsx,mdx}',
    './components/**/*.{js,ts,jsx,tsx,mdx}',
    './app/**/*.{js,ts,jsx,tsx,mdx}',
  ],
  theme: {
    extend: {
      colors: {
        primary: {
          50: '#eff6ff',
          100: '#dbeafe',
          200: '#bfdbfe',
          300: '#93c5fd',
          400: '#60a5fa',
          500: '#3b82f6',
          600: '#2563eb',
          700: '#1d4ed8',
          800: '#1e40af',
          900: '#1e3a8a',
        },
      },
    },
  },
  plugins: [],
}
EOF

    # Update globals.css
    cat > app/globals.css << 'EOF'
@tailwind base;
@tailwind components;
@tailwind utilities;

/* Custom styles can be added here */
@layer base {
  html {
    font-family: system-ui, sans-serif;
  }
}

@layer components {
  .btn-primary {
    @apply bg-primary-600 hover:bg-primary-700 text-white font-medium py-2 px-4 rounded-lg transition-colors duration-200;
  }
  
  .btn-secondary {
    @apply bg-gray-200 hover:bg-gray-300 text-gray-800 font-medium py-2 px-4 rounded-lg transition-colors duration-200;
  }
  
  .card {
    @apply bg-white rounded-lg shadow-md p-6;
  }
  
  .input-field {
    @apply w-full px-3 py-2 border border-gray-300 rounded-md focus:outline-none focus:ring-2 focus:ring-primary-500 focus:border-transparent;
  }
}
EOF

    echo "✅ Tailwind CSS setup complete for $app"
    cd ../..
done

echo "🎉 Tailwind CSS setup complete for all apps!"
echo ""
echo "Next steps:"
echo "1. Run 'npm run dev' in the root directory to start all apps"
echo "2. Each app will be available at:"
echo "   - Backoffice: http://localhost:3000"
echo "   - Client Portal: http://localhost:3001"
echo "   - Merchant Portal: http://localhost:3002"
echo "   - Organization Portal: http://localhost:3003" 