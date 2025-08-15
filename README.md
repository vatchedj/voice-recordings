# voice-recordings

This is the voice-recordings project for recording stories.

## High-Level Organization

### Frontend

The frontend files are located in `src/cljs`.

#### core.cljs

Initializes the UI and contains the high-level template structure that applies to all pages via the `current-page` function.

#### common.cljs

Contains the router and a few other common functions.

#### pages folder

Contains a file for each page and corresponding logic.

#### To add a new page

1. Add a new file with in the `src/cljs/voice_recordings/pages` directory or make a copy of an existing page.
2. Add an entry to `router` within the `src/cljs/voice_recordings/common.cljs` file.
3. Add an entry to `page-for` within `src/cljs/core.cljs`.
4. Add an entry to `app` in the server-side code located at `src/clj/handler.clj`.

### Backend

#### db.clj

Contains functions to interact with the PostgreSQL database.

#### handler.clj

Contains the routes and related logic.

#### server.clj

Initializes the back-end. You probably won't need to make changes here very often.

#### twilio.clj

Contains logic to connect to Twilio's API.

#### How to add a new route

Add an entry to `app` in `src/clj/voice_recordings/handler.clj` with a reference to the handler function.

Look at the existing route handlers for examples.

### Twilio API

Twilio's API handles making and recording calls, as well as storing those recordings.

## Local setup

### Prerequisites

1. **Java 8** - Required for running Leiningen and Clojure
    - Install via your package manager or download from Oracle/OpenJDK

2. **Leiningen** - Clojure build tool
    - Install from [leiningen.org](https://leiningen.org/)
    - Run `lein version` to verify installation

3. **PostgreSQL** - Database server
    - Install PostgreSQL locally (version 9.5+ recommended)
    - Create a database for the application
    - Note your database connection details (host, port, database name, username, password)

4. **Node.js** (optional) - For additional frontend tooling
    - Install from [nodejs.org](https://nodejs.org/) if using npm packages

### Configuration

Create a `profiles.clj` file in the project root with your local configuration:

```clojure
{:local  {:env {:postgres-db         "voice_recordings"
                :postgres-user       "YOUR_LOCAL_POSTGRES_USER"
                :postgres-password   "YOUR_LOCAL_POSTGRES_PASSWORD"
                :pghost              "localhost"
                :pgport              5432
                :twilio-account-sid  "YOUR_TWILIO_ACCOUNT_SID"
                :twilio-auth-token   "YOUR_TWILIO_AUTH_TOKEN"
                :twilio-phone-number "YOUR_TWILIO_PHONE_NUMBER"}}}
```

## How to run locally

To run this service locally, run:

```
lein with-profile +local figwheel
```

The front-end and back-end will be accessible at [http://localhost:3449](http://localhost:3449) once Figwheel starts up.

Changes are automatically hot-loaded.

To run the backend separately, run:

```
lein with-profile +local run
```

The application will now be available at [http://localhost:3000](http://localhost:3000).

## Building for release

```
lein do clean, uberjar
```
