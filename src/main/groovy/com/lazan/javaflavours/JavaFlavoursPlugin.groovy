package com.lazan.javaflavours

import org.gradle.api.*
import org.gradle.api.artifacts.*
import org.gradle.api.tasks.*
import org.gradle.api.tasks.testing.*
import org.gradle.api.plugins.*
import org.gradle.api.tasks.bundling.*

class JavaFlavoursPlugin implements Plugin<Project> {
	void apply(Project project) {
		project.with {
			apply plugin: 'java'
			JavaFlavoursExtension model = extensions.create('javaFlavours', JavaFlavoursExtension)
			afterEvaluate {
				model.flavours.each { String flavour ->
					SourceSet sourceSet = sourceSets.create(flavour)
					sourceSet.compileClasspath += sourceSets.main.output
					sourceSet.runtimeClasspath += sourceSets.main.output
					sourceSet.java.srcDir "src/$flavour/java"
					sourceSet.resources.srcDir "src/$flavour/resources"

					SourceSet testSourceSet = sourceSets.create("${flavour}Test")
					testSourceSet.compileClasspath += (sourceSets.main.output + sourceSets.test.output + sourceSet.output)
					testSourceSet.runtimeClasspath += (sourceSets.main.output + sourceSets.test.output + sourceSet.output)
					testSourceSet.java.srcDir "src/${flavour}Test/java"
					testSourceSet.resources.srcDir "src/${flavour}Test/resources"

					['compile', 'compileOnly', 'compileClasspath', 'runtime'].each { String suffix ->

						// these configurations were magically created when we added the source sets above
						Configuration config = configurations.getByName("${flavour}${suffix.capitalize()}")
						Configuration testConfig = configurations.getByName("${flavour}Test${suffix.capitalize()}")

						config.extendsFrom(configurations.getByName(suffix))
						testConfig.extendsFrom(configurations.getByName("test${suffix.capitalize()}"))
						testConfig.extendsFrom(config)
					}

					Task testTask = tasks.create(name: "${flavour}Test", type: Test) {
						group = JavaBasePlugin.VERIFICATION_GROUP
						description = "Runs the tests for ${flavour}."
						testClassesDir = testSourceSet.output.classesDir
						classpath = testSourceSet.runtimeClasspath
					}
					check.dependsOn testTask

					Task jarTask = tasks.create(name: "${flavour}Jar", type: Jar) {
						group = BasePlugin.BUILD_GROUP
						description = "Assembles a jar archive containing the $flavour classes combined with the main classes."
						from sourceSet.output
						from sourceSets.main.output
						classifier flavour
					}

					artifacts {
						   archives jarTask
					}
					assemble.dependsOn jarTask
				}
			}
		}
	}

	static class JavaFlavoursExtension {
		List<String> flavours = []

		void flavour(String flavour) {
			flavours << flavour
		}
	}
}