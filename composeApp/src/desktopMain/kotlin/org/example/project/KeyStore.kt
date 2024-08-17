package org.example.project

data class KeyStore(
    val path: String = "",
    val keyStorePassword: String = "",
    val alias: String = "",
    val aliasPassword: String = ""
) {
    fun valid() = path.isNotEmpty() && keyStorePassword.isNotEmpty() && alias.isNotEmpty() && aliasPassword.isNotEmpty()

    fun showInvalidMessage(): String {
        var message = "your "
        if (path.isEmpty()) {
            message += "path, "
        }
        if (keyStorePassword.isEmpty()) {
            message += "keyStorePassword, "
        }
        if (alias.isEmpty()) {
            message += "keyStoreAlias, "
        }
        if (aliasPassword.isEmpty()) {
            message += "keyStoreAliasPassword, "
        }
        message += " is/are empty"
        return message
    }
}