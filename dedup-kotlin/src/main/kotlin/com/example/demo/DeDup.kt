package com.example.demo

import java.io.File
import java.io.FileInputStream
import java.security.MessageDigest

open class DeDup(val roots: Set<File>,
                 val hashAlgo: String,
                 val fileTypes: Set<String>) {

    fun execute(): Map<Boolean, List<File>> {
        val sizes = roots.map {
            it.walkTopDown().filter { it.isFile }.filter { fileTypes.contains(it.extension) }}
                .flatMap { it.asIterable() }.groupBy { it.length() }

            val hashes = sizes.values.flatMap { it.asIterable() }.groupBy { hash(it) }

            return hashes.values.map { it.subList(1, it.size) }.flatMap { it.asIterable() }.groupBy { it.exists() && it.delete() }

        }

        private fun hash(f: File): String {
            MessageDigest.getInstance(hashAlgo)?.let { digest ->
                FileInputStream(f).let { fis ->
                    fis.readBytes().forEach { digest.update(it) }
                    fis.close()
                }
                return digest.digest().joinToString("") { "%02x".format(it) }
            }
            throw RuntimeException("Something went wrong")
        }
    }

    fun main(args: Array<String>) {
        val app = DeDup(setOf(),"MD5", setOf(".jpg",".gif"));
        val results = app.execute()
        println("${results.getOrDefault(true, listOf()).size} files deleted")
    }