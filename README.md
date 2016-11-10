# Android Localization Utils

> **WARNING:** This project was only ever intended as quick-and-dirty tools for personal use. Given some time in the future I may properly clean it up or release it as an Android Studio plugin but I make no guarantees.

A collection of utilities intended to make identifying and applying translations easier during Android app development.

## Usage
1. Either checkout and build the project yourself or download the latest release from the [releases page](https://github.com/skidson/android-localization-utils/releases)

2. Execute a command

### `identify`
Identifies missing translations.
Searches all values-XX/strings.xml files for translations missing from the default strings.xml and generates
separate strings_XX.xml files at the specified output directory for each locale. These files are intended to
be passed to a translation team and then passed back in to the `apply` command after being updated.

Usage: `java -jar android-localization-utils-<version>-all.jar identify <res folder> <new translations folder>`

Example: `java -jar android-localization-utils-0.1-all.jar identify /Users/skidson/Code/my-android-app/app/src/main/res /Users/skidson/Documents/new-translations`

### `diff`
Compares an incoming strings.xml file to an existing strings.xml file and lists new, modified, and removed strings. Useful for when your project manager has asked for all the strings and updates them but doesn't have a clue about XML.

Usage: `java -jar android-localization-utils-<version>-all.jar diff <old strings.xml> <new strings.xml>`

Example: `java -jar diff /Users/skidson/Code/my-android-app/app/src/main/res/values/strings.xml /Users/skidson/Documents/new-translations/strings_cs.xml`

### `apply`
Reads in all strings_XX.xml files in the specified directory and applies them to the project. This command will maintain the  order and any comments of existing values-XX/strings.xml files. New translations, however, will be appended to the bottom of the file.

Usage: `java -jar android-localization-utils-<version>-all.jar apply <new translations folder> <res folder>`

Example: `java -jar apply /Users/skidson/Documents/new-translations /Users/skidson/Code/my-android-app/app/src/main/res/`
