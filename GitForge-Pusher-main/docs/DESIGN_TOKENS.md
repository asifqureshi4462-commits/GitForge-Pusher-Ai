# GitForge Pusher - Design Tokens (final, Phase 2)

Source of truth in code: `app/src/main/res/values/colors.xml` (light), `values-night/colors.xml` (dark),
`type.xml`, `dimens.xml`, `styles.xml`, `themes.xml`.

**Provenance.** *Sampled* = measured from the pixels of the approved reference image. *Derived* = computed
by us (contrast or consistency) because the image shows no usable value. The image's own palette labels are
garbled (two swatches are both labelled "Warning"), so no hex was copied from its text. Please confirm these.

## Colour

| Token | Light | Dark | Provenance / role |
|---|---|---|---|
| gf_background / gf_surface | #EEF4FD | #001028 | Sampled (light thumbnail, splash). Screen background |
| gf_surface_container (cards) | #FFFFFF | #0F2849 | Dark sampled (Surface swatch). Light derived |
| gf_surface_container_low | #F6F9FE | #08203F | Derived |
| gf_surface_container_high | #E3ECFA | #163256 | Derived |
| gf_on_surface | #0B1B33 | #F2F6FF | Derived (image text is near white) |
| gf_on_surface_muted | #51627F | #9FB3D1 | Derived |
| gf_outline / outline_variant | #8393B0 / #C5D3EA | #5D7399 / #2A4570 | Derived |
| gf_primary | #4D58F4 | #4D58F4 | Sampled (Primary swatch, buttons #4252EE-#4D58F4) |
| gf_on_primary | #FFFFFF | #FFFFFF | Contrast 5.23:1 |
| gf_primary_container / on | #DDE1FF / #0F1A8C | #1B2478 / #DDE1FF | Derived |
| gf_secondary | #4F65F6 | #4F65F6 | Sampled. Nearly identical to primary in the reference |
| gf_error | #D83140 | #F55069 | Dark sampled (Failed badge); light sampled (red accent) |
| gf_success | #0E9F7E | #34D4AC | Dark sampled (Success badge/check); light derived |
| gf_warning | #B7791F | #EEB63C | Dark sampled (gold icon); light derived |
| gf_info | #1F6FC4 | #4499EA | Dark sampled (blue swatch); light derived |
| gf_splash_background | = background | = background | Splash screen |

Status colours are exposed to layouts as theme attributes `?attr/gfColorSuccess|Warning|Info|TextMuted`.

### Accent choices (Appearance, reference screen 16)
Indigo #4D58F4 (default), Slate #587295, Green #04B688, Cyan #00A7D5, Violet #6F5FF1, Purple #9F4FE6,
Orange #E48E1D, Amber #F0A637, Red #D83140. All sampled. On-accent text is white or #0B1220, whichever has
higher contrast (Purple reaches only 4.43:1 white / 4.22:1 dark - acceptable for large/bold text, not body text).
The accent overlay changes `colorPrimary` / `colorOnPrimary` only.

## Typography (reference "Inter or similar")
| Style | Size/Line | Weight | Theme attribute |
|---|---|---|---|
| H1 | 28/32 | Bold | textAppearanceHeadlineLarge |
| H2 | 22/28 | SemiBold | textAppearanceHeadlineMedium |
| H3 | 18/24 | SemiBold | textAppearanceHeadlineSmall |
| Body | 16/24 | Regular | textAppearanceBodyLarge |
| Caption | 14/20 | Regular | textAppearanceBodyMedium |
| Button | 16/20 | Medium | textAppearanceLabelLarge |

Open item: Inter is **not bundled** (no font files available offline). Styles use system sans-serif;
SemiBold is approximated with sans-serif-medium; line height needs API 28+ (ignored on 26-27).

## Spacing, shape, size
Spacing 4/8/12/16/24/32 dp (`gf_space_*`). Card radius 16 dp, button 12 dp, input 12 dp, hairline stroke 1 dp.
Button height 48 dp; minimum touch target 48 dp; icon 24 dp.

## Components styled so far
Button (`Widget.GitForge.Button`), Card (`Widget.GitForge.Card`, filled surface container + hairline outline),
BottomNavigation (`Widget.GitForge.BottomNavigation`, active item = primary), TextInputLayout (outlined, 12 dp).

## Open design items
1. Final logo asset: the hexagon + arrow mark is an interim vector, not the reference's artwork.
2. Inter font files.
3. Light-theme values are derived; the reference shows only a thumbnail.
