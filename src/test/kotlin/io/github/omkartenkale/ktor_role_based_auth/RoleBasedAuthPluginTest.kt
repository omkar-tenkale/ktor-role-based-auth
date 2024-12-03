package io.github.omkartenkale.ktor_role_based_auth

import io.ktor.client.request.basicAuth
import io.ktor.client.request.get
import io.ktor.http.HttpStatusCode
import io.ktor.server.application.call
import io.ktor.server.application.install
import io.ktor.server.auth.UserIdPrincipal
import io.ktor.server.auth.authenticate
import io.ktor.server.auth.authentication
import io.ktor.server.auth.basic
import io.ktor.server.plugins.statuspages.StatusPages
import io.ktor.server.response.respond
import io.ktor.server.response.respondText
import io.ktor.server.routing.get
import io.ktor.server.routing.route
import io.ktor.server.testing.ApplicationTestBuilder
import io.ktor.server.testing.testApplication
import org.junit.jupiter.api.Test
import org.junit.jupiter.api.Assertions.*


internal class RoleBasedAuthPluginTest {

    object Roles {
        const val ADMIN = "ADMIN"
        const val SUPER_ADMIN = "SUPER_ADMIN"
    }

    private fun withServer(
        throwErrorOnUnauthorizedResponse: Boolean = false,
        block: suspend ApplicationTestBuilder.() -> Unit
    ) {
        val usersWithRoles = mapOf("Leon" to setOf(), "Amy" to setOf(Roles.ADMIN), "Jay" to setOf(Roles.SUPER_ADMIN))
        testApplication {
            application {
                if (throwErrorOnUnauthorizedResponse) {
                    install(StatusPages) {
                        exception<Throwable> { call, cause ->
                            call.respond(HttpStatusCode.InternalServerError)
                        }
                    }
                }
                authentication {
                    basic {
                        validate { credentials ->
                            if (usersWithRoles.containsKey(credentials.name) && credentials.password == "1234") {
                                UserIdPrincipal(credentials.name)
                            } else {
                                null
                            }
                        }
                    }
                    roleBased<UserIdPrincipal> {
                        extractRoles { principal -> usersWithRoles[principal.name]!! }
                        this.throwErrorOnUnauthorizedResponse = throwErrorOnUnauthorizedResponse
                    }
                }
            }

            routing {
                route("/") {

                    get {
                        call.respondText("Welcome!")
                    }

                    authenticate {

                        route("/profile") {
                            get {
                                call.respondText("Joined: 2 years ago")
                            }
                        }

                        route("/dashboard") {
                            withAnyRole(Roles.ADMIN, Roles.SUPER_ADMIN) {
                                get {
                                    call.respondText("Total users: 2443")
                                }
                            }
                        }

                        route("/system-stats") {
                            withRole(Roles.SUPER_ADMIN) {
                                get {
                                    call.respondText("CPU: 34%")
                                }
                            }
                        }
                    }
                }
            }
            block()
        }
    }

    @Test
    fun `No auth required for root route`() {
        withServer {
            with(client.get("/")) {
                assertEquals(HttpStatusCode.OK, status)
            }
        }
    }

    @Test
    fun `Allow only authenticated users`() {
        withServer {
            with(client.get("/profile")) {
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
            with(client.get("/profile") {
                basicAuth("Leon", "0000")
            }) {
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
            with(client.get("/profile") {
                basicAuth("Leon", "1234")
            }) {
                assertEquals(HttpStatusCode.OK, status)
            }
        }
    }

    @Test
    fun `Allow only users with admin role`() {
        withServer {
            with(client.get("/dashboard")) {
                assertEquals(status, HttpStatusCode.Unauthorized)
            }
            with(client.get("/dashboard") {
                basicAuth("Leon", "1234")
            }) {
                assertEquals(HttpStatusCode.Forbidden, status)
            }

            with(client.get("/dashboard") {
                basicAuth("Amy", "1234")
            }) {
                assertEquals(HttpStatusCode.OK, status)
            }
        }
    }

    @Test
    fun `Allow only users with superadmin role`() {
        withServer {
            with(client.get("/system-stats")) {
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
            with(client.get("/system-stats") {
                basicAuth("Amy", "1234")
            }) {
                assertEquals(HttpStatusCode.Forbidden, status)
            }
            with(client.get("/system-stats") {
                basicAuth("Jay", "1234")
            }) {
                assertEquals(HttpStatusCode.OK, status)
            }
        }
    }

    @Test
    fun `Test throwErrorOnUnauthorizedResponse`() {
        withServer(true) {
            with(client.get("/system-stats")) {
                assertEquals(HttpStatusCode.Unauthorized, status)
            }
            with(client.get("/system-stats") {
                basicAuth("Amy", "1234")
            }) {
                assertEquals(HttpStatusCode.InternalServerError, status)
            }
        }
    }
}
