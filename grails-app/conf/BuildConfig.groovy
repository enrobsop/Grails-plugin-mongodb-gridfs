grails.project.work.dir = 'target'
grails.project.class.dir = 'target/classes'
grails.project.test.class.dir = 'target/test-classes'
grails.project.test.reports.dir = 'target/test-reports'
grails.project.dependency.resolution = {

	inherits 'global'
	log 'warn'

	repositories {
		grailsCentral()
		mavenLocal()
		mavenCentral()
		mavenRepo 'http://repo.grails.org/grails/libs-releases'
		mavenRepo 'https://oss.sonatype.org/content/groups/public'
	}

	dependencies {
		test "org.spockframework:spock-grails-support:0.7-groovy-2.0"
	}

	plugins {
		
		compile ":mongodb:1.2.0"
		compile ":burning-image:0.5.1"
		
		test(":spock:0.7") {
			exclude "spock-grails-support"
		}
		
	}

}
