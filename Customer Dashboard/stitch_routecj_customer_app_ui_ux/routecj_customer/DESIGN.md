---
name: RouteCJ Customer
colors:
  surface: '#0f131f'
  surface-dim: '#0f131f'
  surface-bright: '#353946'
  surface-container-lowest: '#0a0e1a'
  surface-container-low: '#171b28'
  surface-container: '#1b1f2c'
  surface-container-high: '#262a37'
  surface-container-highest: '#313442'
  on-surface: '#dfe2f3'
  on-surface-variant: '#c2c6d8'
  inverse-surface: '#dfe2f3'
  inverse-on-surface: '#2c303d'
  outline: '#8c90a1'
  outline-variant: '#424656'
  surface-tint: '#b3c5ff'
  primary: '#b3c5ff'
  on-primary: '#002b75'
  primary-container: '#0066ff'
  on-primary-container: '#f8f7ff'
  inverse-primary: '#0054d6'
  secondary: '#b7c8e1'
  on-secondary: '#213145'
  secondary-container: '#3a4a5f'
  on-secondary-container: '#a9bad3'
  tertiary: '#4edea3'
  on-tertiary: '#003824'
  tertiary-container: '#008259'
  on-tertiary-container: '#e1ffec'
  error: '#ffb4ab'
  on-error: '#690005'
  error-container: '#93000a'
  on-error-container: '#ffdad6'
  primary-fixed: '#dae1ff'
  primary-fixed-dim: '#b3c5ff'
  on-primary-fixed: '#001849'
  on-primary-fixed-variant: '#003fa4'
  secondary-fixed: '#d3e4fe'
  secondary-fixed-dim: '#b7c8e1'
  on-secondary-fixed: '#0b1c30'
  on-secondary-fixed-variant: '#38485d'
  tertiary-fixed: '#6ffbbe'
  tertiary-fixed-dim: '#4edea3'
  on-tertiary-fixed: '#002113'
  on-tertiary-fixed-variant: '#005236'
  background: '#0f131f'
  on-background: '#dfe2f3'
  surface-variant: '#313442'
typography:
  headline-lg:
    fontFamily: Inter
    fontSize: 32px
    fontWeight: '700'
    lineHeight: 40px
    letterSpacing: -0.02em
  headline-md:
    fontFamily: Inter
    fontSize: 24px
    fontWeight: '600'
    lineHeight: 32px
    letterSpacing: -0.01em
  headline-sm:
    fontFamily: Inter
    fontSize: 20px
    fontWeight: '600'
    lineHeight: 28px
  title-lg:
    fontFamily: Inter
    fontSize: 18px
    fontWeight: '600'
    lineHeight: 24px
  body-lg:
    fontFamily: Inter
    fontSize: 16px
    fontWeight: '400'
    lineHeight: 24px
  body-md:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '400'
    lineHeight: 20px
  label-lg:
    fontFamily: Inter
    fontSize: 14px
    fontWeight: '600'
    lineHeight: 20px
    letterSpacing: 0.1px
  label-md:
    fontFamily: Inter
    fontSize: 12px
    fontWeight: '500'
    lineHeight: 16px
    letterSpacing: 0.5px
  headline-lg-mobile:
    fontFamily: Inter
    fontSize: 28px
    fontWeight: '700'
    lineHeight: 36px
rounded:
  sm: 0.25rem
  DEFAULT: 0.5rem
  md: 0.75rem
  lg: 1rem
  xl: 1.5rem
  full: 9999px
spacing:
  none: 0px
  xs: 4px
  sm: 8px
  md: 16px
  lg: 24px
  xl: 32px
  2xl: 48px
  edge_margin: 16px
  gutter: 12px
---

## Brand & Style

The design system is engineered for the high-stakes environment of logistics and transportation. It prioritizes **Trust, Velocity, and Precision**. The aesthetic leans into **Modern Corporate Minimalism** with a technical edge, ensuring that data-heavy screens remain legible and actionable under various lighting conditions.

