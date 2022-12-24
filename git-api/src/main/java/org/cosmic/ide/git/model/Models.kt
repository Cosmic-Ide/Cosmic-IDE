package org.cosmic.ide.git.model

import org.eclipse.jgit.api.Git
import org.eclipse.jgit.lib.PersonIdent

data class Author(val name: String, val email: String)
fun Author.of() = PersonIdent(name, email)

data class Gitter(val git: Git)

sealed class Result
object Success : Result()
object Failure : Result()
