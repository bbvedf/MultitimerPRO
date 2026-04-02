```markdown
# Design System Document: Cyber-Precision Editorial

## 1. Overview & Creative North Star
### Creative North Star: "The Kinetic Chronometer"
This design system moves away from static, utility-first layouts toward a high-end, editorial experience that treats time as a luxury. We are not building a simple utility; we are designing a high-precision instrument. 

By utilizing **Organic Cyber-Minimalism**, we break the traditional "app grid" through intentional asymmetry, overlapping translucent layers, and high-contrast typographic scales. The interface should feel like a heads-up display (HUD) in a luxury spacecraft—sophisticated, luminous, and impossibly sharp. We emphasize "breathing room" (negative space) to ensure that the user’s focus is never divided, only directed.

---

## 2. Colors & Tonal Depth
Our palette is rooted in absolute darkness and pure light, punctuated by "vitals"—vibrant neon accents that signal activity and precision.

### The "No-Line" Rule
**Strict Mandate:** Prohibit the use of 1px solid borders for sectioning or containment. 
Boundaries must be defined solely through background color shifts. For example, a `surface-container-low` component should sit on a `surface` background to define its edges. This creates a seamless, "molded" look rather than a segmented "boxed" look.

### Surface Hierarchy & Nesting
Treat the UI as a series of physical, stacked layers of smoked glass.
- **Base Layer:** `surface` (#0e0e0e)
- **Secondary Depth:** `surface-container-low` (#131313) for large layout sections.
- **Component Elevation:** `surface-container-highest` (#262626) for interactive cards.
- **The Glass Rule:** Use `surface-variant` with a 60% opacity and `backdrop-blur: 20px` for floating overlays. This allows the neon accents to bleed through the UI, creating "visual soul."

### Signature Textures
Avoid flat fills for primary actions. Use a linear gradient from `primary` (#69daff) to `primary-container` (#00cffc) at a 135-degree angle. For active timer states, apply a soft outer glow (`box-shadow: 0 0 15px rgba(0, 209, 255, 0.4)`).

---

## 3. Typography: The Editorial Scale
We pair the technical precision of **Inter** with the futuristic, wide-set character of **Space Grotesk** to create a sophisticated hierarchy.

*   **Display (Space Grotesk):** Reserved for the timer digits. Use `display-lg` (3.5rem) with `font-weight: 700`. Tracking should be set to `-0.02em` to feel like a high-end watch face.
*   **Headlines (Space Grotesk):** Use `headline-md` for screen titles. These should be placed with asymmetric padding (e.g., more left-margin than right) to break the "standard" layout.
*   **Body & Labels (Inter):** All functional text uses Inter. Use `label-md` for metadata. Ensure `on-surface-variant` (#adaaaa) is used for secondary information to maintain the high-contrast "editorial" feel.

---

## 4. Elevation & Depth
Depth in this system is achieved through **Tonal Layering** rather than drop shadows.

*   **The Layering Principle:** To lift a card, do not add a shadow. Instead, transition the background from `surface-container-lowest` to `surface-container-high`.
*   **Ambient Shadows:** For floating modals only, use a "Cyan-Tinted Shadow": `box-shadow: 0 20px 40px rgba(0, 209, 255, 0.08)`. This mimics the light emitted from the neon accents.
*   **The Ghost Border:** If a boundary is required for accessibility, use `outline-variant` at **15% opacity**. High-contrast borders are strictly forbidden as they clutter the "cyber-minimal" aesthetic.

---

## 5. Components

### Primary Action Buttons
*   **Style:** Gradient fill (`primary` to `primary-dim`). 
*   **Radius:** `xl` (1.5rem / 24px) for a "pill" feel that contrasts against the `md` radius of containers.
*   **Interaction:** On press, increase the glow intensity rather than changing the color.

### Precision Timer Cards
*   **Layout:** Forbid divider lines. Separate "Elapsed Time" from "Total Time" using a `2.5rem` vertical gap (Spacing 10).
*   **Background:** Use `surface-container-low`. When a timer is active, add a `primary` "glow-bar" (2px thick) at the very top edge of the card.

### Input Fields
*   **Visuals:** No background fill. Use a `ghost border` (outline-variant at 20%) only on the bottom edge. 
*   **Focus State:** The bottom border transforms into a `primary` neon line with a `2px` thickness and a subtle glow.

### Glass Control Modules (Secondary Containers)
*   **Context:** Used for lap times or settings.
*   **Specs:** `surface-variant` at 40% opacity, `backdrop-filter: blur(12px)`, and `rounded-lg` (1rem).

---

## 6. Do’s and Don’ts

### Do
*   **Use Asymmetry:** Place the "Start" button off-center or use oversized typography that bleeds toward the edge of the screen.
*   **Embrace Negative Space:** If a screen feels "empty," leave it. High-end design is defined by what isn't there.
*   **Color as Data:** Use `secondary` (#2ff801) exclusively for "Success" or "Completed" states. Use `primary` (#69daff) for "In Progress."

### Don’t
*   **No Dividers:** Never use a 1px line to separate list items. Use a background shift or `1.5rem` of vertical space.
*   **No Generic Shadows:** Avoid the default `rgba(0,0,0,0.5)` shadows. They muddy the deep charcoal backgrounds.
*   **No Standard Grids:** Avoid perfectly centered, boxy layouts. Think like a magazine editor—layer your elements.
*   **No Heavy Borders:** Never use an opaque `outline`. It breaks the "glass" illusion of the interface.

---

## 7. Iconography
*   **Weight:** All icons must be "Thin" (approx 1px to 1.5px stroke).
*   **Size:** Standardized at 24px, but placed within a 48px touch target.
*   **Style:** Icons should be "Open-ended"—line paths that don't always fully close, reinforcing the "Cyber" technical drawing aesthetic.```