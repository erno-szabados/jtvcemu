# JAVA TVC Emulator

**jtvcemu** is an emulator for the Videoton TV-Computer (TVC), a Hungarian home computer from the 1980s.

## Features

- Emulates the core TVC hardware
- SD Card support
- MC6845 CRT controller emulation
- VTDOS ROM emulation
- Fullscreen mode and screen scaling at preset sizes
- Improved keyboard handling
- Improved Z80 CPU emulation

## Getting Started

### Prerequisites

- Java Development Kit (JDK) 8 or higher

### Building

Building the Project with Ant

This project uses Apache Ant for its build process. You'll find the build configuration in the build.xml file at the project's root.
Prerequisites

Apache Ant: Make sure you have Ant installed and configured on your system. You can download it from https://ant.apache.org/bindownload.cgi and follow their installation instructions.
Java Development Kit (JDK): A compatible JDK is essential for compiling and running the Java code.

#### How to Build

Navigate to the Project Root: Open your terminal or command prompt and change your current directory to the project's root, where build.xml is located.
```Bash

cd /path/to/your/project
```
Run Ant: Execute the ant command. By default, Ant will look for build.xml and run its default target (usually named build or all).
```Bash

ant
```
This command typically compiles the Java source code, creates JAR files, and performs any other defined build tasks.

Specific Targets (Optional): Your build.xml file might define various targets for different actions (e.g., clean to remove compiled files, test to run tests, javadoc to generate documentation). You can execute a specific target by providing its name:
```Bash

ant clean
ant test
ant javadoc
```
To see a list of available targets, you can often run:
```Bash

ant -p
```

### Running

After compiling, you can run the emulator from the command line:

```sh
java -jar tvc.jar
```

Replace `bin` with your actual build output directory if different.

## Project Structure

- `emulator/tvc/` — Main emulator source code, including hardware emulation (e.g., Z80 CPU, MC6845 CRTC)
- `README.md` — Project overview (this file)

## Status

The project is under sporadic development.

There is little contribution from me, but I published the code for the public good.
Recent updates include:

- SD Card support
- MC6845 emulation
- VTDOS ROM support
- Enhanced graphics/display features
- Improved keyboard and CPU emulation

## Contribution

Contributions are welcome! Please open issues or pull requests for bug fixes, features, or suggestions.

## Emulator License
Copyright 2003-2025  Gabor Hoffer, Erno Szabados, Pal Sebestyen

Licensed under the Apache License, Version 2.0 (the "License");
you may not use this file except in compliance with the License.
You may obtain a copy of the License at

    http://www.apache.org/licenses/LICENSE-2.0

Unless required by applicable law or agreed to in writing, software
distributed under the License is distributed on an "AS IS" BASIS,
WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
See the License for the specific language governing permissions and
limitations under the License.

## ROM License

The TVC ROM is copyrighted by Videoton Zrt.: https://www.videoton.hu/


## Acknowledgments

- MC6845 CRTC emulation is based on original VHDL code by Mike Stirling (2011)
- The original authors
- http://tvc.homeserver.hu/
---

*For more details, refer to the source code and commit history.*
