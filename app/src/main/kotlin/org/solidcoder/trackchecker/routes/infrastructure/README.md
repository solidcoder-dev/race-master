# Route infrastructure

Implementations will be added in the next change:
- Android file parsing adapters for KML, GPX and KMZ.
- Room repository with atomic route/point persistence.
- Dependency wiring in the Android composition root.

No domain or application code must depend on Android, XML parsers or Room.
