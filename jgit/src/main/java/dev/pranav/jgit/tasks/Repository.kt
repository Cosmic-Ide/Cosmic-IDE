/*
 * This file is part of Cosmic IDE.
 * Cosmic IDE is a free software: you can redistribute it and/or modify it under the terms of the GNU General Public License as published by the Free Software Foundation, either version 3 of the License, or (at your option) any later version.
 * Cosmic IDE is distributed in the hope that it will be useful, but WITHOUT ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for more details.
 * You should have received a copy of the GNU General Public License along with Foobar. If not, see <https://www.gnu.org/licenses/>.
 */

package dev.pranav.jgit.tasks

import com.github.syari.kgit.KGit
import dev.pranav.jgit.api.Author
import org.eclipse.jgit.lib.TextProgressMonitor
import org.eclipse.jgit.pgm.Main
import org.eclipse.jgit.revwalk.RevCommit
import org.eclipse.jgit.revwalk.RevWalk
import org.eclipse.jgit.transport.HttpTransport
import org.eclipse.jgit.transport.UsernamePasswordCredentialsProvider
import java.io.File
import java.io.PrintStream
import java.io.Writer

class Repository(val git: KGit) {

    fun getBranches(): List<String> {
        return git.branchList().map {
            it.toString()
        }
    }

    fun isClean(): Boolean {
        return git.status().isClean
    }

    fun getCommitList(): List<RevCommit> {
        val localBranch = git.repository.branch
        val revWalk = RevWalk(git.repository)
        val latestCommit = revWalk.parseCommit(git.repository.resolve(localBranch))
        revWalk.close()
        return if (latestCommit != null) {
            git.log().toList()
        } else {
            println("No commits found")
            emptyList()
        }
    }

    fun addAll() {
        add(".")
    }

    fun commit(author: Author, commit: String) {
        git.commit {
            setAuthor(author.name, author.email)
            message = commit
        }
    }

    fun add(directory: String) {
        git.add {
            addFilepattern(directory)
        }
    }

    fun pull(writer: Writer, isRebase: Boolean = false, creds: Credentials) {
        git.fetch {
            setProgressMonitor(TextProgressMonitor(writer))
            setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    creds.username, creds.password
                )
            )
        }

        git.pull {
            setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    creds.username, creds.password
                )
            )
            setProgressMonitor(TextProgressMonitor(writer))
            setRebase(isRebase)
        }
    }

    fun push(writer: Writer, creds: Credentials) {
        git.push {
            setCredentialsProvider(
                UsernamePasswordCredentialsProvider(
                    creds.username, creds.password
                )
            )
            progressMonitor = TextProgressMonitor(writer)
        }
    }
}

fun File.toRepository(): Repository {
    val git = KGit.open(this)
    git.checkout {
        setName("main")
    }
    return Repository(git)
}

fun File.cloneRepository(url: String, writer: Writer, creds: Credentials): Repository {
    val file = this
    println("Cloning repository from $url to ${file.absolutePath}")
    val git = KGit.cloneRepository {
        setURI(url)
        setDirectory(file)
        setProgressMonitor(TextProgressMonitor(writer))
        setTransportConfigCallback { transport ->
            if (transport is HttpTransport) {
                transport.credentialsProvider = UsernamePasswordCredentialsProvider(
                    creds.username, creds.password
                )
            }
        }
    }
    println("Cloned repository from $url to ${file.absolutePath}")
    return Repository(git)
}

fun File.createRepository(author: Author): Repository {
    val file = this
    println("Creating repository at ${file.absolutePath}")
    val git = KGit.init {
        setDirectory(file)
        setGitDir(file.resolve(".git"))
        setBare(false)
    }
    println("Created repository at ${file.absolutePath}")
    println("Creating gitignore")
    file.resolve(".gitignore").writeText(
        "build/\n"
    )
    println("Creating initial commit")
    git.add {
        addFilepattern(".")
    }
    git.commit {
        setAuthor(author.name, author.email)
        message = "Initial commit"
    }
    println("Created initial commit")
    println("Creating main branch")
    git.branchCreate {
        setName("main")
    }
    git.checkout {
        setName("main")
    }
    println("Setting fetch refs")
    git.repository.config.apply {
        setString("remote", "origin", "fetch", "+refs/heads/*:refs/remotes/origin/*")
        setString("remote", "origin", "username", author.name)
        setString("remote", "origin", "email", author.email)
        save()
    }
    println("Created main branch")
    return Repository(git)
}

fun File.execGit(args: MutableList<String>, writer: PrintStream) {
    System.setErr(writer)
    System.setOut(writer)
    args.add(0, "--git-dir")
    args.add(1, this.absolutePath)
    Main.main(args.toTypedArray())
}

