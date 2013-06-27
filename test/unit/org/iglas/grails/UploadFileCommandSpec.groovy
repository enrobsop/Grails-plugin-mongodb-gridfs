package org.iglas.grails

import grails.plugin.spock.UnitSpec
import grails.test.mixin.*
import grails.test.mixin.web.ControllerUnitTestMixin

import org.iglas.grails.gridfs.UploadFileCommand

import spock.lang.Ignore
import spock.lang.Unroll

//
// Grails 2.2.2 bug causing errors in TestMixins for validation - waiting for fix
//

@TestMixin(ControllerUnitTestMixin)
class UploadFileCommandSpec extends UnitSpec {
	
	def setup() {
//		mockCommandObject UploadFileCommand  // See bug info above
	}

	@Unroll
	def "the correct target filename should be given when #scenario"() {
	
		given:
			def newUploadFile = new UploadFileCommand(
				idparent: 			idparent,
				parentclass:		parentclass,
				originalFilename:	originalFilename
			) 
			
		expect:
			newUploadFile.targetFilename == expectedNewFilename
		
		where:
			scenario			| idparent	| parentclass	| originalFilename	| expectedNewFilename
			"all atts defined"	| "myId"	| "User"		| "helloWorld.txt"	| "User_myid_helloworld.txt"
			"no parent class"	| "myId"	| null			| "helloWorld.txt"	| "myid_helloworld.txt"
			
	}
	
	@Ignore // See Grails Bug above
	@Unroll
	def "the UploadFileCommand should validate correctly when #scenario"() {

		given: "a mock command object"
			def newUploadFile = new UploadFileCommand(
				idparent: 			idparent ?: null,
				parentclass:		parentclass,
				originalFilename:	originalFilename
			)
		
		when: "it is validated"
			boolean isValid = newUploadFile.validate()
			
		then: "it correctly validates"
			isValid == !errors	
			newUploadFile.hasErrors() == errors
		and: "the correct error is identified"
			newUploadFile.errors?.getFieldError(errorField)?.code == errorCode
		
		where:
			scenario				| idparent	| parentclass	| originalFilename	| errors	| errorField			| errorCode 
			"all atts defined"		| "myId"	| "User"		| "helloWorld.txt"	| false		| null					| null
			"no parent class"		| "myId"	| null			| "helloWorld.txt"	| false		| null					| null
			"no idparent"			| null		| "User"		| "helloWorld.txt"	| true		| "idparent"			| "nullable"
			"no originalFilename"	| "myId"	| "User"		| "helloWorld.txt"	| true		| "originalFilename"	| "nullable"

	}
	
}
