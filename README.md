## Planets Position

Planets Position is an android app that calculates the position of the planets,
eclipses, and lunar occultations in the sky.

## Installation

1. Install Android Studio [(link)](http://developer.android.com/sdk/index.html)
2. Download the project form the link on the right side of this page.
3. Import the project into Android Studio.

The Android NDK is required to build this project.  Download it [here](http://developer.android.com/tools/sdk/ndk/index.html).

## Google Maps

This project requires a [Google Maps key](https://developers.google.com/maps/documentation/android/start).

Create a file google_maps_api.xml in one or more of the following locations:

 - /app/src/release/res/values/google_maps_api.xml
 - /app/src/debug/res/values/google_maps_api.xml
 - /app/src/beta/res/values/google_maps_api.xml

The file should contain the following:
```xml
<resources>
    <string name="google_maps_key" templateMergeStrategy="preserve" translatable="false">
        [YOUR KEY HERE]
    </string>
</resources>
```

## License

Copyright (C) 2016 Tim Gaddis

This program is free software: you can redistribute it and/or modify
it under the terms of the GNU General Public License as published by
the Free Software Foundation, either version 3 of the License, or
(at your option) any later version.

This program is distributed in the hope that it will be useful,
but WITHOUT ANY WARRANTY; without even the implied warranty of
MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
GNU General Public License for more details.

You should have received a copy of the GNU General Public License
along with this program.  If not, see <http://www.gnu.org/licenses/>.

