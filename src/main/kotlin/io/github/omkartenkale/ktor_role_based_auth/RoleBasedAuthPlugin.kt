package io.github.omkartenkale.ktor_role_based_auth

import io.ktor.http.*
import io.ktor.server.application.*
import io.ktor.server.auth.*
import io.ktor.server.request.*
import io.ktor.server.response.*
import io.ktor.server.routing.*
import io.ktor.util.pipeline.*

typealias Role = String

class RoleBasedAuthConfiguration(
    var any: Set<Role>? = null,
    var all: Set<Role>? = null,
    var none: Set<Role>? = null,
)

fun Route.withRole(role: Role, build: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit) =
    withAnyRole(setOf(role), build)

fun Route.withAllRoles(roles: Set<Role>, build: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit) {
    install(RoleBasedAuthPlugin) {
        all = roles.toSet()
    }
    handle { build() }
}

fun Route.withoutRoles(roles: Set<Role>, build: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit) {
    install(RoleBasedAuthPlugin) {
        none = roles.toSet()
    }
    handle { build() }
}

fun Route.withAnyRole(roles: Set<Role>, build: suspend PipelineContext<Unit, ApplicationCall>.() -> Unit) {
    install(RoleBasedAuthPlugin) {
        any = roles.toSet()
    }
    handle { build() }
}

val RoleBasedAuthPlugin =
    createRouteScopedPlugin(name = "RoleBasedAuthorization", createConfiguration = ::RoleBasedAuthConfiguration) {
        with(pluginConfig) {
            on(AuthenticationChecked) { call ->
                val principal = call.principal<Principal>() ?: error("Missing principal")
                val roles = roleBasedAuthPluginConfiguration?.roleExtractor?.invoke(principal)
                    ?: error("RoleBasedAuthPlugin is not initialized,You can initialize it by calling 'installRoleBasedAuthPlugin()'")
                val denyReasons = mutableListOf<String>()
                all?.let {
                    val missing = it - roles
                    if (missing.isNotEmpty()) {
                        denyReasons += "Principal $principal lacks required role(s) ${missing.joinToString(" and ")}"
                    }
                }
                any?.let {
                    if (it.none { it in roles }) {
                        denyReasons += "Principal $principal has none of the sufficient role(s) ${
                            it.joinToString(
                                " or "
                            )
                        }"
                    }
                }
                none?.let {
                    if (it.any { it in roles }) {
                        denyReasons += "Principal $principal has forbidden role(s) ${
                            (it.intersect(roles)).joinToString(
                                " and "
                            )
                        }"
                    }
                }
                if (denyReasons.isNotEmpty()) {
                    val message = denyReasons.joinToString(". ")
                    println("Authorization failed for ${call.request.path()}. $message")
                    call.respond(HttpStatusCode.Forbidden)
                }
            }
        }
    }

private var roleBasedAuthPluginConfiguration: RoleBasedAuthPluginConfiguration? = null
fun Application.installRoleBasedAuthPlugin(configuration: RoleBasedAuthPluginConfiguration.() -> Unit) {
    roleBasedAuthPluginConfiguration = RoleBasedAuthPluginConfiguration().apply { configuration() }
}


class RoleBasedAuthPluginConfiguration {
    var roleExtractor: ((Principal) -> Set<Role>)? = null
        private set

    fun extractRoles(extractor: (Principal) -> Set<Role>) {
        roleExtractor = extractor
    }
}