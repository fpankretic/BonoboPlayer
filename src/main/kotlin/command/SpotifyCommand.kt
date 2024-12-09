package command

class SpotifyCommand : PlayCommand() {

    override fun help(): String {
        return "Searches for a song on Spotify."
    }

    override fun searchProvider(): String {
        return "spsearch"
    }
}