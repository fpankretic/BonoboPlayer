package command

object YoutubeCommand : PlayCommandBase() {
    override fun help(): String {
        return "Searches for a song on YouTube."
    }

    override fun searchProvider(): String {
        return "ytsearch"
    }
}