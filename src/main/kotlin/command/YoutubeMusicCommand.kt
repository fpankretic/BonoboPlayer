package command

class YoutubeMusicCommand : PlayCommand() {

    override fun help(): String {
        return "Searches for a song on YouTube music."
    }

    override fun searchProvider(): String {
        return "ytmsearch"
    }
}