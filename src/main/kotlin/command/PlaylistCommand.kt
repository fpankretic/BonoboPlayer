package command

object PlaylistCommand : PlayCommandBase() {
    override fun help(): String {
        return "Plays a playlist."
    }

    override fun getPlaylistMode(): Boolean {
        return true
    }
}