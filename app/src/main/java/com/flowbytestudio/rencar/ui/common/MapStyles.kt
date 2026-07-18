package com.flowbytestudio.rencar.ui.common

/**
 * MapLibre raster stil tanımları. MapScreen ve ActiveRentalScreen aynı harita
 * sağlayıcılarını kullandığı için tek yerden tanımlanır; tile URL'i değişirse
 * tek dosya güncellenir.
 */
object MapStyles {

    const val OSM_LIGHT = """
{
  "version": 8,
  "sources": {
    "osm-tiles": {
      "type": "raster",
      "tiles": ["https://tile.openstreetmap.org/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "&copy; OpenStreetMap contributors"
    }
  },
  "layers": [
    {
      "id": "osm-tiles-layer",
      "type": "raster",
      "source": "osm-tiles",
      "minzoom": 0,
      "maxzoom": 19
    }
  ]
}
"""

    const val CARTO_DARK = """
{
  "version": 8,
  "sources": {
    "carto-dark-tiles": {
      "type": "raster",
      "tiles": ["https://a.basemaps.cartocdn.com/dark_all/{z}/{x}/{y}.png"],
      "tileSize": 256,
      "attribution": "&copy; OpenStreetMap contributors &copy; CARTO"
    }
  },
  "layers": [
    {
      "id": "carto-dark-tiles-layer",
      "type": "raster",
      "source": "carto-dark-tiles",
      "minzoom": 0,
      "maxzoom": 19,
      "paint": {
        "raster-brightness-min": 0.15,
        "raster-brightness-max": 1.0,
        "raster-contrast": -0.1
      }
    }
  ]
}
"""
}
