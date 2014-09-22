GeoLing - a statistical software package for geolinguistic data
===============================================================

Changelog
---------

Version 1.0 (2014-09-22)
- Data import: better error messages.
- Data import: set configuration option "useLocationAggregation"
  automatically when importing locations where the same geographical
  coordinates are present several times.
- Included German translation of user guide.
- Some small improvements.

Version 1.0 RC2 (2014-08-16)
- Batch script for starting GeoLing using Windows: try to detect
  Java even if "javaw.exe" is not found in the system paths. For
  example, a 32-bit Java runtime is now detected on a 64-bit Windows.
- Create new SQLite database: do not fail if file extension is missing.
- New directory "logs": if existent, then all messages written to
  the standard output or standard error stream are copied to a new
  log file in this directory.
- Some small improvements.

Version 1.0 RC1 (2014-08-13)
- Initial release.
