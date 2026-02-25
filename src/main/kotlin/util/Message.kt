package util

enum class Message(val message: String) {
    QUEUE_EMPTY("Queue is empty."),
    QUEUE_SHUFFLED("Queue shuffled."),
    NO_SONGS("No songs currently playing."),
    INACTIVITY("Left the voice channel due to inactivity."),
    WRONG_GUILD("You must be in the same guild as me to use this command."),
    INVALID_ARGUMENTS("Invalid arguments provided."),
    NOT_IN_VOICE_CHANNEL("You must be in a voice channel to use this command."),
    SONG_MOVED("Song moved."),
    DRAGANA_ENTERED("Dragana has entered the room!")
}