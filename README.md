# Ktor Role based Auth
[![](https://jitpack.io/v/omkar-tenkale/ktor-role-based-auth.svg)](https://jitpack.io/#omkar-tenkale/ktor-role-based-auth)

This library provides a Ktor plugin to handle role based authorization in ktor


## Features

- minimal and fast
- supports ktor 2.0.0+ unlike other similar plugins
- supports JWT/Session or any custom mechanism to retrieve roles


## Installation

Step 1. Add the JitPack repository

```kotlin
repositories {
    maven { url "https://jitpack.io" }
}
```

Step 2. Add the dependency

```kotlin
dependencies {
    implementation("com.github.omkar-tenkale:ktor-role-based-auth:0.1.0")
}
```


## Usage

Initialize when setting up application
```kotlin
fun Application.module(){
    installRoleBasedAuthPlugin{
        extractRoles{ call ->
            //Return roles for this request
            //For example in JWT authentication retrieve roles from jwt payload
            (principal as JWTPrincipal).payload.claims?.get("roles")?.asList(String::class.java)?.toSet() ?: emptySet()
        }
    }
}
```
```kotlin
fun Application.routing() {
    route("/posts/") {
        method(HttpMethod.Get) {
            call.respondText("Any user can access this route")
        }
        method(HttpMethod.Post) {
            withRole("admin") {
                call.respondText("Only user with admin role can access this route, others will get a HTTP 403 (Forbidden) response")
            }
        }
    }
}
```
