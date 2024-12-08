package command

class YoutubeCommand : PlayCommand() {

    override fun help(): String {
        return "Searches for a song on YouTube."
    }

    override fun searchProvider(): String {
        return "ytsearch"
    }
}