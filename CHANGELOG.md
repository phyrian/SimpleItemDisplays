# 0.3.0
- Added a small and a large display case as well as an experimental weapon display case block. Thank you to SigynLaufeyson for the 3D models
- Fixed display blocks not updating when replaced
- Replaced the ItemDisplayBlock component with a more flexible DisplayContainerBlock component. Existing blocks using the old component will be migrated upon chunk loading.

# 0.2.0
- Added custom prop transformation for items with no block model:
  - Ingredients are now aligned flat against the surface (as flat as the model allows) 
  - Weapons, tools and gliders are now rotated diagonally, stretching from the bottom left to the top right
  - Armour pieces are displayed similarly to blocks, facing outward from the Item Frame

# 0.1.1
- Resized all flowerpot icons to fit better and removed the custom rotation from the Feran Flowerpot model
- Reworked interactions

# 0.1.0
- Initial version with 9 flowerpot variants and 1 item frame