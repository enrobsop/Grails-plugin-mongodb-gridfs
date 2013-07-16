package org.iglas.grails.gridfs

import grails.plugin.spock.UnitSpec
import grails.test.mixin.*
import grails.test.mixin.web.ControllerUnitTestMixin

import org.bson.types.ObjectId
import org.iglas.grails.utils.ConfigHelper
import org.springframework.web.multipart.MultipartFile

import spock.lang.Shared
import spock.lang.Unroll

import com.mongodb.gridfs.GridFSDBFile
import com.mongodb.gridfs.GridFSFile

@TestFor(GridfsController)
@TestMixin(ControllerUnitTestMixin)
class GridfsControllerSpec extends UnitSpec {

	@Shared gridfsService

	def setup() {
		gridfsService = Mock(GridfsService)
		controller.gridfsService = gridfsService
		configureWith()
	}
	
	def "upload should fail when idparent is missing"() {
		given: "a command object without idparent" 
			def ufc = mockCommandObject(UploadFileCommand)
		
		when: "attempt upload"
			controller.upload(ufc)
			
		then: "an error message is given"
			flash.message != null
			flash.message =~ ~/(?i).*invalid.*/
		and: "the client is redirected to the error controller"
			response.redirectUrl == "/error/index"
	}
	
	def "upload should fail when file is missing"() {
		
		given: "a command object without a file"
			def theId	= "anId"
			def uploadCommand = new UploadFileCommand(
				idparent: theId
			)
			
		when: "upload is attempted"
			controller.upload(uploadCommand)
			
		then: "an error message is given"
			flash.message != null
			flash.message =~ ~/(?i).*no file.*/
		and: "the client is redirected to the error controller"
			response.redirectUrl == "/error/index/$theId"

	}
	
	def "upload should fail when file is empty"() {
		
		given: "an empty file"
			def multipartFile = Mock(MultipartFile)
			multipartFile.getOriginalFilename() >> "empty.jpg"
			multipartFile.getSize() >> 0
		and: "a command object"
			def theId	= "anId"
			def uploadCommand = new UploadFileCommand(
				idparent: 			theId,
				file:				multipartFile
			)
			
		when: "upload is attempted"
			controller.upload(uploadCommand)
			
		then: "an error message is given"
			flash.message != null
			flash.message =~ ~/(?i).*no file.*/
		and: "the client is redirected to the error controller"
			response.redirectUrl == "/error/index/$theId"
			// compare with redirectUrl for invalid file extension... should they use the same ID?			
	}
	
	def "upload should fail when file extension is not on allowed list"() {

		given: "an uploaded file with a disallowed extension"
			def disallowedExtension	= "bad"
			def multipartFile = Mock(MultipartFile)
			multipartFile.getOriginalFilename() >> "aFile.$disallowedExtension"
			multipartFile.getSize() >> 123
		and: "a command object"
			def theId	= "anId"
			def paramId = "123"
			def uploadCommand = new UploadFileCommand(
				id:					paramId,
				idparent: 			theId,
				file:				multipartFile
			)

		when: "upload is attempted"
			controller.upload(uploadCommand)
			
		then: "an error message is given"
			flash.message != null
			flash.message =~ ~/(?i).*unauthorized extension.*${disallowedExtension}.*/
			flash.message =~ ~/(?i).*allowed extensions.*jpg.*/
		and: "the client is redirected to the error controller"
			response.redirectUrl == "/error/index/$paramId"
			// compare with redirectUrl for missing file... should they use the same ID?
	}
	
	def "upload should fail when the file size exceeds the max size"() {
		given: "a file size limit"
			def KB 				= 1024
			def theSizeLimit	= 123 * KB 
			def tooBig			= theSizeLimit + 1
			configureWith([maxSize: theSizeLimit])
		and: "an uploaded file that is too big"
			def multipartFile = Mock(MultipartFile)
			multipartFile.getOriginalFilename() >> "aFile.jpg"
			multipartFile.getSize() >> tooBig
		and: "a command object"
			def theId	= "anId"
			def paramId = "123"
			def uploadCommand = new UploadFileCommand(
				id:					paramId,
				idparent: 			theId,
				file:				multipartFile
			)

		when: "upload is attempted"
			controller.upload(uploadCommand)
			
		then: "an error message is given"
			flash.message != null
			flash.message =~ ~/(?i).*file size*/
			flash.message =~ ~/(?i).*${theSizeLimit / KB}.*/
		and: "the client is redirected to the error controller"
			response.redirectUrl == "/error/index/$paramId"
			// compare with redirectUrl for missing file... should they use the same ID?
	}

	@Unroll	
	def "file upload should not check the maxSize when the limit is #theMaxSize"() {
		given: "a file size limit"
			configureWith([maxSize: theMaxSize])
		and: "an uploaded file"
			def multipartFile = Mock(MultipartFile)
			multipartFile.getOriginalFilename() >> "aFile.jpg"
			multipartFile.getSize() >> 1024000
		and: "a command object"
			def theId = "anId"
			def paramId = "123"
			def uploadCommand = new UploadFileCommand(
				gridfsService:		gridfsService,
				id:					paramId,
				idparent: 			theId,
				file:				multipartFile
			)
		and: "file is added okay"
			def theGridFile = Mock(GridFSFile)
			gridfsService.addToGridFS(_,_,_) >> theGridFile
		and: "upload access is given"
			gridfsService.attemptUpload(_, _) >> [isAllowed: true, msg: null]

		when: "upload is attempted"
			controller.upload(uploadCommand)
			
		then: "no error message is given"
			flash.message == null
		and: "the client is redirected to the success controller"
			response.redirectUrl == "/home/index"
			
		where:
			theMaxSize << [0, -1]
	}

