# Official artwork and proportional exports

Current owner-supplied masters are in branding/current. AI was used, as confirmed by the owner. scripts/prepare_brand_artwork.py exports the Android launcher resources, Android TV banners and Play graphics from these unchanged inputs. Pillow is needed only for this local asset-export operation.

- The square masters supply the launcher icons. Legacy round icons use the platform round shape; adaptive foregrounds place the complete image inside the central66dp safe area of a108dp canvas, keeping the title inside common masks.
- The supplied wallpaper fills the16:9 TV banner using uniform scaling and centered padding at most a rounding pixel wide.
- The supplied3:1 banner fits the1024x500 Play feature graphic with centered padding, preserving the whole artwork without stretching or cropping the title.
- The512px Play icon is copied byte-for-byte from the supplied512px icon.

Package identity, official signing, About notices, player behavior and saved settings are independent of these assets. The same compiled resources serve phone and TV in both distributions. Replacing installed artwork requires a signed bundle/APK with a new unused versionCode. The earlier code53 candidate is preserved; the corrected1.1.2 artwork candidate uses code54.

Artwork-Provenance.json in GooglePlay-Release-Ready/Graphics records source/export hashes, dimensions and fitting method. Existing source history preserves earlier designs. Use current exports for uploads; do not reuse the old draft assets.
