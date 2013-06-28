package org.iglas.grails
import grails.plugin.spock.UnitSpec
import grails.test.mixin.*
import grails.test.mixin.web.ControllerUnitTestMixin

import org.iglas.grails.gridfs.GridfsService
import org.iglas.grails.gridfs.UploadFileCommand
import org.springframework.web.multipart.MultipartFile

import spock.lang.Ignore
import spock.lang.Shared
import spock.lang.Unroll

import com.mongodb.gridfs.GridFSFile

//
// Grails 2.2.2 bug causing errors in TestMixins for validation - waiting for fix
//

@TestMixin(ControllerUnitTestMixin)
class UploadFileCommandSpec extends UnitSpec {
	
	@Shared def gridfsService
	
	def setup() {
//		mockCommandObject UploadFileCommand  // See bug info above
		gridfsService = Mock(GridfsService)
	}

	@Unroll
	def "the correct target filename should be given when #scenario"() {
	
		given: "a mock file upload"
			def multipartFile = Mock(MultipartFile)
			multipartFile.getOriginalFilename() >> originalFilename
		and: "a command object"
			def uploadCommand = new UploadFileCommand(
				idparent: 			idparent,
				parentclass:		parentclass,
				file:				multipartFile
			) 
			
		expect:
			uploadCommand.targetFilename == expectedNewFilename
		
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
	
	def "should populate a MongoDB metadata object correctly"() {
		
		given: "some attributes" 
			def theFileExtension	= "JPG"
			def theOriginalFilename	= "MyFile.${theFileExtension}"
			def theIdParent 		= "myId"
			def theParentClass		= "User"
			def theText				= "Hello World!"
			def theAccess			= "private"
		and: "an uploaded file"
			def multipartFile = Mock(MultipartFile)
			multipartFile.getOriginalFilename() >> theOriginalFilename
		and: "a command object"
			def uploadCommand = new UploadFileCommand(
				idparent: 			theIdParent,
				parentclass:		theParentClass,
				file:				multipartFile,
				text:				theText,
				accesspolitic:		theAccess
			)
		
		when: "getting the MongoDB metadata"
			def metadata = uploadCommand.getMetadata()
		
		then: "the metadata exists"
			metadata != null
		and: "it has all of the correct values"
		 	metadata.get("idparent")			== theIdParent
			metadata.get("originalFilename") 	== theOriginalFilename.toLowerCase()
			metadata.get("fileExtension") 		== theFileExtension.toLowerCase()
			metadata.get("parentclass") 		== theParentClass
			metadata.get("text") 				== theText
			metadata.get("access") 				== theAccess
			
	}

	def "should use correct default values in a MongoDB metadata"() {

		given: "some attributes"
			def theFileExtension	= "JPG"
			def theOriginalFilename	= "MyFile.${theFileExtension}"
			def theIdParent 		= "myId"
		and: "an uploaded file"
			def multipartFile = Mock(MultipartFile)
			multipartFile.getOriginalFilename() >> theOriginalFilename
		and: "a command object"
			def uploadCommand = new UploadFileCommand(
				idparent: 			theIdParent,
				file:				multipartFile
			)
				
		when: "getting the MongoDB metadata"
			def metadata = uploadCommand.getMetadata()
		
		then: "the metadata exists"
			metadata != null
		and: "it has all of the correct values"
			metadata.get("idparent")			== theIdParent
			metadata.get("originalFilename") 	== theOriginalFilename.toLowerCase()
			metadata.get("fileExtension") 		== theFileExtension.toLowerCase()
			metadata.get("access") 				== "public"

			metadata.keySet().contains("parentclass")	== false
			metadata.keySet().contains("text")			== false

	}

	@Unroll	
	def "should be able to check if target file exists=#expectedResult"() {
		
		given: "an uploaded file"
			def multipartFile = Mock(MultipartFile)
			multipartFile.getOriginalFilename() >> "myFile.jpg"
		and: "a command object"
			def myConfig = []
			def uploadCommand = new UploadFileCommand(
				idparent:		"myId",
				file:			multipartFile,
				gridfsService:	gridfsService,
				config:			myConfig
			)
			
		when: "checking whether a target file exists"
			def result = uploadCommand.targetFileExists()
		
		then: "the check is correctly delegated"
			1 * gridfsService.exists(myConfig, uploadCommand.targetFilename) >> expectedResult
		and: "the correct result is returned"
			result == expectedResult
		
		where:
			expectedResult << [true, false]
	}
	
	@Unroll
	def "should be able to add file to GridFS"() {
		
		given: "an uploaded file"
			def multipartFile = Mock(MultipartFile)
			multipartFile.getOriginalFilename() >> "my File.jpg"
		and: "a command object"
			def myConfig = []
			def uploadCommand = new UploadFileCommand(
				idparent:		"myId",
				file:			multipartFile,
				gridfsService:	gridfsService,
				config:			myConfig
			)
		and: "a gridFS file"
			def gfsFile = Mock(GridFSFile)
		and: "an expected result following upload"
			def expectedUploadResult = [isAllowed: true, msg: null]
			
		when: "adding the file to gridFS"
			def result = uploadCommand.createTargetFile()
		
		then: "the check is correctly delegated"
			1 * gridfsService.addToGridFS(myConfig, multipartFile, "myid_myfile.jpg") >> gfsFile
		and: "the file is configured with metadata"
			1 * gfsFile.setMetaData(uploadCommand.metadata)
		and: "an attempt is made to upload the file"
			1 * gridfsService.attemptUpload(myConfig, gfsFile) >> expectedUploadResult
		and: "the correct result is returned"
			result == expectedUploadResult
			
	}
	
	@Unroll
	def "the command object should fallback to the config object conrrectly for '#field'='#value'"() {
		
		given: "a config object containing default values"
			def defaultConfig = [
				allowedExtensions:	["jpg"],
				db:					[host: "mongoHost"],
				controllers:		[
					successController:	"configSuccess",
					successAction:		"configSuccessAction",
					successType:		"chain",
					errorController:	"configError",
					errorAction:		"configErrorAction",
					errorType:			"redirect",
				]
			]
			
		when: "a command object containing override values is created"
			def uploadCommand = new UploadFileCommand(config: defaultConfig)
			uploadCommand."${field}" = value
		
		then: "the command object uses the default value from config when no value is found"
			uploadCommand."${field}" == expectedValue
			
		where:
			field				| value			| expectedValue
			"successController"	| "mySuccess"	| "mySuccess"
			"successController"	| null			| "configSuccess"
			"successController"	| ""			| "configSuccess"
			"successController"	| " "			| "configSuccess"
			
			"successAction"		| "success"		| "success"
			"successAction"		| null			| "configSuccessAction"
			"successAction"		| ""			| "configSuccessAction"
			"successAction"		| " "			| "configSuccessAction"
			
			"successType"		| "forward"		| "forward"
			"successType"		| null			| "chain"
			"successType"		| ""			| "chain"
			"successType"		| " "			| "chain"
			
			"errorController"	| "myError"		| "myError"
			"errorController"	| null			| "configError"
			"errorController"	| ""			| "configError"
			"errorController"	| " "			| "configError"
			
			"errorAction"		| "error"		| "error"
			"errorAction"		| null			| "configErrorAction"
			"errorAction"		| ""			| "configErrorAction"
			"errorAction"		| " "			| "configErrorAction"
			
			"errorType"			| "forward"		| "forward"
			"errorType"			| null			| "redirect"
			"errorType"			| ""			| "redirect"
			"errorType"			| " "			| "redirect"

	}

}
