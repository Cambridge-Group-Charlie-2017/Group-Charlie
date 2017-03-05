# Group-Charlie [![Build Status](https://travis-ci.com/nbdd0121/Group-Charlie.svg?token=UCx7UNqgL7Ahyxp6kpA2&branch=master)](https://travis-ci.com/nbdd0121/Group-Charlie)
This is the repository for Group Charlie of Cambridge Computer Science Part IB Group Project.

## Auto Archive v1.0

This Group-Charlie module is the locally-run Java component, necessary for supporting the mail client, which does the following things:
- Maintains persistent storage of information such as the cluster representation, word counts, archiving data etc. in a folder AutoArchive in the AppData directory.
- Retrieves emails from the server and provides them to the UI
- Supports sending new emails
- Clusters the newest 500 emails on the IMAP server and finds reasonable names for the clusters
- Scans for new emails periodically
- Classifies any new emails received since the last time clustering was run, and inserts them into the closest match of the existing clusters
- Finds vector representations of emails and documents
- Supports smart attachment suggestion, whereby the folder opened when the user wants to attach a file to an email contains similar documents


Then UI is dependent on this module, so this module must be run prior to running the UI. To run the program, use command `./gradlew run`, or, if you are using Windows, `gradlew.bat run`.

To run this for the first time, the following commands must be run in the Group-Charlie directory:
```
chmod +x gradlew
./gradlew assemble
./gradlew populateDB
```

or Windows equivalent:
```
gradlew.bat assemble
gradlew.bat populateDB
```

