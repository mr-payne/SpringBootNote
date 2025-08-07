package com.plcoding.spring_boot_crash_course.controllers

import com.plcoding.spring_boot_crash_course.controllers.NoteController.NoteResponse
import com.plcoding.spring_boot_crash_course.database.model.Note
import com.plcoding.spring_boot_crash_course.database.repository.NoteRepository
import jakarta.validation.Valid
import jakarta.validation.constraints.NotBlank
import org.bson.types.ObjectId
import org.springframework.security.core.context.SecurityContextHolder
import org.springframework.web.bind.annotation.*
import java.time.Instant

// POST http://localhost:8085/notes
// GET http://localhost:8085/notes?ownerId=123
// DELETE http://localhost:8085/notes/123

@RestController
@RequestMapping("/notes")
class NoteController(
    private val repository: NoteRepository,
    private val noteRepository: NoteRepository
) {

    data class NoteRequest(
        val id: String?,
        @field:NotBlank(message = "Title can't be blank.")
        val title: String,
        val content: String,
        val color: Long,
    )

    data class NoteResponse(
        val id: String,
        val title: String,
        val content: String,
        val color: Long,
        val createdAt: Instant
    )

    @PostMapping
    fun save(
        @Valid @RequestBody body: NoteRequest
    ): NoteResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val note = repository.save(
            Note(
                id = body.id?.let { ObjectId(it) } ?: ObjectId.get(),
                title = body.title,
                content = body.content,
                color = body.color,
                createdAt = Instant.now(),
                ownerId = ObjectId(ownerId)
            )
        )

        return note.toResponse()
    }

    @PutMapping("/{id}")
    fun update(
        @PathVariable id: String,
        @Valid @RequestBody body: NoteRequest
    ): NoteResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val existingNote = repository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Note with id $id not found")
        }

        if (existingNote.ownerId.toHexString() != ownerId) {
            throw IllegalArgumentException("You are not authorized to update this note.")
        }

        // Create an updated note instance, preserving the original ID, owner, and creation time
        val updatedNote = Note(
            id = existingNote.id,
            title = body.title,
            content = body.content,
            color = body.color,
            createdAt = existingNote.createdAt,
            ownerId = existingNote.ownerId
        )

        return repository.save(updatedNote).toResponse()
    }

    @GetMapping
    fun findByOwnerId(): List<NoteResponse> {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        return repository.findByOwnerId(ObjectId(ownerId)).map {
            it.toResponse()
        }
    }

    @DeleteMapping(path = ["/{id}"])
    fun deleteById(@PathVariable id: String) {
        val note = noteRepository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Note not found")
        }
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        if(note.ownerId.toHexString() == ownerId) {
            repository.deleteById(ObjectId(id))
        }
    }

    @GetMapping(path = ["/{id}"])
    fun findById(@PathVariable id: String): NoteResponse {
        val ownerId = SecurityContextHolder.getContext().authentication.principal as String
        val note = repository.findById(ObjectId(id)).orElseThrow {
            IllegalArgumentException("Note not found")
        }

        if (note.ownerId.toHexString() != ownerId) {
            throw IllegalArgumentException("Invalid Owner Id")
        }

        return note.toResponse()
    }
}

private fun Note.toResponse(): NoteController.NoteResponse {
    return NoteResponse(
        id = id.toHexString(),
        title = title,
        content = content,
        color = color,
        createdAt = createdAt
    )
}