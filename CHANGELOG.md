# 0.3.0
Sorry for the long wait, and thank you all for your patience!

Unfortunately, I had to rework a lot of things under the hood to implement some of the features I planned for this mod, and this time I really wanted to get it right to avoid headaches in the future.

Also, I'm aware of some weird interactions with the new display cases, but I did not want to delay this update any further. I will fix these soon!

- Added 4 new display blocks:
  - Balcony planter box with two display slots, that can hold any combination of flowerpot plantable items. It currently comes in a 'tavern' set variant but more set variants will be added soon!
  - A small and a large display case block to display items and resources in a glass case - a friendly face to all the VS players.
  - An **Experimental!** Weapon display case block was also added, but it is not finished and subject to change!
  > Thank you to SigynLaufeyson for the 3D models for the display case and weapon display case blocks!
- Big changes under the hood:
  - Replaced the ItemDisplayBlock component with a more flexible DisplayContainerBlock component
  - Retired the state-based interaction hints due to complexity
  > Note: Existing display blocks using the old component and/or having the 'Full' state will be changed back to default state upon chunk loading.

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