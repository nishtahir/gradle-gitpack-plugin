package com.nishtahir

import org.gradle.testkit.runner.BuildResult;
import org.gradle.testkit.runner.GradleRunner;
import org.junit.Before;
import org.junit.Rule;
import org.junit.Test;
import org.junit.rules.TemporaryFolder;
import java.io.File


open class GitpackPluginTest {

    @get:Rule
    val testProjectDir = TemporaryFolder()

    lateinit var buildFile: File

    @Before
    fun setup() {
        buildFile = testProjectDir.newFile("build.gradle")
    }


    @Test
    fun testFetchTaskIsAppliedCorrectly() {
        buildFile.writeText("""
            plugins {
                id 'com.nishtahir.gradle-gitpack-plugin'
                id 'java'
            }

            dependencies {
                git 'com.github.nishtahir:ALang:SNAPSHOT'
            }""".trimIndent())

        val result: BuildResult = GradleRunner.create()
                .withProjectDir(testProjectDir.root)
                .withPluginClasspath()
                .build()

        println(result.output)

    }
}