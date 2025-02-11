package command

object YoutubeMusicCommand : PlayCommandBase() {
    override fun help(): String {
        return "Searches for a song on YouTube music."
    }

    override fun searchProvider(): String {
        return "ytmsearch"
    }
}