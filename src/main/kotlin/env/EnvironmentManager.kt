package env

import io.github.cdimascio.dotenv.Dotenv

class EnvironmentManager {

    companion object {

        private val dotenv = Dotenv.load()

        @JvmStatic
        fun get(value: EnvironmentValue): String {
            return dotenv.get(value.toString(), "")
        }

    }

}