	@Unroll
	def "controller should send correct response when successType is '#successType'"() {
		
		given: "an uploaded file"
			def theFileExtension	= "jpg"
			def theOriginalFilename	= "myImage.${theFileExtension}"
			def multipartFile = Mock(MultipartFile)
			multipartFile.getOriginalFilename() >> theOriginalFilename
			multipartFile.getSize() >> 1024000
		and: "a command object"
			def uploadCommand = new UploadFileCommand(
				gridfsService:		gridfsService,
				idparent: 			"testUser",
				file:				multipartFile,
				successController: 	"success",
				successAction:		"onupload",
				successType: 		successType
			)
		
		and: "file is added okay"
			def theGridFile = Mock(GridFSFile)
			theGridFile.getId() >> new ObjectId("51d319ca0364087b266e6f19")
			gridfsService.addToGridFS(_,_,_) >> theGridFile
		and: "upload access is given"
			gridfsService.attemptUpload(_, _) >> [isAllowed: true, msg: null]
		
		when: "uploaded successfully"
			controller.upload(uploadCommand)
		
		then: "there are no error messages"
			flash.message == null
		and: "the request should be forwarded/redirected to the correct controller"
			response.redirectUrl	== redirectUrl
			response.forwardedUrl	== forwardedUrl
			
		where:
			successType	| redirectUrl			| forwardedUrl
			null		| "/success/onupload"	| null
			"redirect"	| "/success/onupload"	| null
			"chain"		| "/success/onupload"	| null
			"forward"	| null					| "/grails/success/onupload.dispatch?fileId=51d319ca0364087b266e6f19"
		
	}
	
	def "error messages are given when a required parameter is missing"() {
		
		given: "a request with missing property"
			params.filename	= null
		and: "an id"
			params.id = "123"
		
		when: "get file is invoked"
			controller.get()
		
		then: "a flash messsage is created"
			flash.message =~ /(?i).*filename.*null.*/
		and: "is redirected to the error controller"
			response.redirectedUrl == "/error/index/123"
			
	}
	
	def "can get a valid file when no access control is provided"() {
		
		given: "the correct parameters"
			def theFilename = "myFile.txt"
			params.filename = theFilename
		and: "a file"
			def theFile = Mock(GridFSDBFile)
			theFile.getContentType() >> "image/jpeg"
			theFile.getInputStream() >> new ByteArrayInputStream("the content...".bytes) 
		
		when: "get file is invoked"
			controller.get()
		
		then: "calls the service correctly"
			1 * gridfsService.getByFilename(theFilename) >> theFile
		and: "returns the correct content"
			response.status 			== 200
			response.contentType		== "image/jpeg"
			response.contentAsString	== "the content..."
			
	}
	
	def "getting a file behaves correctly when access is denied"() {

		given: "a non-existent filename"
			params.filename = "iDoNotExist.txt"
		and: "an id"
			params.id = 123
			
		when: "get file is invoked"
			controller.get()
			
		then: "calls the service correctly"
			1 * gridfsService.getByFilename("iDoNotExist.txt") >> null
		and: "a flash messsage is created"
			flash.message != null
			flash.message =~ /(?i).*file.*not.*found.*/
		and: "the client is redirected to the error controller"
			response.redirectedUrl == "/error/index/123"
					
	}
	
	def "getting a file behaves correctly when access is permitted"() {
	
		given: "someone else's file"
			params.filename = "notMy.jpg"
			def theFile = Mock(GridFSDBFile)
		and: "an id"
			params.id = 123
		and: "a custom access class"
			configureWith([accessClass: "org.iglas.grails.gridfs.PreventAccess", accessMethod: "permits"])
			
		when: "get file is invoked"
			
			controller.get()
			
		then: "the service is called"
			1 * gridfsService.getByFilename("notMy.jpg") >> theFile
		and: "a flash messsage is created"
			flash.message != null
			flash.message =~ /(?i).*access.*(deny|denied).*/
		and: "the client is redirected to the error controller"
			response.redirectedUrl == "/access/denied/123"

	}

	def "getting a file behaves correctly when the file can be accessed"() {
		
		given: "my file"
			params.filename = "my.jpg"
			def theFile = Mock(GridFSDBFile)
			theFile.getContentType() >> "image/jpeg"
			theFile.getInputStream() >> new ByteArrayInputStream("the content...".bytes)
		and: "an id"
			params.id = 123
		and: "a custom access class"
			configureWith([accessClass: "org.iglas.grails.gridfs.AllowAccess", accessMethod: "permits"])

		when: "get file is invoked"
			controller.get()
			
		then: "calls the service correctly"
			1 * gridfsService.getByFilename("my.jpg") >> theFile
		and: "returns the correct content"
			assert response.status 		== 200 
			response.contentType		== "image/jpeg"
			response.contentAsString	== "the content..."
			
	}
	
	
	private def configureWith(options=[:]) {
		def defaultConfig = [
			allowedExtensions:	["jpg"],
			db:					[host: "mongoHost"],
			controllers:		[
				successController:	"home",
				successAction:		"index",
				errorController:	"error",
				errorAction:		"index"
			],
			accessController:	"access",
			accessAction:		"denied"
		]
		def config = defaultConfig << options 
		
		def configHelper = Mock(ConfigHelper)
		configHelper.getConfig(_) >> config
		controller.configHelper = configHelper
	}
	
}

class PreventAccess {
	boolean permits(file, action) {
		false
	}
}

class AllowAccess {
	boolean permits(file, action) {
		true
	}
}