The UI evokes a sense of reliability through:
- **Spaciousness:** Generous padding to prevent information overload.
- **Subtle Tactility:** Utilizing soft depth and layered surfaces rather than aggressive shadows.
- **Functional Sophistication:** A blend of professional utility and premium finishes that distinguishes the product as a leader in logistics technology.

## Colors

The color system is anchored by a high-performance **Primary Blue (#0066FF)**, signifying movement and connectivity. 

- **Dark Mode (Default):** Uses a deep navy foundation to reduce eye strain for drivers and dispatchers. Surfaces use a slightly lighter navy to create a hierarchy of information layers.
- **Light Mode:** Shifts to a clean, architectural white and light gray palette to maximize outdoor legibility.
- **Semantic Colors:** Green (#10B981) for "Delivered/Success," Amber (#F59E0B) for "In Transit/Pending," and Red (#EF4444) for "Delayed/Error."

## Typography

This design system utilizes **Inter** for its exceptional legibility and systematic feel. The type scale is optimized for Android devices, ensuring high contrast between headers and body text.

- **Weight Usage:** Use Bold (700) for primary tracking numbers or status headlines. Use Medium (500) for labels to ensure they don't disappear against dark backgrounds.
- **Tracking:** Headlines use slightly tighter letter spacing to maintain a compact, "tech-first" appearance.

## Layout & Spacing

The layout follows a **Fluid Grid** model optimized for Android's flexible screen ratios. 

- **Margins:** A standard 16px horizontal margin is maintained across all mobile views.
- **Rhythm:** Spacing follows an 8px base unit. Most vertical separations between distinct content blocks should use `lg` (24px) to ensure the "spacious" premium feel.
- **Safe Areas:** Adhere strictly to system bars and gesture navigation zones to ensure the UI feels native to the Android OS.

## Elevation & Depth

Hierarchy is established through **Tonal Layering** rather than heavy shadows.

- **Level 0 (Background):** The base layer (`#0A0E1A` in dark mode).
- **Level 1 (Cards/Containers):** Raised surfaces using `#161C2E`. These should have a very subtle 1px border (`#FFFFFF` at 5% opacity) to define edges.
- **Level 2 (Modals/Popups):** Higher elevation using a slight backdrop blur and a soft, diffused shadow (0px 8px 24px rgba(0,0,0, 0.4)).
- **Interactions:** On-press states should use a subtle brightness increase (approx 5%) rather than a traditional ripple to maintain the premium aesthetic.

## Shapes

The shape language is defined by **pronounced, friendly rounding**.

- **Cards & Primary Containers:** Use `radius_md` (16px) as the standard.
- **Buttons:** Use `radius_lg` (24px) or full pill-shape to distinguish them as highly interactive elements.
- **Small Elements:** Chips and Input fields use `radius_sm` (8px) to maintain a crisp relationship with larger containers.

## Components

### Buttons
- **Primary:** Solid Blue (#0066FF) with White text. Bold, 56px height for main mobile actions.
- **Secondary:** Surface-colored with a 1px border.
- **Ghost:** No background, primary blue text for low-priority actions like "View Details."

### Cards
Cards are the primary vehicle for shipment data. They must feature `radius_md`, 16px internal padding, and clear separation between the "Tracking Number" (Headline-SM) and "Status" (Status Badge).

### Status Badges
Pill-shaped with low-opacity backgrounds (e.g., Green text on 10% Green background) to indicate shipment state without overwhelming the visual field.

### Input Fields
Outlined style with a 16px corner radius. The border should thicken and turn Primary Blue when focused. Floating labels are preferred for Jetpack Compose implementation.

### Bottom Navigation
A fixed-bottom surface with 4-5 icons. Use active-state tinting (Primary Blue) and a small dot indicator below the icon to signal the current destination.

### State Transitions
- **Loading:** Use a custom shimmer effect on cards rather than a generic spinner.
- **Empty State:** Centered illustration with a muted blue-gray color profile, accompanied by a clear CTA to "Create Shipment" or "Refresh."