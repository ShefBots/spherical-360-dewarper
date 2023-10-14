# spherical-360-dewarper
A tool that generates dewarping lookup tables for a configured spherical reflection camera so that a 360-degree panoramic image can be generated.

More detailed usage instructions will be coming soon, for now please refer to the [ShefBots PiWars blog post](https://shefbots.github.io/vision/camera/2020/03/16/making-round-images-flat-and-flat-images-round.html) about it.

### Usage
This tool assumes that a spherical reflector (usually a ball-bearing) is suspended above an upward-facing camera, then generates a lookup table in a choosen language that can take an image from the upward-facing camera and convert it to a panoramic image from the perspective of the ball bearing.

#### Tool Usage
TODO: Explain what each bit of the UI actually does

#### Generated Code API
TODO: Explain how to use the generated code (probably with a python example

### Compilation and Running
At the moment the project is just raw java files, so to run you must first clone the repository (make sure to download the submodulealong with it).
After this compile all the appropriate java files by running `javac *.java`, the program can then be run with `java SphericalDewarper`.

### Versioning Convention
The current version is `1.2.0`.

Versioning in the format `Major.minor.bugfix`, where `Major` is the major version - incremented when a major milestone update is
perfomed and backwards-compatability is no longer guaranteed, the `minor` version increments as individual features are implemented,
and `bugfix` increments as bugs in that version are fixed.

Basic notes on revisions can be found in `revisions.log`, further notes can be found in commit messages.
