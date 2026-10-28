# Route infrastructure

- `XmlRouteSourceParser`: KML, GPX, KMZ official waypoint imports; does **not** infer route order from Placemark order.
- `RoomRouteRepository`: transactional persistence; every import gets new route and point identities.
- `RoutesContainer`: Android composition root.

The XML importer disables DTDs and external entities. The parser rejects oversized files and malformed coordinates. The original source is not modified. Maps use demonstration tiles until a production tile provider is configured.
