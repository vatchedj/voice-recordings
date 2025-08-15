# voice-recordings

This is the voice-recordings project for recording stories.

## High-Level Organization

### Frontend

### Backend

### Twilio API



## Development mode

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
