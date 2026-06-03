/** @type {import('tailwindcss').Config} */
export default {
  content: ['./index.html', './src/**/*.{js,jsx}'],
  theme: {
    extend: {
      colors: {
        cinema: {
          950: '#050608',
          900: '#090b10',
          850: '#0d1118',
          800: '#121722',
          700: '#1d2636',
        },
        ember: {
          500: '#f97316',
          400: '#fb923c',
        },
        gold: {
          500: '#f7c948',
        },
      },
      boxShadow: {
        glow: '0 0 60px rgba(249, 115, 22, 0.18)',
        panel: '0 24px 80px rgba(0, 0, 0, 0.35)',
      },
      backgroundImage: {
        'cinema-radial': 'radial-gradient(circle at 20% 10%, rgba(249,115,22,0.22), transparent 30%), radial-gradient(circle at 80% 0%, rgba(247,201,72,0.12), transparent 28%), linear-gradient(135deg, #050608 0%, #0d1118 48%, #121722 100%)',
      },
    },
  },
  plugins: [],
};
