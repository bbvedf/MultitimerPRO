# Design System Specification: Luminous Cyber-Minimalism

## 1. Overview & Creative North Star
**Creative North Star: "The Ethereal Terminal"**

This design system is a sophisticated evolution of high-performance interfaces, reimagined for high-clarity, light-mode environments. It moves away from the aggressive "hacker" tropes of dark-mode aesthetics, instead embracing an **Editorial Cyber** feel. 

We achieve a premium, custom experience by breaking the traditional rigid grid through **intentional asymmetry** and **tonal depth**. The interface should feel like a series of translucent, high-tech glass panels floating in a sunlit laboratory. We prioritize "white space as a luxury" and use vibrant neon accents as precision instruments rather than blunt decorative tools.

---

## 2. Colors & Surface Philosophy
The palette is built on a foundation of "Oxygenated Neutrals" punctuated by high-energy "Data-Fluorescent" accents.

### The Palette (Material Design Tokens)
*   **Surface Foundation:** `surface` (#f5f6f7), `surface_container_lowest` (#ffffff)
*   **Accents:** `primary` (#00647b / Neon Base), `secondary` (#0f6b00 / Bio-Digital Green)
*   **Contrast:** `on_surface` (#2c2f30)

### The "No-Line" Rule
**Explicit Instruction:** Designers are prohibited from using 1px solid borders for sectioning. 
Structure must be defined through **Background Color Shifts** or **Tonal Transitions**. For instance, a side panel in `surface_container_low` should simply sit flush against a `surface` main area. The eye should perceive the boundary through the shift in luminance, not a drawn line.

### Surface Hierarchy & Nesting
Treat the UI as a physical stack of glass and paper:
1.  **Level 0 (Base):** `surface` (#f5f6f7) - The desktop or main canvas.
2.  **Level 1 (Sections):** `surface_container_low` (#eff1f2) - Large content areas.
3.  **Level 2 (Objects):** `surface_container_lowest` (#ffffff) - Cards and interactive elements that need to "pop."
4.  **Level 3 (Overlays):** Glassmorphism (see below).

### The "Glass & Gradient" Rule
To escape the "bootstrap" look, utilize:
*   **Backdrop Blur:** Floating menus and modals must use a semi-transparent `surface_container_lowest` with a `blur(12px)` effect.
*   **Signature Gradients:** Main CTAs should not be flat. Use a linear gradient: `primary` (#00647b) to `primary_container` (#00cffc) at a 135° angle. This adds "digital soul" and depth.

---

## 3. Typography
We use **Space Grotesk** exclusively. Its geometric quirks provide a "tech" flavor while its open apertures ensure readability.

*   **Display (lg/md/sm):** Used for "Statement Headings." Set with a slight negative letter-spacing (-0.02em) to feel tighter and more editorial.
*   **Headline & Title:** Use for section anchors. These should always be `on_surface` to maintain authority.
*   **Body (lg/md/sm):** High-legibility blocks. Ensure `body-lg` is used for introductory paragraphs to maintain a premium feel.
*   **Labels:** Use for technical metadata. Often paired with `secondary` (Green) or `primary` (Blue) to denote status or category.

**Hierarchy Note:** Use dramatic scale shifts. A `display-lg` headline next to a `body-md` description creates the intentional asymmetry that defines this system.

---

## 4. Elevation & Depth

### The Layering Principle
Depth is achieved via **Tonal Layering**. Instead of shadows, use the Spacing Scale to create "breathing room" between tiered surfaces.
*   *Example:* A `surface_container_highest` header floating over a `surface` body creates an immediate, clean hierarchy without a single drop shadow.

### Ambient Shadows
When an element must float (e.g., a primary Modal or Fab), use **Ambient Shadows**:
*   **Value:** `0px 20px 40px rgba(44, 47, 48, 0.06)`
*   **Note:** Never use pure black for shadows. Always tint the shadow with the `on_surface` color to mimic natural light dispersion.

### The "Ghost Border" Fallback
If accessibility requires a container boundary, use a **Ghost Border**:
*   **Token:** `outline_variant` (#abadae) at **15% opacity**.
*   **Prohibition:** Never use 100% opaque borders. They clutter the "Ethereal" aesthetic.

---

## 5. Components

### Buttons
*   **Primary:** Gradient-filled (`primary` to `primary_container`). `rounded-md` (0.375rem). Text: `on_primary_container`.
*   **Secondary:** Ghost style. `outline_variant` (15% opacity) border with `primary` text.
*   **States:** On hover, increase the `backdrop-filter: brightness(1.1)`.

### Input Fields
*   **Style:** Minimalist. No bottom line or full box. Use `surface_container_low` as a subtle background fill with a `rounded-sm` corner.
*   **Focus State:** A 2px "Neon Glow" on the left edge using the `primary_fixed` token.

### Cards & Lists
*   **Rule:** No dividers. Separate items using `8` (2rem) from the Spacing Scale.
*   **Interaction:** On hover, a card should transition from `surface_container_low` to `surface_container_lowest` and gain a subtle `Ambient Shadow`.

### Selection Chips
*   **Active:** `secondary_container` (#2ff801) background with `on_secondary_container` (#0b5800) text. This provides a "high-visibility" data-point look.

---

## 6. Do’s and Don’ts

### Do:
*   **Do** embrace negative space. If a layout feels "full," increase the spacing token by two increments.
*   **Do** use the `secondary` (Green) accent for success states and "live" data indicators.
*   **Do** align text-heavy content to a strict editorial column while allowing decorative "cyber" elements to bleed off-grid.

### Don’t:
*   **Don’t** use pure black (#000000). It breaks the light-mode "Ethereal" quality.
*   **Don’t** use 1px dividers to separate list items; use vertical white space (Token `4` or `6`).
*   **Don’t** use standard "Blue" for links. Use the `primary` (#00647b) for a more sophisticated, curated appearance.
*   **Don't** over-round corners. Stick to `md` (0.375rem) for most components to keep the "Cyber" edge sharp and modern.