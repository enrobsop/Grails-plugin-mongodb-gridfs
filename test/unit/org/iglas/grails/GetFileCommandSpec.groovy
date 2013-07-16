package org.iglas.grails

import grails.plugin.spock.UnitSpec
import grails.test.mixin.*
import grails.test.mixin.web.ControllerUnitTestMixin

import org.iglas.grails.gridfs.GetFileCommand

import spock.lang.Ignore
import spock.lang.Unroll

//
// Grails 2.2.2 bug causing errors in TestMixins for validation - waiting for fix
//
@TestMixin(ControllerUnitTestMixin)
class GetFileCommandSpec extends UnitSpec{

	@Unroll
	@Ignore // See bug message above 
	def "the command should validate correctly when [#theFieldName=#theValue]"() {
		
		given: "a command"
			def command = new GetFileCommand()
			command."${theFieldName}" = theValue
		
		when: "it is validated"
			theResult = command.validate()
			
		then: "it shows the correct errors"
			command.errors.getFieldError(theFieldName)?.code == theExpectedErrorCode
		
		where:
			theFieldName	| theValue		| theExpectedErrorCode
			"filename"		| "hello.txt"	| null
			"filename"		| null			| "notblank"
			"filename"		| ""			| "notblank"
			"filename"		| " "			| "notblank"
	}
	
}
