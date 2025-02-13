package command

object PlayCommand : PlayCommandBase() {
    override fun help(): String {
        return "Plays a song."
    }
}