# esw-segment-web
This subproject contains the Typescript based ESW Segment DB web app.

![screenshot](../images/esw-segment-web.png)

The "Installed" view shows the sectors of the TMT mirror with their positions (A1 to F82).
Optionally, you can view the segment ids instead. The outlined segments are the
ones that changed most recently. Using the arrow buttons in the tool bar you can
go back and forth in time to see the previous configurations of the mirror.

On the left are some controls that let you view various information gathered from the
JIRA tasks that are used to manage segment information.
Updating the JIRA information (via the "Sync with JIRA" item) requires a valid JIRA token.

## Prerequisites Required for Running App

The latest version of [Node.js](https://nodejs.org/en/download/package-manager/) must be installed.

## Run the App in Local Environment

Run following commands in the terminal.
   ```
   npm install
   npm start
   ```
Then, open http://localhost:8080 in a browser

## Build the App for Production

Run following commands in the terminal.
```
npm install
npm run build
```

## Running Tests

```
npm test
```

## How to Use the Project

The project has following structure:
```bash
.
├── src
│   ├── assets
│   ├── components
│   ├── config
│   ├── helpers
├── test
├── types
```

* `assets`: This directory contains all the files (images, audio etc) that are used by the UI component.
* `components`: This directory contains all the components created for this UI application.
* `config`: This contains the application specific configurations.
* `helpers`: App reusable functions / utilities goes here.
* `test`: This directory contains all the tests for the UI application.
* `types`: This directory contains all the types that needs to be imported externally for UI application.


## References
- ESW-TS Library - [Link](https://tmtsoftware/esw-ts/)
- ESW-TS Library Documentation - [Link](https://tmtsoftware.github.io/esw-ts/)
