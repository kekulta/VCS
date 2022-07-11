import kotlin.random.Random
import java.io.File
import java.nio.ByteBuffer
import java.security.MessageDigest

class VCS constructor(val args: Array<String>) {
    private val vcsDir = File("vcs")
    private val config = File("vcs/config.txt")
    private val index = File("vcs/index.txt")
    private val commits = File("vcs/commits")
    private val log = File("vcs/log.txt")
    private val sha = MessageDigest.getInstance("SHA-256")

    init {
        if (!vcsDir.exists() || !vcsDir.isDirectory) vcsDir.mkdir()
        if (!config.exists()) config.writeText("")
        if (!index.exists()) index.writeText("")
        if (!commits.exists()) commits.mkdir()
        if (!log.exists()) log.writeText("")
    }

    fun help() = println(
        "These are SVCS commands:\n" +
                "config     Get and set a username.\n" +
                "add        Add a file to the index.\n" +
                "log        Show commit logs.\n" +
                "commit     Save changes.\n" +
                "checkout   Restore a file."
    )

    fun config() {
        val text = this.config.readText()
        if (text == "" && this.args.size == 1) {
            println("Please, tell me who you are.")
        } else if (this.args.size > 1) {
            println("The username is ${this.args[1]}.")
            this.config.writeText(this.args[1])
        } else {
            println("The username is ${text}.")
        }
    }

    fun add() {
        if (this.args.size > 1) {
            val file = File(this.args[1])
            if (!file.exists()) {
                println("Can't find '${file.name}'.")
                return
            }
        }

        val tracks = this.index.readText().split("\\s+".toRegex()).filter { it != "" }
        if (this.args.size == 1 && tracks.isEmpty()) {
            println("Add a file to the index.")
        } else if (this.args.size > 1) {
            index.appendText("${this.args[1]} ")
            println("The file '${this.args[1]}' is tracked.")
        } else {
            println("Tracked files:")
            for (track in tracks) println(track)
        }
    }

    fun log() {
        val coms = log.readText().split("|||").filter { it != "" }.map { it.split("//") }
        if (coms.isEmpty()) {
            println("No commits yet.")
            return
        }
        for (it in coms.lastIndex downTo 0) {
            println("commit ${coms[it][0]}")
            println("Author: ${coms[it][1]}")
            println(coms[it][2])

        }
    }


    fun commit() {
        fun ByteArray.toHex(): String = joinToString(separator = "") { eachByte -> "%02x".format(eachByte) }
        fun littleEndianConversion(bytes: ByteArray): Int {
            return ByteBuffer.wrap(bytes).getInt()
        }

        fun getLastCommit(): String {
            val coms = log.readText().split("/n").filter { it != "" }.map { it.split("//") }
            if (coms.isEmpty()) return ""
            return coms.last().first()
        }

        fun changeCheck(tracks: List<String>): Boolean {
            val lastCommitID = getLastCommit()
            if (lastCommitID == "") return true
            var newHash = 0
            for (it in tracks) {
                val file = File(it)
                newHash += littleEndianConversion(sha.digest(file.readBytes()))
            }
            var oldHash = 0
            val lastCommitFiles = File("vcs/commits/${lastCommitID}").list()
            for (it in lastCommitFiles) {
                val file = File("vcs/commits/${lastCommitID}/$it")
                oldHash += littleEndianConversion(sha.digest(file.readBytes()))
            }
            return newHash != oldHash
        }

        if (args.size == 1) {
            println("Message was not passed.")
            return
        }


        val tracks = this.index.readText().split("\\s+".toRegex()).filter { it != "" }
        if (!changeCheck(tracks)) {
            println("Nothing to commit.")
            return
        }
        val newCommit = File("vcs/commits/${sha.digest(Random.nextInt().toString().toByteArray()).toHex()}")
        newCommit.mkdir()
        for (track in tracks) {
            val new = File("vcs/commits/${newCommit.name}/$track")
            File(track).copyTo(new)
        }
        this.log.appendText("${newCommit.name}//${this.config.readText()}//${this.args[1]}|||")
        println("Changes are committed.")
    }

    fun checkout() {
        if (args.size == 1) {
            println("Commit id was not passed.")
            return
        }
        val commitsIDs = commits.list()
        if (commitsIDs != null && args[1] !in commitsIDs) {
            println("Commit does not exist.")
            return
        }
        val tracks = this.index.readText().split("\\s+".toRegex()).filter { it != "" }
        for (track in tracks) {
            File(track).delete()
            File("vcs/commits/${args[1]}/$track").copyTo(File(track))
        }
        println("Switched to commit ${args[1]}.")
    }
}

fun help() = println(
    "These are SVCS commands:\n" +
            "config     Get and set a username.\n" +
            "add        Add a file to the index.\n" +
            "log        Show commit logs.\n" +
            "commit     Save changes.\n" +
            "checkout   Restore a file."
)

fun main(args: Array<String>) {
    val vcs = VCS(args)

    if (args.isEmpty()) {
        help()
        return
    }

    when (args[0]) {
        "--help" -> vcs.help()
        "config" -> vcs.config()
        "add" -> vcs.add()
        "log" -> vcs.log()
        "commit" -> vcs.commit()
        "checkout" -> vcs.checkout()
        else -> println(
            "'${args[0]}' is not a SVCS command."
        )

    }
}

