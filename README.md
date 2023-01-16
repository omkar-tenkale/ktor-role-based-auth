> UPDATE: Read official announcement for role based auth in [Ktor 2023 roadmap](https://blog.jetbrains.com/ktor/2022/12/16/ktor-2023-roadmap/)      
> Simplifying support for authorization and authentication. Providing end-to-end support for database authentication and session management, as well as role-based authorization

# Ktor Role based Auth
[![](https://jitpack.io/v/omkar-tenkale/ktor-role-based-auth.svg)](https://jitpack.io/#omkar-tenkale/ktor-role-based-auth)

ktor-role-based-auth is an easy to use and intuitive role-based access control library for Ktor Server

It works with the official [ktor-server-auth](https://ktor.io/docs/authentication.html) library and adds role based authorization on top of it

Supported methods
- HTTP authentication (Basic/Digest/Bearer)
- Form-based authentication
- JWT
- Session
- OAuth
- LDAP
- Custom authentication


## Installation

Step 1. Add the JitPack repository in `build.gradle.kts`

```kotlin
repositories {
    mavenCentral()
    maven { url = uri("https://jitpack.io") }
}
```

Step 2. Add the dependency

```kotlin
dependencies {
    implementation("com.github.omkar-tenkale:ktor-role-based-auth:0.2.0")
}
```


## Usage

Initialize the plugin when configuring authentication
```kotlin
fun Application.configureSecurity(){
    authentication {
        jwt {
            // Configure jwt authentication
        }
        roleBased {
            extractRoles { principal ->
                //Extract roles from JWT payload
                (principal as JWTPrincipal).payload.claims?.get("roles")?.asList(String::class.java)?.toSet() ?: emptySet()
            }
        }
    }
}
```

```kotlin
fun Application.routing() {
    route("/") {
        
        //No authentication required to access this route
        get {
            call.respondText("Welcome!")
        }

        authenticate {

            //JWT authenticated route
            route("/profile") {
                get {
                    call.respondText("Joined: 2 years ago")
                }
            }

            //JWT authenticated + role authorized route
            route("/dashboard") {
                withAnyRole("ADMIN", "SUPER_ADMIN") {
                    get {
                        call.respondText("Total users: 2443")
                    }
                }
            }
            
            //JWT authenticated + role authorized route
            route("/system-stats") {
                withRole("SUPER_ADMIN") {
                    get {
                        call.respondText("CPU: 34%")
                    }
                }
            }
        }
    }
}
```

The plugin responds with `403 (Forbidden)` by default if roles don't match
Optionally, follow these steps to send a custom response
1. Set `throwErrorOnUnauthorizedResponse` to `true`
```kotlin
fun Application.configureSecurity(){
    authentication {
        jwt {
            // Configure jwt authentication
        }
        roleBased {
            extractRoles { principal ->
                //Extract roles from JWT payload
                (principal as JWTPrincipal).payload.claims?.get("roles")?.asList(String::class.java)?.toSet() ?: emptySet()
            }
            throwErrorOnUnauthorizedResponse = true
        }
    }
}
```
2. Catch the `UnauthorizedAccessException` exception globally with help of [StatusPages plugin](https://ktor.io/docs/status-pages.html)
```kotlin
fun Application.configureSecurity() {
    install(StatusPages) {
        exception<Throwable> { call, cause ->
            if (cause is UnauthorizedAccessException) {
                call.respondText(text = "You don't have enough permissions to access this route", status = HttpStatusCode.Forbidden)
            }
        }
    }
}
```


For complete example, Check out [tests](src/test/kotlin/io/github/omkartenkale/ktor_role_based_auth/RoleBasedAuthPluginTest.kt)

## Thanks
- [Joris Portegies Zwart](https://github.com/ximedes/ktor-authorization) - Original implementation with pipelines and phases for older ktor versions
