# 🔥 Firewatcher

A Progressive Web App for detecting fires in your neighborhood and getting alerts if new fires are detected.
This project is a contribution to the New Relic Hackathon.

### Demo: 

Frontend: https://new-relic-hackathon-firewatch.herokuapp.com/
Backend: https://firewatcher-backend.herokuapp.com/


## Get started

Change to `frontend` directory.

Run app: `./gradlew run`

Run app with hot reload: `./gradlew run --continuous` _*will not hot reload service worker_

Build for production: `./gradlew build`. The bundled files reside in `build/distributions` directory.

Change to `backend` directory.

Run backend: `./gradlew run`

Run backend with hot reload: `./gradlew build -t` and `./gradlew run -Dio.ktor.development=true`

## Used resources

Data:
 - https://nominatim.org/release-docs/latest/api/Search/
