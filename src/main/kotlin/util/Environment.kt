package util

import io.github.cdimascio.dotenv.Dotenv

class EnvironmentManager {
    companion object {
        private val dotenv = Dotenv.load()

        fun valueOf(value: EnvironmentValue): String {
            return dotenv.get(value.toString(), "")
        }
    }
}

enum class EnvironmentValue {
    DISCORD_API_TOKEN,
    IPV6_CIDR,
    IPV6_ENABLED,
    PREFIX,
    PO_TOKEN,
    VISITOR_DATA,
    REFRESH_TOKEN,
    PORT,
    SPOTIFY_CLIENT_ID,
    SPOTIFY_CLIENT_SECRET,
    FILTERED_GUILDS,
    ALLOWED_CHANNELS,
    ADD_DEFAULT_ROLE_GUILDS,
    DEFAULT_ROLE_ID,
    JOIN_MUTE_IDS,
    JOIN_MUTE_GUILDS
}