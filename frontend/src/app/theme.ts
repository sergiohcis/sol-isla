import { definePreset } from '@primeuix/themes';
import Aura from '@primeuix/themes/aura';

/**
 * Placeholder Sol Isla preset — a neutral Aura override, not yet based on real brand colors.
 * Replace `semantic.primary` (and `surface` if needed) once there's an actual design reference,
 * same structure SweetHome used for its own brand pass (see SweetHome's theme.ts).
 */
export const SolIslaPreset = definePreset(Aura, {
  primitive: {
    borderRadius: { none: '0', xs: '2px', sm: '4px', md: '6px', lg: '8px', xl: '12px' },
  },
  semantic: {
    primary: {
      50: '#ECFDF5', 100: '#D1FAE5', 200: '#A7F3D0', 300: '#6EE7B7', 400: '#34D399',
      500: '#10B981', 600: '#059669', 700: '#047857', 800: '#065F46', 900: '#064E3B', 950: '#022C22',
      color: '{primary.600}',
      contrastColor: '#ffffff',
      hoverColor: '{primary.700}',
      activeColor: '{primary.800}',
    },
    surface: {
      0: '#ffffff', 50: '#F8F9FA', 100: '#F3F4F6', 200: '#E5E7EB', 300: '#D1D5DB',
      400: '#9CA3AF', 500: '#6B7280', 600: '#4B5563', 700: '#374151', 800: '#1F2937',
      900: '#1A1F36', 950: '#0F1419',
    },
  },
});
