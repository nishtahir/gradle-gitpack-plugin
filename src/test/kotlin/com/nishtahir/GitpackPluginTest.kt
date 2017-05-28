package com.nishtahir

import org.gradle.testfixtures.ProjectBuilder
import org.junit.Assert.assertTrue
import org.junit.Test

class GitpackPluginTest {

    @Test
    fun testFetchTaskIsAppliedCorrectly() {
        val projectBuilder = ProjectBuilder.builder().build()
        projectBuilder.pluginManager.apply {
            apply("java")
            apply("com.nishtahir.gradle-gitpack-plugin")
        }
        assertTrue(projectBuilder.tasks.filter { task ->
            task.name == "fetch"
        }.isNotEmpty())
    }
}