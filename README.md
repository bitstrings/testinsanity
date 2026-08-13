# TestInsanity for IDEA
<img alt="TestInsanity Logo" src="resources/META-INF/pluginIconDoc.png" />
<br/>

TestInsanity is an IDEA plugin for renaming tests methods and classes on refactoring. 

# License
Apache License, Version 2.0

[LICENSE.txt](LICENSE.txt)

# Project configuration

Settings can live in version control instead of in `.idea`. Create `.testinsanity.json` in the project
root:

```json
{
  "testClassPatterns": ["${className}Test", "${className}IT"],
  "testMethodPatterns": ["(test|)${subjectName}*"],
  "capitalizeSubject": "ifPrefixed",
  "testAnnotations": ["junit4", "junit5", "testng"],
  "includeInheritedMethods": true,
  "includeInterfacesAndAbstracts": false,
  "includeNestedClasses": true,
  "syncDisplayName": false,
  "refactoring": true,
  "navigation": true,
  "gutterIcons": true,
  "preselectRenames": true
}
```

Every key is optional, and precedence works like `.editorconfig`: a key the file declares governs the
project and is shown disabled in Settings -> Tools -> TestInsanity; a key the file omits stays an
individual preference. The file is re-read whenever it changes, so switching branches switches scheme.

The file is bound to a JSON schema shipped with the plugin, so keys, allowed values and their
documentation complete as you type and a typo is flagged immediately.

`capitalizeSubject` is one of `unchanged`, `ifPrefixed`, `always`. `testAnnotations` accepts `junit4`,
`junit5` and `testng`. Patterns are validated on load; an invalid pattern, a malformed file or an
unexpected value is reported in a notification and leaves the previous settings in force. An unknown key
is reported as a warning and ignored, so a file written for a newer plugin version still works.
