package secret

import io.github.cdimascio.dotenv.Dotenv

class CredentialManager {

    companion object {

        private val dotenv = Dotenv.load()

        @JvmStatic
        fun get(credential: Credential): String {
            return dotenv[credential.toString()]
        }

    }

}