package org.iglas.grails.gridfs

import grails.plugin.spock.UnitSpec
import grails.test.mixin.*
import grails.test.mixin.web.ControllerUnitTestMixin

import org.iglas.grails.utils.ConfigHelper
import org.springframework.web.multipart.MultipartFile

import spock.lang.Shared
import spock.lang.Unroll

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
			def theGridFile = []
			gridfsService.addToGridFS(_,_,_,_) >> theGridFile
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

	def "upload should forward to the successController when successType is 'forward'"() {
		
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
				successController: 	"mySuccessController",
				successAction:		"theAction",
				successType: 		"forward"
			)
		and: "a configuration"
			configureWith([
				controllers:		[
					successController:	"home",
					successAction:		"afterUpload",
					successType:		"forward"
				]
			])
		
		and: "file is added okay"
			def theGridFile = []
			gridfsService.addToGridFS(_,_,_,_) >> theGridFile
		and: "upload access is given"
			gridfsService.attemptUpload(_, _) >> [isAllowed: true, msg: null]
		
		when: "uploaded successfully"
			controller.upload(uploadCommand)
		
		then: "there are no error messages"
			flash.message == null
		and: "the request should be forwarded to the successController"
			assert response.forwardedUrl == "/grails/home/afterUpload.dispatch"
		
	}
	
	private def configureWith(options=[:]) {
		def defaultConfig = [
			allowedExtensions:	["jpg"],
			db:					[host: "mongoHost"],
			controllers:		[
				successController:	"home",
				successAction:		"index",
				errorController:	"error",
				errorAction:		"index",
			]
		]
		def config = defaultConfig << options 
		
		def configHelper = Mock(ConfigHelper)
		configHelper.getConfig(_) >> config
		controller.configHelper = configHelper
	}
	
}
