package org.jetbrains.kotlin.gradle.plugin.test

import org.gradle.testkit.runner.BuildResult
import org.gradle.testkit.runner.TaskOutcome
import org.jetbrains.kotlin.konan.target.KonanTarget
import org.jetbrains.kotlin.konan.target.TargetManager
import spock.lang.Requires
import spock.lang.Unroll

class TaskSpecification extends BaseKonanSpecification {

    def 'Configs should allow user to add dependencies to them'() {
        when:
        def project = KonanProject.createWithInterop(projectDirectory, ArtifactType.LIBRARY)
        project.buildFile.append("""
            task beforeInterop(type: DefaultTask) { doLast { println("Before Interop") } }
            task beforeCompilation(type: DefaultTask) { doLast { println("Before compilation") } }
        """.stripIndent())
        project.addSetting(KonanProject.DEFAULT_INTEROP_NAME,"dependsOn", "beforeInterop")
        project.addSetting("dependsOn", "beforeCompilation")
        def result = project.createRunner().withArguments('build').build()

        then:
        def beforeInterop = result.task(":beforeInterop")
        beforeInterop != null && beforeInterop.outcome == TaskOutcome.SUCCESS
        def beforeCompilation = result.task(":beforeCompilation")
        beforeCompilation != null && beforeCompilation.outcome == TaskOutcome.SUCCESS
    }

    def 'Compiler should print time measurements if measureTime flag is set'() {
        when:
        def project = KonanProject.create(projectDirectory, ArtifactType.LIBRARY)
        project.addSetting("measureTime", "true")
        def result = project.createRunner().withArguments('build').build()

        then:
        result.output.findAll(~/FRONTEND:\s+\d+\s+msec/).size() == 1
        result.output.findAll(~/BACKEND:\s+\d+\s+msec/).size() == 1
    }

    @Unroll('Plugin should support #option option for cinterop')
    def 'Plugin should support includeDir option for cinterop'() {
        expect:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.addCompilerArtifact("interopLib", "headers=foo.h\n$headerFilter", ArtifactType.INTEROP)
            it.generateSrcFile(it.projectPath, "foo.h", "#include <bar.h>")
            def fooDir = it.projectPath.resolve("foo")
            it.generateSrcFile(fooDir, "bar.h", "const int foo = 5;")
            it.addSetting("interopLib", option, fooDir.toFile())
            it.addSetting("interopLib", option, it.projectDir)
        }
        project.createRunner().withArguments("build").build()

        where:
        option                         | headerFilter
        "includeDirs.headerFilterOnly" | "headerFilter=foo.h bar.h"
        "includeDirs.allHeaders"       | ""
        "includeDirs"                  | ""
    }

    @Requires({ TargetManager.host == KonanTarget.MACBOOK })
    def 'Plugin should create framework tasks only for Apple targets'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.buildFile.append("""
                konan.targets = ['wasm32', 'macbook', 'iphone', 'iphone_sim']
                
                konanArtifacts {
                    framework('foo')
                }
            """.stripIndent())
        }
        def result = project.createRunner().withArguments('tasks', '--all').build()

        then:
        !compilationTaskExists(result,'foo', 'wasm32')
        compilationTaskExists (result,'foo', 'macbook')
        compilationTaskExists (result,'foo', 'iphone')
        compilationTaskExists (result,'foo', 'iphone_sim')
    }

    def 'Plugin should support different targets for different artifacts'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory, ['host']) { KonanProject it ->
            it.buildFile.append("""
                konanArtifacts {
                    program('defaultTarget')
                    program('customTarget', targets: ['wasm32'])
                    program('customTargets', targets: ['host', 'wasm32'])
                }
            """.stripIndent())
        }
        def result = project.createRunner().withArguments('tasks', '--all').build()
        def hostName = TargetManager.getHostName()

        then:
        compilationTaskExists (result, 'defaultTarget', hostName)
        !compilationTaskExists(result, 'defaultTarget', 'wasm32')
        !compilationTaskExists(result, 'customTarget', hostName)
        compilationTaskExists (result, 'customTarget', 'wasm32')
        compilationTaskExists (result, 'customTargets', hostName)
        compilationTaskExists (result, 'customTargets', 'wasm32')
    }

    def 'Plugin should not create dynamic task for wasm'() {
        when:
        def project = KonanProject.createEmpty(projectDirectory) { KonanProject it ->
            it.buildFile.append("""
                konan.targets = ['wasm32']
                
                konanArtifacts {
                    dynamic('foo')
                }
            """.stripIndent())
        }
        def result = project.createRunner().withArguments('tasks', '--all').build()

        then:
        !compilationTaskExists(result, 'foo', 'wasm32')
    }


    boolean taskExists(BuildResult result, String taskName) {
        def taskNameForSearch = taskName.startsWith(':') ? taskName.substring(1) : taskName
        return result.output.contains(taskNameForSearch)
    }

    boolean compilationTaskExists(BuildResult result, String artifactName, String targetName) {
        return taskExists(result, KonanProject.compilationTask(artifactName, targetName))
    }

    BuildResult failOnPropertyAccess(KonanProject project, String property) {
         project.buildFile.append("""
            task testTask(type: DefaultTask) {
                doLast {
                    println(${project.defaultInteropConfig()}.$property)
                }
            }
        """.stripIndent())
        return project.createRunner().withArguments("testTask").buildAndFail()
    }

    BuildResult failOnTaskAccess(KonanProject project, String task) {
        project.buildFile.append("""
            task testTask(type: DefaultTask) {
                dependsOn $task
            }
        """.stripIndent())
        return project.createRunner().withArguments("testTask").buildAndFail()
    }
}
